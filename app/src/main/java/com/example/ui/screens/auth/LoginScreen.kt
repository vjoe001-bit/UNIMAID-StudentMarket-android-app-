package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidTextField
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.network.SupabaseResult
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.ErrorRed
import com.example.core.theme.ErrorRedContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.core.validation.StudentEmailValidator
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * Modern Sign In Screen for UNIMAID StudentMarket.
 *
 * Supports email/password credentials, password visibility toggle,
 * remember session checkbox, forgot password recovery, pending email confirmation
 * handling with resend capability, and friendly error banners.
 */
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var resendSuccess by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun validate(): Boolean {
        var isValid = true
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            emailError = "Please enter your email address"
            isValid = false
        } else if (!trimmedEmail.contains("@")) {
            emailError = "Please enter a valid email address (e.g. user@gmail.com)"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = "Please enter your password"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    fun submitSignIn() {
        if (!validate() || isLoading) return
        isLoading = true
        generalError = null
        resendSuccess = null

        scope.launch {
            when (val result = authRepository.signIn(email.trim(), password)) {
                is SupabaseResult.Success -> {
                    isLoading = false
                    onLoginSuccess()
                }
                is SupabaseResult.Error -> {
                    isLoading = false
                    generalError = result.userFriendlyMessage
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    fun resendConfirmationEmail() {
        val trimmed = email.trim()
        if (trimmed.isBlank() || isResending) return
        isResending = true
        resendSuccess = null

        scope.launch {
            when (val res = authRepository.resendVerificationEmail(trimmed)) {
                is SupabaseResult.Success -> {
                    isResending = false
                    resendSuccess = "Verification email sent to $trimmed! Please check your inbox and spam folder."
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
            title = "Sign In",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Text(
                    text = "Log in with your registered student credentials to access your campus listings, cart, and student chats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.xl))

                // Resend Success Banner
                if (resendSuccess != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppSpacing.md)
                            .testTag("login_resend_success_banner"),
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
                                tint = VerifiedGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Text(
                                text = resendSuccess!!,
                                color = VerifiedGreenOnContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Friendly Error Banner
                AnimatedVisibility(
                    visible = generalError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    generalError?.let { errorText ->
                        val isEmailConfirmationError = errorText.contains("confirm", ignoreCase = true) ||
                                errorText.contains("verified", ignoreCase = true)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = AppSpacing.lg)
                                .testTag("login_error_banner"),
                            colors = CardDefaults.cardColors(containerColor = ErrorRedContainer),
                            shape = AppRadius.md
                        ) {
                            Column(modifier = Modifier.padding(AppSpacing.md)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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

                                if (isEmailConfirmationError && email.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                                    TextButton(
                                        onClick = { resendConfirmationEmail() },
                                        enabled = !isResending && !isLoading,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(
                                            text = if (isResending) "Sending..." else "Resend Confirmation Email",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Email Address Field
                UnimaidTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) emailError = null
                        if (generalError != null) generalError = null
                    },
                    label = "Email Address",
                    placeholder = "e.g. user@gmail.com",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
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
                    testTag = "login_email_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                // Password Field
                UnimaidTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) passwordError = null
                        if (generalError != null) generalError = null
                    },
                    label = "Password",
                    isPassword = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitSignIn() }
                    ),
                    testTag = "login_password_input"
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Remember Me & Forgot Password Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { if (!isLoading) rememberMe = !rememberMe }
                            .padding(vertical = AppSpacing.xs)
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            enabled = !isLoading,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("login_remember_me_checkbox")
                        )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("login_forgot_password_button")
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                UnimaidButton(
                    text = "Sign In",
                    onClick = { submitSignIn() },
                    isLoading = isLoading,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "login_submit_button"
                )
            }

            // Bottom Navigation to Register
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.xl),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onNavigateToSignUp,
                    enabled = !isLoading,
                    modifier = Modifier.testTag("login_navigate_to_signup_button")
                ) {
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
