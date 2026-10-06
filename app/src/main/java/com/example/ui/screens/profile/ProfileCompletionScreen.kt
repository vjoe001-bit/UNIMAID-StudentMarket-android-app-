package com.example.ui.screens.profile

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.data.models.Profile
import com.example.data.repository.AuthRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ProfileRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * UNIMAID Student Profile Setup / Complete Your Profile Screen.
 *
 * Appears when required student information is missing or when the user initiates setup.
 * Collects full name, matric number, faculty, department, academic level, phone number,
 * and optional avatar photo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCompletionScreen(
    authRepository: AuthRepository,
    onProfileComplete: () -> Unit,
    onSkipOrCancel: () -> Unit,
    modifier: Modifier = Modifier,
    profileRepository: ProfileRepository = ProfileRepositoryImpl()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val sessionState by authRepository.sessionState.collectAsState()
    val session = (sessionState as? SessionState.Authenticated)?.session

    var fullName by remember { mutableStateOf(session?.fullName.orEmpty()) }
    var matricNumber by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("100L") }
    var phoneNumber by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var currentAvatarUrl by remember { mutableStateOf<String?>(null) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var matricError by remember { mutableStateOf<String?>(null) }
    var departmentError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    var isFacultyDropdownExpanded by remember { mutableStateOf(false) }
    var isLoadingInitialProfile by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val levels = listOf("100L", "200L", "300L", "400L", "500L", "Postgraduate")

    val unimaidFaculties = listOf(
        "Faculty of Agriculture",
        "Faculty of Arts",
        "College of Medical Sciences",
        "Faculty of Dentistry",
        "Faculty of Education",
        "Faculty of Engineering",
        "Faculty of Environmental Studies",
        "Faculty of Law",
        "Faculty of Management Sciences",
        "Faculty of Pharmacy",
        "Faculty of Science",
        "Faculty of Social Sciences",
        "Faculty of Veterinary Medicine",
        "School of Postgraduate Studies",
        "General Studies & Other"
    )

    // Load existing profile info if available
    LaunchedEffect(session?.userId) {
        val uid = session?.userId
        if (!uid.isNullOrBlank()) {
            when (val res = profileRepository.getProfile(uid)) {
                is SupabaseResult.Success -> {
                    val p = res.data
                    if (!p.fullName.isNullOrBlank()) fullName = p.fullName
                    if (!p.matricNumber.isNullOrBlank()) matricNumber = p.matricNumber
                    if (!p.faculty.isNullOrBlank()) faculty = p.faculty
                    if (!p.department.isNullOrBlank()) department = p.department
                    if (!p.level.isNullOrBlank()) selectedLevel = p.level
                    if (!p.phoneNumber.isNullOrBlank()) phoneNumber = p.phoneNumber
                    if (!p.bio.isNullOrBlank()) bio = p.bio
                    if (!p.avatarUrl.isNullOrBlank()) currentAvatarUrl = p.avatarUrl
                }
                else -> {}
            }
        }
        isLoadingInitialProfile = false
    }

    // Android Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                scope.launch(Dispatchers.IO) {
                    val bytes = compressImageUri(context, uri)
                    withContext(Dispatchers.Main) {
                        selectedImageBytes = bytes
                    }
                }
            }
        }
    )

    // Compute completion progress (0.0 to 1.0)
    val progress: Float = run {
        var completed = 0
        if (fullName.trim().isNotBlank()) completed++
        if (matricNumber.trim().isNotBlank()) completed++
        if (department.trim().isNotBlank()) completed++
        if (phoneNumber.trim().isNotBlank()) completed++
        if (selectedImageUri != null || !currentAvatarUrl.isNullOrBlank()) completed++
        completed / 5f
    }

    fun validate(): Boolean {
        var isValid = true

        if (fullName.trim().isBlank()) {
            fullNameError = "Please enter your full student name"
            isValid = false
        } else if (fullName.trim().length < 3) {
            fullNameError = "Name must be at least 3 characters"
            isValid = false
        } else {
            fullNameError = null
        }

        val trimmedMatric = matricNumber.trim()
        if (trimmedMatric.isBlank()) {
            matricError = "Please enter your UNIMAID matriculation number"
            isValid = false
        } else if (trimmedMatric.length < 5) {
            matricError = "Please enter a valid matric format (e.g. 19/04/02/001)"
            isValid = false
        } else {
            matricError = null
        }

        if (department.trim().isBlank()) {
            departmentError = "Please enter your academic department"
            isValid = false
        } else {
            departmentError = null
        }

        val cleanPhone = phoneNumber.trim().replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isBlank()) {
            phoneError = "Please enter your phone or WhatsApp number"
            isValid = false
        } else if (cleanPhone.length < 10) {
            phoneError = "Please enter a valid 11-digit phone number (e.g. 08012345678)"
            isValid = false
        } else {
            phoneError = null
        }

        return isValid
    }

    fun saveProfile() {
        val uid = session?.userId
        if (uid.isNullOrBlank()) {
            generalError = "Session expired. Please log in again."
            return
        }

        if (!validate() || isSaving) return
        isSaving = true
        generalError = null

        scope.launch {
            var uploadedAvatarUrl = currentAvatarUrl

            // 1. Upload avatar photo if newly selected
            if (selectedImageBytes != null) {
                val fileName = "avatar_${System.currentTimeMillis()}.jpg"
                when (val uploadResult = profileRepository.uploadAvatar(uid, fileName, selectedImageBytes!!)) {
                    is SupabaseResult.Success -> {
                        uploadedAvatarUrl = uploadResult.data
                    }
                    is SupabaseResult.Error -> {
                        // Non-blocking warning; continue updating text fields
                    }
                    is SupabaseResult.Loading -> {}
                }
            }

            // 2. Persist profile fields to Supabase
            val updates = mutableMapOf<String, Any?>(
                "full_name" to fullName.trim(),
                "matric_number" to matricNumber.trim(),
                "faculty" to faculty.trim().takeIf { it.isNotBlank() },
                "department" to department.trim(),
                "level" to selectedLevel,
                "phone_number" to phoneNumber.trim(),
                "bio" to bio.trim().takeIf { it.isNotBlank() },
                "avatar_url" to uploadedAvatarUrl
            )

            when (val updateResult = profileRepository.updateProfile(uid, updates)) {
                is SupabaseResult.Success -> {
                    // Update local session state
                    SessionManager.getInstance(context).updateProfileComplete(true)
                    isSaving = false
                    onProfileComplete()
                }
                is SupabaseResult.Error -> {
                    isSaving = false
                    generalError = updateResult.userFriendlyMessage
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
            title = "Complete Your Profile",
            onBackClick = onSkipOrCancel
        )

        // Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .testTag("profile_completion_progress_bar"),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(AppSpacing.screenPadding)
        ) {
            // Hero Explainer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_completion_hero_card"),
                shape = AppRadius.lg,
                colors = CardDefaults.cardColors(containerColor = UnimaidBlueContainer)
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.md))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Campus Student Identity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = UnimaidBlueOnContainer
                        )
                        Text(
                            text = "Complete your academic profile so UNIMAID peers can easily identify and contact you for campus transactions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UnimaidBlueOnContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Error Banner
            AnimatedVisibility(
                visible = generalError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                generalError?.let { err ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppSpacing.md)
                            .testTag("profile_completion_error_banner"),
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
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 1. Profile Photo / Avatar Picker
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("profile_completion_avatar_box"),
                    contentAlignment = Alignment.Center
                ) {
                    val displayImage = selectedImageUri?.toString() ?: currentAvatarUrl

                    if (!displayImage.isNullOrBlank()) {
                        AsyncImage(
                            model = displayImage,
                            contentDescription = "Student Profile Picture",
                            modifier = Modifier.size(100.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    // Camera floating icon badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Upload photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("profile_completion_choose_photo_button")
                    ) {
                        Text(
                            text = if (selectedImageUri != null || !currentAvatarUrl.isNullOrBlank()) "Change Student Photo" else "Add Student Photo",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (selectedImageUri != null || !currentAvatarUrl.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                selectedImageUri = null
                                selectedImageBytes = null
                                currentAvatarUrl = null
                            },
                            enabled = !isSaving
                        ) {
                            Text(
                                text = "Remove",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 2. Read-Only UNIMAID Academic Email
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_completion_email_card"),
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
                            text = "Email Address",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = session?.email ?: "student@gmail.com",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    UnimaidBadge(
                        text = "ACCOUNT",
                        icon = Icons.Default.Lock,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 3. Full Name (Required *)
            UnimaidTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    if (fullNameError != null) fullNameError = null
                },
                label = "Full Name *",
                placeholder = "e.g. Victoria Drambi",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = fullNameError != null,
                errorMessage = fullNameError,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                testTag = "profile_completion_name_input"
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 4. Matriculation Number (Required *)
            UnimaidTextField(
                value = matricNumber,
                onValueChange = {
                    matricNumber = it
                    if (matricError != null) matricError = null
                },
                label = "Matriculation Number *",
                placeholder = "e.g. 19/04/02/001",
                helperText = "Your official UNIMAID student registration number",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = matricError != null,
                errorMessage = matricError,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                testTag = "profile_completion_matric_input"
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 5. Faculty Selection Dropdown
            ExposedDropdownMenuBox(
                expanded = isFacultyDropdownExpanded,
                onExpandedChange = { if (!isSaving) isFacultyDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                UnimaidTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = "Faculty",
                    placeholder = "Select or enter your faculty",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFacultyDropdownExpanded)
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = !isSaving)
                        .testTag("profile_completion_faculty_dropdown"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                ExposedDropdownMenu(
                    expanded = isFacultyDropdownExpanded,
                    onDismissRequest = { isFacultyDropdownExpanded = false }
                ) {
                    unimaidFaculties.forEach { facultyOption ->
                        DropdownMenuItem(
                            text = { Text(facultyOption, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                faculty = facultyOption
                                isFacultyDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 6. Department (Required *)
            UnimaidTextField(
                value = department,
                onValueChange = {
                    department = it
                    if (departmentError != null) departmentError = null
                },
                label = "Department *",
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
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                testTag = "profile_completion_department_input"
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 7. Academic Level Chips
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

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 8. Phone / WhatsApp (Required *)
            UnimaidTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    if (phoneError != null) phoneError = null
                },
                label = "Phone / WhatsApp Number *",
                placeholder = "e.g. 08012345678",
                helperText = "Needed to coordinate campus handovers & meetup locations",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = phoneError != null,
                errorMessage = phoneError,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                testTag = "profile_completion_phone_input"
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 9. Campus Bio / Hostel Note (Optional)
            UnimaidTextField(
                value = bio,
                onValueChange = { bio = it },
                label = "Campus Bio & Hostel Note (Optional)",
                placeholder = "e.g. Final year student in Titanic Hostel, selling course textbooks and electronics.",
                singleLine = false,
                maxLines = 3,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                testTag = "profile_completion_bio_input"
            )

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // 10. Actions
            UnimaidButton(
                text = "Save & Complete Profile",
                onClick = { saveProfile() },
                isLoading = isSaving,
                enabled = !isSaving,
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                testTag = "profile_completion_save_button"
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            TextButton(
                onClick = onSkipOrCancel,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_completion_skip_button")
            ) {
                Text(
                    text = "I'll Complete This Later",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "Required fields are marked with *. Your information is protected under UNIMAID marketplace privacy guidelines.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Compresses photo to 800x800 and 85% JPEG quality to conserve mobile bandwidth.
 */
private fun compressImageUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val maxDimension = 800
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
