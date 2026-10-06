package com.example.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.components.UnimaidAuthPromptDialog
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidCategoryPill
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidProductCard
import com.example.core.designsystem.components.UnimaidSearchBar
import com.example.core.session.SessionState
import com.example.core.theme.AccentCoral
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.data.models.ItemCondition
import com.example.data.models.MarketplaceSortOption
import com.example.data.repository.AuthRepository
import com.example.ui.viewmodels.MarketplaceViewModel

/**
 * Real-time PostgREST search & discovery screen.
 * Connects to Supabase listings with instant query debouncing,
 * dynamic category filtering, condition chips, sort options, and campus search suggestions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    marketplaceViewModel: MarketplaceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToSignUp: (() -> Unit)? = null
) {
    val uiState by marketplaceViewModel.uiState.collectAsState()
    val sessionState = authRepository?.sessionState?.collectAsState()?.value
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var showFilterSheet by remember { mutableStateOf(false) }
    var showAuthPrompt by remember { mutableStateOf(false) }
    var authPromptAction by remember { mutableStateOf("Sign In Required") }
    var authPromptDesc by remember { mutableStateOf("") }

    val popularCampusSearches = listOf(
        "GST 111", "Standing Fan", "Calculators", "Hostel Mattress",
        "HP Laptop", "Study Lamp", "Gas Cylinder", "Lecture Notes"
    )

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

    if (showFilterSheet) {
        SearchFilterSheet(
            currentCondition = uiState.selectedCondition,
            currentSort = uiState.sortOption,
            currentMinPrice = uiState.minPrice,
            currentMaxPrice = uiState.maxPrice,
            onApply = { condition, min, max, sort ->
                marketplaceViewModel.applyFilters(
                    categoryId = uiState.selectedCategoryId,
                    condition = condition,
                    minPrice = min,
                    maxPrice = max,
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
            .statusBarsPadding()
    ) {
        // Search Header Row with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    onNavigateBack()
                },
                modifier = Modifier.testTag("search_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                UnimaidSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { query ->
                        marketplaceViewModel.onSearchQueryChanged(query)
                    },
                    readOnly = false,
                    placeholder = "Search textbooks, hostel gear...",
                    onFilterClick = {
                        focusManager.clearFocus(force = true)
                        showFilterSheet = true
                    }
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.xs))
        }

        // Category Horizontal Strip from Supabase
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            UnimaidCategoryPill(
                label = "All",
                isSelected = uiState.selectedCategoryId == null,
                onClick = { marketplaceViewModel.selectCategory(null) }
            )

            uiState.categories.forEach { cat ->
                UnimaidCategoryPill(
                    label = cat.name,
                    isSelected = uiState.selectedCategoryId == cat.id,
                    onClick = { marketplaceViewModel.selectCategory(cat.id) }
                )
            }
        }

        // Active Filters Summary Chips
        val hasActiveFilter = uiState.selectedCondition != null ||
                uiState.minPrice != null ||
                uiState.maxPrice != null ||
                uiState.sortOption != MarketplaceSortOption.NEWEST

        if (hasActiveFilter) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.xs),
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
                        label = { Text(ItemCondition.format(cond)) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }

                if (uiState.sortOption != MarketplaceSortOption.NEWEST) {
                    FilterChip(
                        selected = true,
                        onClick = { marketplaceViewModel.setSortOption(MarketplaceSortOption.NEWEST) },
                        label = { Text(uiState.sortOption.label) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }

                TextButton(onClick = { marketplaceViewModel.resetFilters() }) {
                    Text("Clear filters", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Search Content / Results
        val displayList = if (uiState.searchQuery.isNotBlank()) {
            uiState.searchResults
        } else {
            uiState.marketplaceListings
        }

        when {
            uiState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                }
            }

            displayList.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.screenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(AppSpacing.xl))

                    UnimaidEmptyState(
                        title = "No Matches Found",
                        subtitle = "We couldn't find anything matching \"${uiState.searchQuery}\". Try another campus keyword.",
                        actionText = "Clear Search",
                        onActionClick = { marketplaceViewModel.onSearchQueryChanged("") }
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    Text(
                        text = "Popular Campus Searches",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        popularCampusSearches.forEach { term ->
                            Surface(
                                modifier = Modifier
                                    .clip(AppRadius.full)
                                    .clickable { marketplaceViewModel.onSearchQueryChanged(term) },
                                shape = AppRadius.full,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = term,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.screenPadding)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) {
                                "${displayList.size} results for \"${uiState.searchQuery}\""
                            } else {
                                "${displayList.size} campus listings"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(onClick = { showFilterSheet = true }) {
                            Icon(imageVector = Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filters", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_grid"),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        items(displayList, key = { it.id }) { listing ->
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
                                        authPromptDesc = "Sign in to save this listing to your personal favorites and track price changes."
                                        showAuthPrompt = true
                                    } else {
                                        marketplaceViewModel.toggleFavorite(currentUserId, listing.id)
                                    }
                                },
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    onNavigateToProductDetails(listing.id)
                                },
                                onAddClick = {
                                    if (currentUserId.isNullOrBlank()) {
                                        authPromptAction = "Add to Cart"
                                        authPromptDesc = "Sign in to reserve items in your campus cart."
                                        showAuthPrompt = true
                                    } else {
                                        marketplaceViewModel.addToCart(currentUserId, listing.id)
                                    }
                                },
                                testTag = "search_item_${listing.id}"
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
private fun SearchFilterSheet(
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
                    text = "Search Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onReset) {
                    Text("Reset", color = AccentCoral)
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text("Sort Order", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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

            Text("Condition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                ItemCondition.values().forEach { cond ->
                    val isSelected = if (cond == ItemCondition.ALL) selectedCondition == null else selectedCondition == cond.code
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCondition = if (cond == ItemCondition.ALL) null else cond.code },
                        label = { Text(cond.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text("Price Range (₦)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
