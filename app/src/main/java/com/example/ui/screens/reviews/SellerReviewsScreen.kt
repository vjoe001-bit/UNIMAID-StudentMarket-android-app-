package com.example.ui.screens.reviews

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.designsystem.components.RatingDistributionBar
import com.example.core.designsystem.components.StarRatingBar
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidPillButton
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.designsystem.components.UnimaidVerifiedBadge
import com.example.core.network.SupabaseResult
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidGold
import com.example.core.theme.VerifiedGreen
import com.example.core.utils.MarketplaceUtils
import com.example.data.models.Profile
import com.example.data.models.Review
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ProfileRepositoryImpl
import com.example.data.repository.ReviewRepository
import com.example.data.repository.ReviewRepositoryImpl
import com.example.data.repository.SellerRatingSummary
import kotlinx.coroutines.launch

/**
 * Dedicated Reviews Screen for a UNIMAID student seller.
 * Displays:
 * - Seller identity & verified badge
 * - Real calculated average star rating & rating breakdown bars
 * - Full list of legitimate buyer reviews with purchased product context
 * - Review reporting dialog for inappropriate content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerReviewsScreen(
    sellerId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: String? = null,
    reviewRepository: ReviewRepository = remember { ReviewRepositoryImpl() },
    profileRepository: ProfileRepository = remember { ProfileRepositoryImpl() }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sellerProfile by remember { mutableStateOf<Profile?>(null) }
    var ratingSummary by remember { mutableStateOf(SellerRatingSummary()) }
    var reviewsList by remember { mutableStateOf<List<Review>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var reviewToReport by remember { mutableStateOf<Review?>(null) }
    var reportReason by remember { mutableStateOf("Inappropriate Content") }
    var reportNote by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }

    fun loadData() {
        coroutineScope.launch {
            // Load seller profile
            when (val profRes = profileRepository.getProfile(sellerId)) {
                is SupabaseResult.Success -> sellerProfile = profRes.data
                else -> {}
            }

            // Load reviews & summary
            when (val revRes = reviewRepository.getReviewsForSeller(sellerId)) {
                is SupabaseResult.Success -> {
                    reviewsList = revRes.data
                }
                else -> {}
            }

            when (val sumRes = reviewRepository.calculateSellerRating(sellerId)) {
                is SupabaseResult.Success -> ratingSummary = sumRes.data
                else -> {}
            }

            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(sellerId) {
        loadData()
    }

    Scaffold(
        topBar = {
            UnimaidTopAppBar(
                title = "Seller Reputation",
                subtitle = sellerProfile?.displayName ?: "Verified Student Reviews",
                onBackClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadData()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    // Seller Identity & Overall Rating Banner
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("seller_reputation_header_card"),
                            shape = AppRadius.lg,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(AppSpacing.md)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!sellerProfile?.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = sellerProfile!!.avatarUrl,
                                                contentDescription = "Seller avatar",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(AppSpacing.md))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = sellerProfile?.displayName ?: "Student Seller",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (sellerProfile?.isStudentVerified == true) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                UnimaidVerifiedBadge(isVerified = true)
                                            }
                                        }

                                        val dept = listOfNotNull(sellerProfile?.department, sellerProfile?.faculty).filter { it.isNotBlank() }.joinToString(" • ")
                                        if (dept.isNotBlank()) {
                                            Text(
                                                text = dept,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        sellerProfile?.level?.let { lvl ->
                                            Text(
                                                text = "$lvl Level Student",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.md))

                                // Rating summary numbers + breakdown
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(end = AppSpacing.lg)
                                    ) {
                                        Text(
                                            text = if (ratingSummary.totalReviews > 0) "%.1f".format(ratingSummary.averageRating) else "—",
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        StarRatingBar(
                                            rating = ratingSummary.averageRating.toInt(),
                                            starSize = 16.dp
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = if (ratingSummary.totalReviews > 0) "${ratingSummary.totalReviews} reviews" else "No reviews yet",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        RatingDistributionBar(starLevel = 5, count = ratingSummary.fiveStarCount, totalCount = ratingSummary.totalReviews)
                                        RatingDistributionBar(starLevel = 4, count = ratingSummary.fourStarCount, totalCount = ratingSummary.totalReviews)
                                        RatingDistributionBar(starLevel = 3, count = ratingSummary.threeStarCount, totalCount = ratingSummary.totalReviews)
                                        RatingDistributionBar(starLevel = 2, count = ratingSummary.twoStarCount, totalCount = ratingSummary.totalReviews)
                                        RatingDistributionBar(starLevel = 1, count = ratingSummary.oneStarCount, totalCount = ratingSummary.totalReviews)
                                    }
                                }
                            }
                        }
                    }

                    // Section Title
                    item {
                        Text(
                            text = "Student Buyer Feedback (${reviewsList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Empty State or List of Reviews
                    if (reviewsList.isEmpty()) {
                        item {
                            UnimaidEmptyState(
                                title = "No Reviews Yet",
                                subtitle = "This student hasn't received reviews from campus handovers yet. Be the first to buy and leave genuine feedback!",
                                icon = Icons.Default.Star
                            )
                        }
                    } else {
                        items(reviewsList, key = { it.id }) { rev ->
                            ReviewItemCard(
                                review = rev,
                                onNavigateToProduct = onNavigateToProduct,
                                onReport = { reviewToReport = rev }
                            )
                        }
                    }
                }
            }
        }
    }

    // Report Review Dialog
    reviewToReport?.let { rev ->
        AlertDialog(
            onDismissRequest = { reviewToReport = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Report Review", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Why are you reporting this review?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val reasons = listOf("Inappropriate Language", "Harassment / Defamation", "Fake / Manipulative Content", "Spam")
                    reasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = r }
                                .padding(vertical = 4.dp)
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = reportReason == r,
                                onClick = { reportReason = r }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = r, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    OutlinedTextField(
                        value = reportNote,
                        onValueChange = { reportNote = it },
                        label = { Text("Additional Details (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                UnimaidPillButton(
                    text = if (isSubmittingReport) "Submitting..." else "Submit Report",
                    containerColor = MaterialTheme.colorScheme.error,
                    enabled = !isSubmittingReport,
                    onClick = {
                        val reporter = currentUserId ?: rev.reviewerId
                        isSubmittingReport = true
                        coroutineScope.launch {
                            val res = reviewRepository.reportReview(
                                reporterId = reporter,
                                reviewId = rev.id,
                                reportedUserId = rev.reviewerId,
                                reason = reportReason,
                                description = reportNote
                            )
                            isSubmittingReport = false
                            reviewToReport = null
                            when (res) {
                                is SupabaseResult.Success -> {
                                    Toast.makeText(context, "Report submitted to campus moderators.", Toast.LENGTH_SHORT).show()
                                }
                                is SupabaseResult.Error -> {
                                    Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_SHORT).show()
                                }
                                else -> {}
                            }
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { reviewToReport = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReviewItemCard(
    review: Review,
    onNavigateToProduct: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReport: (() -> Unit)? = null
) {
    val reviewer = review.reviewer
    val listing = review.listing

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}"),
        shape = AppRadius.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md)
        ) {
            // Reviewer Info & Star Rating Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!reviewer?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = reviewer!!.avatarUrl,
                                contentDescription = "Reviewer",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.sm))

                    Column {
                        Text(
                            text = reviewer?.displayName ?: "UNIMAID Buyer",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        review.createdAt?.let { dt ->
                            Text(
                                text = MarketplaceUtils.formatRelativeTime(dt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRatingBar(rating = review.rating, starSize = 16.dp)
                    if (onReport != null) {
                        IconButton(onClick = onReport, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report review",
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Review comment if present
            if (!review.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }

            // Purchased Item Context Pill
            if (listing != null) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppRadius.sm)
                        .clickable { onNavigateToProduct(listing.id) },
                    shape = AppRadius.sm,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Item: ${listing.displayTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
