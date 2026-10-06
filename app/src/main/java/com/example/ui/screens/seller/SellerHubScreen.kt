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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerHubScreen(
    authRepository: AuthRepository,
    marketplaceViewModel: MarketplaceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateListing: () -> Unit,
    onNavigateToEditListing: (String) -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToMyListings: (() -> Unit)? = null,
    onNavigateToSellerOrders: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionState by authRepository.sessionState.collectAsState()
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId

    val uiState by marketplaceViewModel.uiState.collectAsState()
    var filterTab by remember { mutableStateOf("ALL") } // ALL, ACTIVE, SOLD, DEACTIVATED
    var listingToDelete by remember { mutableStateOf<Listing?>(null) }
    var listingToDeactivate by remember { mutableStateOf<Listing?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        if (!currentUserId.isNullOrBlank()) {
            marketplaceViewModel.loadSellerListings(currentUserId)
        }
    }

    if (currentUserId.isNullOrBlank()) {
        Scaffold(
            topBar = {
                UnimaidTopAppBar(
                    title = "Seller Hub",
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
                    subtitle = "You must be signed in to your UNIMAID student account to post items and manage your campus listings.",
                    icon = Icons.Default.Storefront,
                    actionText = "Sign In Now",
                    onActionClick = onNavigateToLogin
                )
            }
        }
        return
    }

    val allListings = uiState.sellerListings
    val activeListings = allListings.filter { it.status.equals("ACTIVE", ignoreCase = true) }
    val soldListings = allListings.filter { it.status.equals("SOLD", ignoreCase = true) }
    val deactivatedListings = allListings.filter {
        it.status.equals("UNAVAILABLE", ignoreCase = true) || it.status.equals("DEACTIVATED", ignoreCase = true)
    }

    val activeCount = activeListings.size
    val soldCount = soldListings.size
    val deactivatedCount = deactivatedListings.size
    val totalValue = activeListings.sumOf { it.price * it.quantity }

    val filteredListings = remember(allListings, filterTab) {
        when (filterTab) {
            "ACTIVE" -> activeListings
            "SOLD" -> soldListings
            "DEACTIVATED" -> deactivatedListings
            else -> allListings
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            UnimaidTopAppBar(
                title = "Seller Hub",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                marketplaceViewModel.loadSellerListings(currentUserId)
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = onNavigateToCreateListing,
                        modifier = Modifier.testTag("hub_post_item_top_button")
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
                modifier = Modifier.testTag("hub_fab_post_listing")
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
                    // Guidelines Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppRadius.md,
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
                                    text = "Campus Seller Guidelines",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = UnimaidGoldOnContainer
                                )
                                Text(
                                    text = "Physical in-person campus handover. No online transaction fees. Meet only in daylight in safe campus public areas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UnimaidGoldOnContainer
                                )
                            }
                        }
                    }
                }

                // Stats Cards Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        // Active Listings Card
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
                                    Spacer(modifier = Modifier.width(6.dp))
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

                        // Sold Items Card
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
                                    Spacer(modifier = Modifier.width(6.dp))
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

                        // Total Value Card
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Active Value", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = MarketplaceUtils.formatNaira(totalValue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Campus Buyer Requests & Sales Orders Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSellerOrders?.invoke() }
                            .testTag("hub_buyer_requests_card"),
                        shape = AppRadius.md,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.sm))
                                Column {
                                    Text(
                                        text = "Buyer Purchase Requests",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Review incoming student orders, coordinate meetups & confirm physical payment",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Filter Tabs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterTab == "ALL",
                            onClick = { filterTab = "ALL" },
                            label = { Text("All (${allListings.size})") }
                        )
                        FilterChip(
                            selected = filterTab == "ACTIVE",
                            onClick = { filterTab = "ACTIVE" },
                            label = { Text("Active ($activeCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VerifiedGreenContainer,
                                selectedLabelColor = VerifiedGreenOnContainer
                            )
                        )
                        FilterChip(
                            selected = filterTab == "SOLD",
                            onClick = { filterTab = "SOLD" },
                            label = { Text("Sold ($soldCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UnimaidGoldContainer,
                                selectedLabelColor = UnimaidGoldOnContainer
                            )
                        )
                        FilterChip(
                            selected = filterTab == "DEACTIVATED",
                            onClick = { filterTab = "DEACTIVATED" },
                            label = { Text("Inactive ($deactivatedCount)") }
                        )
                    }
                }

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
                        UnimaidEmptyState(
                            title = if (filterTab == "ALL") "No Listings Yet"
                            else if (filterTab == "ACTIVE") "No Active Listings"
                            else if (filterTab == "SOLD") "No Sold Items Yet"
                            else "No Inactive Listings",
                            subtitle = if (filterTab == "ALL") "Turn your textbooks, appliances, gadgets, or room essentials into cash right on campus."
                            else "Items you mark as sold, active, or deactivated will appear here.",
                            icon = Icons.Default.Storefront,
                            actionText = "Post an Item Now",
                            onActionClick = onNavigateToCreateListing
                        )
                    }
                } else {
                    items(filteredListings, key = { it.id }) { listing ->
                        SellerListingItem(
                            listing = listing,
                            onClick = { onNavigateToProductDetails(listing.id) },
                            onEdit = { onNavigateToEditListing(listing.id) },
                            onToggleSold = {
                                coroutineScope.launch {
                                    val newStatus = if (listing.status.equals("SOLD", ignoreCase = true)) "ACTIVE" else "SOLD"
                                    marketplaceViewModel.updateListingStatus(listing.id, currentUserId, newStatus)
                                }
                            },
                            onDeactivate = { listingToDeactivate = listing },
                            onReactivate = {
                                coroutineScope.launch {
                                    val res = marketplaceViewModel.updateListingStatus(listing.id, currentUserId, "ACTIVE")
                                    if (res is SupabaseResult.Success) {
                                        Toast.makeText(context, "Listing reactivated!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = { listingToDelete = listing }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
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
            text = { Text("\"${listing.displayTitle}\" will be hidden from the campus marketplace. You can reactivate it at any time.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDeactivate = listing
                        listingToDeactivate = null
                        coroutineScope.launch {
                            val res = marketplaceViewModel.updateListingStatus(toDeactivate.id, currentUserId, "UNAVAILABLE")
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
            title = { Text("Delete Listing?") },
            text = { Text("Are you sure you want to remove \"${listing.displayTitle}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = listing
                        listingToDelete = null
                        coroutineScope.launch {
                            when (val res = marketplaceViewModel.deleteProductListing(toDelete.id, currentUserId)) {
                                is SupabaseResult.Success -> {
                                    Toast.makeText(context, "Listing deleted.", Toast.LENGTH_SHORT).show()
                                }
                                is SupabaseResult.Error -> {
                                    Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_LONG).show()
                                }
                                else -> {}
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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

@Composable
private fun SellerListingItem(
    listing: Listing,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleSold: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit
) {
    val isSold = listing.status.equals("SOLD", ignoreCase = true)
    val isDeactivated = listing.status.equals("UNAVAILABLE", ignoreCase = true) ||
        listing.status.equals("DEACTIVATED", ignoreCase = true)
    val isActive = !isSold && !isDeactivated

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("seller_listing_card_${listing.id}"),
        shape = AppRadius.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Image Thumbnail
                val coverUrl = listing.images.firstOrNull()?.imageUrl
                Box(
                    modifier = Modifier
                        .size(80.dp)
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
                                .size(32.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Badge
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

                        // Condition
                        Text(
                            text = listing.condition.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = listing.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = MarketplaceUtils.formatNaira(listing.price),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
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
                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
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
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(36.dp)
                        )
                    } else if (isDeactivated) {
                        UnimaidPillButton(
                            text = "Reactivate",
                            onClick = onReactivate,
                            containerColor = VerifiedGreenContainer,
                            contentColor = VerifiedGreenOnContainer,
                            modifier = Modifier.height(36.dp)
                        )
                    }

                    // Toggle Sold / Active
                    if (!isDeactivated) {
                        UnimaidPillButton(
                            text = if (isSold) "Mark Active" else "Mark Sold",
                            onClick = onToggleSold,
                            containerColor = if (isSold) VerifiedGreenContainer else UnimaidGoldContainer,
                            contentColor = if (isSold) VerifiedGreenOnContainer else UnimaidGoldOnContainer,
                            modifier = Modifier.height(36.dp)
                        )
                    }

                    // Edit Button
                    UnimaidPillButton(
                        text = "Edit",
                        onClick = onEdit,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.height(36.dp)
                    )
                }
            }
        }
    }
}
