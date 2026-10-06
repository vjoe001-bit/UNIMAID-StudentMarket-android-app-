package com.example.ui.screens.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.branding.BrandConfig
import com.example.core.designsystem.components.ButtonVariant
import com.example.core.designsystem.components.UnimaidBadge
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.LocalThemeMode
import com.example.core.theme.ThemeMode
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.UnimaidGoldContainer
import com.example.core.theme.UnimaidGoldOnContainer

/**
 * Premium Welcome Screen for UNIMAID StudentMarket.
 * Features smooth sequential reveal animation:
 * Logo -> Brand identity -> Hero presentation -> Campus safety card -> Primary GET STARTED CTA.
 */
@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToMarketplace: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalThemeMode.current
    val scrollState = rememberScrollState()

    // Smooth sequence entrance animation
    var animLogo by remember { mutableStateOf(false) }
    var animBrand by remember { mutableStateOf(false) }
    var animHero by remember { mutableStateOf(false) }
    var animActions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animLogo = true
        kotlinx.coroutines.delay(80)
        animBrand = true
        kotlinx.coroutines.delay(100)
        animHero = true
        kotlinx.coroutines.delay(100)
        animActions = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar: Theme Switcher & Campus Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnimaidBadge(
                text = "UNIMAID Campus Only",
                icon = Icons.Default.School,
                containerColor = UnimaidBlueContainer,
                contentColor = UnimaidBlueOnContainer
            )

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (currentTheme == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme (${currentTheme.displayName})",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Center Hero & Identity Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Logo Sequence
            AnimatedVisibility(
                visible = animLogo,
                enter = fadeIn(tween(350, easing = FastOutSlowInEasing)) +
                        scaleIn(tween(350, easing = FastOutSlowInEasing), initialScale = 0.85f)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(AppRadius.lg)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                        .testTag("unimaid_app_logo"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = BrandConfig.OFFICIAL_LOGO_RES),
                        contentDescription = BrandConfig.APP_NAME,
                        modifier = Modifier.size(68.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // 2. Brand Name & Tagline
            AnimatedVisibility(
                visible = animBrand,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { 20 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = BrandConfig.APP_NAME,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "University of Maiduguri Student Marketplace",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Text(
                        text = "The dedicated campus platform to buy, sell, exchange textbooks, hostel gear, electronics, and student services securely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = AppSpacing.sm)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 3. Visual Hero Presentation (Clean Campus Illustration/Banner)
            AnimatedVisibility(
                visible = animHero,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 30 }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("welcome_hero_card"),
                        shape = AppRadius.lg,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.unimaid_welcome_hero),
                                contentDescription = "UNIMAID Campus Student Trading",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Subtle bottom gradient pill overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(AppSpacing.sm)
                                    .clip(AppRadius.sm)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                            ) {
                                Text(
                                    text = "Campus Student Exchange",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Campus Policy Card (Crucial Business Rule: In-person handover)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("campus_policy_card"),
                        shape = AppRadius.md,
                        colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(AppSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = UnimaidGoldOnContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Column {
                                Text(
                                    text = "Physical In-Person Campus Handover",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = UnimaidGoldOnContainer
                                )
                                Text(
                                    text = BrandConfig.BUSINESS_POLICY_NO_ONLINE_PAYMENT,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UnimaidGoldOnContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // 4. Action Buttons
        AnimatedVisibility(
            visible = animActions,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 30 }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UnimaidButton(
                    text = "GET STARTED",
                    onClick = onNavigateToSignUp,
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "welcome_get_started_button"
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                UnimaidButton(
                    text = "I Have an Account • Sign In",
                    onClick = onNavigateToLogin,
                    variant = ButtonVariant.OUTLINED,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "welcome_login_button"
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                UnimaidButton(
                    text = "Explore Campus Marketplace as Guest",
                    onClick = onNavigateToMarketplace,
                    variant = ButtonVariant.TEXT,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "welcome_browse_button"
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                Text(
                    text = "University of Maiduguri • Built with pride by ${BrandConfig.CREATOR_ATTRIBUTION}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
