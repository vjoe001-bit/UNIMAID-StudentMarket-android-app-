package com.example.data.models

/**
 * University of Maiduguri Student Verification States.
 *
 * Provides a unified model for tracking a student's verification standing
 * across the campus marketplace.
 */
enum class StudentVerificationState(
    val code: String,
    val title: String,
    val badgeLabel: String,
    val description: String
) {
    UNVERIFIED(
        code = "UNVERIFIED",
        title = "Unverified Student",
        badgeLabel = "UNVERIFIED",
        description = "Submit your UNIMAID Student ID card or Portal Biodata slip to earn the official verified badge and build trust on campus."
    ),
    VERIFICATION_PENDING(
        code = "VERIFICATION_PENDING",
        title = "Verification Under Review",
        badgeLabel = "IN REVIEW",
        description = "Your UNIMAID student credentials have been submitted and are currently being reviewed by campus moderation."
    ),
    VERIFIED(
        code = "VERIFIED",
        title = "Verified UNIMAID Student",
        badgeLabel = "VERIFIED STUDENT",
        description = "Your student identity has been verified. Your marketplace listings and messages carry the official UNIMAID verified badge."
    ),
    REJECTED(
        code = "REJECTED",
        title = "Verification Declined",
        badgeLabel = "DECLINED",
        description = "Your student verification document could not be validated. Please review the feedback and submit an updated student ID."
    ),
    SUSPENDED(
        code = "SUSPENDED",
        title = "Account Suspended",
        badgeLabel = "SUSPENDED",
        description = "Your student marketplace account privileges have been suspended. Please contact campus administration."
    );

    companion object {
        fun fromCode(code: String?): StudentVerificationState {
            return when (code?.uppercase()) {
                "VERIFIED", "APPROVED" -> VERIFIED
                "PENDING", "VERIFICATION_PENDING" -> VERIFICATION_PENDING
                "REJECTED", "DECLINED" -> REJECTED
                "SUSPENDED", "DISABLED", "BANNED" -> SUSPENDED
                else -> UNVERIFIED
            }
        }

        /**
         * Resolves the comprehensive verification state from profile and latest verification request.
         */
        fun resolve(
            profile: Profile?,
            latestRequest: VerificationRequest? = null
        ): StudentVerificationState {
            if (profile == null) return UNVERIFIED

            val accountStatus = profile.accountStatus.orEmpty().uppercase()
            if (accountStatus == "SUSPENDED" || accountStatus == "DISABLED" || accountStatus == "FLAGGED") {
                return SUSPENDED
            }

            if (profile.isVerified || latestRequest?.status.equals("APPROVED", ignoreCase = true)) {
                return VERIFIED
            }

            val reqStatus = latestRequest?.status?.uppercase()
            if (reqStatus == "PENDING") {
                return VERIFICATION_PENDING
            }

            if (reqStatus == "REJECTED") {
                return REJECTED
            }

            return UNVERIFIED
        }
    }
}
