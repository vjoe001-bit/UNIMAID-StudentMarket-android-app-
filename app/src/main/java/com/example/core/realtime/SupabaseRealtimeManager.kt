package com.example.core.realtime

import android.util.Log
import com.example.core.network.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

sealed class RealtimeConnectionState {
    object Disconnected : RealtimeConnectionState()
    object Connecting : RealtimeConnectionState()
    object Connected : RealtimeConnectionState()
    data class Error(val message: String, val throwable: Throwable? = null) : RealtimeConnectionState()
}

/**
 * Event representing an incoming real-time change from Supabase.
 */
data class RealtimeEvent(
    val topic: String,
    val event: String,
    val table: String?,
    val eventType: String?, // INSERT, UPDATE, DELETE
    val payload: JSONObject
)

/**
 * Realtime subscription manager for UNIMAID StudentMarket.
 * Supports live messaging, notifications, orders, and conversations.
 */
class SupabaseRealtimeManager(
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val tag = "SupabaseRealtime"
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private val refCounter = AtomicInteger(1)

    private val _connectionState = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Disconnected)
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    val messagesEvents: Flow<RealtimeEvent> = _events.filter { it.table == "messages" }
    val notificationsEvents: Flow<RealtimeEvent> = _events.filter { it.table == "notifications" }
    val ordersEvents: Flow<RealtimeEvent> = _events.filter { it.table == "orders" }
    val conversationsEvents: Flow<RealtimeEvent> = _events.filter { it.table == "conversations" }
    val listingsEvents: Flow<RealtimeEvent> = _events.filter { it.table == "listings" }

    private val subscribedTopics = mutableSetOf<String>()

    fun connect() {
        if (webSocket != null) return

        _connectionState.value = RealtimeConnectionState.Connecting

        val request = Request.Builder()
            .url(SupabaseConfig.realtimeWebSocketUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(tag, "Supabase Realtime WebSocket connected")
                _connectionState.value = RealtimeConnectionState.Connected
                startHeartbeat()
                // Re-join existing topics if reconnecting
                subscribedTopics.forEach { topic ->
                    sendJoinTopic(ws, topic)
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Supabase Realtime closing: code=$code, reason=$reason")
                stopHeartbeat()
                _connectionState.value = RealtimeConnectionState.Disconnected
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "Supabase Realtime failure: ${t.message}")
                stopHeartbeat()
                webSocket = null
                _connectionState.value = RealtimeConnectionState.Error(t.message ?: "Realtime connection failure", t)
            }
        })
    }

    fun subscribeToChannel(topic: String) {
        subscribedTopics.add(topic)
        webSocket?.let { ws ->
            sendJoinTopic(ws, topic)
        }
    }

    fun subscribeToMessages() = subscribeToChannel(SupabaseConfig.RealtimeTopics.MESSAGES)
    fun subscribeToNotifications() = subscribeToChannel(SupabaseConfig.RealtimeTopics.NOTIFICATIONS)
    fun subscribeToOrders() = subscribeToChannel(SupabaseConfig.RealtimeTopics.ORDERS)
    fun subscribeToConversations() = subscribeToChannel(SupabaseConfig.RealtimeTopics.CONVERSATIONS)
    fun subscribeToListings() = subscribeToChannel(SupabaseConfig.RealtimeTopics.LISTINGS)

    private fun sendJoinTopic(ws: WebSocket, topic: String) {
        val ref = refCounter.incrementAndGet().toString()
        val joinPayload = JSONObject().apply {
            put("topic", topic)
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                put("config", JSONObject().apply {
                    put("broadcast", JSONObject().apply { put("ack", false); put("self", false) })
                    put("presence", JSONObject().apply { put("key", "") })
                    put("postgres_changes", org.json.JSONArray())
                })
            })
            put("ref", ref)
        }
        ws.send(joinPayload.toString())
        Log.d(tag, "Joined topic: $topic")
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30_000)
                val ref = refCounter.incrementAndGet().toString()
                val heartbeat = JSONObject().apply {
                    put("topic", "phoenix")
                    put("event", "heartbeat")
                    put("payload", JSONObject())
                    put("ref", ref)
                }
                webSocket?.send(heartbeat.toString())
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun handleIncomingMessage(text: String) {
        runCatching {
            val json = JSONObject(text)
            val topic = json.optString("topic")
            val event = json.optString("event")
            val payload = json.optJSONObject("payload") ?: JSONObject()

            val table = payload.optJSONObject("data")?.optString("table")
            val type = payload.optJSONObject("data")?.optString("type")

            val realtimeEvent = RealtimeEvent(
                topic = topic,
                event = event,
                table = table,
                eventType = type,
                payload = payload
            )
            _events.tryEmit(realtimeEvent)
        }.onFailure {
            Log.w(tag, "Error parsing incoming realtime message: ${it.message}")
        }
    }

    fun disconnect() {
        stopHeartbeat()
        webSocket?.close(1000, "App closed")
        webSocket = null
        subscribedTopics.clear()
        _connectionState.value = RealtimeConnectionState.Disconnected
    }
}
