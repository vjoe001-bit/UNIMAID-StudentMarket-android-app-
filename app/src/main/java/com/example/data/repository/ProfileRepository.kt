package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseConfig
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.Favorite
import com.example.data.models.Listing
import com.example.data.models.Order
import com.example.data.models.Profile

interface ProfileRepository {
    suspend fun getProfile(userId: String): SupabaseResult<Profile>
    suspend fun updateProfile(userId: String, updates: Map<String, Any?>): SupabaseResult<Profile>
    suspend fun uploadAvatar(userId: String, fileName: String, bytes: ByteArray): SupabaseResult<String>
    suspend fun getUserListings(userId: String): SupabaseResult<List<Listing>>
    suspend fun getUserFavorites(userId: String): SupabaseResult<List<Favorite>>
    suspend fun getUserOrders(userId: String): SupabaseResult<List<Order>>
}

class ProfileRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi(),
    private val storageRepo: StorageRepository = StorageRepositoryImpl()
) : ProfileRepository {

    override suspend fun getProfile(userId: String): SupabaseResult<Profile> {
        return try {
            val response = restApi.getProfileById("eq.$userId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun updateProfile(userId: String, updates: Map<String, Any?>): SupabaseResult<Profile> {
        return try {
            // Strip out any non-column or sensitive fields that ordinary users cannot edit
            val safeUpdates = updates.toMutableMap().apply {
                remove("id")
                remove("email")
                remove("phone_number")
                remove("phone")
                remove("student_id_card_url")
                remove("is_verified")
                remove("is_suspended")
                remove("verification_status")
                remove("account_status")
                remove("can_sell")
                remove("can_buy")
                remove("rating")
                remove("review_count")
                remove("created_at")
                remove("role")
                remove("is_admin")
            }
            safeUpdates["updated_at"] = "now()"

            val response = restApi.updateProfile("eq.$userId", safeUpdates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun uploadAvatar(userId: String, fileName: String, bytes: ByteArray): SupabaseResult<String> {
        val uploadResult = storageRepo.uploadAvatar(userId, fileName, bytes)
        return when (uploadResult) {
            is SupabaseResult.Success -> {
                val avatarUrl = uploadResult.data
                // Also update profile avatar_url
                updateProfile(userId, mapOf("avatar_url" to avatarUrl))
                SupabaseResult.Success(avatarUrl)
            }
            is SupabaseResult.Error -> uploadResult
            is SupabaseResult.Loading -> SupabaseResult.Loading
        }
    }

    override suspend fun getUserListings(userId: String): SupabaseResult<List<Listing>> {
        return try {
            val filters = mapOf("seller_id" to "eq.$userId")
            val response = restApi.getListings(filters = filters)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getUserFavorites(userId: String): SupabaseResult<List<Favorite>> {
        return try {
            val response = restApi.getFavorites("eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getUserOrders(userId: String): SupabaseResult<List<Order>> {
        return try {
            val filters = mapOf("or" to "(buyer_id.eq.$userId,seller_id.eq.$userId)")
            val response = restApi.getOrders(filters = filters)
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
