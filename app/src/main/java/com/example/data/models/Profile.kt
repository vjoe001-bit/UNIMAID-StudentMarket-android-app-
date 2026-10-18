package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Profile(
    val id: String,
    val email: String? = null,
    @Json(name = "full_name")
    val fullName: String? = null,
    @Json(name = "matric_number")
    val matricNumber: String? = null,
    val department: String? = null,
    val faculty: String? = null,
    val level: String? = "100L",
    @Json(name = "phone_number")
    val phoneNumber: String? = null,
    val bio: String? = null,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    @Json(name = "is_verified")
    val isVerified: Boolean = false,
    @Json(name = "student_id_card_url")
    val studentIdCardUrl: String? = null,
    @Json(name = "account_status")
    val accountStatus: String? = "ACTIVE",
    @Json(name = "can_sell")
    val canSell: Boolean = true,
    @Json(name = "can_buy")
    val canBuy: Boolean = true,
    val rating: Double? = 5.0,
    @Json(name = "review_count")
    val reviewCount: Int? = 0,
    @Json(name = "is_suspended")
    val isSuspended: Boolean = false,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
) {
    val displayName: String
        get() = fullName?.ifBlank { "UNIMAID Student" } ?: "UNIMAID Student"

    val isStudentVerified: Boolean
        get() = isVerified

    /**
     * Checks if all mandatory campus student identity fields are filled.
     */
    val isComplete: Boolean
        get() = !fullName.isNullOrBlank() &&
                !department.isNullOrBlank() &&
                !matricNumber.isNullOrBlank() &&
                !phoneNumber.isNullOrBlank()

    /**
     * Returns list of human-readable missing field names.
     */
    val missingRequiredFields: List<String>
        get() = buildList {
            if (fullName.isNullOrBlank()) add("Full Name")
            if (department.isNullOrBlank()) add("Department")
            if (matricNumber.isNullOrBlank()) add("Matric Number")
            if (phoneNumber.isNullOrBlank()) add("Phone Number")
        }

    /**
     * Returns profile completion percentage (0 - 100).
     */
    val completionPercentage: Int
        get() {
            var completed = 0
            val total = 5 // Full Name, Matric, Department, Phone, Avatar
            if (!fullName.isNullOrBlank()) completed++
            if (!matricNumber.isNullOrBlank()) completed++
            if (!department.isNullOrBlank()) completed++
            if (!phoneNumber.isNullOrBlank()) completed++
            if (!avatarUrl.isNullOrBlank()) completed++
            return (completed * 100) / total
        }

    fun getVerificationState(latestRequest: VerificationRequest? = null): StudentVerificationState {
        return StudentVerificationState.resolve(this, latestRequest)
    }
}

@JsonClass(generateAdapter = true)
data class CreateProfileDto(
    val id: String,
    @Json(name = "full_name")
    val fullName: String,
    @Json(name = "matric_number")
    val matricNumber: String? = null,
    val department: String? = null,
    val level: String? = "100L",
    val bio: String? = null,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    @Json(name = "is_verified")
    val isVerified: Boolean = false
)

