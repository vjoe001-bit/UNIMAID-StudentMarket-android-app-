package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CartItem(
    val id: String,
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "listing_id")
    val listingId: String,
    val quantity: Int = 1,
    @Json(name = "created_at")
    val createdAt: String? = null,
    val listing: Listing? = null
) {
    val addedAt: String?
        get() = createdAt
}
