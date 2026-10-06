package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.Conversation
import com.example.data.models.Message
import java.util.UUID

interface ChatRepository {
    suspend fun getConversations(userId: String): SupabaseResult<List<Conversation>>
    suspend fun getOrCreateConversation(currentUserId: String, otherUserId: String, listingId: String? = null): SupabaseResult<Conversation>
    suspend fun getMessages(conversationId: String): SupabaseResult<List<Message>>
    suspend fun sendMessage(conversationId: String, senderId: String, receiverId: String, content: String): SupabaseResult<Message>
    suspend fun markMessageAsRead(messageId: String): SupabaseResult<Unit>
}

class ChatRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : ChatRepository {

    override suspend fun getConversations(userId: String): SupabaseResult<List<Conversation>> {
        return try {
            val filters = mapOf("or" to "(buyer_id.eq.$userId,seller_id.eq.$userId)")
            val response = restApi.getConversations(filters = filters)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getOrCreateConversation(
        currentUserId: String,
        otherUserId: String,
        listingId: String?
    ): SupabaseResult<Conversation> {
        // Safety: Do not open a conversation with oneself
        if (currentUserId == otherUserId) {
            return SupabaseResult.Error("Cannot start a conversation with yourself.")
        }
        return try {
            // First check if an existing conversation between these users exists
            val filters = mutableMapOf<String, String>()
            filters["or"] = "(and(buyer_id.eq.$currentUserId,seller_id.eq.$otherUserId),and(buyer_id.eq.$otherUserId,seller_id.eq.$currentUserId))"
            if (!listingId.isNullOrBlank()) {
                filters["listing_id"] = "eq.$listingId"
            }

            val existing = restApi.getConversations(filters = filters)
            if (existing.isSuccessful && !existing.body().isNullOrEmpty()) {
                SupabaseResult.Success(existing.body()!!.first())
            } else {
                // Create a new conversation
                val newConv = Conversation(
                    id = UUID.randomUUID().toString(),
                    buyerId = currentUserId,
                    sellerId = otherUserId,
                    listingId = listingId
                )
                val response = restApi.createConversation(newConv)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SupabaseResult.Success(response.body()!!.first())
                } else {
                    SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
                }
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getMessages(conversationId: String): SupabaseResult<List<Message>> {
        return try {
            val response = restApi.getMessages("eq.$conversationId")
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: String
    ): SupabaseResult<Message> {
        return try {
            val msg = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                message = content.trim(),
                isRead = false
            )
            val response = restApi.sendMessage(msg)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val sent = response.body()!!.first()
                // Update conversation's updated_at timestamp safely
                runCatching {
                    restApi.updateConversation(
                        idFilter = "eq.$conversationId",
                        updates = mapOf(
                            "updated_at" to "now()"
                        )
                    )
                }
                SupabaseResult.Success(sent)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun markMessageAsRead(messageId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.markMessageAsRead("eq.$messageId", mapOf("is_read" to true))
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }
}
