package com.example.ui.screens.notifications

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.components.ButtonVariant
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.session.SessionState
import com.example.core.theme.AccentCoral
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.VerifiedGreen
import com.example.core.utils.MarketplaceUtils
import com.example.data.models.Notification
import com.example.data.models.NotificationTypes
import com.example.data.repository.AuthRepository
import com.example.ui.viewmodels.NotificationViewModel
import kotlinx.coroutines.launch

/**
 * Modern, User-Specific Real-time Notification Center for UNIMAID StudentMarket.
 * Displays real notifications from Supabase with:
 * - Badges and unread indicators
 * - Grouping by Today, Yesterday, and Earlier
 * - Deep navigation to Messages, Orders, Product Details, or Verification
 * - Mark as read / Mark all as read
 * - Swipe-to-dismiss deletion
 * - Pull to refresh and polished empty/loading/error states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onNavigateToConversation: (recipientId: String?, listingId: String?) -> Unit,
    onNavigateToOrders: (initialTab: String) -> Unit,
    onNavigateToProductDetails: (listingId: String) -> Unit,
    onNavigateToVerification: () -> Unit,
    onNavigateToLogin: () -> Unit,
    notificationViewModel: NotificationViewModel,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionState by authRepository.sessionState.collectAsState()
    val uiState by notificationViewModel.uiState.collectAsState()

    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId

    // Synchronize user session with ViewModel
    LaunchedEffect(currentUserId) {
        notificationViewModel.onUserSessionChanged(currentUserId)
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            UnimaidTopAppBar(
                title = "Notifications",
                subtitle = if (uiState.unreadCount > 0) "${uiState.unreadCount} unread" else "Campus activity",
                onBackClick = onNavigateBack,
                actions = {
                    if (uiState.unreadCount > 0) {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                notificationViewModel.markAllAsRead()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("All notifications marked as read")
                                }
                            },
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            notificationViewModel.refresh()
                        },
                        modifier = Modifier.testTag("refresh_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                currentUserId.isNullOrBlank() -> {
                    // Unauthenticated state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppSpacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        UnimaidEmptyState(
                            title = "Sign In Required",
                            subtitle = "Sign in with your UNIMAID student account to view notifications and stay updated on transactions.",
                            icon = Icons.Outlined.NotificationsNone
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.lg))
                        UnimaidButton(
                            text = "Sign In",
                            onClick = onNavigateToLogin,
                            variant = ButtonVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth(0.6f),
                            testTag = "notification_login_button"
                        )
                    }
                }

                uiState.isLoading && uiState.notifications.isEmpty() -> {
                    // Loading skeleton
                    NotificationSkeletonList()
                }

                uiState.notifications.isEmpty() -> {
                    // Polished empty state
                    val pullState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { notificationViewModel.refresh() },
                        state = pullState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            item {
                                UnimaidEmptyState(
                                    title = "You're all caught up!",
                                    subtitle = "No campus notifications yet. We'll alert you when someone sends a message, places an order, or updates a meetup.",
                                    icon = Icons.Default.CheckCircle
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Populated notifications list with grouped headers
                    val pullState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { notificationViewModel.refresh() },
                        state = pullState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("notifications_list"),
                            contentPadding = PaddingValues(bottom = AppSpacing.xl)
                        ) {
                            if (uiState.todayNotifications.isNotEmpty()) {
                                item {
                                    NotificationSectionHeader(title = "Today")
                                }
                                items(
                                    items = uiState.todayNotifications,
                                    key = { it.id }
                                ) { notification ->
                                    SwipeableNotificationItem(
                                        notification = notification,
                                        onTap = {
                                            handleNotificationClick(
                                                notification = notification,
                                                notificationViewModel = notificationViewModel,
                                                onNavigateToConversation = onNavigateToConversation,
                                                onNavigateToOrders = onNavigateToOrders,
                                                onNavigateToProductDetails = onNavigateToProductDetails,
                                                onNavigateToVerification = onNavigateToVerification
                                            )
                                        },
                                        onDismiss = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            notificationViewModel.deleteNotification(notification.id)
                                        }
                                    )
                                }
                            }

                            if (uiState.yesterdayNotifications.isNotEmpty()) {
                                item {
                                    NotificationSectionHeader(title = "Yesterday")
                                }
                                items(
                                    items = uiState.yesterdayNotifications,
                                    key = { it.id }
                                ) { notification ->
                                    SwipeableNotificationItem(
                                        notification = notification,
                                        onTap = {
                                            handleNotificationClick(
                                                notification = notification,
                                                notificationViewModel = notificationViewModel,
                                                onNavigateToConversation = onNavigateToConversation,
                                                onNavigateToOrders = onNavigateToOrders,
                                                onNavigateToProductDetails = onNavigateToProductDetails,
                                                onNavigateToVerification = onNavigateToVerification
                                            )
                                        },
                                        onDismiss = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            notificationViewModel.deleteNotification(notification.id)
                                        }
                                    )
                                }
                            }

                            if (uiState.earlierNotifications.isNotEmpty()) {
                                item {
                                    NotificationSectionHeader(title = "Earlier")
                                }
                                items(
                                    items = uiState.earlierNotifications,
                                    key = { it.id }
                                ) { notification ->
                                    SwipeableNotificationItem(
                                        notification = notification,
                                        onTap = {
                                            handleNotificationClick(
                                                notification = notification,
                                                notificationViewModel = notificationViewModel,
                                                onNavigateToConversation = onNavigateToConversation,
                                                onNavigateToOrders = onNavigateToOrders,
                                                onNavigateToProductDetails = onNavigateToProductDetails,
                                                onNavigateToVerification = onNavigateToVerification
                                            )
                                        },
                                        onDismiss = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            notificationViewModel.deleteNotification(notification.id)
                                        }
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

private fun handleNotificationClick(
    notification: Notification,
    notificationViewModel: NotificationViewModel,
    onNavigateToConversation: (recipientId: String?, listingId: String?) -> Unit,
    onNavigateToOrders: (initialTab: String) -> Unit,
    onNavigateToProductDetails: (listingId: String) -> Unit,
    onNavigateToVerification: () -> Unit
) {
    if (!notification.isRead) {
        notificationViewModel.markAsRead(notification.id)
    }

    when (notification.type.uppercase()) {
        NotificationTypes.MESSAGE -> {
            onNavigateToConversation(notification.senderId, notification.listingId)
        }

        NotificationTypes.PURCHASE_REQUEST -> {
            // Seller view: open sales tab
            onNavigateToOrders(notification.orderTab ?: "sales")
        }

        NotificationTypes.PURCHASE_ACCEPTED,
        NotificationTypes.PURCHASE_DECLINED,
        NotificationTypes.TRANSACTION_UPDATE,
        NotificationTypes.HANDOVER_UPDATE,
        NotificationTypes.TRANSACTION_COMPLETED,
        "ORDER" -> {
            onNavigateToOrders(notification.orderTab ?: "purchases")
        }

        NotificationTypes.LISTING_UPDATE -> {
            val lid = notification.listingId
            if (!lid.isNullOrBlank()) {
                onNavigateToProductDetails(lid)
            } else {
                onNavigateToOrders("purchases")
            }
        }

        NotificationTypes.VERIFICATION -> {
            onNavigateToVerification()
        }

        else -> {
            // Check payload entity ID
            val lid = notification.listingId
            if (!lid.isNullOrBlank()) {
                onNavigateToProductDetails(lid)
            } else {
                onNavigateToOrders("purchases")
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.sm)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationItem(
    notification: Notification,
    onTap: () -> Unit,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.md, vertical = 4.dp)
                    .clip(AppRadius.md)
                    .background(AccentCoral),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete notification",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = AppSpacing.lg)
                )
            }
        }
    ) {
        NotificationCard(
            notification = notification,
            onClick = onTap
        )
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    val isUnread = !notification.isRead
    val (icon, iconTint, iconBg) = getNotificationIcon(notification.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("notification_card_${notification.id}"),
        shape = AppRadius.md,
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            // Icon Indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.md))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(AppSpacing.xs))

                    Text(
                        text = MarketplaceUtils.formatRelativeTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.displayBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Unread Dot
            if (isUnread) {
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun getNotificationIcon(type: String): Triple<ImageVector, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    return when (type.uppercase()) {
        NotificationTypes.MESSAGE -> Triple(
            Icons.AutoMirrored.Filled.Chat,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )

        NotificationTypes.PURCHASE_REQUEST -> Triple(
            Icons.Default.ReceiptLong,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer
        )

        NotificationTypes.PURCHASE_ACCEPTED -> Triple(
            Icons.Default.Handshake,
            VerifiedGreen,
            VerifiedGreen.copy(alpha = 0.2f)
        )

        NotificationTypes.PURCHASE_DECLINED -> Triple(
            Icons.Default.ReceiptLong,
            AccentCoral,
            AccentCoral.copy(alpha = 0.2f)
        )

        NotificationTypes.HANDOVER_UPDATE -> Triple(
            Icons.Default.Handshake,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer
        )

        NotificationTypes.TRANSACTION_COMPLETED -> Triple(
            Icons.Default.CheckCircle,
            VerifiedGreen,
            VerifiedGreen.copy(alpha = 0.2f)
        )

        NotificationTypes.LISTING_UPDATE -> Triple(
            Icons.Default.Inventory2,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )

        NotificationTypes.VERIFICATION -> Triple(
            Icons.Default.Verified,
            VerifiedGreen,
            VerifiedGreen.copy(alpha = 0.2f)
        )

        NotificationTypes.SECURITY -> Triple(
            Icons.Default.Security,
            AccentCoral,
            AccentCoral.copy(alpha = 0.2f)
        )

        else -> Triple(
            Icons.Default.NotificationsActive,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
private fun NotificationSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.md)
    ) {
        repeat(5) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = AppRadius.md,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(14.dp)
                                .clip(AppRadius.xs)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp)
                                .clip(AppRadius.xs)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}
