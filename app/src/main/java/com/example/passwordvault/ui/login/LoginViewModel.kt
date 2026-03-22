package com.example.passwordvault.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
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
) : ViewModel() {
    val messages = MutableSharedFlow<UiText>()

    fun login(login: String, password: String) {
        viewModelScope.launch {
            val result = verifyLoginUseCase(login, password)
            if (result.success) {
                sessionManager.unlock()
                messages.emit(UiText.StringResource(R.string.vault_unlocked))
            } else {
                val resId = when (result.reason) {
                    LoginFailureReason.INVALID_CREDENTIALS -> R.string.incorrect_login_or_password
                    LoginFailureReason.TEMPORARILY_LOCKED -> {
                        if (result.lockoutRemainingMillis > 0) R.string.login_temporarily_locked
                        else R.string.login_locked_now
                    }
                    null -> R.string.vault_unlock_failed
                }
                messages.emit(UiText.StringResource(resId))
            }
        }
    }

    fun completeBiometricUnlock() {
        viewModelScope.launch {
            if (authRepository.getSettings().biometricEnabled) {
                sessionManager.unlock()
                messages.emit(UiText.StringResource(R.string.vault_unlocked_biometric))
            }
        }
    }
}
