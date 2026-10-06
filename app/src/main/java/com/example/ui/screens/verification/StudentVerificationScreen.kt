package com.example.ui.screens.verification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.branding.BrandConfig
import com.example.core.designsystem.components.ButtonVariant
import com.example.core.designsystem.components.UnimaidBadge
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidTextField
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.network.SupabaseResult
import com.example.core.session.SessionManager
import com.example.core.session.SessionState
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.ErrorRed
import com.example.core.theme.ErrorRedContainer
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.UnimaidGold
import com.example.core.theme.UnimaidGoldContainer
import com.example.core.theme.UnimaidGoldOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.data.models.StudentVerificationState
import com.example.data.models.VerificationRequest
import com.example.data.repository.AuthRepository
import com.example.data.repository.StorageRepository
import com.example.data.repository.StorageRepositoryImpl
import com.example.data.repository.VerificationRepository
import com.example.data.repository.VerificationRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * UNIMAID Student Verification Experience.
 * Provides transparent communication of verification status:
 * - Pending Review
 * - Approved / Verified Student
 * - Rejected / Action Required
 * - Submission Form (Matric, Faculty, Department, Level, and ID Document proof).
 */
@Composable
fun StudentVerificationScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    verificationRepository: VerificationRepository = VerificationRepositoryImpl(),
    storageRepository: StorageRepository = StorageRepositoryImpl()
) {
    val context = LocalContext.current
    val sessionState by authRepository.sessionState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var existingRequest by remember { mutableStateOf<VerificationRequest?>(null) }
    var isCheckingStatus by remember { mutableStateOf(true) }
    var isResubmitting by remember { mutableStateOf(false) }

    // Form fields
    var matricNumber by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("300 Level") }
    var documentAttached by remember { mutableStateOf(false) }
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var documentBytes by remember { mutableStateOf<ByteArray?>(null) }

    var matricError by remember { mutableStateOf<String?>(null) }
    var facultyError by remember { mutableStateOf<String?>(null) }
    var departmentError by remember { mutableStateOf<String?>(null) }
    var documentError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val levels = listOf("100L", "200L", "300L", "400L", "500L", "Postgraduate")

    // Android Zero-permission Photo Picker for Student ID Card
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                documentUri = uri
                documentAttached = true
                if (documentError != null) documentError = null
                scope.launch(Dispatchers.IO) {
                    val bytes = compressDocumentUri(context, uri)
                    withContext(Dispatchers.Main) {
                        documentBytes = bytes
                    }
                }
            }
        }
    )

    // Fetch existing verification status
    LaunchedEffect(sessionState) {
        val current = sessionState
        if (current is SessionState.Authenticated) {
            isCheckingStatus = true
            when (val result = verificationRepository.getVerificationStatus(current.session.userId)) {
                is SupabaseResult.Success -> {
                    existingRequest = result.data
                    val status = result.data?.status ?: if (current.session.isVerified) "VERIFIED" else "UNVERIFIED"
                    SessionManager.getInstance(context).updateVerificationState(status)
                    isCheckingStatus = false
                }
                is SupabaseResult.Error -> {
                    isCheckingStatus = false
                }
                is SupabaseResult.Loading -> {}
            }
        } else {
            isCheckingStatus = false
        }
    }

    fun validateForm(): Boolean {
        var isValid = true
        if (matricNumber.trim().isBlank()) {
            matricError = "Please enter your UNIMAID matriculation number"
            isValid = false
        } else if (matricNumber.trim().length < 5) {
            matricError = "Enter a valid matric format (e.g. 19/04/02/001)"
            isValid = false
        } else {
            matricError = null
        }

        if (faculty.trim().isBlank()) {
            facultyError = "Please enter your faculty (e.g. Faculty of Engineering)"
            isValid = false
        } else {
            facultyError = null
        }

        if (department.trim().isBlank()) {
            departmentError = "Please enter your department (e.g. Computer Engineering)"
            isValid = false
        } else {
            departmentError = null
        }

        if (!documentAttached) {
            documentError = "Please attach a photo or scan of your UNIMAID Student ID card or Portal Slip"
            isValid = false
        } else {
            documentError = null
        }

        return isValid
    }

    fun submitVerification() {
        val currentSession = sessionState
        if (currentSession !is SessionState.Authenticated) {
            generalError = "You must be signed in to submit verification"
            return
        }

        if (!validateForm() || isSubmitting) return
        isSubmitting = true
        generalError = null

        scope.launch {
            var docUrl = "verification-documents/${currentSession.session.userId}/student_id_${System.currentTimeMillis()}.jpg"

            // Upload to private verification-documents bucket if image picked
            if (documentBytes != null) {
                val fileName = "id_${System.currentTimeMillis()}.jpg"
                when (val uploadRes = storageRepository.uploadVerificationDocument(currentSession.session.userId, fileName, documentBytes!!)) {
                    is SupabaseResult.Success -> {
                        docUrl = uploadRes.data
                    }
                    else -> {}
                }
            }

            val newRequest = VerificationRequest(
                id = UUID.randomUUID().toString(),
                userId = currentSession.session.userId,
                matricNumber = matricNumber.trim(),
                documentUrl = docUrl,
                status = "PENDING"
            )

            when (val result = verificationRepository.submitVerification(newRequest)) {
                is SupabaseResult.Success -> {
                    isSubmitting = false
                    existingRequest = result.data
                    isResubmitting = false
                    SessionManager.getInstance(context).updateVerificationState("VERIFICATION_PENDING")
                }
                is SupabaseResult.Error -> {
                    isSubmitting = false
                    generalError = result.userFriendlyMessage
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        UnimaidTopAppBar(
            title = "Student Verification",
            onBackClick = onNavigateBack
        )

        if (isCheckingStatus) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = "Checking verification records...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (sessionState !is SessionState.Authenticated) {
            // Unauthenticated state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.screenPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(UnimaidBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                Text(
                    text = "Sign In Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                Text(
                    text = "Please log in to your UNIMAID student account to view your verification status or submit your student ID.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppSpacing.xl))

                UnimaidButton(
                    text = "Sign In to Verify",
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "verification_signin_prompt_button"
                )
            }
        } else {
            val request = existingRequest
            val isAuthenticated = sessionState as SessionState.Authenticated
            val isUserVerified = isAuthenticated.session.isVerified || request?.status == "APPROVED"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(AppSpacing.screenPadding)
            ) {
                // UNIMAID Student Academic Email Verification Indicator
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppSpacing.md)
                        .testTag("verification_email_status_card"),
                    shape = AppRadius.md,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UNIMAID Academic Email",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = isAuthenticated.session.email,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        UnimaidBadge(
                            text = if (isAuthenticated.session.isEmailConfirmed) "EMAIL CONFIRMED" else "CONFIRMATION PENDING",
                            icon = if (isAuthenticated.session.isEmailConfirmed) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            containerColor = if (isAuthenticated.session.isEmailConfirmed) VerifiedGreenContainer else MaterialTheme.colorScheme.surface,
                            contentColor = if (isAuthenticated.session.isEmailConfirmed) VerifiedGreenOnContainer else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Status 1: APPROVED / VERIFIED
                if (isUserVerified && !isResubmitting) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verification_approved_card"),
                        shape = AppRadius.lg,
                        colors = CardDefaults.cardColors(containerColor = VerifiedGreenContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(VerifiedGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            Text(
                                text = "Verified UNIMAID Student",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = VerifiedGreenOnContainer
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.xs))

                            Text(
                                text = "Your student status is confirmed. Your campus marketplace listings and messages carry the official verified badge.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VerifiedGreenOnContainer,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            UnimaidBadge(
                                text = "STATUS: VERIFIED & ACTIVE",
                                icon = Icons.Default.CheckCircle,
                                containerColor = VerifiedGreen,
                                contentColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xl))

                    UnimaidButton(
                        text = "Return to Marketplace",
                        onClick = onNavigateBack,
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Status 2: PENDING
                else if (request?.status == "PENDING" && !isResubmitting) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verification_pending_card"),
                        shape = AppRadius.lg,
                        colors = CardDefaults.cardColors(containerColor = UnimaidBlueContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            Text(
                                text = "Verification In Review",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = UnimaidBlueOnContainer
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.xs))

                            Text(
                                text = "Your UNIMAID student credentials have been submitted and are being reviewed by campus moderators. Processing typically takes less than 24 hours.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = UnimaidBlueOnContainer,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            Card(
                                shape = AppRadius.md,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(AppSpacing.md)) {
                                    Text(
                                        text = "Matric Number: ${request.matricNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Status: Pending Review",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    Text(
                        text = "You can still browse campus listings and interact with fellow students while your verification badge is being processed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    UnimaidButton(
                        text = "Back to Campus Marketplace",
                        onClick = onNavigateBack,
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Status 3: REJECTED
                else if (request?.status == "REJECTED" && !isResubmitting) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verification_rejected_card"),
                        shape = AppRadius.lg,
                        colors = CardDefaults.cardColors(containerColor = ErrorRedContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(ErrorRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            Text(
                                text = "Verification Declined",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.xs))

                            Text(
                                text = request.rejectionReason ?: "Your student document could not be validated. Please ensure your student ID is clear and unexpired.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    UnimaidButton(
                        text = "Submit New Verification",
                        onClick = { isResubmitting = true },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "verification_resubmit_button"
                    )
                }
                // Status 4: NOT SUBMITTED or RESUBMITTING (The Verification Form)
                else {
                    // Educational Banner: Why Verification Matters
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verification_explainer_card"),
                        shape = AppRadius.lg,
                        colors = CardDefaults.cardColors(containerColor = UnimaidBlueContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(AppSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = UnimaidBlueOnContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.md))
                            Column {
                                Text(
                                    text = "Why Get UNIMAID Verified?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = UnimaidBlueOnContainer
                                )
                                Text(
                                    text = "Earn student buyer/seller badges, build campus reputation, and prevent non-student scams on campus.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UnimaidBlueOnContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    // Error banner
                    AnimatedVisibility(
                        visible = generalError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        generalError?.let { errorText ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = AppSpacing.md),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = AppRadius.md
                            ) {
                                Row(
                                    modifier = Modifier.padding(AppSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                                    Text(
                                        text = errorText,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "1. Student Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    UnimaidTextField(
                        value = matricNumber,
                        onValueChange = {
                            matricNumber = it
                            if (matricError != null) matricError = null
                        },
                        label = "Matriculation Number",
                        placeholder = "e.g. 19/04/02/001",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        isError = matricError != null,
                        errorMessage = matricError,
                        enabled = !isSubmitting,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        testTag = "verification_matric_input"
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    UnimaidTextField(
                        value = faculty,
                        onValueChange = {
                            faculty = it
                            if (facultyError != null) facultyError = null
                        },
                        label = "Faculty",
                        placeholder = "e.g. Faculty of Engineering",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        isError = facultyError != null,
                        errorMessage = facultyError,
                        enabled = !isSubmitting,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        testTag = "verification_faculty_input"
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    UnimaidTextField(
                        value = department,
                        onValueChange = {
                            department = it
                            if (departmentError != null) departmentError = null
                        },
                        label = "Department",
                        placeholder = "e.g. Computer Engineering",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        isError = departmentError != null,
                        errorMessage = departmentError,
                        enabled = !isSubmitting,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        testTag = "verification_department_input"
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    Text(
                        text = "Current Academic Level",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        levels.take(3).forEach { lvl ->
                            FilterChip(
                                selected = selectedLevel == lvl,
                                onClick = { selectedLevel = lvl },
                                label = { Text(lvl, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        levels.drop(3).forEach { lvl ->
                            FilterChip(
                                selected = selectedLevel == lvl,
                                onClick = { selectedLevel = lvl },
                                label = { Text(lvl, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    Text(
                        text = "2. Student Identity Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Text(
                        text = "Accepted: UNIMAID Student ID Card, School Portal Biodata Slip, or Course Registration Form.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    // ID Document attachment container
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("verification_document_attach_card"),
                        shape = AppRadius.md,
                        colors = CardDefaults.cardColors(
                            containerColor = if (documentAttached) VerifiedGreenContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (documentAttached) VerifiedGreen else MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (documentAttached) Icons.Outlined.Check else Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(AppSpacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (documentAttached) "UNIMAID Student ID Document Attached" else "Attach UNIMAID Student ID Card",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (documentAttached) VerifiedGreenOnContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (documentAttached) "Document ready for encrypted upload to verification-documents" else "Tap to choose photo from gallery or camera",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (documentAttached) VerifiedGreenOnContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Image preview if selected
                            if (documentUri != null) {
                                Spacer(modifier = Modifier.height(AppSpacing.md))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(AppRadius.md)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    AsyncImage(
                                        model = documentUri,
                                        contentDescription = "Selected ID Proof",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.height(AppSpacing.xs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            documentUri = null
                                            documentBytes = null
                                            documentAttached = false
                                        }
                                    ) {
                                        Text(
                                            text = "Remove Photo",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (documentError != null) {
                        Text(
                            text = documentError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = AppSpacing.xs, top = AppSpacing.xs)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xl))

                    UnimaidButton(
                        text = "Submit Verification for Review",
                        onClick = { submitVerification() },
                        isLoading = isSubmitting,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "verification_submit_button"
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    Text(
                        text = "Your document is securely stored in UNIMAID verification storage and reviewed solely by authorized campus administrators.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Compresses document image to 1024x1024 and 85% JPEG quality for bandwidth optimization.
 */
private fun compressDocumentUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val maxDimension = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                if (ratio > 1) {
                    maxDimension.toFloat() / width
                } else {
                    maxDimension.toFloat() / height
                }
            } else 1.0f

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}
