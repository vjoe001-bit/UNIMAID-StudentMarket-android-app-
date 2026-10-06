package com.example.ui.screens.details

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.data.models.Order
import com.example.data.repository.OrderRepository
import com.example.data.repository.OrderRepositoryImpl
import com.example.ui.screens.orders.UNIMAID_CAMPUS_LOCATIONS
import java.util.UUID
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.branding.BrandConfig
import com.example.core.utils.MarketplaceUtils
import com.example.core.designsystem.components.UnimaidAuthPromptDialog
import com.example.core.designsystem.components.UnimaidBadge
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidErrorState
import com.example.core.designsystem.components.UnimaidPillButton
import com.example.core.designsystem.components.UnimaidVerifiedBadge
import com.example.core.network.SupabaseResult
import com.example.core.session.SessionState
import com.example.core.theme.AccentCoral
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.UnimaidGold
import com.example.core.theme.UnimaidGoldContainer
import com.example.core.theme.UnimaidGoldOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.core.theme.WarningAmber
import com.example.core.theme.WarningAmberContainer
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
 * Real production Product Details Screen connected to Supabase listings.
 * Features:
 * - Real listing details loaded by ID
 * - Multi-image gallery with thumbnail selection and full-screen zoom dialog
 * - Real seller verification status card
 * - In-person physical handover campus guidelines
 * - Authenticated favorite toggle with Supabase persistence
 * - Add to Cart reservation with Supabase persistence
 * - Contact Seller inquiry flow
 */
