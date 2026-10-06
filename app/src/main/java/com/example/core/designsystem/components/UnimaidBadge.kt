package com.example.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.core.theme.WarningAmber
import com.example.core.theme.WarningAmberContainer

@Composable
fun UnimaidVerifiedBadge(
    modifier: Modifier = Modifier,
    isVerified: Boolean = true
) {
    if (isVerified) {
        UnimaidBadge(
            text = "UNIMAID Verified",
            icon = Icons.Default.Verified,
            containerColor = VerifiedGreenContainer,
            contentColor = VerifiedGreenOnContainer,
            modifier = modifier
        )
    } else {
        UnimaidBadge(
            text = "Unverified Student",
            icon = Icons.Default.Info,
            containerColor = WarningAmberContainer,
            contentColor = WarningAmber,
            modifier = modifier
        )
    }
}

@Composable
fun CampusMeetupBadge(
    modifier: Modifier = Modifier
) {
    UnimaidBadge(
        text = "Campus In-Person Only",
        icon = Icons.Default.CheckCircle,
        containerColor = UnimaidBlueContainer,
        contentColor = UnimaidBlueOnContainer,
        modifier = modifier
    )
}

@Composable
fun UnimaidBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = modifier
            .clip(AppRadius.full)
            .background(containerColor)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
