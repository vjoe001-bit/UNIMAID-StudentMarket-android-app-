package com.example.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.designsystem.components.MainNavTab
import com.example.core.designsystem.components.UnimaidBottomNav
import com.example.core.theme.ThemeMode
import com.example.data.repository.AuthRepository
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.marketplace.MarketplaceScreen
import com.example.ui.screens.messages.MessagesScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.seller.SellerHubScreen
import com.example.ui.viewmodels.MarketplaceViewModel

@Composable
fun MainScreen(
    authRepository: AuthRepository,
    onNavigateToSearch: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    onNavigateToVerification: () -> Unit,
    onNavigateToSellerHub: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onToggleTheme: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfileCompletion: () -> Unit = {},
    onNavigateToCart: (() -> Unit)? = null,
    onNavigateToCreateListing: (() -> Unit)? = null,
    onNavigateToEditListing: ((String) -> Unit)? = null,
    onNavigateToMyListings: (() -> Unit)? = null,
    onNavigateToOrders: (() -> Unit)? = null
) {
    var currentTab by rememberSaveable { mutableStateOf(MainNavTab.HOME) }
    val marketplaceViewModel: MarketplaceViewModel = viewModel()
    val sessionState by authRepository.sessionState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(sessionState) {
        val uid = (sessionState as? com.example.core.session.SessionState.Authenticated)?.session?.userId
        marketplaceViewModel.onUserSessionChanged(uid)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            UnimaidBottomNav(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    if (tab == MainNavTab.SELL_POST) {
                        if (sessionState is com.example.core.session.SessionState.Authenticated) {
                            onNavigateToSellerHub()
                        } else {
                            onNavigateToLogin()
                        }
                    } else {
                        currentTab = tab
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainNavTab.HOME -> {
                    HomeScreen(
                        authRepository = authRepository,
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToMarketplace = { currentTab = MainNavTab.MARKETPLACE },
                        onNavigateToCategory = onNavigateToCategory,
                        onNavigateToProductDetails = onNavigateToProductDetails,
                        onNavigateToVerification = onNavigateToVerification,
                        onNavigateToSellerHub = onNavigateToSellerHub,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onToggleTheme = onToggleTheme,
                        onNavigateToProfileCompletion = onNavigateToProfileCompletion,
                        marketplaceViewModel = marketplaceViewModel,
                        onNavigateToCart = onNavigateToCart,
                        onNavigateToLogin = onNavigateToLogin
                    )
                }
                MainNavTab.MARKETPLACE -> {
                    MarketplaceScreen(
                        marketplaceViewModel = marketplaceViewModel,
                        onNavigateToProductDetails = onNavigateToProductDetails,
                        onNavigateToSearch = onNavigateToSearch,
                        authRepository = authRepository,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToCart = onNavigateToCart
                    )
                }
                MainNavTab.SELL_POST -> {
                    SellerHubScreen(
                        authRepository = authRepository,
                        marketplaceViewModel = marketplaceViewModel,
                        onNavigateBack = { currentTab = MainNavTab.HOME },
                        onNavigateToCreateListing = { onNavigateToCreateListing?.invoke() ?: onNavigateToSellerHub() },
                        onNavigateToEditListing = { id -> onNavigateToEditListing?.invoke(id) },
                        onNavigateToProductDetails = onNavigateToProductDetails,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToSellerOrders = onNavigateToOrders
                    )
                }
                MainNavTab.MESSAGES -> {
                    MessagesScreen(
                        authRepository = authRepository,
                        onNavigateToLogin = onNavigateToLogin,
                        onExploreMarketplace = { currentTab = MainNavTab.MARKETPLACE }
                    )
                }
                MainNavTab.PROFILE -> {
                    ProfileScreen(
                        authRepository = authRepository,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToVerification = onNavigateToVerification,
                        onNavigateToSellerHub = onNavigateToSellerHub,
                        onNavigateToAdminDashboard = onNavigateToAdminDashboard,
                        onSetThemeMode = onSetThemeMode,
                        onNavigateToProfileCompletion = onNavigateToProfileCompletion,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToMyListings = { onNavigateToMyListings?.invoke() ?: onNavigateToSellerHub() },
                        onNavigateToOrders = onNavigateToOrders
                    )
                }
            }
        }
    }
}
