package com.example.data.repository

import com.example.core.admin.AdminConfig
import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.auth.AuthRecoverRequest
import com.example.core.network.auth.AuthRefreshRequest
import com.example.core.network.auth.AuthResendRequest
import com.example.core.network.auth.AuthSignInRequest
import com.example.core.network.auth.AuthSignUpRequest
import com.example.core.network.auth.SupabaseAuthApi
import com.example.core.network.rest.SupabaseRestApi
import com.example.core.session.SessionManager
import com.example.core.session.SessionState
import com.example.core.session.UserSession
import com.example.core.validation.StudentEmailValidator
import com.example.data.models.Profile
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        matricNumber: String?,
        department: String?,
        phoneNumber: String? = null,
        level: String? = "100L"
    ): SupabaseResult<UserSession>

    suspend fun signIn(email: String, password: String): SupabaseResult<UserSession>

    suspend fun signOut(): SupabaseResult<Unit>

    suspend fun sendPasswordReset(email: String): SupabaseResult<Unit>

    suspend fun resendVerificationEmail(email: String): SupabaseResult<Unit>

    suspend fun validateAndRestoreSession(): SessionState

    fun refreshCurrentSession()
}

class AuthRepositoryImpl(
    private val sessionManager: SessionManager,
    private val authApi: SupabaseAuthApi = SupabaseClient.createAuthApi(),
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi()
) : AuthRepository {

    override val sessionState: StateFlow<SessionState> = sessionManager.sessionState

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        matricNumber: String?,
        department: String?,
        phoneNumber: String?,
        level: String?
    ): SupabaseResult<UserSession> {
        val trimmedEmail = email.trim()

        // 1. Enforce Student Email Validation rule
        val emailValidation = StudentEmailValidator.validate(trimmedEmail)
        if (emailValidation is StudentEmailValidator.ValidationResult.Invalid) {
            return SupabaseResult.Error(
                userFriendlyMessage = emailValidation.message,
                errorCode = "INVALID_STUDENT_EMAIL"
            )
        }

        if (password.length < 6) {
            return SupabaseResult.Error(
                userFriendlyMessage = "Password must be at least 6 characters long.",
                errorCode = "WEAK_PASSWORD"
            )
        }

        return try {
            val metadata = mutableMapOf<String, String>("full_name" to fullName.trim())
            if (!matricNumber.isNullOrBlank()) metadata["matric_number"] = matricNumber.trim()
            if (!department.isNullOrBlank()) metadata["department"] = department.trim()
            if (!phoneNumber.isNullOrBlank()) metadata["phone_number"] = phoneNumber.trim()
            if (!level.isNullOrBlank()) metadata["level"] = level.trim()

            val response = authApi.signUp(
                AuthSignUpRequest(
                    email = trimmedEmail,
                    password = password,
                    data = metadata
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val user = body.user
                val accessToken = body.accessToken.orEmpty()
                val refreshToken = body.refreshToken.orEmpty()
                val userId = user?.id.orEmpty()
                val isEmailConfirmed = !user?.emailConfirmedAt.isNullOrBlank() ||
                        !user?.confirmedAt.isNullOrBlank() ||
                        accessToken.isNotBlank()

                // Check and initialize Profile in public.profiles table (do not duplicate)
                if (userId.isNotBlank() && accessToken.isNotBlank()) {
                    SupabaseClient.setAccessToken(accessToken)
                    try {
                        val existingResp = restApi.getProfileById("eq.$userId")
                        val alreadyExists = existingResp.isSuccessful && existingResp.body()?.isNotEmpty() == true
                        if (!alreadyExists) {
                            val newProfile = Profile(
                                id = userId,
                                fullName = fullName.trim().ifBlank { "UNIMAID Student" },
                                matricNumber = matricNumber?.trim().takeIf { !it.isNullOrBlank() },
                                department = department?.trim().takeIf { !it.isNullOrBlank() },
                                level = level?.trim().takeIf { !it.isNullOrBlank() } ?: "100L",
                                isVerified = false,
                                isSuspended = false,
                                email = trimmedEmail,
                                phoneNumber = phoneNumber?.trim().takeIf { !it.isNullOrBlank() }
                            )
                            restApi.insertProfile(newProfile)
                        }
                    } catch (e: Throwable) {
                        // Profile insertion non-fatal if database trigger or RLS handled it
                    }
                }

                val session = UserSession(
                    userId = userId,
                    email = trimmedEmail,
                    fullName = fullName.trim(),
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    isVerified = false,
                    isAdmin = false,
                    isEmailConfirmed = isEmailConfirmed,
                    isProfileComplete = fullName.isNotBlank() && !department.isNullOrBlank()
                )

                // Only save local authenticated session if Supabase issued immediate tokens
                if (accessToken.isNotBlank()) {
                    sessionManager.saveSession(session)
                }

                SupabaseResult.Success(session)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun signIn(email: String, password: String): SupabaseResult<UserSession> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            return SupabaseResult.Error(
                userFriendlyMessage = "Please enter both your student email and password.",
                errorCode = "EMPTY_CREDENTIALS"
            )
        }

        return try {
            val response = authApi.signInWithPassword(
                request = AuthSignInRequest(
                    email = trimmedEmail,
                    password = password
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val user = body.user
                val accessToken = body.accessToken.orEmpty()
                val refreshToken = body.refreshToken.orEmpty()
                val userId = user?.id.orEmpty()
                val fullName = (user?.userMetadata?.get("full_name") as? String).orEmpty()
                val isEmailConfirmed = !user?.emailConfirmedAt.isNullOrBlank() ||
                        !user?.confirmedAt.isNullOrBlank() ||
                        accessToken.isNotBlank()

                // CRITICAL: Set active user token IMMEDIATELY so PostgREST calls carry the user JWT and auth.uid()!
                if (accessToken.isNotBlank()) {
                    SupabaseClient.setAccessToken(accessToken)
                }

                val userMetaName = (user?.userMetadata?.get("full_name") as? String)
                    ?: (user?.userMetadata?.get("name") as? String)
                val fallbackName = userMetaName?.ifBlank { null }
                    ?: trimmedEmail.substringBefore("@").ifBlank { "UNIMAID Student" }

                // Resolve profile and admin status directly from Supabase tables
                val isDesignatedAdmin = AdminConfig.isDesignatedAdmin(userId, trimmedEmail)
                var isVerified = isDesignatedAdmin
                var isAdmin = isDesignatedAdmin
                var resolvedFullName = fallbackName
                var isProfileComplete = false
                var verificationStatus = if (isDesignatedAdmin) "VERIFIED" else "UNVERIFIED"

                if (userId.isNotBlank()) {
                    try {
                        val profileResp = restApi.getProfileById("eq.$userId")
                        if (profileResp.isSuccessful && profileResp.body()?.isNotEmpty() == true) {
                            val profile = profileResp.body()!!.first()
                            isVerified = profile.isVerified
                            if (!profile.fullName.isNullOrBlank()) {
                                resolvedFullName = profile.fullName
                            }
                            isProfileComplete = profile.isComplete
                            verificationStatus = if (profile.isVerified) "VERIFIED" else "UNVERIFIED"
                        } else {
                            // First time login with no profile: create and recover one
                            val newProfile = Profile(
                                id = userId,
                                fullName = fallbackName,
                                department = (user?.userMetadata?.get("department") as? String),
                                faculty = (user?.userMetadata?.get("faculty") as? String),
                                level = (user?.userMetadata?.get("level") as? String) ?: "100L",
                                matricNumber = (user?.userMetadata?.get("matric_number") as? String),
                                isVerified = false,
                                isSuspended = false,
                                email = user?.email ?: trimmedEmail,
                                phoneNumber = (user?.userMetadata?.get("phone_number") as? String)
                            )
                            val insertResp = restApi.insertProfile(newProfile)
                            if (insertResp.isSuccessful && insertResp.body()?.isNotEmpty() == true) {
                                val created = insertResp.body()!!.first()
                                isVerified = created.isVerified
                                isProfileComplete = created.isComplete
                                resolvedFullName = created.fullName ?: newProfile.fullName ?: fallbackName
                                verificationStatus = if (created.isVerified) "VERIFIED" else "UNVERIFIED"
                            } else {
                                isProfileComplete = newProfile.isComplete
                                resolvedFullName = newProfile.fullName ?: fallbackName
                            }
                        }
                    } catch (e: Throwable) {
                        // Non-fatal; continue with session metadata
                    }

                    // Query latest verification request status
                    try {
                        val verifResp = restApi.getVerificationRequests(filters = mapOf("student_id" to "eq.$userId"))
                        if (verifResp.isSuccessful) {
                            val latestReq = verifResp.body()?.firstOrNull()
                            if (latestReq != null) {
                                when (latestReq.status.uppercase()) {
                                    "APPROVED" -> {
                                        isVerified = true
                                        verificationStatus = "VERIFIED"
                                    }
                                    "PENDING" -> {
                                        verificationStatus = "VERIFICATION_PENDING"
                                    }
                                    "REJECTED" -> {
                                        verificationStatus = "REJECTED"
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        // Non-fatal
                    }

                    // Query admin_roles table for authorized administrative access
                    try {
                        val roleResp = restApi.getAdminRoleForUser("eq.$userId")
                        if (roleResp.isSuccessful && roleResp.body()?.isNotEmpty() == true) {
                            isAdmin = true
                        }
                    } catch (e: Throwable) {
                        // Non-fatal
                    }
                }

                val session = UserSession(
                    userId = userId,
                    email = trimmedEmail,
                    fullName = resolvedFullName,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    isVerified = isVerified,
                    isAdmin = isAdmin,
                    isEmailConfirmed = isEmailConfirmed,
                    isProfileComplete = isProfileComplete,
                    verificationStatus = verificationStatus
                )

                sessionManager.saveSession(session)
                SupabaseResult.Success(session)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun signOut(): SupabaseResult<Unit> {
        return try {
            val token = SupabaseClient.getAccessToken()
            if (!token.isNullOrBlank()) {
                authApi.logout("Bearer $token")
            }
            sessionManager.clearSession()
            SupabaseResult.Success(Unit)
        } catch (t: Throwable) {
            sessionManager.clearSession()
            SupabaseResult.Success(Unit)
        }
    }

    override suspend fun sendPasswordReset(email: String): SupabaseResult<Unit> {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return SupabaseResult.Error(
                userFriendlyMessage = "Please enter your UNIMAID student email address.",
                errorCode = "EMPTY_EMAIL"
            )
        }

        return try {
            val response = authApi.recoverPassword(AuthRecoverRequest(trimmed))
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun resendVerificationEmail(email: String): SupabaseResult<Unit> {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return SupabaseResult.Error(
                userFriendlyMessage = "Please enter your email address to resend confirmation.",
                errorCode = "EMPTY_EMAIL"
            )
        }

        return try {
            val response = authApi.resendVerificationEmail(
                AuthResendRequest(type = "signup", email = trimmed)
            )
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun validateAndRestoreSession(): SessionState {
        sessionManager.setLoading()
        val currentSession = (sessionManager.sessionState.value as? SessionState.Authenticated)?.session
            ?: run {
                sessionManager.loadSession()
                (sessionManager.sessionState.value as? SessionState.Authenticated)?.session
            }

        if (currentSession == null || currentSession.accessToken.isBlank()) {
            sessionManager.loadSession()
            return sessionManager.sessionState.value
        }

        val token = currentSession.accessToken
        val refreshToken = currentSession.refreshToken
        SupabaseClient.setAccessToken(token)

        return try {
            // Verify current access token with Supabase Auth
            val userResp = authApi.getCurrentUser("Bearer $token")
            if (userResp.isSuccessful && userResp.body() != null) {
                val user = userResp.body()!!
                val isEmailConfirmed = !user.emailConfirmedAt.isNullOrBlank() || !user.confirmedAt.isNullOrBlank()

                // Refresh profile details in background
                val isDesignatedAdmin = AdminConfig.isDesignatedAdmin(user.id, user.email ?: currentSession.email)
                var isVerified = currentSession.isVerified || isDesignatedAdmin
                var isAdmin = currentSession.isAdmin || isDesignatedAdmin
                var isProfileComplete = currentSession.isProfileComplete
                var verificationStatus = if (isDesignatedAdmin) "VERIFIED" else currentSession.verificationStatus
                var resolvedFullName = currentSession.fullName

                try {
                    val pResp = restApi.getProfileById("eq.${user.id}")
                    if (pResp.isSuccessful && pResp.body()?.isNotEmpty() == true) {
                        val p = pResp.body()!!.first()
                        isVerified = p.isVerified
                        isProfileComplete = p.isComplete
                        if (!p.fullName.isNullOrBlank()) {
                            resolvedFullName = p.fullName
                        }
                        verificationStatus = if (p.isVerified) "VERIFIED" else "UNVERIFIED"
                    } else if (pResp.isSuccessful && pResp.body().isNullOrEmpty()) {
                        // Profile missing: safely recover it
                        val userMetaName = (user.userMetadata?.get("full_name") as? String)
                            ?: (user.userMetadata?.get("name") as? String)
                        val fallbackName = userMetaName?.ifBlank { null }
                            ?: currentSession.email.substringBefore("@").ifBlank { "UNIMAID Student" }
                        val recoverProfile = Profile(
                            id = user.id,
                            fullName = resolvedFullName.ifBlank { fallbackName },
                            department = (user.userMetadata?.get("department") as? String),
                            faculty = (user.userMetadata?.get("faculty") as? String),
                            level = (user.userMetadata?.get("level") as? String) ?: "100L",
                            matricNumber = (user.userMetadata?.get("matric_number") as? String),
                            isVerified = false,
                            isSuspended = false,
                            email = user.email ?: currentSession.email,
                            phoneNumber = (user.userMetadata?.get("phone_number") as? String)
                        )
                        val insResp = restApi.insertProfile(recoverProfile)
                        if (insResp.isSuccessful && insResp.body()?.isNotEmpty() == true) {
                            val created = insResp.body()!!.first()
                            isVerified = created.isVerified
                            isProfileComplete = created.isComplete
                            resolvedFullName = created.fullName ?: recoverProfile.fullName ?: fallbackName
                            verificationStatus = if (created.isVerified) "VERIFIED" else "UNVERIFIED"
                        }
                    }

                    // Also check latest verification request
                    try {
                        val verifResp = restApi.getVerificationRequests(filters = mapOf("student_id" to "eq.${user.id}"))
                        if (verifResp.isSuccessful) {
                            val latestReq = verifResp.body()?.firstOrNull()
                            if (latestReq != null) {
                                when (latestReq.status.uppercase()) {
                                    "APPROVED" -> {
                                        isVerified = true
                                        verificationStatus = "VERIFIED"
                                    }
                                    "PENDING" -> {
                                        verificationStatus = "VERIFICATION_PENDING"
                                    }
                                    "REJECTED" -> {
                                        verificationStatus = "REJECTED"
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        // Non-fatal
                    }

                    val roleResp = restApi.getAdminRoleForUser("eq.${user.id}")
                    if (roleResp.isSuccessful && roleResp.body()?.isNotEmpty() == true) {
                        isAdmin = true
                    }
                } catch (e: Throwable) {
                    // Non-fatal
                }

                val validated = currentSession.copy(
                    fullName = resolvedFullName,
                    isEmailConfirmed = isEmailConfirmed,
                    isVerified = isVerified,
                    isAdmin = isAdmin,
                    isProfileComplete = isProfileComplete,
                    verificationStatus = verificationStatus
                )
                sessionManager.saveSession(validated)
                SessionState.Authenticated(validated)
            } else if (userResp.code() == 401 && refreshToken.isNotBlank()) {
                // Token expired; attempt refresh token grant
                val refreshResp = authApi.refreshToken(request = AuthRefreshRequest(refreshToken))
                if (refreshResp.isSuccessful && refreshResp.body() != null) {
                    val body = refreshResp.body()!!
                    val newAccessToken = body.accessToken.orEmpty()
                    val newRefreshToken = body.refreshToken.orEmpty()
                    if (newAccessToken.isNotBlank()) {
                        val refreshedSession = currentSession.copy(
                            accessToken = newAccessToken,
                            refreshToken = newRefreshToken.ifBlank { refreshToken }
                        )
                        sessionManager.saveSession(refreshedSession)
                        SessionState.Authenticated(refreshedSession)
                    } else {
                        sessionManager.clearSession()
                        SessionState.Unauthenticated
                    }
                } else {
                    // Refresh token invalid or revoked
                    sessionManager.clearSession()
                    SessionState.Unauthenticated
                }
            } else {
                // Invalid response
                sessionManager.clearSession()
                SessionState.Unauthenticated
            }
        } catch (ioe: IOException) {
            // Network connection error; retain existing offline session
            SessionState.Authenticated(currentSession)
        } catch (t: Throwable) {
            sessionManager.clearSession()
            SessionState.Unauthenticated
        }
    }

    override fun refreshCurrentSession() {
        sessionManager.loadSession()
    }
}
