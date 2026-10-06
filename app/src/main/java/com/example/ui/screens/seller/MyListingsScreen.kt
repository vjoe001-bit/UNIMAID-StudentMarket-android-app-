package com.example.ui.screens.seller

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidPillButton
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.network.SupabaseResult
import com.example.core.session.SessionState
import com.example.core.theme.AccentCoral
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidGold
import com.example.core.theme.UnimaidGoldContainer
import com.example.core.theme.UnimaidGoldOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.core.theme.WarningAmber
import com.example.core.utils.MarketplaceUtils
import com.example.data.models.Listing
import com.example.data.repository.AuthRepository
import com.example.ui.viewmodels.MarketplaceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Real My Listings & Listing Management Screen.
 *
 * Provides real student listing management with authenticated Supabase queries,
 * data isolation per student, status updates (ACTIVE, UNAVAILABLE / DEACTIVATED, SOLD),
 * deletion safety, pull-to-refresh, inventory metrics, and direct transitions to
 * edit and product details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    authRepository: AuthRepository,
    marketplaceViewModel: MarketplaceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionState by authRepository.sessionState.collectAsState()
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId

    val uiState by marketplaceViewModel.uiState.collectAsState()

    // Filter & Search states
    var selectedFilterTab by remember { mutableStateOf("ALL") } // ALL, ACTIVE, SOLD, DEACTIVATED
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }

    // Dialog states
    var listingToDelete by remember { mutableStateOf<Listing?>(null) }
    var listingToDeactivate by remember { mutableStateOf<Listing?>(null) }
    var isPerformingAction by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        if (!currentUserId.isNullOrBlank()) {
            marketplaceViewModel.loadSellerListings(currentUserId)
        }
    }

    // Unauthenticated State
    if (currentUserId.isNullOrBlank()) {
        Scaffold(
            topBar = {
                UnimaidTopAppBar(
                    title = "My Listings",
                    onBackClick = onNavigateBack
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(AppSpacing.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UnimaidEmptyState(
                    title = "Sign In Required",
                    subtitle = "Sign in to your UNIMAID student account to manage your listings, update prices, and mark items sold.",
                    icon = Icons.Default.Storefront,
                    actionText = "Sign In Now",
                    onActionClick = onNavigateToLogin
                )
            }
        }
        return
    }

    // Compute status counts
    val allListings = uiState.sellerListings
    val activeListings = allListings.filter { it.status.equals("ACTIVE", ignoreCase = true) }
    val soldListings = allListings.filter { it.status.equals("SOLD", ignoreCase = true) }
    val deactivatedListings = allListings.filter {
        it.status.equals("UNAVAILABLE", ignoreCase = true) || it.status.equals("DEACTIVATED", ignoreCase = true)
    }

    val activeCount = activeListings.size
    val soldCount = soldListings.size
    val deactivatedCount = deactivatedListings.size
    val totalActiveValue = activeListings.sumOf { it.price * it.quantity }

    // Filter by tab and search
    val filteredListings = remember(allListings, selectedFilterTab, searchQuery) {
        val tabFiltered = when (selectedFilterTab) {
            "ACTIVE" -> activeListings
            "SOLD" -> soldListings
            "DEACTIVATED" -> deactivatedListings
            else -> allListings
        }

        if (searchQuery.isBlank()) {
            tabFiltered
        } else {
            val queryClean = searchQuery.trim().lowercase(Locale.ROOT)
            tabFiltered.filter { listing ->
                listing.displayTitle.lowercase(Locale.ROOT).contains(queryClean) ||
                    (listing.description?.lowercase(Locale.ROOT)?.contains(queryClean) == true) ||
                    listing.displayLocation.lowercase(Locale.ROOT).contains(queryClean)
            }
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            UnimaidTopAppBar(
                title = "My Listings",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                marketplaceViewModel.loadSellerListings(currentUserId)
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.testTag("my_listings_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh listings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = onNavigateToCreateListing,
                        modifier = Modifier.testTag("my_listings_post_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Post Item",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateListing,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("my_listings_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Post Item")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Post Item", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing || uiState.isLoadingSellerListings,
            state = pullToRefreshState,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    marketplaceViewModel.loadSellerListings(currentUserId)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    // Inventory Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        // Active Count
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = AppRadius.md,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(AppSpacing.md)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$activeCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Sold Count
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = AppRadius.md,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(AppSpacing.md)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = VerifiedGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sold", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$soldCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Active Inventory Value
                        Card(
                            modifier = Modifier.weight(1.3f),
                            shape = AppRadius.md,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(AppSpacing.md)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Paid,
                                        contentDescription = null,
                                        tint = UnimaidGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active Value", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = MarketplaceUtils.formatNaira(totalActiveValue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Search Bar inside My Listings
                if (allListings.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("my_listings_search_input"),
                            placeholder = { Text("Search your listings...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = AppRadius.full
                        )
                    }
                }

                // Filter Tabs (All, Active, Sold, Deactivated)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilterTab == "ALL",
                            onClick = { selectedFilterTab = "ALL" },
                            label = { Text("All (${allListings.size})") }
                        )
                        FilterChip(
                            selected = selectedFilterTab == "ACTIVE",
                            onClick = { selectedFilterTab = "ACTIVE" },
                            label = { Text("Active ($activeCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VerifiedGreenContainer,
                                selectedLabelColor = VerifiedGreenOnContainer
                            )
                        )
                        FilterChip(
                            selected = selectedFilterTab == "SOLD",
                            onClick = { selectedFilterTab = "SOLD" },
                            label = { Text("Sold ($soldCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UnimaidGoldContainer,
                                selectedLabelColor = UnimaidGoldOnContainer
                            )
                        )
                        FilterChip(
                            selected = selectedFilterTab == "DEACTIVATED",
                            onClick = { selectedFilterTab = "DEACTIVATED" },
                            label = { Text("Inactive ($deactivatedCount)") }
                        )
                    }
                }

                // List Items or Loading or Empty State
                if (uiState.isLoadingSellerListings && allListings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (filteredListings.isEmpty()) {
                    item {
                        val emptyTitle = when {
                            searchQuery.isNotBlank() -> "No matching listings"
                            selectedFilterTab == "ACTIVE" -> "No active listings"
                            selectedFilterTab == "SOLD" -> "No sold items yet"
                            selectedFilterTab == "DEACTIVATED" -> "No deactivated listings"
                            else -> "You haven't posted anything yet"
                        }
                        val emptySubtitle = when {
                            searchQuery.isNotBlank() -> "No items matched \"$searchQuery\". Try a different search."
                            selectedFilterTab == "ACTIVE" -> "Items you publish or reactivate will appear here as available."
                            selectedFilterTab == "SOLD" -> "Items you mark as sold will be tracked here for your records."
                            selectedFilterTab == "DEACTIVATED" -> "Listings you temporarily deactivate will appear here."
                            else -> "Turn textbooks, electronics, appliances, and campus essentials into cash right on campus."
                        }

                        UnimaidEmptyState(
                            title = emptyTitle,
                            subtitle = emptySubtitle,
                            icon = Icons.Default.Storefront,
                            actionText = if (selectedFilterTab == "ALL" || allListings.isEmpty()) "Post an Item" else null,
                            onActionClick = if (selectedFilterTab == "ALL" || allListings.isEmpty()) onNavigateToCreateListing else null
                        )
                    }
                } else {
                    items(filteredListings, key = { it.id }) { listing ->
                        MyListingCard(
                            listing = listing,
                            onView = { onNavigateToProductDetails(listing.id) },
                            onEdit = { onNavigateToEditListing(listing.id) },
                            onToggleSold = {
                                coroutineScope.launch {
                                    val newStatus = if (listing.status.equals("SOLD", ignoreCase = true)) "ACTIVE" else "SOLD"
                                    isPerformingAction = true
                                    marketplaceViewModel.updateListingStatus(listing.id, currentUserId, newStatus)
                                    isPerformingAction = false
                                }
                            },
                            onDeactivate = { listingToDeactivate = listing },
                            onReactivate = {
                                coroutineScope.launch {
                                    isPerformingAction = true
                                    val res = marketplaceViewModel.updateListingStatus(listing.id, currentUserId, "ACTIVE")
                                    isPerformingAction = false
                                    if (res is SupabaseResult.Success) {
                                        Toast.makeText(context, "Listing reactivated!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = { listingToDelete = listing },
                            isActionEnabled = !isPerformingAction
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }
        }
    }

    // Deactivate Confirmation Dialog
    listingToDeactivate?.let { listing ->
        AlertDialog(
            onDismissRequest = { listingToDeactivate = null },
            icon = { Icon(Icons.Default.PauseCircle, contentDescription = null, tint = WarningAmber) },
            title = { Text("Deactivate Listing?") },
            text = {
                Text(
                    "\"${listing.displayTitle}\" will be hidden from the campus marketplace. You can reactivate it at any time from My Listings."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDeactivate = listing
                        listingToDeactivate = null
                        coroutineScope.launch {
                            isPerformingAction = true
                            val res = marketplaceViewModel.updateListingStatus(toDeactivate.id, currentUserId, "UNAVAILABLE")
                            isPerformingAction = false
                            if (res is SupabaseResult.Success) {
                                Toast.makeText(context, "Listing deactivated.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Deactivate", color = WarningAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { listingToDeactivate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    listingToDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { listingToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Permanently Delete Listing?") },
            text = {
                Text(
                    "Are you sure you want to delete \"${listing.displayTitle}\"? All photos and listing data will be permanently removed. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = listing
                        listingToDelete = null
                        coroutineScope.launch {
                            isPerformingAction = true
                            when (val res = marketplaceViewModel.deleteProductListing(toDelete.id, currentUserId)) {
                                is SupabaseResult.Success -> {
                                    Toast.makeText(context, "Listing deleted permanently.", Toast.LENGTH_SHORT).show()
                                }
                                is SupabaseResult.Error -> {
                                    Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_LONG).show()
                                }
                                else -> {}
                            }
                            isPerformingAction = false
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { listingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Individual Student Listing Card for My Listings Screen.
 */
@Composable
private fun MyListingCard(
    listing: Listing,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onToggleSold: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
    isActionEnabled: Boolean = true
) {
    val isSold = listing.status.equals("SOLD", ignoreCase = true)
    val isDeactivated = listing.status.equals("UNAVAILABLE", ignoreCase = true) ||
        listing.status.equals("DEACTIVATED", ignoreCase = true)
    val isActive = !isSold && !isDeactivated

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .testTag("my_listing_card_${listing.id}"),
        shape = AppRadius.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Main Top Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Image Box with photo counter
                val coverUrl = listing.images.firstOrNull()?.imageUrl
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(AppRadius.md)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = listing.displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                        )
                    }

                    // Photo count badge
                    if (listing.images.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${listing.images.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                // Listing Info Column
                Column(modifier = Modifier.weight(1f)) {
                    // Status Badge & Condition Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status badge
                        Surface(
                            shape = AppRadius.full,
                            color = when {
                                isSold -> UnimaidGoldContainer
                                isDeactivated -> MaterialTheme.colorScheme.surfaceVariant
                                else -> VerifiedGreenContainer
                            }
                        ) {
                            Text(
                                text = when {
                                    isSold -> "SOLD"
                                    isDeactivated -> "DEACTIVATED"
                                    else -> "ACTIVE"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSold -> UnimaidGoldOnContainer
                                    isDeactivated -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> VerifiedGreenOnContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Condition badge
                        Text(
                            text = listing.condition.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = listing.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Price & Stock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = MarketplaceUtils.formatNaira(listing.price),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Qty: ${listing.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Campus Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = listing.displayLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                IconButton(
                    onClick = onDelete,
                    enabled = isActionEnabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Listing",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Deactivate / Reactivate
                    if (isActive) {
                        UnimaidPillButton(
                            text = "Deactivate",
                            onClick = onDeactivate,
                            enabled = isActionEnabled,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(34.dp)
                        )
                    } else if (isDeactivated) {
                        UnimaidPillButton(
                            text = "Reactivate",
                            onClick = onReactivate,
                            enabled = isActionEnabled,
                            containerColor = VerifiedGreenContainer,
                            contentColor = VerifiedGreenOnContainer,
                            modifier = Modifier.height(34.dp)
                        )
                    }

                    // Mark Sold / Mark Active
                    if (!isDeactivated) {
                        UnimaidPillButton(
                            text = if (isSold) "Mark Active" else "Mark Sold",
                            onClick = onToggleSold,
                            enabled = isActionEnabled,
                            containerColor = if (isSold) VerifiedGreenContainer else UnimaidGoldContainer,
                            contentColor = if (isSold) VerifiedGreenOnContainer else UnimaidGoldOnContainer,
                            modifier = Modifier.height(34.dp)
                        )
                    }

                    // Edit
                    UnimaidPillButton(
                        text = "Edit",
                        onClick = onEdit,
                        enabled = isActionEnabled,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }
    }
}
