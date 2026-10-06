package com.example.ui.screens.cart

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.branding.BrandConfig
import com.example.core.designsystem.components.UnimaidAuthPromptDialog
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidPillButton
import com.example.core.designsystem.components.UnimaidQuantityStepper
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
import com.example.data.models.CartItem
import com.example.data.models.ItemCondition
import com.example.data.models.Order
import com.example.data.models.OrderItem
import com.example.data.repository.AuthRepository
import com.example.data.repository.CartRepository
import com.example.data.repository.CartRepositoryImpl
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.MarketplaceRepositoryImpl
import com.example.data.repository.OrderRepository
import com.example.data.repository.OrderRepositoryImpl
import com.example.ui.viewmodels.MarketplaceViewModel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Enhanced Cart Display Item carrying full real-time availability and stock metadata.
 */
data class CartDisplayItem(
    val cartItemId: String,
    val listingId: String,
    val title: String,
    val condition: String,
    val unitPrice: Double,
    val imageUrl: String?,
    val sellerId: String,
    val sellerName: String,
    val sellerDepartment: String?,
    val sellerVerified: Boolean,
    val campusLocation: String,
    val cartQuantity: Int,
    val availableStock: Int,
    val listingStatus: String,
    val isAvailable: Boolean
) {
    val isSold: Boolean
        get() = listingStatus.equals("SOLD", ignoreCase = true)

    val isDeactivated: Boolean
        get() = listingStatus.equals("DEACTIVATED", ignoreCase = true) ||
                listingStatus.equals("UNAVAILABLE", ignoreCase = true)

    val isOutOfStock: Boolean
        get() = availableStock <= 0

    val isUnavailable: Boolean
        get() = isSold || isDeactivated || isOutOfStock || !isAvailable

    val hasQuantityConflict: Boolean
        get() = !isUnavailable && cartQuantity > availableStock

    val maxAllowedQuantity: Int
        get() = if (availableStock > 0) availableStock else 1

    val isSelectable: Boolean
        get() = !isUnavailable && !hasQuantityConflict
}

/**
 * Popular UNIMAID campus locations for safe, daytime physical handover.
 */
val UNIMAID_MEETUP_LOCATIONS = listOf(
    "UNIMAID Central Library (Ramat)",
    "Senate Building Quadrangle",
    "Faculty of Science Complex",
    "Complex Lecture Theatres (LT 1-4)",
    "Faculty of Arts / Social Sciences",
    "Faculty of Engineering Workshops",
    "University Commercial Centre / Bank Area",
    "Student Hostels Quadrangle (Amina / Aisha / Murtala)"
)

