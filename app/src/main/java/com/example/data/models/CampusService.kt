package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CampusService(
    val id: String,
    @Json(name = "provider_id")
    val providerId: String,
    val title: String,
    val description: String? = null,
    @Json(name = "starting_price")
    val startingPrice: Double = 0.0,
    @Json(name = "category_id")
    val categoryId: String? = null,
    @Json(name = "pricing_type")
    val pricingType: String = "FIXED", // FIXED, HOURLY, NEGOTIABLE
    val availability: String = "AVAILABLE",
    @Json(name = "created_at")
    val createdAt: String? = null,
    val images: List<ServiceImage> = emptyList(),
    val provider: Profile? = null
)

@JsonClass(generateAdapter = true)
data class ServiceImage(
    val id: String? = null,
    @Json(name = "service_id")
    val serviceId: String? = null,
    @Json(name = "image_url")
    val imageUrl: String,
    @Json(name = "is_primary")
    val isPrimary: Boolean = false
)
