package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Conversation(
    val id: String,
    @Json(name = "buyer_id")
    val buyerId: String? = null,
    @Json(name = "seller_id")
    val sellerId: String? = null,
    @Json(name = "listing_id")
    val listingId: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null,

    // Nested relations loaded via PostgREST joins
    val buyer: Profile? = null,
    val seller: Profile? = null,
    val listing: Listing? = null,

    // In-memory / transient properties for UI ease
    @Transient
    val lastMessageText: String? = null,
    @Transient
    val otherParticipant: Profile? = null
) {
    val participant1Id: String
        get() = buyerId ?: ""

    val participant2Id: String
        get() = sellerId ?: ""

    fun getCounterpartyId(currentUserId: String): String {
        return if (currentUserId == buyerId) {
            sellerId ?: ""
        } else {
            buyerId ?: ""
        }
    }

    fun getCounterpartyProfile(currentUserId: String): Profile? {
        return if (currentUserId == buyerId) {
            seller ?: otherParticipant
        } else {
            buyer ?: otherParticipant
        }
    }
}

@JsonClass(generateAdapter = true)
data class Message(
    val id: String,
    @Json(name = "conversation_id")
    val conversationId: String,
    @Json(name = "sender_id")
    val senderId: String,
    val message: String? = null,
    @Json(name = "is_read")
    val isRead: Boolean = false,
    @Json(name = "created_at")
    val createdAt: String? = null,

    // Joined relation
    val sender: Profile? = null,

    // Fallback/compat property
    @Transient
    val receiverId: String? = null,
    @Transient
    val content: String? = null
) {
    val displayContent: String
        get() = message ?: content ?: ""
}

