package com.example.core.network.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class AuthSignInRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthRefreshRequest(
    @Json(name = "refresh_token")
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class AuthRecoverRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class AuthResendRequest(
    val type: String = "signup",
    val email: String
)

@JsonClass(generateAdapter = true)
data class AuthSessionResponse(
    @Json(name = "access_token")
    val accessToken: String?,
    @Json(name = "token_type")
    val tokenType: String?,
    @Json(name = "expires_in")
    val expiresIn: Long?,
    @Json(name = "refresh_token")
    val refreshToken: String?,
    val user: AuthUserData?
)

@JsonClass(generateAdapter = true)
data class AuthUserData(
    val id: String,
    val email: String?,
    @Json(name = "created_at")
    val createdAt: String?,
    @Json(name = "email_confirmed_at")
    val emailConfirmedAt: String? = null,
    @Json(name = "confirmed_at")
    val confirmedAt: String? = null,
    @Json(name = "user_metadata")
    val userMetadata: Map<String, Any?>? = null
)
