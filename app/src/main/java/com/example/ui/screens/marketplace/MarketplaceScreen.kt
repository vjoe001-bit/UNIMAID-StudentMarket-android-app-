package com.example.ui.screens.marketplace

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.components.UnimaidAuthPromptDialog
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidCategoryPill
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidErrorState
import com.example.core.designsystem.components.UnimaidProductCard
import com.example.core.designsystem.components.UnimaidSearchBar
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.session.SessionState
import com.example.core.theme.AccentCoral
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.data.models.ItemCondition
import com.example.data.models.MarketplaceSortOption
import com.example.data.repository.AuthRepository
import com.example.ui.viewmodels.MarketplaceViewModel

/**
 * Real production marketplace screen connected to Supabase listings.
 * Displays real student products, dynamic categories, live filters,
 * real authenticated favorites and cart reservations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    marketplaceViewModel: MarketplaceViewModel,
    onNavigateToProductDetails: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToSignUp: (() -> Unit)? = null,
    onNavigateToCart: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by marketplaceViewModel.uiState.collectAsState()
    val sessionState = authRepository?.sessionState?.collectAsState()?.value
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId

    var showFilterSheet by remember { mutableStateOf(false) }
    var showAuthPrompt by remember { mutableStateOf(false) }
    var authPromptAction by remember { mutableStateOf("Sign In Required") }
    var authPromptDesc by remember { mutableStateOf("") }

    // Feedback message display
    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            marketplaceViewModel.clearFeedbackMessage()
        }
    }

    // Auth prompt handling
    if (showAuthPrompt) {
        UnimaidAuthPromptDialog(
            actionTitle = authPromptAction,
            actionDescription = authPromptDesc,
            onDismiss = { showAuthPrompt = false },
            onNavigateToLogin = {
                showAuthPrompt = false
                onNavigateToLogin?.invoke()
            },
            onNavigateToSignUp = {
                showAuthPrompt = false
                onNavigateToSignUp?.invoke()
            }
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        MarketplaceFilterSheet(
            currentCondition = uiState.selectedCondition,
            currentSort = uiState.sortOption,
            currentMinPrice = uiState.minPrice,
            currentMaxPrice = uiState.maxPrice,
            onApply = { condition, minPrice, maxPrice, sort ->
                marketplaceViewModel.applyFilters(
                    categoryId = uiState.selectedCategoryId,
                    condition = condition,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    sortOption = sort
                )
                showFilterSheet = false
            },
            onReset = {
                marketplaceViewModel.resetFilters()
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar with Refresh and Cart
        UnimaidTopAppBar(
            title = "Campus Marketplace",
            actions = {
                if (onNavigateToCart != null) {
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.testTag("marketplace_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.cartItemCount > 0) {
                                    Badge { Text(uiState.cartItemCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { marketplaceViewModel.refreshAll() },
                    modifier = Modifier.testTag("marketplace_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh listings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.screenPadding)
        ) {
            // Search Bar & Filter trigger
            UnimaidSearchBar(
                query = "",
                onQueryChange = {},
                readOnly = true,
                onClick = onNavigateToSearch,
                placeholder = "Search textbooks, laptops, hostel gear...",
                onFilterClick = { showFilterSheet = true }
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Categories horizontal bar from Supabase
            val categories = uiState.categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                UnimaidCategoryPill(
                    label = "All Items",
                    isSelected = uiState.selectedCategoryId == null,
                    onClick = { marketplaceViewModel.selectCategory(null) }
                )

                if (categories.isNotEmpty()) {
                    categories.forEach { category ->
                        UnimaidCategoryPill(
                            label = category.name,
                            isSelected = uiState.selectedCategoryId == category.id,
                            onClick = { marketplaceViewModel.selectCategory(category.id) }
                        )
                    }
                } else {
                    // Standard UNIMAID fallback categories while loading
                    val fallback = listOf(
                        "textbooks" to "Textbooks",
                        "hostel" to "Hostel Items",
                        "tech" to "Laptops & Tech",
                        "fashion" to "Fashion",
                        "services" to "Services"
                    )
                    fallback.forEach { (catId, catLabel) ->
                        UnimaidCategoryPill(
                            label = catLabel,
                            isSelected = uiState.selectedCategoryId == catId,
                            onClick = { marketplaceViewModel.selectCategory(catId) }
                        )
                    }
                }
            }

            // Active filters indicator chip row
            val hasActiveFilter = uiState.selectedCondition != null ||
                    uiState.minPrice != null ||
                    uiState.maxPrice != null ||
                    uiState.sortOption != MarketplaceSortOption.NEWEST

            if (hasActiveFilter) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.selectedCondition?.let { cond ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                marketplaceViewModel.applyFilters(
                                    categoryId = uiState.selectedCategoryId,
                                    condition = null,
                                    minPrice = uiState.minPrice,
                                    maxPrice = uiState.maxPrice,
                                    sortOption = uiState.sortOption
                                )
                            },
                            label = { Text("Condition: ${ItemCondition.format(cond)}") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove condition filter",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }

                    if (uiState.minPrice != null || uiState.maxPrice != null) {
                        val priceLabel = when {
                            uiState.minPrice != null && uiState.maxPrice != null -> "₦${uiState.minPrice?.toInt()} - ₦${uiState.maxPrice?.toInt()}"
                            uiState.minPrice != null -> "From ₦${uiState.minPrice?.toInt()}"
                            else -> "Up to ₦${uiState.maxPrice?.toInt()}"
                        }
                        FilterChip(
                            selected = true,
                            onClick = {
                                marketplaceViewModel.applyFilters(
                                    categoryId = uiState.selectedCategoryId,
                                    condition = uiState.selectedCondition,
                                    minPrice = null,
                                    maxPrice = null,
                                    sortOption = uiState.sortOption
                                )
                            },
                            label = { Text(priceLabel) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove price filter",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }

                    if (uiState.sortOption != MarketplaceSortOption.NEWEST) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                marketplaceViewModel.setSortOption(MarketplaceSortOption.NEWEST)
                            },
                            label = { Text(uiState.sortOption.label) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Reset sort",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }

                    TextButton(onClick = { marketplaceViewModel.resetFilters() }) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Main Content Area
            when {
                uiState.isLoading && uiState.marketplaceListings.isEmpty() -> {
                    // Loading skeleton grid
                    MarketplaceSkeletonGrid()
                }

                uiState.errorMessage != null && uiState.marketplaceListings.isEmpty() -> {
                    // Error state with retry
                    UnimaidErrorState(
                        title = "Unable to Load Marketplace",
                        message = uiState.errorMessage ?: "Please check your campus network connection and try again.",
                        onRetry = { marketplaceViewModel.loadMarketplaceListings() }
                    )
                }

                uiState.marketplaceListings.isEmpty() -> {
                    // Empty state
                    UnimaidEmptyState(
                        title = "No Listings Found",
                        subtitle = if (hasActiveFilter || uiState.selectedCategoryId != null) {
                            "No campus items matched your filter criteria. Try adjusting your filters or browsing all categories."
                        } else {
                            "Be the first UNIMAID student to post an item or service for sale!"
                        },
                        actionText = if (hasActiveFilter || uiState.selectedCategoryId != null) "Reset Filters" else "Refresh",
                        onActionClick = {
                            marketplaceViewModel.resetFilters()
                            marketplaceViewModel.selectCategory(null)
                        }
                    )
                }

                else -> {
                    // Real 2-Column Product Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.marketplaceListings.size} Items on Campus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        TextButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sort & Filter",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("marketplace_product_grid"),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        items(uiState.marketplaceListings, key = { it.id }) { listing ->
                            val isFav = uiState.favoriteListingIds.contains(listing.id)
                            val firstImage = listing.images.firstOrNull()?.imageUrl

                            UnimaidProductCard(
                                title = listing.title,
                                price = "₦${"%,.0f".format(listing.price)}",
                                imageUrl = firstImage,
                                condition = ItemCondition.format(listing.condition),
                                isFavorite = isFav,
                                onFavoriteToggle = {
                                    if (currentUserId.isNullOrBlank()) {
                                        authPromptAction = "Save to Favorites"
                                        authPromptDesc = "Sign in with your UNIMAID student account to save listings and get notifications."
                                        showAuthPrompt = true
                                    } else {
                                        marketplaceViewModel.toggleFavorite(currentUserId, listing.id)
                                    }
                                },
                                onClick = { onNavigateToProductDetails(listing.id) },
                                onAddClick = {
                                    if (currentUserId.isNullOrBlank()) {
                                        authPromptAction = "Add to Cart"
                                        authPromptDesc = "Sign in to reserve items and arrange a safe in-person campus handover."
                                        showAuthPrompt = true
                                    } else {
                                        marketplaceViewModel.addToCart(currentUserId, listing.id)
                                    }
                                },
                                testTag = "marketplace_item_${listing.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketplaceFilterSheet(
    currentCondition: String?,
    currentSort: MarketplaceSortOption,
    currentMinPrice: Double?,
    currentMaxPrice: Double?,
    onApply: (condition: String?, minPrice: Double?, maxPrice: Double?, sort: MarketplaceSortOption) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCondition by remember { mutableStateOf(currentCondition) }
    var selectedSort by remember { mutableStateOf(currentSort) }
    var minPriceInput by remember { mutableStateOf(currentMinPrice?.toInt()?.toString() ?: "") }
    var maxPriceInput by remember { mutableStateOf(currentMaxPrice?.toInt()?.toString() ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter & Sort Listings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onReset) {
                    Text("Reset All", color = AccentCoral)
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Sort Options
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                MarketplaceSortOption.values().forEach { option ->
                    FilterChip(
                        selected = selectedSort == option,
                        onClick = { selectedSort = option },
                        label = { Text(option.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Condition Filter
            Text(
                text = "Item Condition",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                ItemCondition.values().forEach { cond ->
                    val isSelected = if (cond == ItemCondition.ALL) {
                        selectedCondition == null
                    } else {
                        selectedCondition == cond.code
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCondition = if (cond == ItemCondition.ALL) null else cond.code
                        },
                        label = { Text(cond.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Price Range
            Text(
                text = "Price Range (₦)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                OutlinedTextField(
                    value = minPriceInput,
                    onValueChange = { minPriceInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Min ₦") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = maxPriceInput,
                    onValueChange = { maxPriceInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Max ₦") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // Apply Button
            UnimaidButton(
                text = "Apply Filters",
                onClick = {
                    val min = minPriceInput.toDoubleOrNull()
                    val max = maxPriceInput.toDoubleOrNull()
                    onApply(selectedCondition, min, max, selectedSort)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MarketplaceSkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        items(6) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(AppRadius.lg),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {}
        }
    }
}
