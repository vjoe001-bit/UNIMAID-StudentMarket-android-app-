package com.example.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.branding.BrandConfig
import com.example.core.designsystem.components.ButtonVariant
import com.example.core.designsystem.components.UnimaidBadge
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidErrorState
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.designsystem.components.UnimaidVerifiedBadge
import com.example.core.network.SupabaseResult
import com.example.core.session.SessionState
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.ErrorRed
import com.example.core.theme.ErrorRedContainer
import com.example.core.theme.LocalThemeMode
import com.example.core.theme.ThemeMode
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.UnimaidGold
import com.example.core.theme.UnimaidGoldContainer
import com.example.core.theme.UnimaidGoldOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.data.models.Profile
import com.example.data.models.StudentVerificationState
import com.example.data.models.VerificationRequest
import com.example.data.repository.AuthRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ProfileRepositoryImpl
import com.example.data.repository.VerificationRepository
import com.example.data.repository.VerificationRepositoryImpl
import kotlinx.coroutines.launch

/**
 * UNIMAID Student Profile Screen.
 *
 * Connected to Supabase profiles & verification_requests tables.
 * Displays student avatar, identity credentials, department, level,
 * email verification status, student verification standing, marketplace
 * activity, and theme settings.
 */
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    onNavigateToLogin: () -> Unit,
    onNavigateToVerification: () -> Unit,
    onNavigateToSellerHub: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfileCompletion: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMyListings: () -> Unit = onNavigateToSellerHub,
    onNavigateToOrders: (() -> Unit)? = null,
    profileRepository: ProfileRepository = ProfileRepositoryImpl(),
    verificationRepository: VerificationRepository = VerificationRepositoryImpl()
) {
    val sessionState by authRepository.sessionState.collectAsState()
    val currentTheme = LocalThemeMode.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isAuthenticated = sessionState is SessionState.Authenticated
    val session = (sessionState as? SessionState.Authenticated)?.session

    var profileData by remember { mutableStateOf<Profile?>(null) }
    var latestVerificationRequest by remember { mutableStateOf<VerificationRequest?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var updateSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    var myListingsCount by remember { mutableIntStateOf(0) }
    var myFavoritesCount by remember { mutableIntStateOf(0) }
    var myOrdersCount by remember { mutableIntStateOf(0) }

    fun refreshProfile(uid: String) {
        isLoadingProfile = true
        profileError = null
        scope.launch {
            when (val res = profileRepository.getProfile(uid)) {
                is SupabaseResult.Success -> {
                    profileData = res.data
                    isLoadingProfile = false
                    profileError = null
                }
                is SupabaseResult.Error -> {
                    isLoadingProfile = false
                    if (profileData == null) {
                        profileError = res.userFriendlyMessage
                    }
                }
                is SupabaseResult.Loading -> {}
            }
        }

        // Fetch verification status
        scope.launch {
            when (val vRes = verificationRepository.getVerificationStatus(uid)) {
                is SupabaseResult.Success -> {
                    latestVerificationRequest = vRes.data
                }
                else -> {}
            }
        }

        // Fetch activity metrics in parallel
        scope.launch {
            when (val listRes = profileRepository.getUserListings(uid)) {
                is SupabaseResult.Success -> myListingsCount = listRes.data.size
                else -> {}
            }
        }
        scope.launch {
            when (val favRes = profileRepository.getUserFavorites(uid)) {
                is SupabaseResult.Success -> myFavoritesCount = favRes.data.size
                else -> {}
            }
        }
        scope.launch {
            when (val ordRes = profileRepository.getUserOrders(uid)) {
                is SupabaseResult.Success -> myOrdersCount = ordRes.data.size
                else -> {}
            }
        }
    }

    // Load live profile and verification request from Supabase
    LaunchedEffect(session?.userId) {
        val uid = session?.userId
        if (!uid.isNullOrBlank()) {
            refreshProfile(uid)
        }
    }

    if (showEditProfileDialog && profileData != null) {
        EditProfileDialog(
            profile = profileData!!,
            profileRepository = profileRepository,
            onDismiss = { showEditProfileDialog = false },
            onProfileUpdated = { updated ->
                profileData = updated
                updateSuccessMessage = "Student profile updated successfully!"
            }
        )
    }

    val verificationState = remember(profileData, latestVerificationRequest, session) {
        if (profileData != null) {
            profileData!!.getVerificationState(latestVerificationRequest)
        } else if (session != null) {
            StudentVerificationState.fromCode(session.verificationStatus)
        } else {
            StudentVerificationState.UNVERIFIED
        }
    }

    val isProfileIncomplete = isAuthenticated && (profileData?.isComplete == false || session?.isProfileComplete == false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnimaidTopAppBar(title = "Student Profile")

        if (isAuthenticated && isLoadingProfile && profileData == null) {
            ProfileSkeletonContent()
        } else if (isAuthenticated && profileError != null && profileData == null) {
            UnimaidErrorState(
                title = "Couldn't Load Profile",
                message = profileError ?: "Unable to fetch student profile from server.",
                onRetry = { session?.userId?.let { refreshProfile(it) } },
                modifier = Modifier.padding(top = AppSpacing.xl)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.sm)
                    .padding(bottom = 90.dp)
            ) {
                // Profile update feedback banner
                AnimatedVisibility(
                    visible = updateSuccessMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    updateSuccessMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = AppSpacing.sm)
                                .testTag("profile_update_success_banner"),
                            colors = CardDefaults.cardColors(containerColor = VerifiedGreenContainer),
                            shape = AppRadius.md
                        ) {
                            Row(
                                modifier = Modifier.padding(AppSpacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VerifiedGreenOnContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.sm))
                                Text(
                                    text = msg,
                                    color = VerifiedGreenOnContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { updateSuccessMessage = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = VerifiedGreenOnContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            // Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_info_card"),
                shape = AppRadius.lg,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Photo
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val avatarUrl = profileData?.avatarUrl
                            if (!avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Student Photo",
                                    modifier = Modifier.size(72.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(AppSpacing.md))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profileData?.fullName ?: session?.fullName?.ifBlank { "UNIMAID Student" } ?: "Guest Student",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val deptAndLevel = listOfNotNull(
                                profileData?.department?.takeIf { it.isNotBlank() },
                                profileData?.level?.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")

                            if (deptAndLevel.isNotBlank()) {
                                Text(
                                    text = deptAndLevel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = session?.email ?: "Sign in to unlock student features",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.xs))

                            UnimaidVerifiedBadge(isVerified = verificationState == StudentVerificationState.VERIFIED)
                        }

                        if (isAuthenticated && profileData != null) {
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.testTag("profile_edit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Campus Bio
                    profileData?.bio?.takeIf { it.isNotBlank() }?.let { bioText ->
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Text(
                            text = bioText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = AppSpacing.xs)
                        )
                    }

                    if (isAuthenticated && profileData != null) {
                        Spacer(modifier = Modifier.height(AppSpacing.md))
                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppRadius.md
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text("Edit Profile & Photo")
                        }
                    }
                }
            }

            // Incomplete Profile Banner
            if (isProfileIncomplete) {
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProfileCompletion() }
                        .testTag("profile_completion_alert_card"),
                    shape = AppRadius.lg,
                    colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = UnimaidGoldOnContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profile Setup Incomplete",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = UnimaidGoldOnContainer
                            )
                            Text(
                                text = "Add your matric number, department, and phone to trade on campus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = UnimaidGoldOnContainer
                            )
                        }
                        UnimaidButton(
                            text = "Complete",
                            onClick = onNavigateToProfileCompletion,
                            modifier = Modifier.testTag("profile_complete_now_button")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Student Verification Status Card
            if (isAuthenticated) {
                VerificationStatusCard(
                    state = verificationState,
                    rejectionReason = latestVerificationRequest?.rejectionReason,
                    onVerifyClick = onNavigateToVerification,
                    modifier = Modifier.testTag("profile_verification_status_card")
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))
            }

            // Student Academic Credentials Card
            if (isAuthenticated && profileData != null) {
                Text(
                    text = "Academic Credentials",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_student_details_card"),
                    shape = AppRadius.lg,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        StudentInfoRow(
                            label = "Email Address",
                            value = session?.email ?: "Not Available",
                            icon = Icons.Outlined.Email,
                            badgeText = if (session?.isEmailConfirmed == true) "CONFIRMED" else "UNCONFIRMED"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.xs))

                        StudentInfoRow(
                            label = "Matriculation Number",
                            value = profileData?.matricNumber?.ifBlank { "Not provided yet" } ?: "Not provided yet",
                            icon = Icons.Outlined.Badge
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.xs))

                        StudentInfoRow(
                            label = "Faculty",
                            value = profileData?.faculty?.ifBlank { "University of Maiduguri" } ?: "University of Maiduguri",
                            icon = Icons.Default.School
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.xs))

                        StudentInfoRow(
                            label = "Department & Level",
                            value = "${profileData?.department ?: "Not set"} (${profileData?.level ?: "100L"})",
                            icon = Icons.Default.School
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.xs))

                        StudentInfoRow(
                            label = "Phone / WhatsApp",
                            value = profileData?.phoneNumber?.ifBlank { "Not set" } ?: "Not set",
                            icon = Icons.Default.Phone
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.xs))

                        StudentInfoRow(
                            label = "Account Status",
                            value = profileData?.accountStatus ?: "ACTIVE",
                            icon = Icons.Default.Security,
                            badgeText = profileData?.accountStatus ?: "ACTIVE"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))
            }

            // Marketplace Activity Summary Cards (Listings, Favorites, Orders)
            if (isAuthenticated) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    MetricCard(
                        title = "Listings",
                        count = myListingsCount,
                        icon = Icons.Default.Storefront,
                        onClick = onNavigateToMyListings,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_listings"
                    )
                    MetricCard(
                        title = "Favorites",
                        count = myFavoritesCount,
                        icon = Icons.Default.Favorite,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        testTag = "metric_favorites"
                    )
                    MetricCard(
                        title = "Orders",
                        count = myOrdersCount,
                        icon = Icons.Default.ReceiptLong,
                        onClick = { onNavigateToOrders?.invoke() },
                        modifier = Modifier.weight(1f),
                        testTag = "metric_orders"
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))
            }

            // Navigation Actions Card
            Text(
                text = "Marketplace Portals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.lg,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ProfileMenuRow(
                        title = "Student Identity Verification",
                        subtitle = "Submit UNIMAID ID card for badge review",
                        icon = Icons.Default.Shield,
                        onClick = onNavigateToVerification,
                        testTag = "menu_student_verification"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = AppSpacing.md))

                    ProfileMenuRow(
                        title = "My Campus Listings",
                        subtitle = "View, edit, deactivate and manage your listed items",
                        icon = Icons.Default.Inventory2,
                        onClick = onNavigateToMyListings,
                        testTag = "menu_my_listings"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = AppSpacing.md))

                    ProfileMenuRow(
                        title = "Seller Hub & Guidelines",
                        subtitle = "Post items and review campus safety guidelines",
                        icon = Icons.Default.Storefront,
                        onClick = onNavigateToSellerHub,
                        testTag = "menu_seller_hub"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = AppSpacing.md))

                    ProfileMenuRow(
                        title = "Campus Handovers & Orders",
                        subtitle = "Track purchases, incoming buyer requests & meetups",
                        icon = Icons.Default.ReceiptLong,
                        onClick = { onNavigateToOrders?.invoke() },
                        testTag = "menu_campus_handovers"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = AppSpacing.md))

                    ProfileMenuRow(
                        title = "Campus Notifications",
                        subtitle = "Orders, verification alerts and messages",
                        icon = Icons.Outlined.Notifications,
                        onClick = onNavigateToNotifications,
                        testTag = "menu_notifications"
                    )

                    if (session?.isAdmin == true) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = AppSpacing.md))

                        ProfileMenuRow(
                            title = "Campus Administration Portal",
                            subtitle = "Moderation, verification requests & user management",
                            icon = Icons.Default.AdminPanelSettings,
                            onClick = onNavigateToAdminDashboard,
                            testTag = "menu_admin_portal"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Theme Switcher Card (LIGHT, DARK, SYSTEM)
            Text(
                text = "Appearance & Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.lg,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.md)) {
                    ThemeOptionRow(
                        title = "Light Mode",
                        icon = Icons.Default.LightMode,
                        isSelected = currentTheme == ThemeMode.LIGHT,
                        onSelect = { onSetThemeMode(ThemeMode.LIGHT) },
                        testTag = "theme_option_light"
                    )
                    ThemeOptionRow(
                        title = "Dark Mode",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentTheme == ThemeMode.DARK,
                        onSelect = { onSetThemeMode(ThemeMode.DARK) },
                        testTag = "theme_option_dark"
                    )
                    ThemeOptionRow(
                        title = "System Default",
                        icon = Icons.Default.SettingsBrightness,
                        isSelected = currentTheme == ThemeMode.SYSTEM,
                        onSelect = { onSetThemeMode(ThemeMode.SYSTEM) },
                        testTag = "theme_option_system"
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // Sign In / Sign Out button
            if (isAuthenticated) {
                UnimaidButton(
                    text = "Sign Out",
                    onClick = {
                        scope.launch {
                            authRepository.signOut()
                            onNavigateToLogin()
                        }
                    },
                    variant = ButtonVariant.DANGER,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_signout_button"
                )
            } else {
                UnimaidButton(
                    text = "Sign In / Register",
                    onClick = onNavigateToLogin,
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_signin_button"
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // Creator Attribution
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = BrandConfig.APP_NAME,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Built for University of Maiduguri Students",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Attribution: ${BrandConfig.CREATOR_ATTRIBUTION} • ${BrandConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
}

@Composable
private fun VerificationStatusCard(
    state: StudentVerificationState,
    rejectionReason: String?,
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, icon) = when (state) {
        StudentVerificationState.VERIFIED -> Triple(
            VerifiedGreenContainer,
            VerifiedGreenOnContainer,
            Icons.Default.CheckCircle
        )
        StudentVerificationState.VERIFICATION_PENDING -> Triple(
            UnimaidBlueContainer,
            UnimaidBlueOnContainer,
            Icons.Default.HourglassTop
        )
        StudentVerificationState.REJECTED -> Triple(
            ErrorRedContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Warning
        )
        StudentVerificationState.SUSPENDED -> Triple(
            ErrorRedContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Lock
        )
        StudentVerificationState.UNVERIFIED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Shield
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppRadius.lg,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = if (state == StudentVerificationState.REJECTED && !rejectionReason.isNullOrBlank()) {
                            "Reason: $rejectionReason"
                        } else {
                            state.description
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }

            if (state != StudentVerificationState.VERIFIED && state != StudentVerificationState.SUSPENDED) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onVerifyClick) {
                        Text(
                            text = when (state) {
                                StudentVerificationState.VERIFICATION_PENDING -> "View Details"
                                StudentVerificationState.REJECTED -> "Resubmit Document"
                                else -> "Get Verified"
                            },
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(AppSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (badgeText != null) {
            val isSuccess = badgeText.equals("ACTIVE", ignoreCase = true) || badgeText.equals("CONFIRMED", ignoreCase = true)
            UnimaidBadge(
                text = badgeText,
                containerColor = if (isSuccess) VerifiedGreenContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSuccess) VerifiedGreenOnContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = AppRadius.md,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(AppSpacing.md)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(AppSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = AppSpacing.sm)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(AppSpacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
    }
}

@Composable
private fun ProfileSkeletonContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.sm)
    ) {
        // Skeleton Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppRadius.lg,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(20.dp)
                            .clip(AppRadius.sm)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp)
                            .clip(AppRadius.sm)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(18.dp)
                            .clip(AppRadius.full)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Skeleton Verification Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(AppRadius.lg)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Skeleton Credentials Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppRadius.lg,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                repeat(4) { idx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.35f)
                                    .height(12.dp)
                                    .clip(AppRadius.xs)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(14.dp)
                                    .clip(AppRadius.xs)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    if (idx < 3) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.xs))
                    }
                }
            }
        }
    }
}
