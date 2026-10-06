package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.VerificationRequest

interface VerificationRepository {
    suspend fun getVerificationStatus(studentId: String): SupabaseResult<VerificationRequest?>
    suspend fun submitVerification(request: VerificationRequest): SupabaseResult<VerificationRequest>
}

class VerificationRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : VerificationRepository {

    override suspend fun getVerificationStatus(studentId: String): SupabaseResult<VerificationRequest?> {
        return try {
            val filters = mapOf("user_id" to "eq.$studentId")
            val response = restApi.getVerificationRequests(filters = filters)
            if (response.isSuccessful && response.body() != null) {
                // Return latest verification request
                val latest = response.body()!!.firstOrNull()
                SupabaseResult.Success(latest)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun submitVerification(request: VerificationRequest): SupabaseResult<VerificationRequest> {
        return try {
            val response = restApi.submitVerificationRequest(request)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!.firstOrNull() ?: request
                SupabaseResult.Success(created)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }
}
