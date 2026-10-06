package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.UserSettings

interface UserSettingsRepository {
    suspend fun getUserSettings(userId: String): SupabaseResult<UserSettings>
    suspend fun updateUserSettings(settings: UserSettings): SupabaseResult<UserSettings>
}

class UserSettingsRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : UserSettingsRepository {

    override suspend fun getUserSettings(userId: String): SupabaseResult<UserSettings> {
        return try {
            val response = restApi.getUserSettings("eq.$userId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                // Return default settings for user if none exists yet
                SupabaseResult.Success(UserSettings(userId = userId))
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun updateUserSettings(settings: UserSettings): SupabaseResult<UserSettings> {
        return try {
            val response = restApi.upsertUserSettings(settings)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }
}