/**
 * Real production Cart & Purchase Request Screen for UNIMAID StudentMarket.
 *
 * Implements:
 * - Real Supabase cart_items queries isolated to the authenticated user ID.
 * - Dynamic stock and availability conflict detection (detects reduced stock / sold / deactivated items).
 * - Multi-seller grouping: transactions are grouped by seller so distinct handovers can be coordinated.
 * - Multi-item selection with subtotal calculation.
 * - Physical handover purchase request creation in Supabase orders & order_items tables.
 * - Swipe-to-remove with haptic feedback & accessible standard button.
 * - Synchronized live cart count badge.
 * - Pull-to-refresh with state validation.
 * - Strict university business policy: NO online payment, physical campus exchange only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onExploreMarketplace: () -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository? = null,
    marketplaceViewModel: MarketplaceViewModel? = null,
    cartRepository: CartRepository = remember { CartRepositoryImpl() },
    marketplaceRepository: MarketplaceRepository = remember { MarketplaceRepositoryImpl() },
    orderRepository: OrderRepository = remember { OrderRepositoryImpl() },
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToSignUp: (() -> Unit)? = null,
    onNavigateToProductDetails: ((String) -> Unit)? = null,
    onNavigateToOrders: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val sessionState = authRepository?.sessionState?.collectAsState()?.value
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId
    val isAuthenticated = !currentUserId.isNullOrBlank()

    var cartDisplayList by remember { mutableStateOf<List<CartDisplayItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isManualRefreshing by remember { mutableStateOf(false) }
    var showAuthPrompt by remember { mutableStateOf(!isAuthenticated) }

    // Selection & Quantity Maps keyed by cartItemId
    val selectedMap = remember { mutableStateMapOf<String, Boolean>() }
    val quantityMap = remember { mutableStateMapOf<String, Int>() }

    // Dialog & Sheet States
    var itemToDelete by remember { mutableStateOf<CartDisplayItem?>(null) }
    var sellerForHandoverSheet by remember { mutableStateOf<String?>(null) }
    var isSubmittingOrder by remember { mutableStateOf(false) }
    var orderSuccessMessage by remember { mutableStateOf<String?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()

    fun loadCartData() {
        if (currentUserId.isNullOrBlank()) {
            isLoading = false
            return
        }
        coroutineScope.launch {
            isLoading = true
            when (val cartRes = cartRepository.getCartItems(currentUserId)) {
                is SupabaseResult.Success -> {
                    val rawItems = cartRes.data
                    val displayList = mutableListOf<CartDisplayItem>()

                    for (item in rawItems) {
                        // Load listing if not nested
                        val listing = item.listing ?: run {
                            when (val lRes = marketplaceRepository.getListingById(item.listingId)) {
                                is SupabaseResult.Success -> lRes.data
                                else -> null
                            }
                        }

                        if (listing != null) {
                            val availableStock = listing.quantity
                            val isSold = listing.status.equals("SOLD", ignoreCase = true) || listing.isSold
                            val isDeactivated = listing.status.equals("DEACTIVATED", ignoreCase = true) ||
                                    listing.status.equals("UNAVAILABLE", ignoreCase = true)
                            val isUnavailable = isSold || isDeactivated || (availableStock <= 0) || !listing.isAvailable

                            val displayItem = CartDisplayItem(
                                cartItemId = item.id,
                                listingId = listing.id,
                                title = listing.displayTitle,
                                condition = ItemCondition.format(listing.condition),
                                unitPrice = listing.price,
                                imageUrl = listing.images.firstOrNull()?.imageUrl,
                                sellerId = listing.sellerId.ifBlank { listing.seller?.id ?: "unknown" },
                                sellerName = listing.seller?.fullName ?: "UNIMAID Student",
                                sellerDepartment = listing.seller?.department,
                                sellerVerified = listing.seller?.isVerified == true,
                                campusLocation = listing.displayLocation,
                                cartQuantity = item.quantity,
                                availableStock = availableStock,
                                listingStatus = listing.status,
                                isAvailable = listing.isAvailable
                            )
                            displayList.add(displayItem)

                            // Initial selection: select only if available and no quantity conflict
                            if (!selectedMap.containsKey(item.id)) {
                                selectedMap[item.id] = displayItem.isSelectable
                            } else if (displayItem.isUnavailable || displayItem.hasQuantityConflict) {
                                selectedMap[item.id] = false
                            }

                            quantityMap[item.id] = item.quantity
                        }
                    }

                    cartDisplayList = displayList
                    marketplaceViewModel?.refreshCartCount(currentUserId)
                    isLoading = false
                }
                is SupabaseResult.Error -> {
                    isLoading = false
                    Toast.makeText(context, "Could not load cart: ${cartRes.userFriendlyMessage}", Toast.LENGTH_SHORT).show()
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    LaunchedEffect(currentUserId) {
        loadCartData()
    }

    // Group items by Seller ID for multi-seller checkout compliance
    val groupedBySeller = remember(cartDisplayList) {
        cartDisplayList.groupBy { it.sellerId }
    }

    // Selected items calculation
    val selectedItems = remember(cartDisplayList, selectedMap) {
        cartDisplayList.filter { selectedMap[it.cartItemId] == true && it.isSelectable }
    }

    val subtotal = remember(selectedItems, quantityMap) {
        selectedItems.sumOf { it.unitPrice * (quantityMap[it.cartItemId] ?: it.cartQuantity) }
    }

    val allSelectableCount = remember(cartDisplayList) {
        cartDisplayList.count { it.isSelectable }
    }
    val areAllSelected = remember(selectedItems, allSelectableCount) {
        allSelectableCount > 0 && selectedItems.size == allSelectableCount
    }

    // Back handling
    BackHandler {
        if (sellerForHandoverSheet != null) {
            sellerForHandoverSheet = null
        } else if (itemToDelete != null) {
            itemToDelete = null
        } else {
            onNavigateBack?.invoke() ?: onExploreMarketplace()
        }
    }

    if (showAuthPrompt && !isAuthenticated) {
        UnimaidAuthPromptDialog(
            actionTitle = "Student Cart Access",
            actionDescription = "Sign in to your UNIMAID student account to manage your item reservations and coordinate campus meetups.",
            onDismiss = {
                showAuthPrompt = false
                onExploreMarketplace()
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

    // Confirmation Dialog for item deletion
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Remove from Cart", fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${item.title}\" from your reserved campus items?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cartId = item.cartItemId
                        itemToDelete = null
                        coroutineScope.launch {
                            when (cartRepository.removeFromCart(cartId)) {
                                is SupabaseResult.Success -> {
                                    cartDisplayList = cartDisplayList.filter { it.cartItemId != cartId }
                                    selectedMap.remove(cartId)
                                    quantityMap.remove(cartId)
                                    if (currentUserId != null) {
                                        marketplaceViewModel?.refreshCartCount(currentUserId)
                                    }
                                    Toast.makeText(context, "Item removed from cart", Toast.LENGTH_SHORT).show()
                                }
                                is SupabaseResult.Error -> {
                                    Toast.makeText(context, "Failed to remove item", Toast.LENGTH_SHORT).show()
                                }
                                else -> {}
                            }
                        }
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Keep Item")
                }
            }
        )
    }

    // Order Success Dialog
    orderSuccessMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { orderSuccessMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = VerifiedGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Handover Request Placed!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                UnimaidPillButton(
                    text = "View My Orders",
                    onClick = {
                        orderSuccessMessage = null
                        if (onNavigateToOrders != null) {
                            onNavigateToOrders()
                        } else {
                            onExploreMarketplace()
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        orderSuccessMessage = null
                        onExploreMarketplace()
                    }
                ) {
                    Text("Continue Browsing")
                }
            }
        )
    }

    // Campus Handover Request Modal Bottom Sheet (Grouped by Seller)
    sellerForHandoverSheet?.let { sellerId ->
        val sellerItems = cartDisplayList.filter { it.sellerId == sellerId && (selectedMap[it.cartItemId] == true) && it.isSelectable }
        val sellerName = sellerItems.firstOrNull()?.sellerName ?: "UNIMAID Student"
        val sellerDept = sellerItems.firstOrNull()?.sellerDepartment
        val sellerTotal = sellerItems.sumOf { it.unitPrice * (quantityMap[it.cartItemId] ?: it.cartQuantity) }

        CampusHandoverModalSheet(
            sellerName = sellerName,
            sellerDepartment = sellerDept,
            items = sellerItems,
            quantityMap = quantityMap,
            totalAmount = sellerTotal,
            isSubmitting = isSubmittingOrder,
            onDismiss = { sellerForHandoverSheet = null },
            onSubmitRequest = { location, notes ->
                if (currentUserId.isNullOrBlank()) return@CampusHandoverModalSheet
                isSubmittingOrder = true

                coroutineScope.launch {
                    val orderId = UUID.randomUUID().toString()
                    val firstItem = sellerItems.firstOrNull()
                    val totalQty = sellerItems.sumOf { quantityMap[it.cartItemId] ?: it.cartQuantity }
                    val newOrder = Order(
                        id = orderId,
                        buyerId = currentUserId,
                        sellerId = sellerId,
                        listingId = firstItem?.listingId,
                        quantity = totalQty,
                        totalAmount = sellerTotal,
                        status = "PENDING",
                        paymentStatus = "PHYSICAL_PENDING",
                        deliveryMethod = "PHYSICAL_MEETUP",
                        meetingLocation = location,
                        buyerNote = notes
                    )

                    val orderItems = sellerItems.map { itm ->
                        val qty = quantityMap[itm.cartItemId] ?: itm.cartQuantity
                        OrderItem(
                            id = UUID.randomUUID().toString(),
                            orderId = orderId,
                            listingId = itm.listingId,
                            quantity = qty,
                            unitPrice = itm.unitPrice,
                            subtotal = itm.unitPrice * qty
                        )
                    }

                    when (val orderRes = orderRepository.createOrder(newOrder, orderItems)) {
                        is SupabaseResult.Success -> {
                            // Remove ordered items from Supabase cart_items
                            for (itm in sellerItems) {
                                cartRepository.removeFromCart(itm.cartItemId)
                            }
                            // Refresh local list
                            cartDisplayList = cartDisplayList.filter { itm -> sellerItems.none { it.cartItemId == itm.cartItemId } }
                            sellerItems.forEach {
                                selectedMap.remove(it.cartItemId)
                                quantityMap.remove(it.cartItemId)
                            }
                            marketplaceViewModel?.refreshCartCount(currentUserId)

                            isSubmittingOrder = false
                            sellerForHandoverSheet = null
                            orderSuccessMessage = "Your purchase request for ₦${"%,.0f".format(sellerTotal)} was sent to $sellerName! You will meet at $location. Remember: pay in person upon receiving and verifying your items."
                        }
                        is SupabaseResult.Error -> {
                            isSubmittingOrder = false
                            Toast.makeText(context, "Could not send request: ${orderRes.userFriendlyMessage}", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            isSubmittingOrder = false
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            UnimaidTopAppBar(
                title = "Campus Item Reservations",
                onBackClick = { onNavigateBack?.invoke() ?: onExploreMarketplace() },
                actions = {
                    if (cartDisplayList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (areAllSelected) {
                                    cartDisplayList.forEach { selectedMap[it.cartItemId] = false }
                                } else {
                                    cartDisplayList.forEach {
                                        if (it.isSelectable) selectedMap[it.cartItemId] = true
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = if (areAllSelected) "Deselect" else "Select All",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            isManualRefreshing = true
                            loadCartData()
                            isManualRefreshing = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Cart",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = (isLoading && cartDisplayList.isNotEmpty()) || isManualRefreshing,
            state = pullToRefreshState,
            onRefresh = {
                isManualRefreshing = true
                loadCartData()
                isManualRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && cartDisplayList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                !isAuthenticated -> {
                    UnimaidEmptyState(
                        title = "Sign In Required",
                        subtitle = "Sign in with your University of Maiduguri account to access your cart and coordinate campus meetups.",
                        icon = Icons.Outlined.ShoppingBag,
                        actionText = "Sign In",
                        onActionClick = { onNavigateToLogin?.invoke() }
                    )
                }

                cartDisplayList.isEmpty() -> {
                    UnimaidEmptyState(
                        title = "Your Cart is Empty",
                        subtitle = "Items you reserve from UNIMAID students will appear here to coordinate safe on-campus meetups.",
                        icon = Icons.Outlined.ShoppingBag,
                        actionText = "Explore Marketplace",
                        onActionClick = onExploreMarketplace
                    )
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("cart_items_list"),
                            contentPadding = PaddingValues(
                                start = AppSpacing.screenPadding,
                                end = AppSpacing.screenPadding,
                                top = AppSpacing.xs,
                                bottom = 220.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                            // Campus Handover Policy Banner
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                text = "Campus Physical Exchange",
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

                            // Items Grouped by Student Seller
                            groupedBySeller.forEach { (sellerId, sellerItems) ->
                                item(key = "header_$sellerId") {
                                    val sellerName = sellerItems.firstOrNull()?.sellerName ?: "UNIMAID Student"
                                    val isVerified = sellerItems.firstOrNull()?.sellerVerified == true
                                    val sellerDept = sellerItems.firstOrNull()?.sellerDepartment
                                    val sellerSelectableItems = sellerItems.filter { it.isSelectable }
                                    val allSellerSelected = sellerSelectableItems.isNotEmpty() &&
                                            sellerSelectableItems.all { selectedMap[it.cartItemId] == true }

                                    SellerGroupHeader(
                                        sellerName = sellerName,
                                        isVerified = isVerified,
                                        department = sellerDept,
                                        allSelected = allSellerSelected,
                                        hasSelectable = sellerSelectableItems.isNotEmpty(),
                                        onToggleSelectAll = { select ->
                                            sellerSelectableItems.forEach {
                                                selectedMap[it.cartItemId] = select
                                            }
                                        }
                                    )
                                }

                                items(sellerItems, key = { it.cartItemId }) { item ->
                                    val isChecked = selectedMap[item.cartItemId] == true
                                    val currentQty = quantityMap[item.cartItemId] ?: item.cartQuantity

                                    // Swipe-to-dismiss setup with haptic feedback
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                itemToDelete = item
                                                false // Let the dialog handle actual removal
                                            } else {
                                                false
                                            }
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(AppRadius.lg)
                                                    .background(MaterialTheme.colorScheme.error)
                                                    .padding(horizontal = AppSpacing.lg),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color.White
                                                )
                                            }
                                        },
                                        enableDismissFromStartToEnd = true,
                                        enableDismissFromEndToStart = true
                                    ) {
                                        CartItemCard(
                                            item = item,
                                            quantity = currentQty,
                                            isChecked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (item.isSelectable) {
                                                    selectedMap[item.cartItemId] = checked
                                                } else if (item.hasQuantityConflict) {
                                                    Toast.makeText(
                                                        context,
                                                        "Please adjust quantity to ${item.availableStock} before selecting",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onQuantityChange = { newQty ->
                                                quantityMap[item.cartItemId] = newQty
                                                coroutineScope.launch {
                                                    cartRepository.updateQuantity(item.cartItemId, newQty)
                                                    if (currentUserId != null) {
                                                        marketplaceViewModel?.refreshCartCount(currentUserId)
                                                    }
                                                }
                                            },
                                            onAutoAdjustQuantity = {
                                                val target = item.availableStock
                                                quantityMap[item.cartItemId] = target
                                                coroutineScope.launch {
                                                    cartRepository.updateQuantity(item.cartItemId, target)
                                                    cartDisplayList = cartDisplayList.map {
                                                        if (it.cartItemId == item.cartItemId) it.copy(cartQuantity = target) else it
                                                    }
                                                    if (currentUserId != null) {
                                                        marketplaceViewModel?.refreshCartCount(currentUserId)
                                                    }
                                                    Toast.makeText(context, "Quantity adjusted to $target", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onDeleteItem = {
                                                itemToDelete = item
                                            },
                                            onClickProduct = {
                                                onNavigateToProductDetails?.invoke(item.listingId)
                                            }
                                        )
                                    }
                                }

                                // Seller section footer action
                                item(key = "footer_$sellerId") {
                                    val sellerSelectedItems = sellerItems.filter { selectedMap[it.cartItemId] == true && it.isSelectable }
                                    if (sellerSelectedItems.isNotEmpty()) {
                                        val sellerName = sellerItems.firstOrNull()?.sellerName ?: "Student"
                                        val sellerTotal = sellerSelectedItems.sumOf { it.unitPrice * (quantityMap[it.cartItemId] ?: it.cartQuantity) }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Subtotal: ₦${"%,.0f".format(sellerTotal)}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            TextButton(
                                                onClick = { sellerForHandoverSheet = sellerId },
                                                modifier = Modifier.testTag("checkout_seller_$sellerId")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Handshake,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Request Handover with $sellerName",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = AppSpacing.xs),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // Sticky Bottom Summary Bar
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 12.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.screenPadding)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Selected Items (${selectedItems.size})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "₦${"%,.0f".format(subtotal)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Badge showing campus free handover policy
                                    Surface(
                                        shape = AppRadius.full,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Campus Handover FREE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Estimated Handover Total",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "₦${"%,.0f".format(subtotal)}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCoral
                                        )
                                        Text(
                                            text = "Pay physical cash/transfer at meetup",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    UnimaidPillButton(
                                        text = if (selectedItems.isEmpty()) "Select Items" else "Coordinate Handover",
                                        onClick = {
                                            if (selectedItems.isEmpty()) {
                                                Toast.makeText(context, "Please select at least one item", Toast.LENGTH_SHORT).show()
                                                return@UnimaidPillButton
                                            }

                                            // Determine which seller group to checkout
                                            val distinctSellers = selectedItems.map { it.sellerId }.distinct()
                                            if (distinctSellers.size == 1) {
                                                sellerForHandoverSheet = distinctSellers.first()
                                            } else {
                                                // Prompt user: multi-seller grouping must coordinate individually
                                                Toast.makeText(
                                                    context,
                                                    "Items are from ${distinctSellers.size} different students. Please tap 'Request Handover' under each seller to coordinate meetups.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                // Default to opening first seller
                                                sellerForHandoverSheet = distinctSellers.first()
                                            }
                                        },
                                        containerColor = AccentCoral,
                                        enabled = selectedItems.isNotEmpty(),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Handshake,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        },
                                        testTag = "cart_checkout_button"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header representing a student seller grouping in the Cart.
 */
