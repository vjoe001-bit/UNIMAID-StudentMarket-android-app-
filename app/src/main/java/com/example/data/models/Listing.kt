package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Listing(
    val id: String = "",
    @Json(name = "seller_id")
    val sellerId: String = "",
    val name: String = "",
    val title: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    @Json(name = "category_id")
    val categoryId: String? = null,
    val condition: String = "GOOD", // NEW, LIKE_NEW, GOOD, FAIR, USED
    val quantity: Int = 1,
    val status: String = "ACTIVE", // ACTIVE, UNAVAILABLE, SOLD, DEACTIVATED
    @Json(name = "campus_location")
    val campusLocation: String? = "UNIMAID Main Campus",
    @Json(name = "location_campus")
    val locationCampus: String? = "UNIMAID Main Campus",
    @Json(name = "is_available")
    val isAvailable: Boolean = true,
    @Json(name = "is_sold")
    val isSold: Boolean = false,
    @Json(name = "is_featured")
    val isFeatured: Boolean = false,
    @Json(name = "view_count")
    val viewCount: Int = 0,
    @Json(name = "favorite_count")
    val favoriteCount: Int = 0,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null,
    val images: List<ListingImage> = emptyList(),
    val category: Category? = null,
    val seller: Profile? = null
) {
    val coverImageUrl: String?
        get() = images.firstOrNull { it.isPrimary == true }?.imageUrl ?: images.firstOrNull()?.imageUrl

    val displayTitle: String
        get() = if (name.isNotBlank()) name else (title ?: "")

    val displayLocation: String
        get() = campusLocation ?: locationCampus ?: "UNIMAID Main Campus"

    val isItemAvailable: Boolean
        get() = status.equals("ACTIVE", ignoreCase = true) && (isSold != true)

    val isItemSold: Boolean
        get() = status.equals("SOLD", ignoreCase = true) || isSold == true
}

@JsonClass(generateAdapter = true)
data class ListingImage(
    val id: String? = null,
    @Json(name = "listing_id")
    val listingId: String? = null,
    @Json(name = "image_url")
    val imageUrl: String,
    @Json(name = "is_primary")
    val isPrimary: Boolean? = false,
    @Json(name = "display_order")
    val displayOrder: Int = 0,
    @Json(name = "created_at")
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateListingDto(
    @Json(name = "seller_id")
    val sellerId: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    @Json(name = "category_id")
    val categoryId: String? = null,
    val condition: String = "GOOD",
    val quantity: Int = 1,
    val status: String = "ACTIVE",
    @Json(name = "campus_location")
    val campusLocation: String = "UNIMAID Main Campus"
)

@JsonClass(generateAdapter = true)
data class UpdateListingDto(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    @Json(name = "category_id")
    val categoryId: String? = null,
    val condition: String? = null,
    val quantity: Int? = null,
    val status: String? = null,
    @Json(name = "campus_location")
    val campusLocation: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateListingImageDto(
    @Json(name = "listing_id")
    val listingId: String,
    @Json(name = "image_url")
    val imageUrl: String,
    @Json(name = "display_order")
    val displayOrder: Int = 0
)
