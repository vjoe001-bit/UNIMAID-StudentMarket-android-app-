package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserSettings(
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "push_notifications_enabled")
    val pushNotificationsEnabled: Boolean = true,
    @Json(name = "email_notifications_enabled")
    val emailNotificationsEnabled: Boolean = true,
    @Json(name = "show_phone_number")
    val showPhoneNumber: Boolean = false,
    @Json(name = "theme_mode")
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    @Json(name = "campus_location_preference")
    val campusLocationPreference: String = "UNIMAID Main Campus",
    @Json(name = "updated_at")
    val updatedAt: String? = null
)
