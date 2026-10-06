package com.example.core.network.storage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignUrlRequest(
    val expiresIn: Int = 3600
)

@JsonClass(generateAdapter = true)
data class SignUrlResponse(
    @Json(name = "signedURL")
    val signedUrl: String
)

@JsonClass(generateAdapter = true)
data class StorageUploadResponse(
    @Json(name = "Key")
    val key: String?,
    @Json(name = "Id")
    val id: String?,
    val message: String? = null
)
