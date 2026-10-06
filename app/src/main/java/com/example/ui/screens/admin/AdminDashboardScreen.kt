package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.components.UnimaidEmptyState
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer

@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnimaidTopAppBar(
            title = "Admin & Moderation Portal",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacing.screenPadding)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.lg,
                colors = CardDefaults.cardColors(containerColor = UnimaidBlueContainer)
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = UnimaidBlueOnContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Column {
                        Text(
                            text = "Campus Administration System",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = UnimaidBlueOnContainer
                        )
                        Text(
                            text = "Role-based control: Main Admin, Secondary Admins, Student Verification Officers & Content Moderators.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UnimaidBlueOnContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            UnimaidEmptyState(
                title = "Administration Framework Configured",
                subtitle = "Connected to Supabase tables: admin_roles, verification_requests, reports, and blocked_users. Administrative moderation panels will activate in Phase B.",
                icon = Icons.Default.AdminPanelSettings,
                actionText = "Return to Profile",
                onActionClick = onNavigateBack
            )
        }
    }
}
