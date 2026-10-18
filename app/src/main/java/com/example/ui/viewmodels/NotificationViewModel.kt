package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseResult
import com.example.core.realtime.SupabaseRealtimeManager
import com.example.data.models.Notification
import com.example.data.repository.NotificationRepository
import com.example.data.repository.NotificationRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class NotificationUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null,
    val isConnected: Boolean = true
) {
    val todayNotifications: List<Notification>
        get() = notifications.filter { isToday(it.createdAt) }

    val yesterdayNotifications: List<Notification>
        get() = notifications.filter { isYesterday(it.createdAt) }

    val earlierNotifications: List<Notification>
        get() = notifications.filter { !isToday(it.createdAt) && !isYesterday(it.createdAt) }

    private fun isToday(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val parsed = parseDate(dateStr) ?: return false
        val now = System.currentTimeMillis()
        val diff = now - parsed.time
        return diff in 0..(24 * 60 * 60 * 1000L) && isSameDay(parsed, Date(now))
    }

    private fun isYesterday(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val parsed = parseDate(dateStr) ?: return false
        val now = System.currentTimeMillis()
        val yesterday = Date(now - 24 * 60 * 60 * 1000L)
        return isSameDay(parsed, yesterday)
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return fmt.format(d1) == fmt.format(d2)
    }

    private fun parseDate(dateStr: String): Date? {
        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateStr)
            } catch (_: Exception) {}
        }
        return null
    }
}

class NotificationViewModel(
    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl(),
    private val realtimeManager: SupabaseRealtimeManager = SupabaseRealtimeManager(SupabaseClient.okHttpClient)
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var realtimeJob: Job? = null

    init {
        realtimeManager.connect()
        realtimeManager.subscribeToNotifications()
        observeRealtime()
    }

    private fun observeRealtime() {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            realtimeManager.notificationsEvents.collect { event ->
                val uid = currentUserId ?: return@collect
                // Check if notification belongs to current user
                val record = event.record
                val targetUser = record?.optString("user_id")
                if (targetUser.isNullOrBlank() || targetUser == uid) {
                    // Refresh notifications to sync exact state
                    loadNotifications(showLoading = false)
                }
            }
        }
    }

    fun onUserSessionChanged(userId: String?) {
        if (currentUserId != userId) {
            currentUserId = userId
            if (userId.isNullOrBlank()) {
                // Clear state on logout
                _uiState.value = NotificationUiState()
            } else {
                loadNotifications(showLoading = true)
            }
        }
    }

    fun loadNotifications(showLoading: Boolean = false) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }
            when (val res = notificationRepository.getNotifications(uid)) {
                is SupabaseResult.Success -> {
                    // Deduplicate by notification id
                    val distinct = res.data.distinctBy { it.id }
                    val unread = distinct.count { !it.isRead }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        notifications = distinct,
                        unreadCount = unread,
                        errorMessage = null
                    )
                }
                is SupabaseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = res.userFriendlyMessage
                    )
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    fun refresh() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            when (val res = notificationRepository.getNotifications(uid)) {
                is SupabaseResult.Success -> {
                    val distinct = res.data.distinctBy { it.id }
                    val unread = distinct.count { !it.isRead }
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        notifications = distinct,
                        unreadCount = unread,
                        errorMessage = null
                    )
                }
                is SupabaseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        errorMessage = res.userFriendlyMessage
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            // Optimistic update
            val currentList = _uiState.value.notifications
            val updatedList = currentList.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            _uiState.value = _uiState.value.copy(
                notifications = updatedList,
                unreadCount = updatedList.count { !it.isRead }
            )

            // Persist to backend
            when (val res = notificationRepository.markAsRead(notificationId)) {
                is SupabaseResult.Success -> {}
                is SupabaseResult.Error -> {
                    // Rollback if failed
                    _uiState.value = _uiState.value.copy(
                        notifications = currentList,
                        unreadCount = currentList.count { !it.isRead },
                        errorMessage = res.userFriendlyMessage
                    )
                }
                else -> {}
            }
        }
    }

    fun markAllAsRead() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val currentList = _uiState.value.notifications
            val updatedList = currentList.map { it.copy(isRead = true) }
            _uiState.value = _uiState.value.copy(
                notifications = updatedList,
                unreadCount = 0
            )

            when (val res = notificationRepository.markAllAsRead(uid)) {
                is SupabaseResult.Success -> {}
                is SupabaseResult.Error -> {
                    // Rollback
                    _uiState.value = _uiState.value.copy(
                        notifications = currentList,
                        unreadCount = currentList.count { !it.isRead },
                        errorMessage = res.userFriendlyMessage
                    )
                }
                else -> {}
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val currentList = _uiState.value.notifications
            val updatedList = currentList.filterNot { it.id == notificationId }
            _uiState.value = _uiState.value.copy(
                notifications = updatedList,
                unreadCount = updatedList.count { !it.isRead }
            )

            when (val res = notificationRepository.deleteNotification(notificationId)) {
                is SupabaseResult.Success -> {}
                is SupabaseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        notifications = currentList,
                        unreadCount = currentList.count { !it.isRead },
                        errorMessage = res.userFriendlyMessage
                    )
                }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
    }
}
