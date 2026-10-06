package com.example.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.SupabaseResult
import com.example.core.network.withNetworkRetry
import com.example.core.utils.ImageCompressor
import com.example.data.models.CampusService
import com.example.data.models.CartItem
import com.example.data.models.Category
import com.example.data.models.CreateListingDto
import com.example.data.models.CreateListingImageDto
import com.example.data.models.Favorite
import com.example.data.models.Listing
import com.example.data.models.ListingImage
import com.example.data.models.MarketplaceSortOption
import com.example.data.repository.CartRepository
import com.example.data.repository.CartRepositoryImpl
import com.example.data.repository.FavoritesRepository
import com.example.data.repository.FavoritesRepositoryImpl
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MarketplaceRepositoryImpl
import com.example.data.repository.StorageRepository
import com.example.data.repository.StorageRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MarketplaceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingSellerListings: Boolean = false,
    val categories: List<Category> = com.example.data.models.DefaultCategories.LIST,
    val featuredListing: Listing? = null,
    val recentListings: List<Listing> = emptyList(),
    val marketplaceListings: List<Listing> = emptyList(),
    val searchResults: List<Listing> = emptyList(),
    val sellerListings: List<Listing> = emptyList(),
    val services: List<CampusService> = emptyList(),
    val selectedCategoryId: String? = null,
    val selectedCondition: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sortOption: MarketplaceSortOption = MarketplaceSortOption.NEWEST,
    val searchQuery: String = "",
    val favoriteListingIds: Set<String> = emptySet(),
    val favoriteListings: List<Listing> = emptyList(),
    val isLoadingFavorites: Boolean = false,
    val cartItemCount: Int = 0,
    val errorMessage: String? = null,
    val userFeedbackMessage: String? = null
)

/**
 * ViewModel powering the real core marketplace:
 * Home feed, Marketplace browsing, Category filtering, Live search,
 * Sorting, Authenticated student Favorites & Cart integration,
 * Real Sell/Post Item creation, Editing, Storage upload, and Seller Hub management.
 */
