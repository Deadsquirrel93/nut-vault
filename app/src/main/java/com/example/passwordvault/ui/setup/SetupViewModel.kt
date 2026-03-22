package com.example.passwordvault.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.auth.SessionManager
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.usecase.EnableProtectionUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class SetupViewModel(
    private val enableProtectionUseCase: EnableProtectionUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    val messages = MutableSharedFlow<UiText>()

    fun register(login: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            runCatching {
                require(login.isNotBlank()) { "Login is required." }
                require(password.length >= 10) { "Master password must be at least 10 characters." }
                require(password == confirmPassword) { "Passwords do not match." }
                enableProtectionUseCase(login, password)
                sessionManager.requestBiometricEnrollmentOffer()
                sessionManager.unlock()
            }.onSuccess {
                messages.emit(UiText.StringResource(R.string.registration_complete))
            }.onFailure {
                val resId = when (it.message) {
                    "Login is required." -> R.string.login_required
                    "Master password must be at least 10 characters." -> R.string.master_password_min_length
                    "Passwords do not match." -> R.string.passwords_do_not_match
                    else -> R.string.protection_enable_failed
                }
                messages.emit(UiText.StringResource(resId))
            }
        }
    }
}
