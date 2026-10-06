package com.example.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.theme.AppSpacing

@Composable
fun UnimaidLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    caption: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(size),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            if (!caption.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
