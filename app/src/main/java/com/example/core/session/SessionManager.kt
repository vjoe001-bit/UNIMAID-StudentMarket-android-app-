package com.example.core.session

import android.content.Context
import android.content.SharedPreferences
import com.example.core.network.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(
    val userId: String,
    val email: String,
    val fullName: String = "",
    val accessToken: String,
    val refreshToken: String,
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val isEmailConfirmed: Boolean = true,
    val isProfileComplete: Boolean = true,
    val verificationStatus: String = if (isVerified) "VERIFIED" else "UNVERIFIED"
)

sealed class SessionState {
    object Initial : SessionState()
    object Loading : SessionState()
    data class Authenticated(val session: UserSession) : SessionState()
    object Unauthenticated : SessionState()
}

/**
 * Manages active student session persistence across app launches.
 */
class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Initial)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        loadSession()
    }

    fun setLoading() {
        _sessionState.value = SessionState.Loading
    }

    fun loadSession() {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        val email = prefs.getString(KEY_EMAIL, null)

        if (!token.isNullOrBlank() && !userId.isNullOrBlank() && !email.isNullOrBlank()) {
            val isVerified = prefs.getBoolean(KEY_IS_VERIFIED, false)
            val session = UserSession(
                userId = userId,
                email = email,
                fullName = prefs.getString(KEY_FULL_NAME, "").orEmpty(),
                accessToken = token,
                refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "").orEmpty(),
                isVerified = isVerified,
                isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false),
                isEmailConfirmed = prefs.getBoolean(KEY_IS_EMAIL_CONFIRMED, true),
                isProfileComplete = prefs.getBoolean(KEY_IS_PROFILE_COMPLETE, true),
                verificationStatus = prefs.getString(KEY_VERIFICATION_STATUS, if (isVerified) "VERIFIED" else "UNVERIFIED") ?: "UNVERIFIED"
            )
            SupabaseClient.setAccessToken(token)
            _sessionState.value = SessionState.Authenticated(session)
        } else {
            SupabaseClient.setAccessToken(null)
            _sessionState.value = SessionState.Unauthenticated
        }
    }

    fun saveSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_FULL_NAME, session.fullName)
            .putBoolean(KEY_IS_VERIFIED, session.isVerified)
            .putBoolean(KEY_IS_ADMIN, session.isAdmin)
            .putBoolean(KEY_IS_EMAIL_CONFIRMED, session.isEmailConfirmed)
            .putBoolean(KEY_IS_PROFILE_COMPLETE, session.isProfileComplete)
            .putString(KEY_VERIFICATION_STATUS, session.verificationStatus)
            .apply()

        SupabaseClient.setAccessToken(session.accessToken)
        _sessionState.value = SessionState.Authenticated(session)
    }

    fun updateVerificationState(status: String, isVerified: Boolean = status.equals("VERIFIED", ignoreCase = true) || status.equals("APPROVED", ignoreCase = true)) {
        prefs.edit()
            .putBoolean(KEY_IS_VERIFIED, isVerified)
            .putString(KEY_VERIFICATION_STATUS, status)
            .apply()
        val current = _sessionState.value
        if (current is SessionState.Authenticated) {
            _sessionState.value = SessionState.Authenticated(
                current.session.copy(
                    isVerified = isVerified,
                    verificationStatus = status
                )
            )
        }
    }

    fun updateVerificationStatus(isVerified: Boolean) {
        val status = if (isVerified) "VERIFIED" else "UNVERIFIED"
        updateVerificationState(status, isVerified)
    }

    fun updateProfileComplete(isComplete: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PROFILE_COMPLETE, isComplete).apply()
        val current = _sessionState.value
        if (current is SessionState.Authenticated) {
            _sessionState.value = SessionState.Authenticated(current.session.copy(isProfileComplete = isComplete))
        }
    }

    fun updateEmailConfirmed(isConfirmed: Boolean) {
        prefs.edit().putBoolean(KEY_IS_EMAIL_CONFIRMED, isConfirmed).apply()
        val current = _sessionState.value
        if (current is SessionState.Authenticated) {
            _sessionState.value = SessionState.Authenticated(current.session.copy(isEmailConfirmed = isConfirmed))
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        SupabaseClient.setAccessToken(null)
        _sessionState.value = SessionState.Unauthenticated
    }

    companion object {
        private const val PREFS_NAME = "unimaid_student_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_IS_VERIFIED = "is_verified"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_IS_EMAIL_CONFIRMED = "is_email_confirmed"
        private const val KEY_IS_PROFILE_COMPLETE = "is_profile_complete"
        private const val KEY_VERIFICATION_STATUS = "verification_status"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