class MarketplaceViewModel(
    private val marketplaceRepository: MarketplaceRepository = MarketplaceRepositoryImpl(),
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(),
    private val cartRepository: CartRepository = CartRepositoryImpl(),
    private val storageRepository: StorageRepository = StorageRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentUserId: String? = null

    init {
        loadInitialData()
    }

    /**
     * Updates current user context to bind favorites, cart, and seller listings to the authenticated account.
     */
    fun onUserSessionChanged(userId: String?) {
        if (currentUserId == userId) return
        currentUserId = userId
        if (!userId.isNullOrBlank()) {
            loadUserAccountData(userId)
            loadSellerListings(userId)
        } else {
            _uiState.value = _uiState.value.copy(
                favoriteListingIds = emptySet(),
                favoriteListings = emptyList(),
                cartItemCount = 0,
                sellerListings = emptyList()
            )
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            loadCategoriesInternal()
            loadHomeFeedInternal()
            loadMarketplaceListingsInternal()
            currentUserId?.let {
                loadUserAccountData(it)
                loadSellerListings(it)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            loadCategoriesInternal()
            loadHomeFeedInternal()
            loadMarketplaceListingsInternal()
            currentUserId?.let {
                loadUserAccountData(it)
                loadSellerListings(it)
            }
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            loadCategoriesInternal()
        }
    }

    private suspend fun loadCategoriesInternal() {
        when (val res = withNetworkRetry { marketplaceRepository.getCategories() }) {
            is SupabaseResult.Success -> {
                _uiState.value = _uiState.value.copy(categories = res.data)
            }
            is SupabaseResult.Error -> {
                // If categories fail, keep any existing
            }
            is SupabaseResult.Loading -> {}
        }
    }

    private suspend fun loadHomeFeedInternal() {
        when (val res = withNetworkRetry {
            marketplaceRepository.getListings(order = "created_at.desc", limit = 10)
        }) {
            is SupabaseResult.Success -> {
                val list = res.data
                val featured = list.firstOrNull { it.isFeatured == true } ?: list.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    featuredListing = featured,
                    recentListings = list
                )
            }
            is SupabaseResult.Error -> {
                _uiState.value = _uiState.value.copy(errorMessage = res.userFriendlyMessage)
            }
            is SupabaseResult.Loading -> {}
        }
    }

    fun loadMarketplaceListings() {
        viewModelScope.launch {
            loadMarketplaceListingsInternal()
        }
    }

    private suspend fun loadMarketplaceListingsInternal() {
        val state = _uiState.value
        val result = withNetworkRetry {
            marketplaceRepository.searchListings(
                categoryId = state.selectedCategoryId,
                condition = state.selectedCondition,
                minPrice = state.minPrice,
                maxPrice = state.maxPrice,
                order = state.sortOption.postgrestOrder,
                isAvailable = true,
                limit = 40
            )
        }

        when (result) {
            is SupabaseResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    marketplaceListings = result.data,
                    errorMessage = null
                )
            }
            is SupabaseResult.Error -> {
                _uiState.value = _uiState.value.copy(errorMessage = result.userFriendlyMessage)
            }
            is SupabaseResult.Loading -> {}
        }
    }

    fun loadSellerListings(sellerId: String) {
        if (sellerId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSellerListings = true)
            val result = withNetworkRetry {
                marketplaceRepository.getSellerListings(sellerId)
            }
            when (result) {
                is SupabaseResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        sellerListings = result.data,
                        isLoadingSellerListings = false
                    )
                }
                is SupabaseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingSellerListings = false,
                        errorMessage = result.userFriendlyMessage
                    )
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        val normalized = if (categoryId == "all" || categoryId.isNullOrBlank()) null else categoryId
        if (_uiState.value.selectedCategoryId == normalized) return
        _uiState.value = _uiState.value.copy(selectedCategoryId = normalized)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadMarketplaceListingsInternal()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun applyFilters(
        categoryId: String?,
        condition: String?,
        minPrice: Double?,
        maxPrice: Double?,
        sortOption: MarketplaceSortOption
    ) {
        val normCategory = if (categoryId == "all" || categoryId.isNullOrBlank()) null else categoryId
        val normCondition = if (condition == "ALL" || condition.isNullOrBlank()) null else condition
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = normCategory,
            selectedCondition = normCondition,
            minPrice = minPrice,
            maxPrice = maxPrice,
            sortOption = sortOption
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadMarketplaceListingsInternal()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = null,
            selectedCondition = null,
            minPrice = null,
            maxPrice = null,
            sortOption = MarketplaceSortOption.NEWEST
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadMarketplaceListingsInternal()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setSortOption(option: MarketplaceSortOption) {
        if (_uiState.value.sortOption == option) return
        _uiState.value = _uiState.value.copy(sortOption = option)
        viewModelScope.launch {
            loadMarketplaceListingsInternal()
        }
    }

    /**
     * Debounced real search against Supabase listings.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()

        if (query.trim().isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce keystrokes
            _uiState.value = _uiState.value.copy(isSearching = true)
            val result = withNetworkRetry {
                marketplaceRepository.searchListings(
                    query = query.trim(),
                    categoryId = _uiState.value.selectedCategoryId,
                    condition = _uiState.value.selectedCondition,
                    minPrice = _uiState.value.minPrice,
                    maxPrice = _uiState.value.maxPrice,
                    order = _uiState.value.sortOption.postgrestOrder,
                    isAvailable = true,
                    limit = 30
                )
            }
            when (result) {
                is SupabaseResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        searchResults = result.data,
                        isSearching = false
                    )
                }
                is SupabaseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        errorMessage = result.userFriendlyMessage
                    )
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    private fun loadUserAccountData(userId: String) {
        loadFavorites(userId)
        viewModelScope.launch {
            // Load student's cart count
            when (val cartRes = cartRepository.getCartItems(userId)) {
                is SupabaseResult.Success -> {
                    val count = cartRes.data.sumOf { it.quantity }
                    _uiState.value = _uiState.value.copy(cartItemCount = count)
                }
                else -> {}
            }
        }
    }

    /**
     * Loads the student's favorites with joined listing details from Supabase.
     */
    fun loadFavorites(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFavorites = true)
            when (val favRes = favoritesRepository.getFavorites(userId)) {
                is SupabaseResult.Success -> {
                    val favIds = favRes.data.map { it.listingId }.toSet()
                    val nestedListings = favRes.data.mapNotNull { it.listing }
                    val fullListings = if (nestedListings.size == favRes.data.size) {
                        nestedListings
                    } else {
                        val fetched = mutableListOf<Listing>()
                        for (f in favRes.data) {
                            if (f.listing != null) {
                                fetched.add(f.listing)
                            } else {
                                when (val lRes = marketplaceRepository.getListingById(f.listingId)) {
                                    is SupabaseResult.Success -> fetched.add(lRes.data)
                                    else -> {}
                                }
                            }
                        }
                        fetched
                    }
                    _uiState.value = _uiState.value.copy(
                        favoriteListingIds = favIds,
                        favoriteListings = fullListings,
                        isLoadingFavorites = false
                    )
                }
                is SupabaseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingFavorites = false,
                        errorMessage = favRes.userFriendlyMessage
                    )
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    /**
     * Optimistic toggle of listing favorite with Supabase persistence and rollback on failure.
     */
    fun toggleFavorite(userId: String, listingId: String, listingHint: Listing? = null) {
        val currentlyFavorited = _uiState.value.favoriteListingIds.contains(listingId)
        val updatedSet = if (currentlyFavorited) {
            _uiState.value.favoriteListingIds - listingId
        } else {
            _uiState.value.favoriteListingIds + listingId
        }

        val updatedFavoritesList = if (currentlyFavorited) {
            _uiState.value.favoriteListings.filter { it.id != listingId }
        } else {
            val itemToAdd = listingHint
                ?: _uiState.value.marketplaceListings.find { it.id == listingId }
                ?: _uiState.value.recentListings.find { it.id == listingId }
                ?: _uiState.value.searchResults.find { it.id == listingId }
            if (itemToAdd != null && _uiState.value.favoriteListings.none { it.id == listingId }) {
                listOf(itemToAdd) + _uiState.value.favoriteListings
            } else {
                _uiState.value.favoriteListings
            }
        }

        // Optimistic UI update
        _uiState.value = _uiState.value.copy(
            favoriteListingIds = updatedSet,
            favoriteListings = updatedFavoritesList
        )

        viewModelScope.launch {
            val result = if (currentlyFavorited) {
                favoritesRepository.removeFavorite(userId, listingId)
            } else {
                favoritesRepository.addFavorite(userId, listingId)
            }

            when (result) {
                is SupabaseResult.Success -> {
                    // Success, state stays updated
                }
                is SupabaseResult.Error -> {
                    // Rollback on failure
                    val rolledBack = if (currentlyFavorited) {
                        _uiState.value.favoriteListingIds + listingId
                    } else {
                        _uiState.value.favoriteListingIds - listingId
                    }
                    _uiState.value = _uiState.value.copy(
                        favoriteListingIds = rolledBack,
                        userFeedbackMessage = "Unable to update favorite: ${result.userFriendlyMessage}"
                    )
                    loadFavorites(userId)
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    /**
     * Adds an item to the authenticated student's cart and increments cart count.
     */
    fun addToCart(
        userId: String,
        listingId: String,
        quantity: Int = 1,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            when (val res = cartRepository.addToCart(userId, listingId, quantity)) {
                is SupabaseResult.Success -> {
                    loadUserAccountData(userId)
                    _uiState.value = _uiState.value.copy(
                        userFeedbackMessage = "Added to your campus cart!"
                    )
                    onResult(true, "Added to your campus cart!")
                }
                is SupabaseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        userFeedbackMessage = res.userFriendlyMessage
                    )
                    onResult(false, res.userFriendlyMessage)
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    fun refreshCartCount(userId: String) {
        viewModelScope.launch {
            when (val cartRes = cartRepository.getCartItems(userId)) {
                is SupabaseResult.Success -> {
                    val count = cartRes.data.sumOf { it.quantity }
                    _uiState.value = _uiState.value.copy(cartItemCount = count)
                }
                else -> {}
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(userFeedbackMessage = null)
    }

    suspend fun getListingDetails(listingId: String): SupabaseResult<Listing> {
        return withNetworkRetry {
            marketplaceRepository.getListingById(listingId)
        }
    }

    suspend fun getStudentFavorites(userId: String): SupabaseResult<List<Favorite>> {
        return favoritesRepository.getFavorites(userId)
    }

    suspend fun getStudentCartItems(userId: String): SupabaseResult<List<CartItem>> {
        return cartRepository.getCartItems(userId)
    }

    fun removeCartItem(userId: String, cartItemId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (cartRepository.removeFromCart(cartItemId)) {
                is SupabaseResult.Success -> {
                    loadUserAccountData(userId)
                    onResult(true)
                }
                else -> onResult(false)
            }
        }
    }

    fun updateCartItemQuantity(userId: String, cartItemId: String, quantity: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (cartRepository.updateQuantity(cartItemId, quantity)) {
                is SupabaseResult.Success -> {
                    loadUserAccountData(userId)
                    onResult(true)
                }
                else -> onResult(false)
            }
        }
    }

    // =========================================================================
    // REAL SELL / POST ITEM & EDIT LISTING FLOW
    // =========================================================================

    /**
     * Creates a real marketplace listing under the authenticated student's profile.
     * Compresses and uploads multiple HD product images to Supabase Storage,
     * persists listing & image records in PostgreSQL, and updates UI state.
     */
    suspend fun createProductListing(
        context: Context,
        sellerId: String,
        title: String,
        description: String,
        categoryId: String?,
        condition: String,
        price: Double,
        quantity: Int,
        campusLocation: String,
        imageUris: List<Uri>,
        onProgress: (String) -> Unit = {}
    ): SupabaseResult<Listing> {
        if (sellerId.isBlank()) {
            return SupabaseResult.Error("Unauthorized. Please sign in to post an item.")
        }
        val cleanTitle = title.trim()
        if (cleanTitle.length < 3) {
            return SupabaseResult.Error("Product title must be at least 3 characters.")
        }
        if (price <= 0) {
            return SupabaseResult.Error("Price must be greater than ₦0.")
        }
        if (quantity < 1) {
            return SupabaseResult.Error("Quantity must be at least 1.")
        }

        onProgress("Creating campus listing...")
        val createDto = CreateListingDto(
            sellerId = sellerId,
            name = cleanTitle,
            description = description.trim().ifBlank { null },
            price = price,
            categoryId = categoryId,
            condition = condition,
            quantity = quantity,
            status = "ACTIVE",
            campusLocation = campusLocation.trim().ifBlank { "UNIMAID Main Campus" }
        )

        val listingRes = marketplaceRepository.createListing(createDto)
        if (listingRes !is SupabaseResult.Success) {
            return listingRes
        }

        val createdListing = listingRes.data
        val listingId = createdListing.id

        // Upload images if any
        if (imageUris.isNotEmpty()) {
            val uploadedImages = mutableListOf<CreateListingImageDto>()
            val uploadedPaths = mutableListOf<String>()

            for ((index, uri) in imageUris.withIndex()) {
                onProgress("Uploading image ${index + 1} of ${imageUris.size}...")
                val compressedBytes = ImageCompressor.compressImageFromUri(context, uri)
                if (compressedBytes == null) continue

                val fileName = "img_${System.currentTimeMillis()}_$index.jpg"
                val uploadRes = storageRepository.uploadListingImage(listingId, fileName, compressedBytes)
                if (uploadRes is SupabaseResult.Success) {
                    uploadedImages.add(
                        CreateListingImageDto(
                            listingId = listingId,
                            imageUrl = uploadRes.data,
                            displayOrder = index
                        )
                    )
                    uploadedPaths.add(fileName)
                }
            }

            if (uploadedImages.isNotEmpty()) {
                onProgress("Saving image records...")
                marketplaceRepository.addListingImages(uploadedImages)
            }
        }

        onProgress("Finalizing...")
        loadSellerListings(sellerId)
        refreshAll()

        // Fetch full listing with images
        val fullListing = marketplaceRepository.getListingById(listingId)
        val finalResult = if (fullListing is SupabaseResult.Success) fullListing.data else createdListing
        _uiState.value = _uiState.value.copy(userFeedbackMessage = "Item published successfully!")
        return SupabaseResult.Success(finalResult)
    }

    /**
     * Updates an existing listing owned by the authenticated student.
     */
    suspend fun updateProductListing(
        context: Context,
        listingId: String,
        sellerId: String,
        title: String,
        description: String,
        categoryId: String?,
        condition: String,
        price: Double,
        quantity: Int,
        campusLocation: String,
        status: String,
        newImageUris: List<Uri>,
        existingImagesToKeep: List<ListingImage>,
        removedImages: List<ListingImage>,
        onProgress: (String) -> Unit = {}
    ): SupabaseResult<Listing> {
        if (sellerId.isBlank()) {
            return SupabaseResult.Error("Unauthorized. Please sign in.")
        }
        val cleanTitle = title.trim()
        if (cleanTitle.length < 3) {
            return SupabaseResult.Error("Product title must be at least 3 characters.")
        }

        onProgress("Saving listing changes...")
        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val updates = mutableMapOf<String, Any?>(
            "name" to cleanTitle,
            "description" to description.trim().ifBlank { null },
            "category_id" to categoryId,
            "condition" to condition,
            "price" to price,
            "quantity" to quantity,
            "status" to status,
            "campus_location" to campusLocation.trim().ifBlank { "UNIMAID Main Campus" },
            "updated_at" to isoDate
        )

        val updateRes = marketplaceRepository.updateListing(listingId, updates)
        if (updateRes !is SupabaseResult.Success) {
            return updateRes
        }

        // 1. Delete removed images from DB & storage
        for (img in removedImages) {
            img.id?.let { marketplaceRepository.deleteListingImage(it) }
            storageRepository.deleteListingImageByUrl(img.imageUrl)
        }

        // 2. Update display order of existing kept images (for cover changes / reordering)
        existingImagesToKeep.forEachIndexed { newIndex, img ->
            if (img.id != null && img.displayOrder != newIndex) {
                marketplaceRepository.updateListingImageDisplayOrder(img.id, newIndex)
            }
        }

        // 3. Upload new images
        if (newImageUris.isNotEmpty()) {
            val startOrder = existingImagesToKeep.size
            val newImages = mutableListOf<CreateListingImageDto>()

            for ((idx, uri) in newImageUris.withIndex()) {
                onProgress("Uploading new image ${idx + 1} of ${newImageUris.size}...")
                val compressedBytes = ImageCompressor.compressImageFromUri(context, uri) ?: continue
                val fileName = "img_${System.currentTimeMillis()}_$idx.jpg"
                val uploadRes = storageRepository.uploadListingImage(listingId, fileName, compressedBytes)
                if (uploadRes is SupabaseResult.Success) {
                    newImages.add(
                        CreateListingImageDto(
                            listingId = listingId,
                            imageUrl = uploadRes.data,
                            displayOrder = startOrder + idx
                        )
                    )
                }
            }

            if (newImages.isNotEmpty()) {
                marketplaceRepository.addListingImages(newImages)
            }
        }

        onProgress("Finalizing...")
        loadSellerListings(sellerId)
        refreshAll()

        val fullListing = marketplaceRepository.getListingById(listingId)
        val finalResult = if (fullListing is SupabaseResult.Success) fullListing.data else updateRes.data
        _uiState.value = _uiState.value.copy(userFeedbackMessage = "Listing updated successfully!")
        return SupabaseResult.Success(finalResult)
    }

    /**
     * Updates listing status (e.g. ACTIVE, UNAVAILABLE, SOLD, DEACTIVATED).
     */
    suspend fun updateListingStatus(listingId: String, sellerId: String, newStatus: String): SupabaseResult<Listing> {
        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val updates = mapOf(
            "status" to newStatus,
            "updated_at" to isoDate
        )
        val result = marketplaceRepository.updateListing(listingId, updates)
        if (result is SupabaseResult.Success) {
            loadSellerListings(sellerId)
            refreshAll()
            _uiState.value = _uiState.value.copy(
                userFeedbackMessage = if (newStatus == "SOLD") "Marked as Sold!" else "Listing updated to $newStatus"
            )
        }
        return result
    }

    /**
     * Completely deletes a listing and its images from Supabase.
     */
    suspend fun deleteProductListing(listingId: String, sellerId: String): SupabaseResult<Unit> {
        // Delete image records and listing
        marketplaceRepository.deleteListingImagesByListing(listingId)
        val result = marketplaceRepository.deleteListing(listingId)
        if (result is SupabaseResult.Success) {
            loadSellerListings(sellerId)
            refreshAll()
            _uiState.value = _uiState.value.copy(userFeedbackMessage = "Listing removed successfully.")
        }
        return result
    }
}
