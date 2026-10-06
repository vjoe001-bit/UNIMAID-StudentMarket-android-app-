package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.SupabaseResult
import com.example.core.session.SessionState
import com.example.core.session.UserSession
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthActionState {
    object Idle : AuthActionState()
    object Loading : AuthActionState()
    data class Success(val message: String, val session: UserSession? = null) : AuthActionState()
    data class Error(val message: String, val code: String? = null) : AuthActionState()
}

/**
 * ViewModel managing authentication state and actions.
 * Sits between UI and AuthRepository to keep UI free of raw query logic.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.sessionState

    private val _actionState = MutableStateFlow<AuthActionState>(AuthActionState.Idle)
    val actionState: StateFlow<AuthActionState> = _actionState.asStateFlow()

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _actionState.value = AuthActionState.Loading
            when (val result = authRepository.signIn(email, pass)) {
                is SupabaseResult.Success -> {
                    _actionState.value = AuthActionState.Success("Logged in successfully", result.data)
                    onSuccess()
                }
                is SupabaseResult.Error -> {
                    _actionState.value = AuthActionState.Error(result.userFriendlyMessage, result.errorCode)
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    fun signUp(
        email: String,
        pass: String,
        fullName: String,
        matric: String?,
        dept: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _actionState.value = AuthActionState.Loading
            when (val result = authRepository.signUp(email, pass, fullName, matric, dept)) {
                is SupabaseResult.Success -> {
                    _actionState.value = AuthActionState.Success("Account created successfully", result.data)
                    onSuccess()
                }
                is SupabaseResult.Error -> {
                    _actionState.value = AuthActionState.Error(result.userFriendlyMessage, result.errorCode)
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            _actionState.value = AuthActionState.Loading
            authRepository.signOut()
            _actionState.value = AuthActionState.Idle
            onComplete()
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _actionState.value = AuthActionState.Loading
            when (val result = authRepository.sendPasswordReset(email)) {
                is SupabaseResult.Success -> {
                    _actionState.value = AuthActionState.Success("Password reset instructions sent to your email")
                }
                is SupabaseResult.Error -> {
                    _actionState.value = AuthActionState.Error(result.userFriendlyMessage, result.errorCode)
                }
                is SupabaseResult.Loading -> {}
            }
        }
    }

    fun clearActionState() {
        _actionState.value = AuthActionState.Idle
    }
}
