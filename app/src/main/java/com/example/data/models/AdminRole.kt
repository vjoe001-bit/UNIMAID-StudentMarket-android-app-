package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminRole(
    val id: String,
    @Json(name = "user_id")
    val userId: String,
    val role: String, // "MAIN_ADMIN", "MODERATOR", "VERIFICATION_OFFICER"
    val permissions: List<String> = emptyList(),
    @Json(name = "granted_at")
    val grantedAt: String? = null,
    val user: Profile? = null
)
