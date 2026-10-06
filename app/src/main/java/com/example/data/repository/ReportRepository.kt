package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.Report

interface ReportRepository {
    suspend fun submitReport(report: Report): SupabaseResult<Report>
    suspend fun getMyReports(userId: String): SupabaseResult<List<Report>>
}

class ReportRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : ReportRepository {

    override suspend fun submitReport(report: Report): SupabaseResult<Report> {
        return try {
            val response = restApi.submitReport(report)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getMyReports(userId: String): SupabaseResult<List<Report>> {
        return try {
            val filters = mapOf("reporter_id" to "eq.$userId")
            val response = restApi.getReports(filters = filters)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }
}
