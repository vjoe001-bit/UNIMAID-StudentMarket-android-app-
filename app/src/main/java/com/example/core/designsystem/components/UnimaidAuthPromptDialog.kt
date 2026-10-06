package com.example.core.designsystem.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.branding.BrandConfig
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidBlueContainer

/**
 * Professional UNIMAID Student Authentication Dialog.
 *
 * Displayed when an unauthenticated visitor attempts an action requiring an authenticated student
 * account (e.g. posting a listing, contacting a seller, saving favorites, reserving an item).
 * Preserves guest browsing while guiding students to log in or register.
 */
@Composable
fun UnimaidAuthPromptDialog(
    actionTitle: String = "Student Account Required",
    actionDescription: String = "To post listings, contact student sellers, save favorites, or arrange campus handovers, please sign in with your UNIMAID student account.",
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .testTag("auth_prompt_dialog"),
            shape = AppRadius.xl,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Dismiss Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // UNIMAID Crest / Lock Badge
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(UnimaidBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = BrandConfig.OFFICIAL_LOGO_RES),
                        contentDescription = "University Crest",
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                Text(
                    text = actionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Text(
                    text = actionDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppSpacing.xl))

                // Primary CTA: Sign In
                UnimaidButton(
                    text = "Sign In to Your Account",
                    onClick = {
                        onDismiss()
                        onNavigateToLogin()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "auth_prompt_signin_button"
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Secondary CTA: Register
                UnimaidButton(
                    text = "Create Student Account",
                    onClick = {
                        onDismiss()
                        onNavigateToSignUp()
                    },
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "auth_prompt_signup_button"
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                // Dismiss / Continue browsing
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("auth_prompt_cancel_button")
                ) {
                    Text(
                        text = "Continue Browsing as Guest",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