@Composable
private fun SellerGroupHeader(
    sellerName: String,
    isVerified: Boolean,
    department: String?,
    allSelected: Boolean,
    hasSelectable: Boolean,
    onToggleSelectAll: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.sm, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Checkbox(
                checked = allSelected,
                onCheckedChange = { onToggleSelectAll(it) },
                enabled = hasSelectable,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sellerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = "Verified UNIMAID Student",
                            tint = VerifiedGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (!department.isNullOrBlank()) {
                    Text(
                        text = department,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Individual Cart Item Card with stock conflict detection, availability badges,
 * stepper, and deletion.
 */
@Composable
private fun CartItemCard(
    item: CartDisplayItem,
    quantity: Int,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onAutoAdjustQuantity: () -> Unit,
    onDeleteItem: () -> Unit,
    onClickProduct: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.cartItemId}"),
        shape = AppRadius.lg,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isUnavailable) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isUnavailable) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            // Out of stock / sold banner
            if (item.isUnavailable) {
                Surface(
                    shape = AppRadius.sm,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppSpacing.sm)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                item.isSold -> "This item has been marked as SOLD by the seller."
                                item.isDeactivated -> "This listing is currently DEACTIVATED."
                                item.isOutOfStock -> "This item is currently OUT OF STOCK."
                                else -> "This item is no longer available on campus."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quantity conflict warning banner
            if (item.hasQuantityConflict) {
                Surface(
                    shape = AppRadius.sm,
                    color = UnimaidGoldContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppSpacing.sm)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = UnimaidGoldOnContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Only ${item.availableStock} available now (You requested $quantity)",
                                style = MaterialTheme.typography.labelSmall,
                                color = UnimaidGoldOnContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = onAutoAdjustQuantity,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "Adjust to ${item.availableStock}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = UnimaidGoldOnContainer
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    enabled = item.isSelectable,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        disabledCheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )

                // Thumbnail with click to view details
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(AppRadius.md)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onClickProduct),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                // Info & Stepper
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isUnavailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (item.isUnavailable) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.clickable(onClick = onClickProduct)
                    )

                    Text(
                        text = "${item.condition} • ${item.campusLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₦${"%,.0f".format(item.unitPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isUnavailable) MaterialTheme.colorScheme.onSurfaceVariant else AccentCoral
                        )

                        if (!item.isUnavailable) {
                            UnimaidQuantityStepper(
                                quantity = quantity,
                                onQuantityChange = onQuantityChange,
                                maxQuantity = item.maxAllowedQuantity
                            )
                        } else {
                            Text(
                                text = "Unavailable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Delete Icon Button
                IconButton(
                    onClick = onDeleteItem,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Sheet allowing student to review their purchase request with a specific seller,
 * select a campus meeting landmark, add notes, and submit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampusHandoverModalSheet(
    sellerName: String,
    sellerDepartment: String?,
    items: List<CartDisplayItem>,
    quantityMap: Map<String, Int>,
    totalAmount: Double,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitRequest: (location: String, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedLocation by remember { mutableStateOf(UNIMAID_MEETUP_LOCATIONS.first()) }
    var locationExpanded by remember { mutableStateOf(false) }
    var buyerNote by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Request Campus Handover",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Coordinating with $sellerName ${if (!sellerDepartment.isNullOrBlank()) "($sellerDepartment)" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Policy Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.md,
                colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = UnimaidGoldOnContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        text = "Physical campus exchange only: No online payment. Meet the seller in person, verify items, and pay physically via cash or bank transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UnimaidGoldOnContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "Items in Request (${items.size}):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            items.forEach { item ->
                val qty = quantityMap[item.cartItemId] ?: item.cartQuantity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.title} (x$qty)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "₦${"%,.0f".format(item.unitPrice * qty)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCoral
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = AppSpacing.sm),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Handover Amount",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "₦${"%,.0f".format(totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentCoral
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Landmark Dropdown
            Text(
                text = "Campus Meetup Location",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = !locationExpanded }
            ) {
                OutlinedTextField(
                    value = selectedLocation,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    shape = AppRadius.md,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                ExposedDropdownMenu(
                    expanded = locationExpanded,
                    onDismissRequest = { locationExpanded = false }
                ) {
                    UNIMAID_MEETUP_LOCATIONS.forEach { loc ->
                        DropdownMenuItem(
                            text = { Text(loc) },
                            onClick = {
                                selectedLocation = loc
                                locationExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Optional note for seller
            Text(
                text = "Meeting Note / Free Times (Optional)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = buyerNote,
                onValueChange = { buyerNote = it },
                placeholder = { Text("e.g. Free after 12pm lecture near library steps") },
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.md,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            UnimaidPillButton(
                text = if (isSubmitting) "Sending Request..." else "Send Purchase Request (₦${"%,.0f".format(totalAmount)})",
                onClick = {
                    onSubmitRequest(selectedLocation, buyerNote)
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                containerColor = AccentCoral,
                leadingIcon = {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                testTag = "submit_handover_request_button"
            )
        }
    }
}
