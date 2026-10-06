package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Order representation for UNIMAID StudentMarket.
 * Under UNIMAID business rules, there is NO online payment gateway.
 * Orders represent reserved campus items and physical handover arrangements.
 */
@JsonClass(generateAdapter = true)
data class Order(
    val id: String,
    @Json(name = "buyer_id")
    val buyerId: String,
    @Json(name = "seller_id")
    val sellerId: String,
    @Json(name = "listing_id")
    val listingId: String? = null,
    val quantity: Int = 1,
    @Json(name = "total_amount")
    val totalAmount: Double,
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED, MEETING_ARRANGED, COMPLETED, CANCELLED
    @Json(name = "meeting_location")
    val meetingLocation: String? = "UNIMAID Main Campus",
    @Json(name = "buyer_note")
    val buyerNote: String? = null,
    @Json(name = "seller_note")
    val sellerNote: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null,

    // In-memory UI/status helpers (not database columns in public.orders)
    @Transient
    val paymentStatus: String? = "PHYSICAL_PENDING",
    @Transient
    val deliveryMethod: String? = "PHYSICAL_MEETUP",
    @Transient
    val meetingTime: String? = null,
    @Transient
    val completedAt: String? = null,

    val buyer: Profile? = null,
    val seller: Profile? = null,
    val listing: Listing? = null,
    val items: List<OrderItem> = emptyList(),
    val history: List<OrderStatusHistory> = emptyList()
) {
    /**
     * Resolves the list of ordered items, generating a fallback item from `listing` if nested items table is empty.
     */
    val effectiveItems: List<OrderItem>
        get() = if (items.isNotEmpty()) {
            items
        } else if (listing != null) {
            listOf(
                OrderItem(
                    id = id,
                    orderId = id,
                    listingId = listing.id,
                    quantity = quantity,
                    unitPrice = listing.price,
                    subtotal = totalAmount,
                    listing = listing
                )
            )
        } else {
            emptyList()
        }
}

@JsonClass(generateAdapter = true)
data class OrderStatusHistory(
    val id: String,
    @Json(name = "order_id")
    val orderId: String,
    @Json(name = "old_status")
    val oldStatus: String? = null,
    @Json(name = "new_status")
    val newStatus: String = "PENDING",
    @Transient
    val status: String? = null,
    @Json(name = "changed_by")
    val changedBy: String? = null,
    val note: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
) {
    val displayStatus: String
        get() = newStatus.ifBlank { status ?: "PENDING" }
}
