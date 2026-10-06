package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.theme.AppElevation
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing

@Composable
fun UnimaidCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = AppElevation.low,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color? = MaterialTheme.colorScheme.outlineVariant,
    testTag: String = "unimaid_card",
    content: @Composable ColumnScope.() -> Unit
) {
    val border = borderColor?.let { BorderStroke(1.dp, it) }

    Card(
        modifier = modifier
            .testTag(testTag)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(AppRadius.lg)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = AppRadius.lg,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = border
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.cardPadding),
            content = content
        )
    }
}
