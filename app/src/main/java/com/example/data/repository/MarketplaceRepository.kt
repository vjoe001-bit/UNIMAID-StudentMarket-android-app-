package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.CampusService
import com.example.data.models.CartItem
import com.example.data.models.Category
import com.example.data.models.CreateListingDto
import com.example.data.models.CreateListingImageDto
import com.example.data.models.Favorite
import com.example.data.models.Listing
import com.example.data.models.ListingImage

interface MarketplaceRepository {
    suspend fun getCategories(): SupabaseResult<List<Category>>
    suspend fun getListings(
        categoryId: String? = null,
        query: String? = null,
        order: String = "created_at.desc",
        limit: Int = 30
    ): SupabaseResult<List<Listing>>
    suspend fun getListingById(id: String): SupabaseResult<Listing>
    suspend fun getSellerListings(sellerId: String): SupabaseResult<List<Listing>>
    suspend fun searchListings(
        query: String? = null,
        categoryId: String? = null,
        condition: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        locationCampus: String? = null,
        isAvailable: Boolean? = true,
        order: String = "created_at.desc",
        limit: Int = 30
    ): SupabaseResult<List<Listing>>
    suspend fun getServices(categoryId: String? = null): SupabaseResult<List<CampusService>>
    suspend fun getCartItems(userId: String): SupabaseResult<List<CartItem>>
    suspend fun getFavorites(userId: String): SupabaseResult<List<Favorite>>
    suspend fun createListing(createDto: CreateListingDto): SupabaseResult<Listing>
    suspend fun createListing(listing: Listing): SupabaseResult<Listing>
    suspend fun updateListing(id: String, updates: Map<String, Any?>): SupabaseResult<Listing>
    suspend fun deleteListing(id: String): SupabaseResult<Unit>
    suspend fun addListingImages(images: List<CreateListingImageDto>): SupabaseResult<List<ListingImage>>
    suspend fun addListingImagesLegacy(images: List<ListingImage>): SupabaseResult<List<ListingImage>>
    suspend fun deleteListingImage(imageId: String): SupabaseResult<Unit>
    suspend fun deleteListingImagesByListing(listingId: String): SupabaseResult<Unit>
    suspend fun updateListingImageDisplayOrder(imageId: String, newOrder: Int): SupabaseResult<Unit>
}

class MarketplaceRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : MarketplaceRepository {

    override suspend fun getCategories(): SupabaseResult<List<Category>> {
        return try {
            val response = restApi.getCategories()
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getListings(
        categoryId: String?,
        query: String?,
        order: String,
        limit: Int
    ): SupabaseResult<List<Listing>> {
        return searchListings(query = query, categoryId = categoryId, order = order, limit = limit)
    }

    override suspend fun getListingById(id: String): SupabaseResult<Listing> {
        return try {
            val response = restApi.getListingById("eq.$id")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getSellerListings(sellerId: String): SupabaseResult<List<Listing>> {
        return try {
            val filters = mapOf("seller_id" to "eq.$sellerId")
            val response = restApi.getListings(order = "created_at.desc", limit = 100, filters = filters)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun searchListings(
        query: String?,
        categoryId: String?,
        condition: String?,
        minPrice: Double?,
        maxPrice: Double?,
        locationCampus: String?,
        isAvailable: Boolean?,
        order: String,
        limit: Int
    ): SupabaseResult<List<Listing>> {
        return try {
            val filters = mutableMapOf<String, String>()
            if (!categoryId.isNullOrBlank() && categoryId != "all") {
                filters["category_id"] = "eq.$categoryId"
            }
            if (!query.isNullOrBlank()) {
                val q = query.trim()
                // Column in Supabase is 'name' and 'description'
                filters["or"] = "(name.ilike.*$q*,description.ilike.*$q*)"
            }
            if (!condition.isNullOrBlank() && condition != "ALL") {
                filters["condition"] = "eq.$condition"
            }
            if (minPrice != null && maxPrice != null) {
                filters["and"] = "(price.gte.$minPrice,price.lte.$maxPrice)"
            } else if (minPrice != null) {
                filters["price"] = "gte.$minPrice"
            } else if (maxPrice != null) {
                filters["price"] = "lte.$maxPrice"
            }
            if (!locationCampus.isNullOrBlank()) {
                filters["campus_location"] = "ilike.*${locationCampus.trim()}*"
            }
            if (isAvailable == true) {
                filters["status"] = "eq.ACTIVE"
            }

            val response = restApi.getListings(order = order, limit = limit, filters = filters)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getServices(categoryId: String?): SupabaseResult<List<CampusService>> {
        return try {
            val filters = mutableMapOf<String, String>()
            if (!categoryId.isNullOrBlank()) {
                filters["category_id"] = "eq.$categoryId"
            }
            val response = restApi.getServices(filters = filters)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getCartItems(userId: String): SupabaseResult<List<CartItem>> {
        return try {
            val response = restApi.getCartItems(userIdFilter = "eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getFavorites(userId: String): SupabaseResult<List<Favorite>> {
        return try {
            val response = restApi.getFavorites(userIdFilter = "eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun createListing(createDto: CreateListingDto): SupabaseResult<Listing> {
        return try {
            val response = restApi.createListing(createDto)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun createListing(listing: Listing): SupabaseResult<Listing> {
        val dto = CreateListingDto(
            sellerId = listing.sellerId,
            name = listing.displayTitle,
            description = listing.description,
            price = listing.price,
            categoryId = listing.categoryId,
            condition = listing.condition,
            quantity = listing.quantity,
            status = listing.status,
            campusLocation = listing.displayLocation
        )
        return createListing(dto)
    }

    override suspend fun updateListing(id: String, updates: Map<String, Any?>): SupabaseResult<Listing> {
        return try {
            val response = restApi.updateListing("eq.$id", updates)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SupabaseResult.Success(response.body()!!.first())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun deleteListing(id: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.deleteListing("eq.$id")
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun addListingImages(images: List<CreateListingImageDto>): SupabaseResult<List<ListingImage>> {
        return try {
            val response = restApi.insertListingImages(images)
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun addListingImagesLegacy(images: List<ListingImage>): SupabaseResult<List<ListingImage>> {
        val dtos = images.map { img ->
            CreateListingImageDto(
                listingId = img.listingId ?: "",
                imageUrl = img.imageUrl,
                displayOrder = img.displayOrder
            )
        }
        return addListingImages(dtos)
    }

    override suspend fun deleteListingImage(imageId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.deleteListingImage("eq.$imageId")
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun deleteListingImagesByListing(listingId: String): SupabaseResult<Unit> {
        return try {
            val response = restApi.deleteListingImagesByListing("eq.$listingId")
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun updateListingImageDisplayOrder(imageId: String, newOrder: Int): SupabaseResult<Unit> {
        return try {
            val response = restApi.updateListingImage("eq.$imageId", mapOf("display_order" to newOrder))
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
