package com.example.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT,
    DANGER
}

@Composable
fun UnimaidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "unimaid_button"
) {
    val shape = AppRadius.md
    val minHeight = AppSpacing.minTouchTarget

    when (variant) {
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .heightIn(min = minHeight)
                    .testTag(testTag),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = AppSpacing.xl, vertical = AppSpacing.md)
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.onPrimary)
            }
        }
        ButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .heightIn(min = minHeight)
                    .testTag(testTag),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                contentPadding = PaddingValues(horizontal = AppSpacing.xl, vertical = AppSpacing.md)
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.onSecondary)
            }
        }
        ButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier
                    .heightIn(min = minHeight)
                    .testTag(testTag),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = AppSpacing.xl, vertical = AppSpacing.md)
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.primary)
            }
        }
        ButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .heightIn(min = minHeight)
                    .testTag(testTag),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = AppSpacing.xl, vertical = AppSpacing.md)
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.onError)
            }
        }
        ButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier
                    .heightIn(min = minHeight)
                    .testTag(testTag),
                enabled = enabled && !isLoading,
                shape = shape,
                contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
            ) {
                ButtonContent(text, isLoading, leadingIcon, trailingIcon, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    progressColor: Color
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.5.dp,
            color = progressColor
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(AppSpacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                it()
            }
        }
    }
}
