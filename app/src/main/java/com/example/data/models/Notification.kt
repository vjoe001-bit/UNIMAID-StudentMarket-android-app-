package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Real Supabase Notification Model for UNIMAID StudentMarket.
 * Supports:
 * - Direct mapping to public.notifications table (user_id, title, message/body, type, is_read, data_payload, created_at)
 * - User-specific notification entities (MESSAGE, ORDER, LISTING, VERIFICATION, SYSTEM, SECURITY)
 * - Deep navigation metadata via data_payload
 */
@JsonClass(generateAdapter = true)
data class Notification(
    val id: String,
    @Json(name = "user_id")
    val userId: String,
    val title: String,
    @Json(name = "message")
    val message: String = "",
    @Json(name = "body")
    val body: String? = null,
    val type: String = "SYSTEM", // MESSAGE, PURCHASE_REQUEST, PURCHASE_ACCEPTED, PURCHASE_DECLINED, TRANSACTION_UPDATE, HANDOVER_UPDATE, TRANSACTION_COMPLETED, LISTING_UPDATE, VERIFICATION, SYSTEM, SECURITY
    @Json(name = "is_read")
    val isRead: Boolean = false,
    @Json(name = "data_payload")
    val dataPayload: Map<String, Any?>? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
) {
    /**
     * Resilient display text across either 'message' or 'body' column naming.
     */
    val displayBody: String
        get() = message.ifBlank { body.orEmpty() }

    /**
     * Navigation helper extracting entity ID from dataPayload or fallback keys.
     */
    val entityId: String?
        get() {
            if (dataPayload == null) return null
            return (dataPayload["entity_id"] as? String)
                ?: (dataPayload["order_id"] as? String)
                ?: (dataPayload["conversation_id"] as? String)
                ?: (dataPayload["listing_id"] as? String)
                ?: (dataPayload["sender_id"] as? String)
        }

    val conversationId: String?
        get() = (dataPayload?.get("conversation_id") as? String) ?: entityId

    val orderId: String?
        get() = (dataPayload?.get("order_id") as? String) ?: entityId

    val listingId: String?
        get() = (dataPayload?.get("listing_id") as? String) ?: entityId

    val senderId: String?
        get() = (dataPayload?.get("sender_id") as? String)

    val orderTab: String?
        get() = (dataPayload?.get("tab") as? String) ?: when (type.uppercase()) {
            "PURCHASE_REQUEST" -> "sales" // Seller receives purchase request
            "PURCHASE_ACCEPTED", "PURCHASE_DECLINED", "TRANSACTION_UPDATE", "HANDOVER_UPDATE", "TRANSACTION_COMPLETED" -> "purchases"
            else -> null
        }
}

/**
 * Standard notification category types used throughout the UNIMAID ecosystem.
 */
object NotificationTypes {
    const val MESSAGE = "MESSAGE"
    const val PURCHASE_REQUEST = "PURCHASE_REQUEST"
    const val PURCHASE_ACCEPTED = "PURCHASE_ACCEPTED"
    const val PURCHASE_DECLINED = "PURCHASE_DECLINED"
    const val TRANSACTION_UPDATE = "TRANSACTION_UPDATE"
    const val HANDOVER_UPDATE = "HANDOVER_UPDATE"
    const val TRANSACTION_COMPLETED = "TRANSACTION_COMPLETED"
    const val LISTING_UPDATE = "LISTING_UPDATE"
    const val VERIFICATION = "VERIFICATION"
    const val REVIEW_RECEIVED = "REVIEW_RECEIVED"
    const val SYSTEM = "SYSTEM"
    const val SECURITY = "SECURITY"
}

@JsonClass(generateAdapter = true)
data class CreateNotificationDto(
    val id: String,
    @Json(name = "user_id")
    val userId: String,
    val title: String,
    val message: String,
    val type: String = "SYSTEM",
    @Json(name = "is_read")
    val isRead: Boolean = false
)

