package com.example.data.repository

import com.example.core.admin.AdminConfig
import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseConfig
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.AdminRole
import com.example.data.models.Listing
import com.example.data.models.Profile
import com.example.data.models.Report
import com.example.data.models.VerificationRequest

interface AdminRepository {
    suspend fun getAdminRole(userId: String): SupabaseResult<AdminRole?>
    suspend fun getPendingVerifications(): SupabaseResult<List<VerificationRequest>>
    suspend fun reviewVerification(requestId: String, studentUserId: String, approve: Boolean, reviewerId: String, reason: String? = null): SupabaseResult<Unit>
    suspend fun getAllReports(): SupabaseResult<List<Report>>
    suspend fun resolveReport(reportId: String, newStatus: String): SupabaseResult<Unit>
    suspend fun getAllListingsForModeration(): SupabaseResult<List<Listing>>
    suspend fun updateListingStatus(listingId: String, status: String): SupabaseResult<Unit>
    suspend fun getAllUsersForManagement(): SupabaseResult<List<Profile>>
    suspend fun toggleUserSuspension(userId: String, suspend: Boolean): SupabaseResult<Unit>
}

class AdminRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : AdminRepository {

    override suspend fun getAdminRole(userId: String): SupabaseResult<AdminRole?> {
        if (AdminConfig.isDesignatedAdmin(userId, null)) {
            return SupabaseResult.Success(
                AdminRole(
                    id = "admin-role-$userId",
                    userId = userId,
                    role = "SUPER_ADMIN",
                    permissions = listOf("ALL", "VERIFICATION_REVIEW", "REPORT_MODERATION", "USER_MANAGEMENT")
                )
            )
        }
        return try {
            val response = restApi.getAdminRoleForUser(userIdFilter = "eq.$userId")
            if (response.isSuccessful) {
                val role = response.body()?.firstOrNull()
                SupabaseResult.Success(role)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getPendingVerifications(): SupabaseResult<List<VerificationRequest>> {
        return try {
            val filters = mapOf("status" to "eq.PENDING")
            val response = restApi.getVerificationRequests(filters = filters)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun reviewVerification(
        requestId: String,
        studentUserId: String,
        approve: Boolean,
        reviewerId: String,
        reason: String?
    ): SupabaseResult<Unit> {
        return try {
            val newStatus = if (approve) "APPROVED" else "REJECTED"
            val updates = mutableMapOf<String, Any?>(
                "status" to newStatus,
                "reviewed_by" to reviewerId,
                "reviewed_at" to "now()"
            )
            val updateRes = restApi.updateVerificationRequest("eq.$requestId", updates)
            if (!updateRes.isSuccessful) {
                return SupabaseErrorHandler.parseHttpError(updateRes.code(), updateRes.errorBody())
            }
            if (approve) {
                // Also update profile is_verified = true
                restApi.updateProfileAdmin("eq.$studentUserId", mapOf("is_verified" to true))
            }
            SupabaseResult.Success(Unit)
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getAllReports(): SupabaseResult<List<Report>> {
        return try {
            val response = restApi.getReports()
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun resolveReport(reportId: String, newStatus: String): SupabaseResult<Unit> {
        return try {
            val updates = mapOf(
                "status" to newStatus,
                "updated_at" to "now()"
            )
            val res = restApi.updateReport("eq.$reportId", updates)
            if (res.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(res.code(), res.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getAllListingsForModeration(): SupabaseResult<List<Listing>> {
        return try {
            val response = restApi.getListings(limit = 100)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun updateListingStatus(listingId: String, status: String): SupabaseResult<Unit> {
        return try {
            val updates = mapOf(
                "status" to status,
                "updated_at" to "now()"
            )
            val res = restApi.updateListing("eq.$listingId", updates)
            if (res.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(res.code(), res.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getAllUsersForManagement(): SupabaseResult<List<Profile>> {
        return try {
            val response = restApi.getAllProfiles()
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun toggleUserSuspension(userId: String, suspend: Boolean): SupabaseResult<Unit> {
        return try {
            val res = restApi.updateProfileAdmin("eq.$userId", mapOf("is_suspended" to suspend))
            if (res.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(res.code(), res.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }
}

