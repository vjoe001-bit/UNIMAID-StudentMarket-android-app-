package com.example.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.theme.AppRadius
import com.example.core.theme.UnimaidGold

/**
 * Interactive or read-only Star Rating bar.
 */
@Composable
fun StarRatingBar(
    rating: Int,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    starSize: Dp = 24.dp,
    isInteractive: Boolean = false,
    onRatingChanged: ((Int) -> Unit)? = null,
    activeColor: Color = UnimaidGold,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxRating) {
            val isFilled = i <= rating
            val icon = if (isFilled) Icons.Default.Star else Icons.Outlined.StarBorder
            val tint = if (isFilled) activeColor else inactiveColor

            Icon(
                imageVector = icon,
                contentDescription = "$i stars",
                tint = tint,
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (isInteractive && onRatingChanged != null) {
                            Modifier
                                .clickable { onRatingChanged(i) }
                                .testTag("star_rating_$i")
                        } else Modifier
                    )
            )
        }
    }
}

/**
 * Visual distribution breakdown for 1 to 5 star ratings.
 */
@Composable
fun RatingDistributionBar(
    starLevel: Int,
    count: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) count.toFloat() / totalCount else 0f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$starLevel",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = UnimaidGold,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = UnimaidGold,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
    }
}
