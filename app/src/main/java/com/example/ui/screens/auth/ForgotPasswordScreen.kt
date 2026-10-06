package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.core.designsystem.components.ButtonVariant
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidTextField
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.network.SupabaseResult
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidBlueContainer
import com.example.core.theme.UnimaidBlueOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * Clean, secure Password Reset experience for UNIMAID StudentMarket.
 * Sends recovery link via Supabase GoTrue Auth without leaking account enumeration.
 */
@Composable
fun ForgotPasswordScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun validate(): Boolean {
        return if (email.isBlank()) {
            emailError = "Please enter your email address"
            false
        } else if (!email.contains("@") || !email.contains(".")) {
            emailError = "Please enter a valid email address"
            false
        } else {
            emailError = null
            true
        }
    }

    fun submitReset() {
        if (!validate() || isLoading) return
        isLoading = true
        generalError = null

        scope.launch {
            when (val result = authRepository.sendPasswordReset(email.trim())) {
                is SupabaseResult.Success -> {
                    isLoading = false
                    isSubmitted = true
                }
                is SupabaseResult.Error -> {
                    isLoading = false
                    // For security, if it's rate limit or network, show friendly info
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
            title = "Reset Password",
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
                if (!isSubmitted) {
                    Text(
                        text = "Forgot your password?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Text(
                        text = "Enter your registered UNIMAID student email address. We'll send you a secure link to create a new password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xl))

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
                                    .padding(bottom = AppSpacing.lg),
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

                    UnimaidTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
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
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitReset() }
                        ),
                        testTag = "forgot_password_email_input"
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xl))

                    UnimaidButton(
                        text = "Send Reset Link",
                        onClick = { submitReset() },
                        isLoading = isLoading,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "forgot_password_submit_button"
                    )
                } else {
                    // Success State Presentation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.md)
                            .testTag("reset_success_card"),
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
                                    imageVector = Icons.Outlined.MarkEmailRead,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.md))

                            Text(
                                text = "Reset Link Dispatched",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = VerifiedGreenOnContainer,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.sm))

                            Text(
                                text = "If an account exists for $email, you will receive password reset instructions shortly. Please check your inbox and spam folder.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VerifiedGreenOnContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    UnimaidButton(
                        text = "Back to Sign In",
                        onClick = onNavigateToLogin,
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "forgot_password_back_to_login_button"
                    )
                }
            }

            // Return to Sign In Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.xl),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remembered your password?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToLogin) {
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
