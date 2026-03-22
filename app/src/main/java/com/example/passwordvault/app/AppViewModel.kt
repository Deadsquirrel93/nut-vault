package com.example.passwordvault.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.domain.model.AuthSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AppUiState(
    val settings: AuthSettings = AuthSettings(),
    val isUnlocked: Boolean = false,
    val shouldOfferBiometricEnrollment: Boolean = false,
    val isLoading: Boolean = true,
) {
    val requiresSetup: Boolean
        get() = !settings.protectionEnabled

    val requiresLogin: Boolean
        get() = settings.protectionEnabled && !isUnlocked
}

class AppViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val uiState: StateFlow<AppUiState> = combine(
        container.authRepository.observeSettings(),
        container.sessionManager.isUnlocked,
        container.sessionManager.shouldOfferBiometricEnrollment,
    ) { settings, isUnlocked, shouldOfferBiometricEnrollment ->
        AppUiState(
            settings = settings,
            isUnlocked = isUnlocked,
            shouldOfferBiometricEnrollment = shouldOfferBiometricEnrollment &&
                settings.protectionEnabled &&
                !settings.biometricEnabled,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    fun dismissBiometricEnrollmentOffer() {
        container.sessionManager.clearBiometricEnrollmentOffer()
    }

    fun enableBiometric(iv: String, ciphertext: String) {
        viewModelScope.launch {
            runCatching {
                container.authRepository.saveBiometricSecret(iv, ciphertext)
            }
            container.sessionManager.clearBiometricEnrollmentOffer()
        }
    }
}
