package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.CreateNotificationDto
import com.example.data.models.Notification
import com.example.data.models.NotificationTypes
import java.util.UUID

interface NotificationRepository {
    suspend fun getNotifications(userId: String): SupabaseResult<List<Notification>>
    suspend fun markAsRead(notificationId: String): SupabaseResult<Unit>
    suspend fun markAllAsRead(userId: String): SupabaseResult<Unit>
    suspend fun deleteNotification(notificationId: String): SupabaseResult<Unit>
    suspend fun getUnreadCount(userId: String): SupabaseResult<Int>
    suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String = NotificationTypes.SYSTEM,
        dataPayload: Map<String, Any?>? = null
    ): SupabaseResult<Notification>
}

class NotificationRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : NotificationRepository {

    override suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        dataPayload: Map<String, Any?>?
    ): SupabaseResult<Notification> {
        return try {
            val dto = CreateNotificationDto(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                message = message,
                type = type,
                isRead = false
            )
            val response = restApi.createNotification(dto)
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
            val response = restApi.getNotifications(userIdFilter = "eq.$userId")
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
            val response = restApi.updateNotification(
                idFilter = "eq.$notificationId",
                updates = mapOf("is_read" to true)
            )
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun markAllAsRead(userId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.updateNotifications(
                userIdFilter = "eq.$userId",
                updates = mapOf("is_read" to true)
            )
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun deleteNotification(notificationId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.deleteNotification(idFilter = "eq.$notificationId")
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
