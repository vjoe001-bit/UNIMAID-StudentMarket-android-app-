package com.example.ui.screens.auth

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.branding.BrandConfig
import com.example.core.designsystem.components.ButtonVariant
import com.example.core.designsystem.components.UnimaidBadge
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidTextField
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.network.SupabaseResult
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.ErrorRed
import com.example.core.theme.ErrorRedContainer
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.core.validation.StudentEmailValidator
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * Modern UNIMAID Student Registration Screen.
 *
 * Implements real Supabase registration collecting student academic profile fields,
 * validates against official UNIMAID student email rules, supports pending email confirmation
 * states, and prevents duplicate submissions while loading.
 */
@Composable
fun SignUpScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var matricNumber by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("100L") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var departmentError by remember { mutableStateOf<String?>(null) }
    var matricError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Email verification state (when Supabase email confirmation is enabled)
    var isConfirmationRequired by remember { mutableStateOf(false) }
    var registeredEmail by remember { mutableStateOf("") }
    var resendSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isResending by remember { mutableStateOf(false) }

    val levels = listOf("100L", "200L", "300L", "400L", "500L", "Postgraduate")
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun validate(): Boolean {
        var isValid = true

        if (fullName.trim().isBlank()) {
            fullNameError = "Please enter your full student name"
            isValid = false
        } else if (fullName.trim().length < 3) {
            fullNameError = "Full name must be at least 3 characters"
            isValid = false
        } else {
            fullNameError = null
        }

        val trimmedEmail = email.trim()
        val emailValidation = StudentEmailValidator.validate(trimmedEmail)
        if (emailValidation is StudentEmailValidator.ValidationResult.Invalid) {
            emailError = emailValidation.message
            isValid = false
        } else {
            emailError = null
        }

        if (department.trim().isBlank()) {
            departmentError = "Please enter your academic department"
            isValid = false
        } else {
            departmentError = null
        }

        val trimmedMatric = matricNumber.trim()
        if (trimmedMatric.isNotBlank() && trimmedMatric.length < 4) {
            matricError = "Please enter a valid matriculation / student ID"
            isValid = false
        } else {
            matricError = null
        }

        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isNotBlank() && trimmedPhone.length < 10) {
            phoneError = "Please enter a valid phone number (e.g. 08012345678)"
            isValid = false
        } else {
            phoneError = null
        }

        if (password.isBlank()) {
            passwordError = "Please enter a password"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }

        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        if (!agreeToTerms) {
            termsError = "Please agree to the campus safety & in-person exchange rules"
            isValid = false
        } else {
            termsError = null
        }

        return isValid
    }

    fun submitSignUp() {
        if (!validate() || isLoading) return
        isLoading = true
        generalError = null

        scope.launch {
            val result = authRepository.signUp(
                email = email.trim(),
                password = password,
                fullName = fullName.trim(),
                matricNumber = matricNumber.trim().takeIf { it.isNotBlank() },
                department = department.trim().takeIf { it.isNotBlank() },
                phoneNumber = phoneNumber.trim().takeIf { it.isNotBlank() },
                level = selectedLevel
            )
            when (result) {
                is SupabaseResult.Success -> {
                    isLoading = false
                    val session = result.data
                    if (session.accessToken.isBlank() || !session.isEmailConfirmed) {
                        // Email confirmation is required by Supabase Auth configuration
                        registeredEmail = email.trim()
                        isConfirmationRequired = true
                    } else {
                        // Immediate session granted
                        onSignUpSuccess()
                    }
                }
                is SupabaseResult.Error -> {
                    isLoading = false
                    generalError = result.userFriendlyMessage
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    fun resendConfirmation() {
        if (registeredEmail.isBlank() || isResending) return
        isResending = true
        resendSuccessMessage = null
        generalError = null

        scope.launch {
            when (val res = authRepository.resendVerificationEmail(registeredEmail)) {
                is SupabaseResult.Success -> {
                    isResending = false
                    resendSuccessMessage = "Verification link resent! Please check your inbox and spam folder."
                }
                is SupabaseResult.Error -> {
                    isResending = false
                    generalError = res.userFriendlyMessage
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
            title = if (isConfirmationRequired) "Confirm Email" else "Create Account",
            onBackClick = {
                if (isConfirmationRequired) {
                    isConfirmationRequired = false
                } else {
                    onNavigateBack()
                }
            }
        )

        if (isConfirmationRequired) {
            // Email Confirmation Required Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(AppSpacing.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(UnimaidBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Confirm Email",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                Text(
                    text = "Verify Your Email Address",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                Text(
                    text = "We have created your UNIMAID StudentMarket account and sent a confirmation link to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppRadius.md,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = registeredEmail,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.sm)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                Text(
                    text = "Please open the confirmation email and click the verification link to activate your campus marketplace access. Check your spam/junk folder if you don't see it within a couple minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (resendSuccessMessage != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppRadius.md,
                        colors = CardDefaults.cardColors(containerColor = VerifiedGreenContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VerifiedGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text(
                                text = resendSuccessMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = VerifiedGreenOnContainer
                            )
                        }
                    }
                }

                if (generalError != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = generalError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.xl))

                // Proceed to Login Button
                UnimaidButton(
                    text = "Proceed to Sign In",
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "email_confirm_to_login_button"
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Resend Link Button
                UnimaidButton(
                    text = if (isResending) "Resending..." else "Resend Verification Email",
                    onClick = { resendConfirmation() },
                    variant = ButtonVariant.SECONDARY,
                    isLoading = isResending,
                    enabled = !isResending,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "email_resend_button"
                )
            }
        } else {
            // Main Registration Form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(AppSpacing.screenPadding)
            ) {
                Text(
                    text = "Join UNIMAID Market",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Text(
                    text = "Create your student account to buy, sell, and discover items exclusively within the University of Maiduguri campus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Policy Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppRadius.md,
                    colors = CardDefaults.cardColors(containerColor = UnimaidBlueContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = "UNIMAID Students Only • Safe Physical Campus Handovers",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = UnimaidBlueOnContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                // Error Banner
                AnimatedVisibility(
                    visible = generalError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    generalError?.let { errorText ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = AppSpacing.md)
                                .testTag("signup_error_banner"),
                            shape = AppRadius.md,
                            colors = CardDefaults.cardColors(containerColor = ErrorRedContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(AppSpacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ErrorRed,
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

                // Full Name Input
                UnimaidTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (fullNameError != null) fullNameError = null
                    },
                    label = "Full Name",
                    placeholder = "e.g. Fatima Mohammed",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = fullNameError != null,
                    errorMessage = fullNameError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_fullname_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Email Address Input
                UnimaidTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) emailError = null
                    },
                    label = "Email Address",
                    placeholder = "e.g. user@gmail.com",
                    helperText = if (emailError == null) "Use a valid email address. UNIMAID student verification is handled separately." else null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = emailError != null,
                    errorMessage = emailError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_email_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Department Input
                UnimaidTextField(
                    value = department,
                    onValueChange = {
                        department = it
                        if (departmentError != null) departmentError = null
                    },
                    label = "Department",
                    placeholder = "e.g. Computer Science, Medicine, Law",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = departmentError != null,
                    errorMessage = departmentError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_department_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Academic Level Selector
                Text(
                    text = "Academic Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    levels.take(4).forEach { lvl ->
                        FilterChip(
                            selected = selectedLevel == lvl,
                            onClick = { if (!isLoading) selectedLevel = lvl },
                            label = { Text(lvl) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    levels.drop(4).forEach { lvl ->
                        FilterChip(
                            selected = selectedLevel == lvl,
                            onClick = { if (!isLoading) selectedLevel = lvl },
                            label = { Text(lvl) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Matric / Student ID (Optional during registration)
                UnimaidTextField(
                    value = matricNumber,
                    onValueChange = {
                        matricNumber = it
                        if (matricError != null) matricError = null
                    },
                    label = "Student / Matric ID (Optional)",
                    placeholder = "e.g. 19/08/03/042",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = matricError != null,
                    errorMessage = matricError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_matric_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Phone Number (Optional)
                UnimaidTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        if (phoneError != null) phoneError = null
                    },
                    label = "Phone Number (Optional for campus meetup)",
                    placeholder = "e.g. 08012345678",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = phoneError != null,
                    errorMessage = phoneError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_phone_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Password Input
                UnimaidTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) passwordError = null
                    },
                    label = "Password (min. 6 characters)",
                    placeholder = "••••••••",
                    isPassword = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "signup_password_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Confirm Password Input
                UnimaidTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (confirmPasswordError != null) confirmPasswordError = null
                    },
                    label = "Confirm Password",
                    placeholder = "••••••••",
                    isPassword = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = confirmPasswordError != null,
                    errorMessage = confirmPasswordError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitSignUp() }
                    ),
                    testTag = "signup_confirm_password_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Campus Safety & Terms Agreement Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!isLoading) agreeToTerms = !agreeToTerms }
                        .padding(vertical = AppSpacing.xs)
                ) {
                    Checkbox(
                        checked = agreeToTerms,
                        onCheckedChange = { agreeToTerms = it },
                        enabled = !isLoading,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("signup_terms_checkbox")
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "I agree to UNIMAID campus rules: transactions are strictly in-person with physical inspection at campus landmarks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (termsError != null) {
                    Text(
                        text = termsError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = AppSpacing.xs, top = AppSpacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.xl))

                // Submit Button
                UnimaidButton(
                    text = "Create Student Account",
                    onClick = { submitSignUp() },
                    isLoading = isLoading,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "signup_submit_button"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Sign In Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("signup_navigate_to_login_button")
                    ) {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
