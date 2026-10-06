package com.example.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.branding.BrandConfig
import com.example.core.designsystem.components.UnimaidAuthPromptDialog
import com.example.core.designsystem.components.UnimaidCategoryPill
import com.example.core.designsystem.components.UnimaidHorizontalListingCard
import com.example.core.designsystem.components.UnimaidProductCard
import com.example.core.designsystem.components.UnimaidSearchBar
import com.example.core.session.SessionState
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.LocalThemeMode
import com.example.core.theme.ThemeMode
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.UnimaidGold
import com.example.core.theme.UnimaidGoldContainer
import com.example.core.theme.UnimaidGoldOnContainer
import com.example.data.models.ItemCondition
import com.example.data.repository.AuthRepository
import com.example.ui.viewmodels.MarketplaceViewModel

/**
 * Modern Home Screen connecting authenticated UNIMAID students to real campus listings.
 * Displays official UNIMAID crest, greeting, cart badge, search bar,
 * real categories from Supabase, featured student listing, and popular recent products.
 */
@Composable
fun HomeScreen(
    authRepository: AuthRepository,
    onNavigateToSearch: () -> Unit,
    onNavigateToMarketplace: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    onNavigateToVerification: () -> Unit,
    onNavigateToSellerHub: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfileCompletion: () -> Unit = {},
    marketplaceViewModel: MarketplaceViewModel = viewModel(),
    onNavigateToCart: (() -> Unit)? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToSignUp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sessionState by authRepository.sessionState.collectAsState()
    val currentTheme = LocalThemeMode.current
    val scrollState = rememberScrollState()

    val uiState by marketplaceViewModel.uiState.collectAsState()
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId

    var showAuthPrompt by remember { mutableStateOf(false) }
    var authPromptAction by remember { mutableStateOf("Sign In Required") }
    var authPromptDesc by remember { mutableStateOf("") }

    val userFullName = when (val s = sessionState) {
        is SessionState.Authenticated -> s.session.fullName.ifBlank { "Student" }
        else -> "UNIMAID Student"
    }

    val isVerified = when (val s = sessionState) {
        is SessionState.Authenticated -> s.session.isVerified
        else -> false
    }

    val isProfileIncomplete = when (val s = sessionState) {
        is SessionState.Authenticated -> !s.session.isProfileComplete
        else -> false
    }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp)
        ) {
            // Top Bar with Official UNIMAID Crest, Greeting & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Official UNIMAID School Logo / Crest
                    Image(
                        painter = painterResource(id = BrandConfig.OFFICIAL_LOGO_RES),
                        contentDescription = "University of Maiduguri Crest",
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("home_school_crest")
                    )

                    Spacer(modifier = Modifier.width(AppSpacing.sm))

                    Column {
                        Text(
                            text = "Hello, $userFullName 👋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = BrandConfig.APP_NAME,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Cart shortcut with badge count
                    if (onNavigateToCart != null) {
                        IconButton(
                            onClick = onNavigateToCart,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("home_cart_button")
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                    }

                    // Theme toggle
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("home_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (currentTheme == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.xs))

                    // Campus notifications
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("home_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Modern Pill Search Bar
            Box(modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)) {
                UnimaidSearchBar(
                    query = "",
                    onQueryChange = {},
                    readOnly = true,
                    onClick = onNavigateToSearch,
                    placeholder = "Search textbooks, hostel gear, laptops..."
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Incomplete Profile Banner
            if (isProfileIncomplete) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenPadding)
                        .clickable { onNavigateToProfileCompletion() }
                        .testTag("home_profile_completion_banner"),
                    shape = AppRadius.lg,
                    colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(UnimaidGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(AppSpacing.md))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Complete Your Profile",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = UnimaidGoldOnContainer
                            )
                            Text(
                                text = "Add your matric number, faculty & department to trade on campus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = UnimaidGoldOnContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }

            // Student Verification Banner (if unverified)
            if (!isVerified) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenPadding)
                        .clickable { onNavigateToVerification() }
                        .testTag("verification_callout_banner"),
                    shape = AppRadius.lg,
                    colors = CardDefaults.cardColors(containerColor = UnimaidBlueContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(AppSpacing.md))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Get UNIMAID Verified",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = UnimaidBlueOnContainer
                            )
                            Text(
                                text = "Upload your UNIMAID student ID card to earn trust badge and sell safely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = UnimaidBlueOnContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))
            }

            // Quick Sell / Post Item Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPadding)
                    .clickable { onNavigateToSellerHub() }
                    .testTag("home_sell_callout_banner"),
                shape = AppRadius.lg,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.md))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sell on UNIMAID Market",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Turn your textbooks, electronics, provisions & room essentials into cash.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Category Chips Row (Horizontal Scroll from Supabase)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(onClick = onNavigateToMarketplace) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                UnimaidCategoryPill(
                    label = "All Items",
                    isSelected = uiState.selectedCategoryId == null,
                    onClick = {
                        marketplaceViewModel.selectCategory(null)
                        onNavigateToMarketplace()
                    }
                )

                if (uiState.categories.isNotEmpty()) {
                    uiState.categories.forEach { category ->
                        UnimaidCategoryPill(
                            label = category.name,
                            isSelected = uiState.selectedCategoryId == category.id,
                            onClick = {
                                marketplaceViewModel.selectCategory(category.id)
                                onNavigateToCategory(category.id)
                            }
                        )
                    }
                } else {
                    val defaultCategories = listOf(
                        "textbooks" to "Textbooks",
                        "hostel" to "Hostel Room",
                        "tech" to "Laptops & Tech",
                        "services" to "Services",
                        "fashion" to "Fashion",
                        "provisions" to "Provisions"
                    )
                    defaultCategories.forEach { (catId, catLabel) ->
                        UnimaidCategoryPill(
                            label = catLabel,
                            isSelected = false,
                            onClick = {
                                marketplaceViewModel.selectCategory(catId)
                                onNavigateToCategory(catId)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Featured Real Listing from Supabase
            val featured = uiState.featuredListing
            if (featured != null) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)) {
                    UnimaidHorizontalListingCard(
                        title = featured.title,
                        categoryName = "${ItemCondition.format(featured.condition)} • ${featured.locationCampus ?: "UNIMAID Campus"}",
                        price = "₦${"%,.0f".format(featured.price)}",
                        imageUrl = featured.images.firstOrNull()?.imageUrl,
                        onClick = { onNavigateToProductDetails(featured.id) },
                        testTag = "home_featured_card"
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.xl))
            } else if (uiState.isLoading) {
                // Skeleton for featured card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .padding(horizontal = AppSpacing.screenPadding)
                        .clip(AppRadius.lg),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {}
                Spacer(modifier = Modifier.height(AppSpacing.xl))
            }

            // Popular Campus Items Section Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Popular on Campus",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(onClick = onNavigateToMarketplace) {
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            // 2-Column Responsive Marketplace Cards Preview from Supabase
            val recentItems = uiState.recentListings.ifEmpty { uiState.marketplaceListings }.take(6)

            if (uiState.isLoading && recentItems.isEmpty()) {
                // Skeleton cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                                .clip(AppRadius.lg),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {}
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                                .clip(AppRadius.lg),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {}
                    }
                }
            } else if (recentItems.isNotEmpty()) {
                val chunks = recentItems.chunked(2)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    chunks.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                            val first = pair[0]
                            val isFavFirst = uiState.favoriteListingIds.contains(first.id)
                            Box(modifier = Modifier.weight(1f)) {
                                UnimaidProductCard(
                                    title = first.title,
                                    price = "₦${"%,.0f".format(first.price)}",
                                    imageUrl = first.images.firstOrNull()?.imageUrl,
                                    condition = ItemCondition.format(first.condition),
                                    isFavorite = isFavFirst,
                                    onFavoriteToggle = {
                                        if (currentUserId.isNullOrBlank()) {
                                            authPromptAction = "Save to Favorites"
                                            authPromptDesc = "Sign in to save this listing to your personal favorites and track price changes."
                                            showAuthPrompt = true
                                        } else {
                                            marketplaceViewModel.toggleFavorite(currentUserId, first.id)
                                        }
                                    },
                                    onClick = { onNavigateToProductDetails(first.id) },
                                    onAddClick = {
                                        if (currentUserId.isNullOrBlank()) {
                                            authPromptAction = "Add to Cart"
                                            authPromptDesc = "Sign in to reserve items and arrange a safe in-person campus handover."
                                            showAuthPrompt = true
                                        } else {
                                            marketplaceViewModel.addToCart(currentUserId, first.id)
                                        }
                                    },
                                    testTag = "home_product_${first.id}"
                                )
                            }

                            if (pair.size > 1) {
                                val second = pair[1]
                                val isFavSecond = uiState.favoriteListingIds.contains(second.id)
                                Box(modifier = Modifier.weight(1f)) {
                                    UnimaidProductCard(
                                        title = second.title,
                                        price = "₦${"%,.0f".format(second.price)}",
                                        imageUrl = second.images.firstOrNull()?.imageUrl,
                                        condition = ItemCondition.format(second.condition),
                                        isFavorite = isFavSecond,
                                        onFavoriteToggle = {
                                            if (currentUserId.isNullOrBlank()) {
                                                authPromptAction = "Save to Favorites"
                                                authPromptDesc = "Sign in to save this listing to your personal favorites and track price changes."
                                                showAuthPrompt = true
                                            } else {
                                                marketplaceViewModel.toggleFavorite(currentUserId, second.id)
                                            }
                                        },
                                        onClick = { onNavigateToProductDetails(second.id) },
                                        onAddClick = {
                                            if (currentUserId.isNullOrBlank()) {
                                                authPromptAction = "Add to Cart"
                                                authPromptDesc = "Sign in to reserve items and arrange a safe in-person campus handover."
                                                showAuthPrompt = true
                                            } else {
                                                marketplaceViewModel.addToCart(currentUserId, second.id)
                                            }
                                        },
                                        testTag = "home_product_${second.id}"
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // Friendly card inviting students to explore or sell
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenPadding),
                    shape = AppRadius.lg,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome to UNIMAID Marketplace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Text(
                            text = "Connecting verified University of Maiduguri students for safe on-campus trading.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.md))
                        TextButton(onClick = onNavigateToMarketplace) {
                            Text("Browse All Listings")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // Campus Safety & Physical Handover Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPadding),
                shape = AppRadius.lg,
                colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer)
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        tint = UnimaidGoldOnContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Column {
                        Text(
                            text = "Campus Meetup & Cash Policy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = UnimaidGoldOnContainer
                        )
                        Text(
                            text = BrandConfig.BUSINESS_POLICY_NO_ONLINE_PAYMENT,
                            style = MaterialTheme.typography.bodySmall,
                            color = UnimaidGoldOnContainer
                        )
                    }
                }
            }
        }
    }
}
