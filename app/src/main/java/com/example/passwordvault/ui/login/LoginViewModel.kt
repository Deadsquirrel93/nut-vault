package com.example.passwordvault.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.auth.BiometricCipherProvider
import com.example.passwordvault.core.auth.SessionManager
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.model.LoginFailureReason
import com.example.passwordvault.domain.repository.AuthRepository
import com.example.passwordvault.domain.usecase.VerifyLoginUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val verifyLoginUseCase: VerifyLoginUseCase,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val biometricCipherProvider: BiometricCipherProvider,
) : ViewModel() {
    val messages = MutableSharedFlow<UiText>()

    fun login(login: String, password: String) {
        viewModelScope.launch {
            val result = verifyLoginUseCase(login, password)
            if (result.success) {
                sessionManager.unlock()
            } else {
                val message = when (result.reason) {
                    LoginFailureReason.INVALID_CREDENTIALS ->
                        UiText.StringResource(R.string.incorrect_login_or_password)
                    LoginFailureReason.TEMPORARILY_LOCKED -> {
                        val seconds = ((result.lockoutRemainingMillis + 999L) / 1000L).coerceAtLeast(1L)
                        UiText.StringResource(R.string.lockout_countdown_format, listOf(seconds))
                    }
                    null -> UiText.StringResource(R.string.vault_unlock_failed)
                }
                messages.emit(message)
            }
        }
    }

    fun completeBiometricUnlock() {
        viewModelScope.launch {
            if (authRepository.getSettings().biometricEnabled) {
                sessionManager.unlock()
            }
        }
    }

    fun handleInvalidatedBiometric() {
        viewModelScope.launch {
            biometricCipherProvider.deleteKey()
            runCatching { authRepository.disableBiometric() }
            messages.emit(UiText.StringResource(R.string.biometric_invalidated_message))
        }
    }
}
