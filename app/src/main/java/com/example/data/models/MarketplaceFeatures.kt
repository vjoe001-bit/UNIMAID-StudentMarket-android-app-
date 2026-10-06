package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Favorite(
    val id: String,
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "listing_id")
    val listingId: String,
    @Json(name = "created_at")
    val createdAt: String? = null,
    val listing: Listing? = null
)

@JsonClass(generateAdapter = true)
data class Review(
    val id: String,
    @Json(name = "reviewer_id")
    val reviewerId: String,
    @Json(name = "target_user_id")
    val targetUserId: String,
    @Json(name = "listing_id")
    val listingId: String? = null,
    val rating: Int, // 1 to 5
    val comment: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    val reviewer: Profile? = null
)

@JsonClass(generateAdapter = true)
data class Report(
    val id: String,
    @Json(name = "reporter_id")
    val reporterId: String,
    @Json(name = "reported_user_id")
    val reportedUserId: String? = null,
    @Json(name = "listing_id")
    val listingId: String? = null,
    val reason: String,
    @Json(name = "description")
    val description: String? = null,
    val status: String = "PENDING", // PENDING, INVESTIGATING, RESOLVED, DISMISSED
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
) {
    val reportedListingId: String?
        get() = listingId

    val details: String?
        get() = description
}

@JsonClass(generateAdapter = true)
data class BlockedUser(
    val id: String,
    @Json(name = "blocker_id")
    val blockerId: String,
    @Json(name = "blocked_user_id")
    val blockedUserId: String,
    @Json(name = "created_at")
    val createdAt: String? = null,
    val blockedProfile: Profile? = null
)
