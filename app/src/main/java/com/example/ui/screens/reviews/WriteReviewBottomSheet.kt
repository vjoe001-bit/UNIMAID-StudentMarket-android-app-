package com.example.ui.screens.reviews

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.core.designsystem.components.StarRatingBar
import com.example.core.designsystem.components.UnimaidPillButton
import com.example.core.designsystem.components.UnimaidVerifiedBadge
import com.example.core.network.SupabaseResult
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidGold
import com.example.core.theme.UnimaidGoldContainer
import com.example.core.theme.UnimaidGoldOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.utils.MarketplaceUtils
import com.example.data.models.Order
import com.example.data.models.Review
import com.example.data.repository.ReviewRepository
import com.example.data.repository.ReviewRepositoryImpl
import kotlinx.coroutines.launch

/**
 * Professional Review Submission Bottom Sheet.
 * Allows verified buyers of completed campus transactions to submit a 1-5 star rating
 * and optional written feedback for the seller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewBottomSheet(
    order: Order,
    currentUserId: String,
    onDismiss: () -> Unit,
    onReviewSubmitted: (Review) -> Unit,
    reviewRepository: ReviewRepository = remember { ReviewRepositoryImpl() }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var rating by remember { mutableIntStateOf(5) }
    var commentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val seller = order.seller
    val listing = order.listing ?: order.effectiveItems.firstOrNull()?.listing
    val maxCommentLength = 500

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = AppSpacing.sm)
                    .size(width = 36.dp, height = 4.dp),
                shape = AppRadius.full,
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rate Student Seller",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Help your UNIMAID peers know who is trustworthy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                    Text("Cancel")
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Completed Handover Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.md,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
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
                                modifier = Modifier.size(44.dp),
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
                                text = seller?.displayName ?: "Student Seller",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (seller?.isStudentVerified == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                UnimaidVerifiedBadge(isVerified = true)
                            }
                        }

                        Text(
                            text = "Item: ${listing?.displayTitle ?: "Campus Listing"} • ${MarketplaceUtils.formatNaira(order.totalAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Star Rating Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (rating) {
                        5 -> "Excellent Experience (5/5)"
                        4 -> "Good Handover (4/5)"
                        3 -> "Average Experience (3/5)"
                        2 -> "Disappointing (2/5)"
                        1 -> "Poor Experience (1/5)"
                        else -> "Tap to Rate"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                StarRatingBar(
                    rating = rating,
                    starSize = 38.dp,
                    isInteractive = !isSubmitting,
                    onRatingChanged = { newRating ->
                        rating = newRating
                        errorMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Optional Review Text
            Text(
                text = "Written Review (Optional)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            OutlinedTextField(
                value = commentText,
                onValueChange = {
                    if (it.length <= maxCommentLength) {
                        commentText = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_comment_input"),
                placeholder = {
                    Text(
                        "Was the item in the described condition? Was the meeting at the library or faculty smooth and punctual?",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                minLines = 3,
                maxLines = 5,
                enabled = !isSubmitting,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        errorMessage?.let { err ->
                            Text(text = err, color = MaterialTheme.colorScheme.error)
                        } ?: Spacer(modifier = Modifier.width(1.dp))

                        Text(
                            text = "${commentText.length} / $maxCommentLength",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = AppRadius.md,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Safety notice reminder
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.sm,
                color = UnimaidGoldContainer
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        tint = UnimaidGoldOnContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reviews are visible to UNIMAID students to help maintain safety & trust in the marketplace.",
                        style = MaterialTheme.typography.labelSmall,
                        color = UnimaidGoldOnContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Submit Button
            UnimaidPillButton(
                text = if (isSubmitting) "Submitting Review..." else "Submit Review",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_review_button"),
                containerColor = VerifiedGreen,
                enabled = !isSubmitting && rating in 1..5,
                onClick = {
                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = reviewRepository.submitReview(
                            orderId = order.id,
                            reviewerId = currentUserId,
                            reviewedUserId = order.sellerId,
                            listingId = listing?.id,
                            rating = rating,
                            comment = commentText
                        )
                        when (result) {
                            is SupabaseResult.Success -> {
                                Toast.makeText(context, "Review submitted successfully! Thank you.", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                onReviewSubmitted(result.data)
                                onDismiss()
                            }
                            is SupabaseResult.Error -> {
                                isSubmitting = false
                                errorMessage = result.userFriendlyMessage
                                Toast.makeText(context, result.userFriendlyMessage, Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                isSubmitting = false
                            }
                        }
                    }
                }
            )
        }
    }
}
