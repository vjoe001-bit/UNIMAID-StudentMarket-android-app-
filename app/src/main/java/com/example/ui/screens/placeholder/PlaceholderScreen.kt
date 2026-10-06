package com.example.ui.screens.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.theme.AppSpacing

@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Construction
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnimaidTopAppBar(
            title = title,
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacing.screenPadding)
        ) {
            UnimaidEmptyState(
                title = "$title Foundation Ready",
                subtitle = subtitle,
                icon = icon,
                actionText = "Go Back",
                onActionClick = onNavigateBack
            )
        }
    }
}
