package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Notification(
    val id: String,
    @Json(name = "user_id")
    val userId: String,
    val title: String,
    @Json(name = "message")
    val message: String = "",
    val type: String = "SYSTEM", // MESSAGE, ORDER, VERIFICATION, LISTING, SYSTEM
    @Json(name = "is_read")
    val isRead: Boolean = false,
    @Json(name = "created_at")
    val createdAt: String? = null
) {
    val displayBody: String
        get() = message
}
