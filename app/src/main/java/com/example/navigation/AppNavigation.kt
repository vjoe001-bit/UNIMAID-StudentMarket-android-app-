package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.core.session.SessionState
import com.example.core.theme.ThemeMode
import com.example.data.repository.AuthRepository
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.SignUpScreen
import com.example.ui.screens.cart.CartScreen
import com.example.ui.screens.categories.CategoriesScreen
import com.example.ui.screens.details.ProductDetailsScreen
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.main.MainScreen
import com.example.ui.screens.orders.OrdersScreen
import com.example.ui.screens.placeholder.PlaceholderScreen
import com.example.ui.screens.profile.ProfileCompletionScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.seller.MyListingsScreen
import com.example.ui.screens.seller.PostListingScreen
import com.example.ui.screens.seller.SellerHubScreen
import com.example.ui.screens.verification.StudentVerificationScreen
import com.example.ui.screens.welcome.WelcomeScreen
import com.example.ui.viewmodels.MarketplaceViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavigation(
    navController: NavHostController,
    authRepository: AuthRepository,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionState by authRepository.sessionState.collectAsState()
    val marketplaceViewModel: MarketplaceViewModel = viewModel()
    val focusManager = LocalFocusManager.current

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ ->
            focusManager.clearFocus(force = true)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    val startDestination = remember {
        if (authRepository.sessionState.value is SessionState.Authenticated) {
            NavRoutes.MainHost.route
        } else {
            NavRoutes.Welcome.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Welcome / Onboarding
        composable(NavRoutes.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) },
                onNavigateToSignUp = { navController.navigate(NavRoutes.SignUp.route) },
                onNavigateToMarketplace = {
                    navController.navigate(NavRoutes.MainHost.route) {
                        popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                    }
                },
                onToggleTheme = onToggleTheme
            )
        }

        // Login
        composable(NavRoutes.Login.route) {
            LoginScreen(
                authRepository = authRepository,
                onNavigateBack = { navController.popBackStack() },
                onLoginSuccess = {
                    val session = (authRepository.sessionState.value as? SessionState.Authenticated)?.session
                    if (session != null && !session.isProfileComplete) {
                        navController.navigate(NavRoutes.ProfileCompletion.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoutes.MainHost.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(NavRoutes.SignUp.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(NavRoutes.ForgotPassword.route)
                }
            )
        }

        // Forgot Password
        composable(NavRoutes.ForgotPassword.route) {
            ForgotPasswordScreen(
                authRepository = authRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }

        // Sign Up
        composable(NavRoutes.SignUp.route) {
            SignUpScreen(
                authRepository = authRepository,
                onNavigateBack = { navController.popBackStack() },
                onSignUpSuccess = {
                    val session = (authRepository.sessionState.value as? SessionState.Authenticated)?.session
                    if (session != null && !session.isProfileComplete) {
                        navController.navigate(NavRoutes.ProfileCompletion.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoutes.MainHost.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        // Main Application Hub (with bottom tabs)
        composable(NavRoutes.MainHost.route) {
            MainScreen(
                authRepository = authRepository,
                onNavigateToSearch = { navController.navigate(NavRoutes.Search.route) },
                onNavigateToCategory = { categoryId ->
                    marketplaceViewModel.selectCategory(categoryId)
                    navController.navigate(NavRoutes.Search.route)
                },
                onNavigateToProductDetails = { listingId ->
                    navController.navigate(NavRoutes.ProductDetails.createRoute(listingId))
                },
                onNavigateToVerification = { navController.navigate(NavRoutes.StudentVerification.route) },
                onNavigateToSellerHub = { navController.navigate(NavRoutes.SellerHub.route) },
                onNavigateToAdminDashboard = { navController.navigate(NavRoutes.AdminDashboard.route) },
                onNavigateToNotifications = { navController.navigate(NavRoutes.Notifications.route) },
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) },
                onToggleTheme = onToggleTheme,
                onSetThemeMode = onSetThemeMode,
                onNavigateToProfileCompletion = { navController.navigate(NavRoutes.ProfileCompletion.route) },
                onNavigateToCart = { navController.navigate(NavRoutes.Cart.route) },
                onNavigateToCreateListing = { navController.navigate(NavRoutes.CreateListing.route) },
                onNavigateToEditListing = { id -> navController.navigate(NavRoutes.EditListing.createRoute(id)) },
                onNavigateToMyListings = { navController.navigate(NavRoutes.MyListings.route) },
                onNavigateToOrders = { navController.navigate(NavRoutes.Orders.createRoute("purchases")) }
            )
        }

        // Product Details Screen
        composable(NavRoutes.ProductDetails.route) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            ProductDetailsScreen(
                listingId = listingId,
                authRepository = authRepository,
                marketplaceViewModel = marketplaceViewModel,
                onNavigateBack = { navController.popBackStack() },
                onContactSeller = { sellerId ->
                    navController.navigate(NavRoutes.Messages.route)
                },
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) },
                onNavigateToSignUp = { navController.navigate(NavRoutes.SignUp.route) },
                onNavigateToEditListing = { id ->
                    navController.navigate(NavRoutes.EditListing.createRoute(id))
                },
                onNavigateToCart = { navController.navigate(NavRoutes.Cart.route) },
                onNavigateToOrders = { navController.navigate(NavRoutes.Orders.createRoute("purchases")) }
            )
        }

        // Categories Catalog Screen
        composable(NavRoutes.Categories.route) {
            CategoriesScreen(
                marketplaceViewModel = marketplaceViewModel,
                onCategoryClick = { categoryId ->
                    marketplaceViewModel.selectCategory(categoryId)
                    navController.navigate(NavRoutes.Search.route)
                }
            )
        }

        // Cart Screen
        composable(NavRoutes.Cart.route) {
            CartScreen(
                authRepository = authRepository,
                marketplaceViewModel = marketplaceViewModel,
                onExploreMarketplace = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) },
                onNavigateToSignUp = { navController.navigate(NavRoutes.SignUp.route) },
                onNavigateToProductDetails = { listingId ->
                    navController.navigate(NavRoutes.ProductDetails.createRoute(listingId))
                },
                onNavigateToOrders = { navController.navigate(NavRoutes.Orders.createRoute("purchases")) }
            )
        }

        // Search Screen
        composable(NavRoutes.Search.route) {
            SearchScreen(
                marketplaceViewModel = marketplaceViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProductDetails = { listingId ->
                    navController.navigate(NavRoutes.ProductDetails.createRoute(listingId))
                },
                authRepository = authRepository,
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) },
                onNavigateToSignUp = { navController.navigate(NavRoutes.SignUp.route) }
            )
        }

        // Notifications Screen
        composable(NavRoutes.Notifications.route) {
            PlaceholderScreen(
                title = "Notifications",
                subtitle = "Subscribed to realtime notifications table in Supabase. Ready for Phase B.",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Favorites Screen
        composable(NavRoutes.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProductDetails = { listingId ->
                    navController.navigate(NavRoutes.ProductDetails.createRoute(listingId))
                },
                onExploreMarketplace = {
                    navController.navigate(NavRoutes.MainHost.route) {
                        popUpTo(NavRoutes.MainHost.route) { inclusive = true }
                    }
                },
                authRepository = authRepository,
                marketplaceViewModel = marketplaceViewModel,
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) },
                onNavigateToSignUp = { navController.navigate(NavRoutes.SignUp.route) }
            )
        }

        // Student Verification Screen
        composable(NavRoutes.StudentVerification.route) {
            StudentVerificationScreen(
                authRepository = authRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) }
            )
        }

        // Profile Completion Screen
        composable(NavRoutes.ProfileCompletion.route) {
            ProfileCompletionScreen(
                authRepository = authRepository,
                onProfileComplete = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(NavRoutes.MainHost.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onSkipOrCancel = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(NavRoutes.MainHost.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Seller Hub
        composable(NavRoutes.SellerHub.route) {
            SellerHubScreen(
                authRepository = authRepository,
                marketplaceViewModel = marketplaceViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateListing = { navController.navigate(NavRoutes.CreateListing.route) },
                onNavigateToEditListing = { listingId ->
                    navController.navigate(NavRoutes.EditListing.createRoute(listingId))
                },
                onNavigateToProductDetails = { listingId ->
                    navController.navigate(NavRoutes.ProductDetails.createRoute(listingId))
                },
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) },
                onNavigateToMyListings = { navController.navigate(NavRoutes.MyListings.route) },
                onNavigateToSellerOrders = { navController.navigate(NavRoutes.Orders.createRoute("sales")) }
            )
        }

        // Campus Handovers & Orders Screen
        composable(
            route = NavRoutes.Orders.route,
            arguments = listOf(
                navArgument("initialTab") {
                    type = NavType.StringType
                    defaultValue = "purchases"
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("initialTab") ?: "purchases"
            OrdersScreen(
                authRepository = authRepository,
                initialTab = initialTab,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProductDetails = { listingId ->
                    navController.navigate(NavRoutes.ProductDetails.createRoute(listingId))
                },
                onNavigateToMarketplace = {
                    navController.navigate(NavRoutes.MainHost.route) {
                        popUpTo(NavRoutes.MainHost.route) { inclusive = true }
                    }
                },
                onNavigateToCreateListing = {
                    navController.navigate(NavRoutes.CreateListing.route)
                },
                onContactUser = { _ ->
                    navController.navigate(NavRoutes.Messages.route)
                }
            )
        }

        // Seller Orders route alias
        composable(NavRoutes.SellerOrders.route) {
            OrdersScreen(
                authRepository = authRepository,
                initialTab = "sales",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProductDetails = { listingId ->
                    navController.navigate(NavRoutes.ProductDetails.createRoute(listingId))
                },
                onNavigateToMarketplace = {
                    navController.navigate(NavRoutes.MainHost.route) {
                        popUpTo(NavRoutes.MainHost.route) { inclusive = true }
                    }
                },
                onNavigateToCreateListing = {
                    navController.navigate(NavRoutes.CreateListing.route)
                },
                onContactUser = { _ ->
                    navController.navigate(NavRoutes.Messages.route)
                }
            )
        }

        // My Listings Screen
        composable(NavRoutes.MyListings.route) {
            MyListingsScreen(
                authRepository = authRepository,
                marketplaceViewModel = marketplaceViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateListing = { navController.navigate(NavRoutes.CreateListing.route) },
                onNavigateToEditListing = { listingId ->
                    navController.navigate(NavRoutes.EditListing.createRoute(listingId))
                },
                onNavigateToProductDetails = { listingId ->
                    navController.navigate(NavRoutes.ProductDetails.createRoute(listingId))
                },
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) }
            )
        }

        // Create Listing
        composable(NavRoutes.CreateListing.route) {
            PostListingScreen(
                authRepository = authRepository,
                marketplaceViewModel = marketplaceViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Edit Listing
        composable(NavRoutes.EditListing.route) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            PostListingScreen(
                authRepository = authRepository,
                marketplaceViewModel = marketplaceViewModel,
                listingId = listingId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Admin Dashboard
        composable(NavRoutes.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
