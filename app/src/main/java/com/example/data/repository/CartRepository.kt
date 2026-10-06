package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.CartItem
import java.util.UUID

interface CartRepository {
    suspend fun getCartItems(userId: String): SupabaseResult<List<CartItem>>
    suspend fun addToCart(userId: String, listingId: String, quantity: Int = 1): SupabaseResult<CartItem>
    suspend fun updateQuantity(cartItemId: String, quantity: Int): SupabaseResult<CartItem>
    suspend fun removeFromCart(cartItemId: String): SupabaseResult<Unit>
    suspend fun clearCart(userId: String): SupabaseResult<Unit>
}

class CartRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : CartRepository {

    override suspend fun getCartItems(userId: String): SupabaseResult<List<CartItem>> {
        return try {
            val response = restApi.getCartItems("eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun addToCart(userId: String, listingId: String, quantity: Int): SupabaseResult<CartItem> {
        return try {
            // 1. Verify listing exists and is available
            val listingResp = restApi.getListingById("eq.$listingId")
            if (!listingResp.isSuccessful || listingResp.body().isNullOrEmpty()) {
                return SupabaseResult.Error("Item could not be found or has been removed from marketplace.")
            }
            val listing = listingResp.body()!!.first()
            if (!listing.isAvailable || listing.status.equals("SOLD", ignoreCase = true) || listing.status.equals("DEACTIVATED", ignoreCase = true)) {
                return SupabaseResult.Error("This campus item is marked as sold or unavailable.")
            }
            val availableStock = if (listing.quantity > 0) listing.quantity else 1

            // 2. Check if already in user's cart to prevent duplicate entries
            val existingResp = restApi.getCartItemForListing("eq.$userId", "eq.$listingId")
            if (existingResp.isSuccessful && !existingResp.body().isNullOrEmpty()) {
                val existing = existingResp.body()!!.first()
                val targetQty = existing.quantity + quantity
                val finalQty = minOf(targetQty, availableStock)
                val updateResp = restApi.updateCartItemQuantity("eq.${existing.id}", mapOf("quantity" to finalQty))
                return if (updateResp.isSuccessful && !updateResp.body().isNullOrEmpty()) {
                    SupabaseResult.Success(updateResp.body()!!.first())
                } else {
                    SupabaseErrorHandler.parseHttpError(updateResp.code(), updateResp.errorBody())
                }
            }

            // 3. New cart item with validated initial quantity
            val initialQty = minOf(maxOf(1, quantity), availableStock)
            val item = CartItem(
                id = UUID.randomUUID().toString(),
                userId = userId,
                listingId = listingId,
                quantity = initialQty
            )
            val response = restApi.addCartItem(item)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int): SupabaseResult<CartItem> {
        return try {
            val safeQuantity = maxOf(1, quantity)
            val updates = mapOf("quantity" to safeQuantity)
            val response = restApi.updateCartItemQuantity("eq.$cartItemId", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun removeFromCart(cartItemId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.deleteCartItem("eq.$cartItemId")
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun clearCart(userId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.clearCartForUser("eq.$userId")
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }
}
