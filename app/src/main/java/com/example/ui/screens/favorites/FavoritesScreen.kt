package com.example.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.components.UnimaidAuthPromptDialog
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidProductCard
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.session.SessionState
import com.example.core.theme.AppSpacing
import com.example.core.utils.MarketplaceUtils
import com.example.data.models.ItemCondition
import com.example.data.models.Listing
import com.example.data.repository.AuthRepository
import com.example.data.repository.FavoritesRepository
import com.example.data.repository.FavoritesRepositoryImpl
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MarketplaceRepositoryImpl
import com.example.ui.viewmodels.MarketplaceViewModel
import kotlinx.coroutines.launch

/**
 * Screen displaying the authenticated student's saved favorite listings from Supabase.
 * Synchronized live with MarketplaceViewModel and Supabase PostgreSQL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    onExploreMarketplace: () -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository? = null,
    marketplaceViewModel: MarketplaceViewModel? = null,
    favoritesRepository: FavoritesRepository = remember { FavoritesRepositoryImpl() },
    marketplaceRepository: MarketplaceRepository = remember { MarketplaceRepositoryImpl() },
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToSignUp: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val sessionState = authRepository?.sessionState?.collectAsState()?.value
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId
    val isAuthenticated = !currentUserId.isNullOrBlank()

    val uiState = marketplaceViewModel?.uiState?.collectAsState()?.value

    var isManualRefreshing by remember { mutableStateOf(false) }
    var showAuthPrompt by remember { mutableStateOf(!isAuthenticated) }

    LaunchedEffect(currentUserId) {
        if (!currentUserId.isNullOrBlank() && marketplaceViewModel != null) {
            marketplaceViewModel.loadFavorites(currentUserId)
        }
    }

    if (showAuthPrompt && !isAuthenticated) {
        UnimaidAuthPromptDialog(
            actionTitle = "Saved Favorites",
            actionDescription = "Sign in to your UNIMAID student account to view and sync your saved listings across sessions.",
            onDismiss = {
                showAuthPrompt = false
                onNavigateBack()
            },
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

    val favoriteListings = uiState?.favoriteListings ?: emptyList()
    val isLoading = (uiState?.isLoadingFavorites == true) || isManualRefreshing
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            UnimaidTopAppBar(
                title = "Saved Favorites",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading && favoriteListings.isNotEmpty(),
            state = pullToRefreshState,
            onRefresh = {
                if (currentUserId != null && marketplaceViewModel != null) {
                    coroutineScope.launch {
                        isManualRefreshing = true
                        marketplaceViewModel.loadFavorites(currentUserId)
                        isManualRefreshing = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && favoriteListings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                !isAuthenticated -> {
                    UnimaidEmptyState(
                        title = "Sign In to View Favorites",
                        subtitle = "Sign in with your University of Maiduguri account to access your saved items.",
                        icon = Icons.Outlined.FavoriteBorder,
                        actionText = "Sign In",
                        onActionClick = { onNavigateToLogin?.invoke() }
                    )
                }

                favoriteListings.isEmpty() -> {
                    UnimaidEmptyState(
                        title = "No Saved Items Yet",
                        subtitle = "Tap the heart icon on any campus listing to save items you want to keep an eye on.",
                        icon = Icons.Outlined.FavoriteBorder,
                        actionText = "Explore Marketplace",
                        onActionClick = onExploreMarketplace
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppSpacing.screenPadding)
                            .testTag("favorites_grid"),
                        contentPadding = PaddingValues(top = AppSpacing.sm, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        items(favoriteListings, key = { it.id }) { listing ->
                            UnimaidProductCard(
                                title = listing.displayTitle,
                                price = MarketplaceUtils.formatNaira(listing.price),
                                imageUrl = listing.images.firstOrNull()?.imageUrl,
                                condition = ItemCondition.format(listing.condition),
                                isFavorite = true,
                                onFavoriteToggle = {
                                    currentUserId?.let { uid ->
                                        marketplaceViewModel?.toggleFavorite(uid, listing.id, listing)
                                    }
                                },
                                onClick = { onNavigateToProductDetails(listing.id) },
                                onAddClick = {
                                    currentUserId?.let { uid ->
                                        marketplaceViewModel?.addToCart(uid, listing.id)
                                    }
                                },
                                testTag = "fav_item_${listing.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}
