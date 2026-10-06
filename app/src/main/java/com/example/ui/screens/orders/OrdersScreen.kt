package com.example.ui.screens.orders

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.data.models.Order
import com.example.data.repository.AuthRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.OrderRepositoryImpl
import kotlinx.coroutines.launch

val UNIMAID_CAMPUS_LOCATIONS = listOf(
    "UNIMAID Ramat Library (Main Walkway)",
    "Senate Building Ground Floor",
    "Student Center Complex / Cafeteria",
    "Faculty of Science Lecture Theatres",
    "Faculty of Agriculture Quad",
    "Faculty of Engineering Complex",
    "Faculty of Arts & Education Walkway",
    "Faculty of Social Sciences / Law",
    "Hostel Common Area (Daylight Hours)",
    "Campus Gate 1 Security Area"
)

/**
 * Real Purchase Requests and Campus Handover Management Screen for UNIMAID StudentMarket.
 * Under University policy:
 * - NO online payment gateway is processed.
 * - This screen manages purchase intent, seller acceptance, meeting arrangement, and handover verification.
 * - Payment is executed in-person/physically between students after inspecting goods.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: String = "purchases",
    orderRepository: OrderRepository = remember { OrderRepositoryImpl() },
    onNavigateToProductDetails: ((String) -> Unit)? = null,
    onNavigateToMarketplace: (() -> Unit)? = null,
    onNavigateToCreateListing: (() -> Unit)? = null,
    onContactUser: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionState by authRepository.sessionState.collectAsState()
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId

    // Tab Selection: 0 = My Purchases (Buyer), 1 = Sales Requests (Seller)
    var selectedTab by remember { mutableIntStateOf(if (initialTab.equals("sales", ignoreCase = true)) 1 else 0) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    var buyerOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var sellerOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Dialog States
    var orderToArrangeMeeting by remember { mutableStateOf<Order?>(null) }
    var orderToDecline by remember { mutableStateOf<Order?>(null) }
    var orderToCancel by remember { mutableStateOf<Order?>(null) }
    var orderToComplete by remember { mutableStateOf<Order?>(null) }

    // Action In-Progress state
    var actionInProgressOrderId by remember { mutableStateOf<String?>(null) }

    fun loadAllOrders() {
        if (currentUserId.isNullOrBlank()) {
            isLoading = false
            return
        }
        coroutineScope.launch {
            isLoading = true
            when (val buyerRes = orderRepository.getOrdersForUser(currentUserId, asBuyerOnly = true)) {
                is SupabaseResult.Success -> buyerOrders = buyerRes.data
                else -> {}
            }
            when (val sellerRes = orderRepository.getOrdersForUser(currentUserId, asSellerOnly = true)) {
                is SupabaseResult.Success -> sellerOrders = sellerRes.data
                else -> {}
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(currentUserId) {
        loadAllOrders()
    }

    val pullToRefreshState = rememberPullToRefreshState()

    val currentList = if (selectedTab == 0) buyerOrders else sellerOrders
    val filteredList = remember(currentList, selectedStatusFilter) {
        if (selectedStatusFilter == "ALL") {
            currentList
        } else {
            currentList.filter { order ->
                when (selectedStatusFilter) {
                    "PENDING" -> order.status.equals("PENDING", ignoreCase = true)
                    "ACCEPTED" -> order.status.equals("ACCEPTED", ignoreCase = true)
                    "MEETING_ARRANGED" -> order.status.equals("MEETING_ARRANGED", ignoreCase = true)
                    "COMPLETED" -> order.status.equals("COMPLETED", ignoreCase = true)
                    "CANCELLED" -> order.status.equals("CANCELLED", ignoreCase = true) || order.status.equals("DECLINED", ignoreCase = true)
                    else -> true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            UnimaidTopAppBar(
                title = "Campus Handovers",
                subtitle = "Physical Meetup & In-Person Exchange",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            loadAllOrders()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh orders",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // UNIMAID Security Notice Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                shape = AppRadius.md,
                colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer)
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = UnimaidGoldOnContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Column {
                        Text(
                            text = "UNIMAID Campus Handover Protocol",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = UnimaidGoldOnContainer
                        )
                        Text(
                            text = "No online payment. Inspect items in public campus areas (e.g. Ramat Library) before paying cash or transfer in person.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UnimaidGoldOnContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Tabs: Purchases vs Sales Requests
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "My Purchases (${buyerOrders.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_my_purchases")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val pendingSalesCount = sellerOrders.count { it.status.equals("PENDING", ignoreCase = true) }
                            Text(
                                text = if (pendingSalesCount > 0) "Sales Requests ($pendingSalesCount)" else "Sales (${sellerOrders.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (pendingSalesCount > 0 && selectedTab != 1) AccentCoral else Color.Unspecified
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_sales_requests")
                )
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "ALL",
                        onClick = { selectedStatusFilter = "ALL" },
                        label = { Text("All (${currentList.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "PENDING",
                        onClick = { selectedStatusFilter = "PENDING" },
                        label = { Text("Pending") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                            selectedLabelColor = WarningAmber
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "ACCEPTED",
                        onClick = { selectedStatusFilter = "ACCEPTED" },
                        label = { Text("Accepted") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "MEETING_ARRANGED",
                        onClick = { selectedStatusFilter = "MEETING_ARRANGED" },
                        label = { Text("Meetup Set") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "COMPLETED",
                        onClick = { selectedStatusFilter = "COMPLETED" },
                        label = { Text("Completed") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerifiedGreenContainer,
                            selectedLabelColor = VerifiedGreenOnContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "CANCELLED",
                        onClick = { selectedStatusFilter = "CANCELLED" },
                        label = { Text("Cancelled / Declined") }
                    )
                }
            }

            // Content List
            PullToRefreshBox(
                isRefreshing = isRefreshing || isLoading,
                state = pullToRefreshState,
                onRefresh = {
                    isRefreshing = true
                    loadAllOrders()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppSpacing.screenPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTab == 0) {
                            UnimaidEmptyState(
                                title = "No Purchase Requests",
                                subtitle = "You have not requested any campus items yet. Browse listings, add to cart or request direct handovers from fellow students.",
                                icon = Icons.Default.ReceiptLong,
                                actionText = "Browse Marketplace",
                                onActionClick = { onNavigateToMarketplace?.invoke() }
                            )
                        } else {
                            UnimaidEmptyState(
                                title = "No Sales Requests",
                                subtitle = "You haven't received any purchase requests yet. Post textbooks, gadgets, or campus essentials to start receiving student orders.",
                                icon = Icons.Default.Storefront,
                                actionText = "Post New Listing",
                                onActionClick = { onNavigateToCreateListing?.invoke() }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        items(filteredList, key = { it.id }) { order ->
                            val isSellerView = selectedTab == 1
                            OrderCardItem(
                                order = order,
                                isSeller = isSellerView,
                                isBusy = actionInProgressOrderId == order.id,
                                onNavigateToProduct = { listingId -> onNavigateToProductDetails?.invoke(listingId) },
                                onContactCounterparty = { uid -> onContactUser?.invoke(uid) },
                                onAccept = {
                                    if (currentUserId == null) return@OrderCardItem
                                    actionInProgressOrderId = order.id
                                    coroutineScope.launch {
                                        when (val res = orderRepository.acceptOrder(order.id, currentUserId)) {
                                            is SupabaseResult.Success -> {
                                                Toast.makeText(context, "Purchase request accepted! Meetup can now be coordinated.", Toast.LENGTH_SHORT).show()
                                                loadAllOrders()
                                            }
                                            is SupabaseResult.Error -> {
                                                Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_LONG).show()
                                            }
                                            else -> {}
                                        }
                                        actionInProgressOrderId = null
                                    }
                                },
                                onDeclinePrompt = { orderToDecline = order },
                                onArrangeMeetingPrompt = { orderToArrangeMeeting = order },
                                onCancelPrompt = { orderToCancel = order },
                                onCompletePrompt = { orderToComplete = order }
                            )
                        }
                    }
                }
            }
        }
    }

    // Arrange / Update Meetup Dialog
    orderToArrangeMeeting?.let { ord ->
        ArrangeMeetingDialog(
            currentLocation = ord.meetingLocation ?: "UNIMAID Ramat Library (Main Walkway)",
            currentNote = ord.sellerNote ?: ord.buyerNote ?: "",
            onDismiss = { orderToArrangeMeeting = null },
            onConfirm = { location, note ->
                if (currentUserId == null) return@ArrangeMeetingDialog
                actionInProgressOrderId = ord.id
                orderToArrangeMeeting = null
                coroutineScope.launch {
                    when (val res = orderRepository.arrangeMeeting(ord.id, location, null, currentUserId, note)) {
                        is SupabaseResult.Success -> {
                            Toast.makeText(context, "Meeting location updated to $location", Toast.LENGTH_SHORT).show()
                            loadAllOrders()
                        }
                        is SupabaseResult.Error -> {
                            Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                    actionInProgressOrderId = null
                }
            }
        )
    }

    // Decline Order Dialog (Seller)
    orderToDecline?.let { ord ->
        PromptReasonDialog(
            title = "Decline Purchase Request",
            message = "Provide a brief reason to notify the student buyer why you cannot fulfill this request (e.g., item already reserved):",
            confirmText = "Decline Request",
            isDestructive = true,
            onDismiss = { orderToDecline = null },
            onConfirm = { reason ->
                if (currentUserId == null) return@PromptReasonDialog
                actionInProgressOrderId = ord.id
                orderToDecline = null
                coroutineScope.launch {
                    when (val res = orderRepository.declineOrder(ord.id, currentUserId, reason)) {
                        is SupabaseResult.Success -> {
                            Toast.makeText(context, "Purchase request declined.", Toast.LENGTH_SHORT).show()
                            loadAllOrders()
                        }
                        is SupabaseResult.Error -> {
                            Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                    actionInProgressOrderId = null
                }
            }
        )
    }

    // Cancel Order Dialog (Buyer or Seller)
    orderToCancel?.let { ord ->
        PromptReasonDialog(
            title = "Cancel Campus Handover",
            message = "Are you sure you want to cancel this campus transaction? If inventory was reserved, it will be automatically restored.",
            confirmText = "Cancel Handover",
            isDestructive = true,
            onDismiss = { orderToCancel = null },
            onConfirm = { reason ->
                if (currentUserId == null) return@PromptReasonDialog
                actionInProgressOrderId = ord.id
                orderToCancel = null
                coroutineScope.launch {
                    when (val res = orderRepository.cancelOrder(ord.id, currentUserId, reason)) {
                        is SupabaseResult.Success -> {
                            Toast.makeText(context, "Transaction cancelled. Items restored to campus catalog.", Toast.LENGTH_SHORT).show()
                            loadAllOrders()
                        }
                        is SupabaseResult.Error -> {
                            Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                    actionInProgressOrderId = null
                }
            }
        )
    }

    // Complete Order Confirmation Dialog
    orderToComplete?.let { ord ->
        AlertDialog(
            onDismissRequest = { orderToComplete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Handshake,
                    contentDescription = null,
                    tint = VerifiedGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Handover & Payment",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Did you complete the physical campus exchange for ₦${"%,.0f".format(ord.totalAmount)}?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Item was thoroughly inspected in person.\n• Physical cash or direct bank transfer was verified on the spot.\n• Both students agree the exchange is concluded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                UnimaidPillButton(
                    text = "Confirm Completed",
                    containerColor = VerifiedGreen,
                    onClick = {
                        if (currentUserId == null) return@UnimaidPillButton
                        actionInProgressOrderId = ord.id
                        val currentOrder = orderToComplete
                        orderToComplete = null
                        coroutineScope.launch {
                            when (val res = orderRepository.completeOrder(currentOrder!!.id, currentUserId)) {
                                is SupabaseResult.Success -> {
                                    Toast.makeText(context, "Transaction marked completed! Great campus exchange.", Toast.LENGTH_SHORT).show()
                                    loadAllOrders()
                                }
                                is SupabaseResult.Error -> {
                                    Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_LONG).show()
                                }
                                else -> {}
                            }
                            actionInProgressOrderId = null
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { orderToComplete = null }) {
                    Text("Not Yet")
                }
            }
        )
    }
}

@Composable
private fun OrderCardItem(
    order: Order,
    isSeller: Boolean,
    isBusy: Boolean,
    onNavigateToProduct: (String) -> Unit,
    onContactCounterparty: (String) -> Unit,
    onAccept: () -> Unit,
    onDeclinePrompt: () -> Unit,
    onArrangeMeetingPrompt: () -> Unit,
    onCancelPrompt: () -> Unit,
    onCompletePrompt: () -> Unit
) {
    val items = order.effectiveItems
    val firstItem = items.firstOrNull()
    val listing = order.listing ?: firstItem?.listing
    val counterparty = if (isSeller) order.buyer else order.seller
    val counterpartyId = if (isSeller) order.buyerId else order.sellerId

    val status = order.status.uppercase()
    val isPending = status == "PENDING"
    val isAccepted = status == "ACCEPTED"
    val isMeetingArranged = status == "MEETING_ARRANGED"
    val isCompleted = status == "COMPLETED"
    val isCancelled = status == "CANCELLED" || status == "DECLINED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
        shape = AppRadius.lg,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md)
        ) {
            // Header: Status Badge and Agreed Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OrderStatusBadge(status = status)

                Text(
                    text = MarketplaceUtils.formatNaira(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Listing summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        listing?.id?.let { onNavigateToProduct(it) }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val imageUrl = listing?.coverImageUrl
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(AppRadius.md)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = listing?.title ?: "Item",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listing?.title ?: "Campus Item",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Quantity: ${order.quantity} • Unit: ${MarketplaceUtils.formatNaira(listing?.price ?: (order.totalAmount / maxOf(1, order.quantity)))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Counterparty Info Pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.sm,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.sm, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSeller) "Buyer: ${counterparty?.displayName ?: "Student"}" else "Seller: ${counterparty?.displayName ?: "Student Seller"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (counterparty?.isStudentVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Student",
                                tint = VerifiedGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Contact button
                    TextButton(
                        onClick = { onContactCounterparty(counterpartyId) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Message",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            // Meetup Location / Notes Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.sm,
                color = MaterialTheme.colorScheme.background,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isMeetingArranged) VerifiedGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Campus Location: ${order.meetingLocation ?: "UNIMAID Main Campus"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!order.buyerNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Buyer Note: \"${order.buyerNote}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!order.sellerNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seller Note: \"${order.sellerNote}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Action Buttons based on role and status
            if (isBusy) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (isSeller) {
                // SELLER ACTIONS
                when {
                    isPending -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onDeclinePrompt,
                                modifier = Modifier.weight(0.4f)
                            ) {
                                Text("Decline", color = MaterialTheme.colorScheme.error)
                            }
                            UnimaidPillButton(
                                text = "Accept Request",
                                onClick = onAccept,
                                modifier = Modifier.weight(0.6f),
                                containerColor = VerifiedGreen,
                                contentColor = Color.White
                            )
                        }
                    }
                    isAccepted || isMeetingArranged -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnimaidPillButton(
                                text = "Set Meetup",
                                onClick = onArrangeMeetingPrompt,
                                modifier = Modifier.weight(0.45f),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            UnimaidPillButton(
                                text = "Confirm Handed Over",
                                onClick = onCompletePrompt,
                                modifier = Modifier.weight(0.55f),
                                containerColor = VerifiedGreen,
                                contentColor = Color.White
                            )
                        }
                    }
                    isCompleted -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VerifiedGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Exchange Concluded & Paid in Person",
                                style = MaterialTheme.typography.labelMedium,
                                color = VerifiedGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // BUYER ACTIONS
                when {
                    isPending -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Awaiting seller approval",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningAmber,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(onClick = onCancelPrompt) {
                                Text("Cancel Request", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    isAccepted || isMeetingArranged -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnimaidPillButton(
                                text = "Update Meetup",
                                onClick = onArrangeMeetingPrompt,
                                modifier = Modifier.weight(0.45f),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            UnimaidPillButton(
                                text = "Confirm Received",
                                onClick = onCompletePrompt,
                                modifier = Modifier.weight(0.55f),
                                containerColor = VerifiedGreen,
                                contentColor = Color.White
                            )
                        }
                    }
                    isCompleted -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VerifiedGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Handover Verified & Completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = VerifiedGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderStatusBadge(status: String) {
    val (label, bg, fg) = when (status) {
        "PENDING" -> Triple("Pending Confirmation", WarningAmber.copy(alpha = 0.15f), WarningAmber)
        "ACCEPTED" -> Triple("Accepted", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "MEETING_ARRANGED" -> Triple("Meetup Scheduled", VerifiedGreenContainer, VerifiedGreenOnContainer)
        "COMPLETED" -> Triple("Handover Completed", VerifiedGreenContainer, VerifiedGreenOnContainer)
        "DECLINED" -> Triple("Declined", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        "CANCELLED" -> Triple("Cancelled", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple(status, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = AppRadius.full,
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ArrangeMeetingDialog(
    currentLocation: String,
    currentNote: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedLocation by remember { mutableStateOf(currentLocation) }
    var customLocation by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(!UNIMAID_CAMPUS_LOCATIONS.contains(currentLocation)) }
    var note by remember { mutableStateOf(currentNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Arrange Campus Meetup",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select a safe, well-lit public location on UNIMAID campus for physical inspection and handover:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Common Location Selector
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(UNIMAID_CAMPUS_LOCATIONS) { loc ->
                        val isSelected = !isCustom && selectedLocation == loc
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(AppRadius.sm)
                                .clickable {
                                    isCustom = false
                                    selectedLocation = loc
                                },
                            shape = AppRadius.sm,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = loc,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Meeting note / time (Optional)") },
                    placeholder = { Text("e.g. Meet after 12pm lecture") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppRadius.md,
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            UnimaidPillButton(
                text = "Save Meetup Spot",
                onClick = {
                    val finalLocation = if (isCustom && customLocation.isNotBlank()) customLocation else selectedLocation
                    onConfirm(finalLocation, note)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PromptReasonDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppRadius.md,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            UnimaidPillButton(
                text = confirmText,
                containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                onClick = { onConfirm(reason) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back")
            }
        }
    )
}
