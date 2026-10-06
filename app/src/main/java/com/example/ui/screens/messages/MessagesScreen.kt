package com.example.ui.screens.messages

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.designsystem.components.ButtonVariant
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.network.SupabaseResult
import com.example.core.session.SessionState
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.utils.MarketplaceUtils
import com.example.data.models.Conversation
import com.example.data.models.Message
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ChatRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real-time Student Peer-to-Peer Messaging Screen.
 * Provides Facebook/WhatsApp style chat with counterparty profile, product context card,
 * UNIMAID handover quick chips, and live message synchronization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    authRepository: AuthRepository,
    onNavigateToLogin: () -> Unit,
    onExploreMarketplace: () -> Unit,
    modifier: Modifier = Modifier,
    initialRecipientId: String? = null,
    initialListingId: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToProduct: ((String) -> Unit)? = null,
    chatRepository: ChatRepository = remember { ChatRepositoryImpl() }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionState by authRepository.sessionState.collectAsState()
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoadingConversations by remember { mutableStateOf(false) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoadingMessages by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var isSendingMessage by remember { mutableStateOf(false) }

    fun loadConversationsList(showSpinner: Boolean = true) {
        if (currentUserId.isNullOrBlank()) return
        coroutineScope.launch {
            if (showSpinner) isLoadingConversations = true
            when (val res = chatRepository.getConversations(currentUserId)) {
                is SupabaseResult.Success -> {
                    conversations = res.data
                    isLoadingConversations = false
                }
                is SupabaseResult.Error -> {
                    isLoadingConversations = false
                }
                else -> {
                    isLoadingConversations = false
                }
            }
        }
    }

    fun loadMessagesForActiveConversation(showSpinner: Boolean = false) {
        val convId = activeConversation?.id ?: return
        coroutineScope.launch {
            if (showSpinner) isLoadingMessages = true
            when (val res = chatRepository.getMessages(convId)) {
                is SupabaseResult.Success -> {
                    messages = res.data
                    isLoadingMessages = false
                }
                is SupabaseResult.Error -> {
                    isLoadingMessages = false
                }
                else -> {
                    isLoadingMessages = false
                }
            }
        }
    }

    // Auto-create or fetch conversation if launched with specific recipient
    LaunchedEffect(currentUserId, initialRecipientId) {
        if (!currentUserId.isNullOrBlank() && !initialRecipientId.isNullOrBlank()) {
            if (currentUserId == initialRecipientId) {
                Toast.makeText(context, "You cannot message yourself", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }
            isLoadingConversations = true
            when (val res = chatRepository.getOrCreateConversation(currentUserId, initialRecipientId, initialListingId)) {
                is SupabaseResult.Success -> {
                    activeConversation = res.data
                    loadMessagesForActiveConversation(showSpinner = true)
                    isLoadingConversations = false
                }
                is SupabaseResult.Error -> {
                    isLoadingConversations = false
                    Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    isLoadingConversations = false
                }
            }
        }
    }

    // Periodic live sync while in active chat
    LaunchedEffect(activeConversation?.id) {
        val convId = activeConversation?.id ?: return@LaunchedEffect
        loadMessagesForActiveConversation(showSpinner = true)
        while (isActive) {
            delay(3500)
            if (activeConversation?.id == convId) {
                loadMessagesForActiveConversation(showSpinner = false)
            }
        }
    }

    // Initial load of conversation inbox
    LaunchedEffect(currentUserId) {
        if (!currentUserId.isNullOrBlank()) {
            loadConversationsList(showSpinner = true)
        }
    }

    // Back button handling: If inside an active conversation, return to inbox
    BackHandler(enabled = activeConversation != null) {
        activeConversation = null
        loadConversationsList(showSpinner = false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val current = sessionState) {
            is SessionState.Authenticated -> {
                val myUid = current.session.userId

                if (activeConversation != null) {
                    val conv = activeConversation!!
                    val counterpartyId = conv.getCounterpartyId(myUid)
                    val counterparty = conv.getCounterpartyProfile(myUid)
                    val counterpartyName = counterparty?.displayName ?: "UNIMAID Student"
                    val isVerified = counterparty?.isStudentVerified == true
                    val listing = conv.listing

                    // Chat Screen Top Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                activeConversation = null
                                loadConversationsList(showSpinner = false)
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to messages"
                                )
                            }

                            // Counterparty Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!counterparty?.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = counterparty?.avatarUrl,
                                        contentDescription = counterpartyName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = counterpartyName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = counterpartyName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified UNIMAID Student",
                                            tint = VerifiedGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = counterparty?.department ?: "UNIMAID Student",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            IconButton(onClick = { loadMessagesForActiveConversation(showSpinner = true) }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh messages",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Product Context Banner (if conversation was opened for a listing)
                    if (listing != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToProduct?.invoke(listing.id)
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val coverImg = listing.coverImageUrl
                                if (!coverImg.isNullOrBlank()) {
                                    AsyncImage(
                                        model = coverImg,
                                        contentDescription = listing.title,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(AppRadius.xs),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = listing.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = MarketplaceUtils.formatNaira(listing.price),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "View Item",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Messages Lazy Column
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (isLoadingMessages && messages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (messages.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(AppSpacing.lg),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Start Conversation with $counterpartyName",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Agree on a safe public campus location (Library, Ramat Hall, Complex Gate) for inspection and cash exchange.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    val isMe = msg.senderId == myUid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 16.dp
                                            ),
                                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth(0.82f)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                Text(
                                                    text = msg.displayContent,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = MarketplaceUtils.formatRelativeTime(msg.createdAt),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.align(Alignment.End)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // UNIMAID Quick Meetup Suggestion Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = AppSpacing.md, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val suggestions = listOf(
                            "Is this item still available?",
                            "Can we meet at the University Library?",
                            "Can we meet at Ramat Hall?",
                            "Can we meet at Complex Gate?",
                            "I have cash ready for physical handover."
                        )
                        suggestions.forEach { suggestion ->
                            Surface(
                                shape = AppRadius.full,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable {
                                    messageText = suggestion
                                }
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Input Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.md, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = { Text("Message $counterpartyName...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field"),
                                shape = AppRadius.full,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    val text = messageText.trim()
                                    if (text.isBlank() || isSendingMessage) return@IconButton
                                    isSendingMessage = true
                                    coroutineScope.launch {
                                        when (val sendRes = chatRepository.sendMessage(
                                            conversationId = conv.id,
                                            senderId = myUid,
                                            receiverId = counterpartyId,
                                            content = text
                                        )) {
                                            is SupabaseResult.Success -> {
                                                messageText = ""
                                                loadMessagesForActiveConversation(showSpinner = false)
                                                isSendingMessage = false
                                            }
                                            is SupabaseResult.Error -> {
                                                isSendingMessage = false
                                                Toast.makeText(context, sendRes.userFriendlyMessage, Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                isSendingMessage = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("chat_send_button"),
                                enabled = messageText.isNotBlank() && !isSendingMessage
                            ) {
                                if (isSendingMessage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send message",
                                        tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Inbox / Conversations List View
                    UnimaidTopAppBar(
                        title = "Student Messages",
                        subtitle = "Peer-to-Peer Campus Communication",
                        actions = {
                            IconButton(onClick = { loadConversationsList(showSpinner = true) }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh conversations"
                                )
                            }
                        }
                    )

                    if (isLoadingConversations) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (conversations.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(AppSpacing.screenPadding)
                        ) {
                            UnimaidEmptyState(
                                title = "No Conversations Yet",
                                subtitle = "Direct peer-to-peer messaging between UNIMAID buyers and sellers. When you message a student seller about a textbook, electronics, or campus service, your chat will appear here.",
                                icon = Icons.AutoMirrored.Outlined.Chat,
                                actionText = "Browse Items to Message Sellers",
                                onActionClick = onExploreMarketplace
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(conversations, key = { it.id }) { conv ->
                                val otherUser = conv.getCounterpartyProfile(myUid)
                                val otherName = otherUser?.displayName ?: "UNIMAID Student"
                                val isVerified = otherUser?.isStudentVerified == true
                                val listingTitle = conv.listing?.displayTitle

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppRadius.md)
                                        .clickable {
                                            activeConversation = conv
                                        }
                                        .testTag("conversation_card_${conv.id}"),
                                    shape = AppRadius.md,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(AppSpacing.md),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!otherUser?.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = otherUser?.avatarUrl,
                                                    contentDescription = otherName,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = otherName.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = otherName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (isVerified) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Verified,
                                                            contentDescription = "Verified",
                                                            tint = VerifiedGreen,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = MarketplaceUtils.formatRelativeTime(conv.updatedAt ?: conv.createdAt),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            if (!listingTitle.isNullOrBlank()) {
                                                Text(
                                                    text = "Item: $listingTitle",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Tap to view exchange & chat",
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
                    }
                }
            }
            else -> {
                // Guest visitor prompt state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.screenPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(UnimaidBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    Text(
                        text = "Sign In to Message Students",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    Text(
                        text = "Direct campus messaging and physical meetups require you to log in with your UNIMAID student account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xl))

                    UnimaidButton(
                        text = "Sign In to Your Account",
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "messages_signin_button"
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    UnimaidButton(
                        text = "Browse Campus Marketplace as Guest",
                        onClick = onExploreMarketplace,
                        variant = ButtonVariant.TEXT,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
