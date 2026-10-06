package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.Favorite
import java.util.UUID

interface FavoritesRepository {
    suspend fun getFavorites(userId: String): SupabaseResult<List<Favorite>>
    suspend fun addFavorite(userId: String, listingId: String): SupabaseResult<Favorite>
    suspend fun removeFavorite(userId: String, listingId: String): SupabaseResult<Unit>
    suspend fun isListingFavorited(userId: String, listingId: String): SupabaseResult<Boolean>
}

class FavoritesRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : FavoritesRepository {

    override suspend fun getFavorites(userId: String): SupabaseResult<List<Favorite>> {
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

    override suspend fun addFavorite(userId: String, listingId: String): SupabaseResult<Favorite> {
        return try {
            val favorite = Favorite(
                id = UUID.randomUUID().toString(),
                userId = userId,
                listingId = listingId
            )
            val response = restApi.addFavorite(favorite)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun removeFavorite(userId: String, listingId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.deleteFavorite("eq.$userId", "eq.$listingId")
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun isListingFavorited(userId: String, listingId: String): SupabaseResult<Boolean> {
        return when (val favs = getFavorites(userId)) {
            is SupabaseResult.Success -> {
                val exists = favs.data.any { it.listingId == listingId }
                SupabaseResult.Success(exists)
            }
            is SupabaseResult.Error -> favs
            is SupabaseResult.Loading -> SupabaseResult.Success(false)
        }
    }
}
