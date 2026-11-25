package com.example.passwordvault.ui.credential.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.clipboard.ClipboardHelper
import com.example.passwordvault.core.crypto.TotpGenerator
import com.example.passwordvault.core.preferences.AppPreferences
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.model.CredentialInput
import com.example.passwordvault.domain.model.FolderSummary
import com.example.passwordvault.domain.model.PasswordGeneratorOptions
import com.example.passwordvault.domain.model.PasswordStrengthScore
import com.example.passwordvault.domain.repository.VaultRepository
import com.example.passwordvault.domain.usecase.EvaluatePasswordUseCase
import com.example.passwordvault.domain.usecase.GeneratePasswordUseCase
import com.example.passwordvault.domain.usecase.SaveCredentialUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CredentialEditorUiState(
    val credentialId: Long? = null,
    val folderId: Long? = null,
    val title: String = "",
    val url: String = "",
    val login: String = "",
    val password: String = "",
    val notes: String = "",
    val totpSecret: String = "",
    val isFavorite: Boolean = false,
    val folders: List<FolderSummary> = emptyList(),
    val strength: PasswordStrengthScore? = null,
    val isTotpValid: Boolean = true,
    val isLoading: Boolean = true,
)

class CredentialEditorViewModel(
    private val credentialId: Long?,
    private val initialFolderId: Long?,
    private val vaultRepository: VaultRepository,
    private val saveCredentialUseCase: SaveCredentialUseCase,
    private val generatePasswordUseCase: GeneratePasswordUseCase,
    private val evaluatePasswordUseCase: EvaluatePasswordUseCase,
    private val appPreferences: AppPreferences,
    private val clipboardHelper: ClipboardHelper,
) : ViewModel() {
    private val editorState = MutableStateFlow(
        CredentialEditorUiState(
            credentialId = credentialId,
            folderId = initialFolderId,
            strength = evaluatePasswordUseCase(""),
        ),
    )

    val generatorOptions: StateFlow<PasswordGeneratorOptions> = appPreferences.generatorOptions

    private var existingCredentialLoaded = false

    init {
        if (credentialId != null) {
            viewModelScope.launch {
                vaultRepository.observeCredential(credentialId).collect { credential ->
                    if (credential != null && !existingCredentialLoaded) {
                        existingCredentialLoaded = true
                        editorState.value = editorState.value.copy(
                            credentialId = credential.id,
                            folderId = credential.folderId,
                            title = credential.title,
                            url = credential.url.orEmpty(),
                            login = credential.login,
                            password = credential.password,
                            notes = credential.notes.orEmpty(),
                            totpSecret = credential.totpSecret.orEmpty(),
                            isFavorite = credential.isFavorite,
                            strength = evaluatePasswordUseCase(credential.password),
                            isLoading = false,
                        )
                    }
                }
            }
        } else {
            editorState.value = editorState.value.copy(isLoading = false)
        }
    }

    val uiState: StateFlow<CredentialEditorUiState> = combine(
        editorState,
        vaultRepository.observeFolders(),
    ) { state, folders ->
        val totpValid = state.totpSecret.isBlank() || TotpGenerator.isValidInput(state.totpSecret)
        state.copy(
            folders = folders.sortedBy { it.name.lowercase() },
            strength = evaluatePasswordUseCase(state.password),
            isTotpValid = totpValid,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = editorState.value,
    )

    val messages = MutableSharedFlow<UiText>()
    private val _savedCredentialIds = MutableSharedFlow<Long>()
    val savedCredentialIds: SharedFlow<Long> = _savedCredentialIds

    fun updateTitle(value: String) {
        editorState.value = editorState.value.copy(title = value)
    }

    fun updateUrl(value: String) {
        editorState.value = editorState.value.copy(url = value)
    }

    fun updateLogin(value: String) {
        editorState.value = editorState.value.copy(login = value)
    }

    fun updatePassword(value: String) {
        editorState.value = editorState.value.copy(password = value)
    }

    fun updateNotes(value: String) {
        editorState.value = editorState.value.copy(notes = value)
    }

    fun updateTotpSecret(value: String) {
        editorState.value = editorState.value.copy(totpSecret = value)
    }

    fun updateFavorite(value: Boolean) {
        editorState.value = editorState.value.copy(isFavorite = value)
    }

    fun updateFolder(folderId: Long?) {
        editorState.value = editorState.value.copy(folderId = folderId)
    }

    fun saveGeneratorOptions(options: PasswordGeneratorOptions) {
        appPreferences.setGeneratorOptions(options)
    }

    fun applyGeneratedPassword(options: PasswordGeneratorOptions): String =
        generatePasswordUseCase(options)

    fun evaluatePassword(password: String): PasswordStrengthScore =
        evaluatePasswordUseCase(password)

    fun copyGeneratedPassword(password: String, label: String) {
        viewModelScope.launch {
            clipboardHelper.copy(label = label, value = password, isSensitive = true)
            messages.emit(UiText.StringResource(R.string.password_copied))
        }
    }

    fun save() {
        viewModelScope.launch {
            val current = editorState.value
            runCatching {
                require(current.title.isNotBlank()) { "Title is required." }
                require(current.login.isNotBlank()) { "Login is required." }
                require(current.password.isNotBlank()) { "Password is required." }
                val trimmedTotp = current.totpSecret.trim().takeIf { it.isNotBlank() }
                if (trimmedTotp != null) {
                    require(TotpGenerator.isValidInput(trimmedTotp)) { "Invalid TOTP secret." }
                }
                saveCredentialUseCase(
                    CredentialInput(
                        id = current.credentialId,
                        folderId = current.folderId,
                        title = current.title,
                        url = current.url.takeIf { it.isNotBlank() },
                        login = current.login,
                        password = current.password,
                        notes = current.notes.takeIf { it.isNotBlank() },
                        totpSecret = trimmedTotp,
                        isFavorite = current.isFavorite,
                    ),
                )
            }.onSuccess {
                messages.emit(UiText.StringResource(R.string.credential_saved))
                _savedCredentialIds.emit(it)
            }.onFailure {
                val resId = when (it.message) {
                    "Title is required." -> R.string.title_required
                    "Login is required." -> R.string.login_required
                    "Password is required." -> R.string.password_required
                    "Invalid TOTP secret." -> R.string.totp_invalid_secret
                    else -> R.string.credential_save_failed
                }
                messages.emit(UiText.StringResource(resId))
            }
        }
    }
}
