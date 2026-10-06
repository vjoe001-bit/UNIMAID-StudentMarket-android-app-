package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderItem(
    val id: String,
    @Json(name = "order_id")
    val orderId: String,
    @Json(name = "listing_id")
    val listingId: String,
    val quantity: Int = 1,
    @Json(name = "unit_price")
    val unitPrice: Double,
    val subtotal: Double,
    val listing: Listing? = null
)
