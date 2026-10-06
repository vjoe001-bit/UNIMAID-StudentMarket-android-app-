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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.School
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.designsystem.components.ButtonVariant
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidTextField
import com.example.core.network.SupabaseResult
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.data.models.Profile
import com.example.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Edit Student Profile Dialog.
 * Allows updating display name, department, level, phone, bio, and avatar.
 * Uses zero-permission Android Photo Picker (PickVisualMedia) and optimizes image bytes for mobile bandwidth.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    profile: Profile,
    profileRepository: ProfileRepository,
    onDismiss: () -> Unit,
    onProfileUpdated: (Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var fullName by remember { mutableStateOf(profile.fullName.orEmpty()) }
    var faculty by remember { mutableStateOf(profile.faculty.orEmpty()) }
    var department by remember { mutableStateOf(profile.department.orEmpty()) }
    var selectedLevel by remember { mutableStateOf(profile.level ?: "100L") }
    var phoneNumber by remember { mutableStateOf(profile.phoneNumber.orEmpty()) }
    var bio by remember { mutableStateOf(profile.bio.orEmpty()) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var removeAvatarRequested by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var departmentError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isFacultyDropdownExpanded by remember { mutableStateOf(false) }
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

    // Android Photo Picker Launcher (Zero-permission compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                removeAvatarRequested = false
                scope.launch(Dispatchers.IO) {
                    val bytes = compressImageUri(context, uri)
                    withContext(Dispatchers.Main) {
                        selectedImageBytes = bytes
                    }
                }
            }
        }
    )

    fun validate(): Boolean {
        var isValid = true

        if (fullName.trim().isBlank()) {
            fullNameError = "Student full name cannot be empty"
            isValid = false
        } else if (fullName.trim().length < 3) {
            fullNameError = "Full name must be at least 3 characters"
            isValid = false
        } else {
            fullNameError = null
        }

        if (department.trim().isBlank()) {
            departmentError = "Academic department cannot be empty"
            isValid = false
        } else {
            departmentError = null
        }

        val cleanPhone = phoneNumber.trim().replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isNotBlank() && cleanPhone.length < 10) {
            phoneError = "Please enter a valid phone number (at least 10 digits)"
            isValid = false
        } else {
            phoneError = null
        }

        return isValid
    }

    fun saveProfile() {
        if (!validate() || isSaving) return
        isSaving = true
        generalError = null

        scope.launch {
            var newAvatarUrl = profile.avatarUrl

            // 1. If user removed avatar
            if (removeAvatarRequested) {
                newAvatarUrl = null
            }
            // 2. If user picked a new avatar, upload to Supabase Storage
            else if (selectedImageBytes != null) {
                val fileName = "avatar_${System.currentTimeMillis()}.jpg"
                when (val uploadResult = profileRepository.uploadAvatar(profile.id, fileName, selectedImageBytes!!)) {
                    is SupabaseResult.Success -> {
                        newAvatarUrl = uploadResult.data
                    }
                    is SupabaseResult.Error -> {
                        isSaving = false
                        generalError = "Failed to upload photo: ${uploadResult.userFriendlyMessage}"
                        return@launch
                    }
                    is SupabaseResult.Loading -> {}
                }
            }

            // 3. Update profile fields in Supabase
            val updates = mutableMapOf<String, Any?>(
                "full_name" to fullName.trim(),
                "faculty" to faculty.trim().takeIf { it.isNotBlank() },
                "department" to department.trim().takeIf { it.isNotBlank() },
                "level" to selectedLevel,
                "phone_number" to phoneNumber.trim().takeIf { it.isNotBlank() },
                "bio" to bio.trim().takeIf { it.isNotBlank() },
                "avatar_url" to newAvatarUrl
            )

            when (val result = profileRepository.updateProfile(profile.id, updates)) {
                is SupabaseResult.Success -> {
                    isSaving = false
                    onProfileUpdated(result.data)
                    onDismiss()
                }
                is SupabaseResult.Error -> {
                    isSaving = false
                    generalError = result.userFriendlyMessage
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .imePadding()
                .clip(AppRadius.xl),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(AppSpacing.screenPadding)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Student Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Avatar Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("profile_avatar_picker_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentAvatar = if (!removeAvatarRequested) {
                            selectedImageUri?.toString() ?: profile.avatarUrl
                        } else null

                        if (!currentAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = currentAvatar,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.size(96.dp),
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

                        // Camera overlay badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change photo",
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
                            enabled = !isSaving
                        ) {
                            Text("Change Photo", style = MaterialTheme.typography.labelMedium)
                        }

                        if (selectedImageUri != null || !profile.avatarUrl.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    selectedImageUri = null
                                    selectedImageBytes = null
                                    removeAvatarRequested = true
                                },
                                enabled = !isSaving
                            ) {
                                Text(
                                    "Remove",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Display Name
                UnimaidTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (fullNameError != null) fullNameError = null
                    },
                    label = "Display Name",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    isError = fullNameError != null,
                    errorMessage = fullNameError,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    testTag = "edit_profile_name_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Faculty Selection Dropdown
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
                            Icon(imageVector = Icons.Outlined.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFacultyDropdownExpanded)
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = !isSaving)
                            .testTag("edit_profile_faculty_dropdown"),
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

                // Department
                UnimaidTextField(
                    value = department,
                    onValueChange = {
                        department = it
                        if (departmentError != null) departmentError = null
                    },
                    label = "Department *",
                    placeholder = "e.g. Computer Engineering",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    isError = departmentError != null,
                    errorMessage = departmentError,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    testTag = "edit_profile_department_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Academic Level
                Text(
                    text = "Academic Level",
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

                // Phone / WhatsApp
                UnimaidTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        if (phoneError != null) phoneError = null
                    },
                    label = "Phone / WhatsApp",
                    placeholder = "e.g. 08012345678",
                    helperText = "Shared with campus buyers & sellers for meetups",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    isError = phoneError != null,
                    errorMessage = phoneError,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    testTag = "edit_profile_phone_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Short Bio
                UnimaidTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = "Campus Bio",
                    placeholder = "e.g. 300L student selling clean engineering textbooks and hostel gear.",
                    singleLine = false,
                    maxLines = 3,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    testTag = "edit_profile_bio_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.xl))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    UnimaidButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.OUTLINED,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        testTag = "edit_profile_cancel_button"
                    )

                    UnimaidButton(
                        text = "Save Changes",
                        onClick = { saveProfile() },
                        isLoading = isSaving,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        testTag = "edit_profile_save_button"
                    )
                }
            }
        }
    }
}

/**
 * Compresses selected image to max 800x800 and 85% JPEG quality for low mobile bandwidth.
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
