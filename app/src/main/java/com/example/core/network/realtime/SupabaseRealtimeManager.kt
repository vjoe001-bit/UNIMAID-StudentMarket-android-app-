package com.example.core.network.realtime

import android.util.Log
import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseConfig
import com.example.data.models.Message
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Realtime Event emitted by Supabase Realtime WebSocket for UNIMAID StudentMarket.
 */
sealed class RealtimeEvent {
    data class Connected(val topic: String) : RealtimeEvent()
    data class Disconnected(val reason: String?) : RealtimeEvent()
    data class NewMessage(val message: Message) : RealtimeEvent()
    data class OrderUpdated(val orderId: String, val status: String) : RealtimeEvent()
    data class Error(val error: Throwable) : RealtimeEvent()
}

/**
 * Robust Supabase Realtime Client managing Phoenix Channel WebSocket connections.
 * Handles heartbeats (30s), channel join, automated backoff reconnection, and real-time message streaming.
 */
class SupabaseRealtimeManager(
    private val okHttpClient: OkHttpClient = SupabaseClient.okHttpClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) {
    private val tag = "SupabaseRealtime"
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private val refCounter = AtomicInteger(1)
    private var isConnected = false
    private var isIntentionalClose = false

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val messageAdapter = moshi.adapter(Message::class.java)

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    fun connect() {
        if (isConnected) return
        isIntentionalClose = false

        val wsUrl = SupabaseConfig.realtimeWebSocketUrl
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(tag, "Connected to Supabase Realtime WebSocket")
                isConnected = true
                scope.launch {
                    _events.emit(RealtimeEvent.Connected("realtime:all"))
                }
                startHeartbeat()
                joinTopic(SupabaseConfig.RealtimeTopics.MESSAGES)
                joinTopic(SupabaseConfig.RealtimeTopics.ORDERS)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Closing Realtime WebSocket: $code $reason")
                isConnected = false
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Closed Realtime WebSocket: $code $reason")
                isConnected = false
                heartbeatJob?.cancel()
                if (!isIntentionalClose) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "Realtime WebSocket Failure: ${t.message}", t)
                isConnected = false
                heartbeatJob?.cancel()
                scope.launch {
                    _events.emit(RealtimeEvent.Error(t))
                }
                if (!isIntentionalClose) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(30_000)
                val ref = refCounter.incrementAndGet().toString()
                val heartbeatMsg = JSONObject().apply {
                    put("topic", "phoenix")
                    put("event", "heartbeat")
                    put("payload", JSONObject())
                    put("ref", ref)
                }
                webSocket?.send(heartbeatMsg.toString())
            }
        }
    }

    private fun joinTopic(topic: String) {
        val ref = refCounter.incrementAndGet().toString()
        val joinMsg = JSONObject().apply {
            put("topic", topic)
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                put("config", JSONObject().apply {
                    put("broadcast", JSONObject().apply { put("self", false) })
                    put("presence", JSONObject().apply { put("key", "") })
                    put("postgres_changes", JSONObject().apply {
                        put("event", "*")
                        put("schema", "public")
                    })
                })
            })
            put("ref", ref)
        }
        webSocket?.send(joinMsg.toString())
    }

    private fun handleIncomingMessage(rawText: String) {
        try {
            val json = JSONObject(rawText)
            val event = json.optString("event")
            val payload = json.optJSONObject("payload") ?: return

            when (event) {
                "INSERT", "broadcast", "new_message" -> {
                    val record = payload.optJSONObject("record") ?: payload
                    val messageJson = record.toString()
                    val parsedMsg = messageAdapter.fromJson(messageJson)
                    if (parsedMsg != null) {
                        scope.launch {
                            _events.emit(RealtimeEvent.NewMessage(parsedMsg))
                        }
                    }
                }
                "UPDATE" -> {
                    val table = payload.optString("table")
                    if (table == "orders") {
                        val record = payload.optJSONObject("record")
                        val orderId = record?.optString("id").orEmpty()
                        val status = record?.optString("status").orEmpty()
                        if (orderId.isNotBlank()) {
                            scope.launch {
                                _events.emit(RealtimeEvent.OrderUpdated(orderId, status))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error parsing realtime message: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5_000)
            if (!isConnected && !isIntentionalClose) {
                Log.d(tag, "Reconnecting to Supabase Realtime...")
                connect()
            }
        }
    }

    fun disconnect() {
        isIntentionalClose = true
        isConnected = false
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }
}