@Composable
fun ProductDetailsScreen(
    listingId: String,
    onNavigateBack: () -> Unit,
    onContactSeller: (String) -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository? = null,
    marketplaceViewModel: MarketplaceViewModel? = null,
    marketplaceRepository: MarketplaceRepository = remember { MarketplaceRepositoryImpl() },
    favoritesRepository: FavoritesRepository = remember { FavoritesRepositoryImpl() },
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToSignUp: (() -> Unit)? = null,
    onNavigateToEditListing: ((String) -> Unit)? = null,
    onNavigateToCart: (() -> Unit)? = null,
    onNavigateToOrders: (() -> Unit)? = null,
    orderRepository: OrderRepository = remember { OrderRepositoryImpl() }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var listing by remember { mutableStateOf<Listing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var isImageZoomOpen by remember { mutableStateOf(false) }

    var isFavorite by remember { mutableStateOf(false) }
    var isAddingToCart by remember { mutableStateOf(false) }

    // Direct Handover Request States
    var showHandoverSheet by remember { mutableStateOf(false) }
    var isSubmittingOrder by remember { mutableStateOf(false) }
    var orderSuccessMessage by remember { mutableStateOf<String?>(null) }

    var showAuthPrompt by remember { mutableStateOf(false) }
    var authPromptAction by remember { mutableStateOf("Sign In Required") }
    var authPromptDesc by remember { mutableStateOf("") }

    val sessionState = authRepository?.sessionState?.collectAsState()?.value
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId
    val isAuthenticated = !currentUserId.isNullOrBlank()

    fun loadListingData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = marketplaceRepository.getListingById(listingId)) {
                is SupabaseResult.Success -> {
                    listing = result.data
                    isLoading = false
                    // Check if favorited if authenticated
                    if (currentUserId != null) {
                        when (val favResult = favoritesRepository.isListingFavorited(currentUserId, listingId)) {
                            is SupabaseResult.Success -> isFavorite = favResult.data
                            else -> {}
                        }
                    }
                }
                is SupabaseResult.Error -> {
                    errorMessage = result.userFriendlyMessage
                    isLoading = false
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    LaunchedEffect(listingId) {
        loadListingData()
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
                        onNavigateToOrders?.invoke()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { orderSuccessMessage = null }) {
                    Text("Continue Browsing")
                }
            }
        )
    }

    // Direct Campus Handover Request Modal Bottom Sheet
    if (showHandoverSheet && listing != null) {
        val item = listing!!
        ProductHandoverModalSheet(
            listing = item,
            isSubmitting = isSubmittingOrder,
            onDismiss = { showHandoverSheet = false },
            onSubmitRequest = { quantity, location, notes ->
                if (currentUserId.isNullOrBlank()) return@ProductHandoverModalSheet
                isSubmittingOrder = true

                coroutineScope.launch {
                    val orderId = UUID.randomUUID().toString()
                    val total = item.price * quantity
                    val newOrder = Order(
                        id = orderId,
                        buyerId = currentUserId,
                        sellerId = item.sellerId,
                        listingId = item.id,
                        quantity = quantity,
                        totalAmount = total,
                        status = "PENDING",
                        paymentStatus = "PHYSICAL_PENDING",
                        deliveryMethod = "PHYSICAL_MEETUP",
                        meetingLocation = location,
                        buyerNote = notes
                    )

                    when (val res = orderRepository.createOrder(newOrder, emptyList())) {
                        is SupabaseResult.Success -> {
                            isSubmittingOrder = false
                            showHandoverSheet = false
                            orderSuccessMessage = "Your purchase request for ₦${"%,.0f".format(total)} was sent to the student seller! You will meet at $location. Remember: pay in person upon receiving and verifying your item."
                        }
                        is SupabaseResult.Error -> {
                            isSubmittingOrder = false
                            Toast.makeText(context, "Could not send request: ${res.userFriendlyMessage}", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            isSubmittingOrder = false
                        }
                    }
                }
            }
        )
    }

    // Full screen image zoom viewer dialog
    if (isImageZoomOpen && listing != null) {
        val currentImageUrl = listing?.images?.getOrNull(selectedImageIndex)?.imageUrl
        Dialog(
            onDismissRequest = { isImageZoomOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                if (!currentImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = currentImageUrl,
                        contentDescription = "Zoomed product image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md),
                        contentScale = ContentScale.Fit
                    )
                }

                IconButton(
                    onClick = { isImageZoomOpen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppSpacing.lg)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isLoading -> {
                ProductDetailsSkeleton(onNavigateBack = onNavigateBack)
            }

            errorMessage != null || listing == null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(AppSpacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    UnimaidErrorState(
                        title = "Listing Not Found",
                        message = errorMessage ?: "This campus item may have been deleted, sold, or is currently unavailable.",
                        onRetry = { loadListingData() }
                    )
                }
            }

            else -> {
                val item = listing!!
                val isOwner = !currentUserId.isNullOrBlank() && currentUserId == item.sellerId
                val images = item.images
                val displayImageUrl = images.getOrNull(selectedImageIndex)?.imageUrl

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 96.dp)
                ) {
                    // Hero Image Area with Top Bar overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!displayImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = displayImageUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { isImageZoomOpen = true }
                                    .testTag("detail_hero_image"),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        // Top navigation overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("detail_back_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOwner) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(onClick = { onNavigateToEditListing?.invoke(item.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Listing",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = {
                                        Toast.makeText(context, "Listing link copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = Color.White
                                        )
                                    }
                                }

                                if (onNavigateToCart != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .clickable { onNavigateToCart() }
                                            .testTag("detail_cart_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val cartCount = marketplaceViewModel?.uiState?.collectAsState()?.value?.cartItemCount ?: 0
                                        BadgedBox(
                                            badge = {
                                                if (cartCount > 0) {
                                                    Badge { Text(cartCount.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Cart",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tap to expand zoom badge
                        if (!displayImageUrl.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(AppSpacing.md)
                                    .clickable { isImageZoomOpen = true },
                                shape = AppRadius.full,
                                color = Color.Black.copy(alpha = 0.55f)
                            ) {
                                Text(
                                    text = "Tap to zoom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                                )
                            }
                        }
                    }

                    // Multi-image Thumbnail Strip (if more than 1 image)
                    if (images.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            images.forEachIndexed { index, img ->
                                val isSelected = index == selectedImageIndex
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(AppRadius.md)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = AppRadius.md
                                        )
                                        .clickable { selectedImageIndex = index }
                                ) {
                                    AsyncImage(
                                        model = img.imageUrl,
                                        contentDescription = "Thumbnail ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Curved Content Sheet Container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-16).dp),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.screenPadding)
                        ) {
                            // Price, Status Badges & Floating Favorite Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "₦${"%,.0f".format(item.price)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCoral
                                    )

                                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                                    // Availability & Condition Badges
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        when {
                                            item.isSold -> {
                                                UnimaidBadge(
                                                    text = "SOLD OUT",
                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                            !item.isAvailable -> {
                                                UnimaidBadge(
                                                    text = "RESERVED",
                                                    containerColor = WarningAmberContainer,
                                                    contentColor = WarningAmber
                                                )
                                            }
                                            item.quantity == 1 -> {
                                                UnimaidBadge(
                                                    text = "1 AVAILABLE",
                                                    containerColor = UnimaidBlueContainer,
                                                    contentColor = UnimaidBlueOnContainer
                                                )
                                            }
                                            else -> {
                                                UnimaidBadge(
                                                    text = "IN STOCK (${item.quantity})",
                                                    containerColor = VerifiedGreenContainer,
                                                    contentColor = VerifiedGreenOnContainer
                                                )
                                            }
                                        }

                                        UnimaidBadge(
                                            text = ItemCondition.format(item.condition),
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Floating Circular Heart Button
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .testTag("detail_favorite_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = {
                                        if (!isAuthenticated) {
                                            authPromptAction = "Save to Favorites"
                                            authPromptDesc = "Sign in to save this listing to your personal favorites and track price changes."
                                            showAuthPrompt = true
                                        } else {
                                            val newFavState = !isFavorite
                                            isFavorite = newFavState
                                            coroutineScope.launch {
                                                if (newFavState) {
                                                    favoritesRepository.addFavorite(currentUserId!!, item.id)
                                                    Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    favoritesRepository.removeFavorite(currentUserId!!, item.id)
                                                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFavorite) AccentCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            // Item Title
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("detail_product_title")
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            // Verified Seller Profile Card
                            val seller = item.seller
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppRadius.md,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            ) {
                                Column(modifier = Modifier.padding(AppSpacing.md)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Seller Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!seller?.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = seller!!.avatarUrl,
                                                    contentDescription = "Seller avatar",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(AppSpacing.md))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = seller?.fullName?.ifBlank { "UNIMAID Student Seller" } ?: "UNIMAID Student Seller",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(AppSpacing.xs))
                                                UnimaidVerifiedBadge(isVerified = seller?.isVerified == true)
                                            }

                                            val dept = listOfNotNull(seller?.department, seller?.faculty).filter { it.isNotBlank() }.joinToString(" • ")
                                            if (dept.isNotBlank()) {
                                                Text(
                                                    text = dept,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            seller?.level?.let { lvl ->
                                                Text(
                                                    text = "$lvl Level Student",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            // Campus Location & Meetup Safety Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppRadius.md,
                                colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer)
                            ) {
                                Column(modifier = Modifier.padding(AppSpacing.md)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = UnimaidGoldOnContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                                        Text(
                                            text = "Handover: ${item.locationCampus ?: "UNIMAID Main Campus"}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = UnimaidGoldOnContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Handshake,
                                            contentDescription = null,
                                            tint = UnimaidGoldOnContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                                        Text(
                                            text = "Campus Safety Rule: Arrange physical meetup at busy campus locations (Ramat Library, Faculty Quarters, or Student Center). Inspect the item thoroughly before paying cash.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = UnimaidGoldOnContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.lg))

                            // Item Description
                            Text(
                                text = "Item Description",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.xs))

                            Text(
                                text = item.description?.ifBlank { "No detailed description provided by the student." }
                                    ?: "No detailed description provided by the student.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                            )
                        }
                    }
                }

                // Sticky Bottom Bar with Action Buttons
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val isOwner = isAuthenticated && currentUserId == item.sellerId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwner) {
                            // Owner Actions: Edit Listing & Toggle Sold Status
                            Box(modifier = Modifier.weight(0.45f)) {
                                UnimaidPillButton(
                                    text = "Edit Listing",
                                    onClick = {
                                        onNavigateToEditListing?.invoke(item.id)
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    testTag = "detail_edit_listing_button"
                                )
                            }

                            val isSold = item.status.equals("SOLD", ignoreCase = true)
                            Box(modifier = Modifier.weight(0.55f)) {
                                UnimaidPillButton(
                                    text = if (isSold) "Mark Active" else "Mark as Sold",
                                    onClick = {
                                        coroutineScope.launch {
                                            val newSt = if (isSold) "ACTIVE" else "SOLD"
                                            marketplaceViewModel?.updateListingStatus(item.id, currentUserId!!, newSt)
                                            listing = listing?.copy(status = newSt)
                                            Toast.makeText(context, if (isSold) "Listing is now Active" else "Marked as Sold", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    containerColor = if (isSold) VerifiedGreen else UnimaidGold,
                                    contentColor = Color.Black,
                                    testTag = "detail_toggle_status_button"
                                )
                            }
                        } else {
                            val isSold = item.status.equals("SOLD", ignoreCase = true)
                            val isDeactivated = item.status.equals("DEACTIVATED", ignoreCase = true)
                            val isOutOfStock = item.quantity <= 0
                            val isUnavailable = isSold || isDeactivated || isOutOfStock || !item.isAvailable

                            // Buyer Actions: Contact Seller, Add to Cart, Request Item Handover
                            // 1. Contact Seller Icon
                            IconButton(
                                onClick = {
                                    if (!isAuthenticated) {
                                        authPromptAction = "Contact Student Seller"
                                        authPromptDesc = "Sign in with your UNIMAID student account to message the seller directly."
                                        showAuthPrompt = true
                                    } else {
                                        onContactSeller(item.sellerId)
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("detail_contact_seller_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Contact Seller",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 2. Add to Cart Button
                            Surface(
                                modifier = Modifier
                                    .weight(0.38f)
                                    .height(50.dp)
                                    .clip(AppRadius.full)
                                    .clickable {
                                        if (isUnavailable) {
                                            val reason = when {
                                                isSold -> "This campus item has been marked as sold."
                                                isOutOfStock -> "This item is currently out of stock."
                                                isDeactivated -> "This listing has been deactivated by the seller."
                                                else -> "This item is currently not available."
                                            }
                                            Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
                                        } else if (!isAuthenticated) {
                                            authPromptAction = "Add to Cart"
                                            authPromptDesc = "Sign in to reserve items in your campus cart and coordinate handover times."
                                            showAuthPrompt = true
                                        } else {
                                            isAddingToCart = true
                                            marketplaceViewModel?.addToCart(
                                                userId = currentUserId!!,
                                                listingId = item.id,
                                                quantity = 1
                                            ) { success, msg ->
                                                isAddingToCart = false
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            } ?: run {
                                                isAddingToCart = false
                                                Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                shape = AppRadius.full,
                                color = if (isUnavailable) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAddingToCart) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = if (isUnavailable) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Cart",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isUnavailable) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 3. Request Handover (Direct Purchase Intent)
                            Box(modifier = Modifier.weight(0.62f)) {
                                UnimaidPillButton(
                                    text = when {
                                        isSold -> "Sold Out"
                                        isOutOfStock -> "Out of Stock"
                                        isDeactivated -> "Deactivated"
                                        !item.isAvailable -> "Unavailable"
                                        else -> "Request Item"
                                    },
                                    onClick = {
                                        if (isUnavailable) {
                                            val reason = when {
                                                isSold -> "This campus item has been marked as sold."
                                                isOutOfStock -> "This item is currently out of stock."
                                                isDeactivated -> "This listing has been deactivated by the seller."
                                                else -> "This item is currently not available."
                                            }
                                            Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
                                        } else if (!isAuthenticated) {
                                            authPromptAction = "Purchase Request"
                                            authPromptDesc = "Sign in with your UNIMAID student account to send an in-person handover request to the seller."
                                            showAuthPrompt = true
                                        } else if (currentUserId == item.sellerId) {
                                            Toast.makeText(context, "You cannot purchase your own listing.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showHandoverSheet = true
                                        }
                                    },
                                    enabled = !isUnavailable,
                                    containerColor = if (isUnavailable) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else VerifiedGreen,
                                    contentColor = if (isUnavailable) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.White,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Handshake,
                                            contentDescription = null,
                                            tint = if (isUnavailable) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    testTag = "detail_request_item_button"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductHandoverModalSheet(
    listing: Listing,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitRequest: (Int, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember { mutableIntStateOf(1) }
    var selectedLocation by remember { mutableStateOf(UNIMAID_CAMPUS_LOCATIONS.first()) }
    var customLocation by remember { mutableStateOf("") }
    var isCustomLocation by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    val maxQuantity = maxOf(1, listing.quantity)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Request Campus Handover",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Summary of listing
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.md,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val img = listing.coverImageUrl
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(AppRadius.sm)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!img.isNullOrBlank()) {
                            AsyncImage(
                                model = img,
                                contentDescription = listing.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.md))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = listing.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${MarketplaceUtils.formatNaira(listing.price)} each • (${listing.quantity} available)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quantity Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quantity Needed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        enabled = quantity > 1 && !isSubmitting,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        onClick = { if (quantity < maxQuantity) quantity++ },
                        enabled = quantity < maxQuantity && !isSubmitting,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Total amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated Handover Total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = MarketplaceUtils.formatNaira(listing.price * quantity),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Campus Handover Location Picker
            Text(
                text = "Preferred UNIMAID Meetup Spot",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(modifier = Modifier.height(130.dp)) {
                items(UNIMAID_CAMPUS_LOCATIONS) { loc ->
                    val isSelected = !isCustomLocation && selectedLocation == loc
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(AppRadius.sm)
                            .clickable {
                                isCustomLocation = false
                                selectedLocation = loc
                            },
                        shape = AppRadius.sm,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Note for seller (optional)") },
                placeholder = { Text("e.g. Free after 2pm lecture; call when nearby") },
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.md,
                maxLines = 2,
                enabled = !isSubmitting
            )

            // Security callout
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.sm,
                colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        tint = UnimaidGoldOnContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pay in person upon inspection. UNIMAID StudentMarket never charges card fees or asks for advance payments.",
                        style = MaterialTheme.typography.labelSmall,
                        color = UnimaidGoldOnContainer
                    )
                }
            }

            // Submit Button
            UnimaidPillButton(
                text = if (isSubmitting) "Submitting Request..." else "Send Handover Request (₦${"%,.0f".format(listing.price * quantity)})",
                onClick = {
                    val finalLoc = if (isCustomLocation && customLocation.isNotBlank()) customLocation else selectedLocation
                    onSubmitRequest(quantity, finalLoc, notes)
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                containerColor = VerifiedGreen,
                contentColor = Color.White,
                testTag = "detail_submit_handover_request_button"
            )
        }
    }
}

@Composable
private fun ProductDetailsSkeleton(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(AppSpacing.md)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = AppSpacing.md)
                .clip(AppRadius.lg),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {}

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Column(modifier = Modifier.padding(horizontal = AppSpacing.screenPadding)) {
            Surface(
                modifier = Modifier
                    .width(160.dp)
                    .height(32.dp)
                    .clip(AppRadius.md),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {}

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(AppRadius.sm),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {}

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(AppRadius.md),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {}
        }
    }
}
