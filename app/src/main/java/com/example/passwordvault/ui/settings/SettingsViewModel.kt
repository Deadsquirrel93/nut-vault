package com.example.passwordvault.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.auth.SessionManager
import com.example.passwordvault.core.localization.LocaleManager
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.model.AuthSettings
import com.example.passwordvault.domain.model.ChangePasswordResult
import com.example.passwordvault.domain.repository.AuthRepository
import com.example.passwordvault.domain.repository.VaultRepository
import com.example.passwordvault.domain.usecase.ChangeMasterPasswordUseCase
import com.example.passwordvault.domain.usecase.EnableProtectionUseCase
import com.example.passwordvault.domain.usecase.ExportVaultUseCase
import com.example.passwordvault.domain.usecase.ImportVaultUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val enableProtectionUseCase: EnableProtectionUseCase,
    private val changeMasterPasswordUseCase: ChangeMasterPasswordUseCase,
    private val exportVaultUseCase: ExportVaultUseCase,
    private val importVaultUseCase: ImportVaultUseCase,
    private val sessionManager: SessionManager,
    private val localeManager: LocaleManager,
) : ViewModel() {
    val settings: StateFlow<AuthSettings> = authRepository.observeSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthSettings(),
    )
    val languageCode: StateFlow<String> = localeManager.languageCode

    val messages = MutableSharedFlow<UiText>()

    fun enableProtection(login: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            runCatching {
                require(login.isNotBlank()) { "Login is required." }
                require(password.length >= 10) { "Master password must be at least 10 characters." }
                require(password == confirmPassword) { "Passwords do not match." }
                enableProtectionUseCase(login, password)
                sessionManager.unlock()
            }.onSuccess {
                messages.emit(UiText.StringResource(R.string.protection_enabled_message))
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

    fun enableBiometric(iv: String, ciphertext: String) {
        viewModelScope.launch {
            runCatching {
                authRepository.saveBiometricSecret(iv, ciphertext)
            }.onSuccess {
                messages.emit(UiText.StringResource(R.string.biometric_unlock_enabled))
            }.onFailure {
                messages.emit(UiText.StringResource(R.string.biometric_unlock_enable_failed))
            }
        }
    }

    fun changeMasterPassword(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String,
    ) {
        viewModelScope.launch {
            runCatching {
                require(currentPassword.isNotBlank()) { "Current master password is required." }
                require(newPassword.length >= 10) { "Master password must be at least 10 characters." }
                require(newPassword == confirmNewPassword) { "Passwords do not match." }
                require(newPassword != currentPassword) { "New master password must be different from the current one." }
                changeMasterPasswordUseCase(currentPassword, newPassword)
            }.onSuccess { result ->
                val resId = when (result) {
                    ChangePasswordResult.SUCCESS -> R.string.master_password_changed
                    ChangePasswordResult.INVALID_CURRENT_PASSWORD -> R.string.current_password_incorrect
                    ChangePasswordResult.PROTECTION_NOT_ENABLED -> R.string.master_password_change_failed
                }
                messages.emit(UiText.StringResource(resId))
            }.onFailure {
                val resId = when (it.message) {
                    "Current master password is required." -> R.string.current_password_required
                    "Master password must be at least 10 characters." -> R.string.master_password_min_length
                    "Passwords do not match." -> R.string.passwords_do_not_match
                    "New master password must be different from the current one." -> R.string.new_password_must_differ
                    else -> R.string.master_password_change_failed
                }
                messages.emit(UiText.StringResource(resId))
            }
        }
    }

    fun disableBiometric() {
        viewModelScope.launch {
            runCatching { authRepository.disableBiometric() }
                .onSuccess { messages.emit(UiText.StringResource(R.string.biometric_unlock_disabled)) }
                .onFailure { messages.emit(UiText.StringResource(R.string.biometric_unlock_disable_failed)) }
        }
    }

    fun exportVault(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            if (passphrase.length < 8) {
                messages.emit(UiText.StringResource(R.string.export_passphrase_min_length))
                return@launch
            }
            runCatching { exportVaultUseCase(uri, passphrase) }
                .onSuccess { messages.emit(UiText.StringResource(R.string.export_saved)) }
                .onFailure { messages.emit(UiText.StringResource(R.string.export_failed)) }
        }
    }

    fun importVault(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            if (passphrase.length < 8) {
                messages.emit(UiText.StringResource(R.string.import_passphrase_min_length))
                return@launch
            }
            runCatching { importVaultUseCase(uri, passphrase) }
                .onSuccess { messages.emit(UiText.StringResource(R.string.import_saved)) }
                .onFailure { messages.emit(UiText.StringResource(R.string.import_failed)) }
        }
    }

    fun deleteAllCredentials() {
        viewModelScope.launch {
            runCatching { vaultRepository.deleteAllCredentials() }
                .onSuccess { messages.emit(UiText.StringResource(R.string.delete_all_credentials_done)) }
                .onFailure { messages.emit(UiText.StringResource(R.string.delete_all_credentials_failed)) }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            runCatching { vaultRepository.deleteAllData() }
                .onSuccess { messages.emit(UiText.StringResource(R.string.delete_all_data_done)) }
                .onFailure { messages.emit(UiText.StringResource(R.string.delete_all_data_failed)) }
        }
    }

    fun setLanguage(languageCode: String) {
        localeManager.setLanguage(languageCode)
    }

    fun lockApp() {
        sessionManager.lock()
    }
}
