package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerificationRequest(
    val id: String,
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "matric_number")
    val matricNumber: String,
    @Json(name = "document_url")
    val documentUrl: String? = null,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    @Json(name = "reviewed_by")
    val reviewedBy: String? = null,
    @Json(name = "reviewed_at")
    val reviewedAt: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null,

    // In-memory / transient properties
    @Transient
    val rejectionReason: String? = null,
    @Transient
    val submittedAt: String? = null,

    val student: Profile? = null
) {
    val studentId: String
        get() = userId ?: ""

    val studentIdCardUrl: String
        get() = documentUrl ?: ""
}
