package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.Notification

interface NotificationRepository {
    suspend fun getNotifications(userId: String): SupabaseResult<List<Notification>>
    suspend fun markAsRead(notificationId: String): SupabaseResult<Unit>
    suspend fun getUnreadCount(userId: String): SupabaseResult<Int>
    suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String = "ORDER"
    ): SupabaseResult<Notification>
}

class NotificationRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : NotificationRepository {

    override suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String
    ): SupabaseResult<Notification> {
        return try {
            val notification = Notification(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                message = message,
                type = type
            )
            val response = restApi.createNotification(notification)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getNotifications(userId: String): SupabaseResult<List<Notification>> {
        return try {
            val response = restApi.getNotifications("eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun markAsRead(notificationId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.updateNotification("eq.$notificationId", mapOf("is_read" to true))
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getUnreadCount(userId: String): SupabaseResult<Int> {
        return when (val notifs = getNotifications(userId)) {
            is SupabaseResult.Success -> {
                val count = notifs.data.count { !it.isRead }
                SupabaseResult.Success(count)
            }
            is SupabaseResult.Error -> notifs
            is SupabaseResult.Loading -> SupabaseResult.Success(0)
        }
    }
}
