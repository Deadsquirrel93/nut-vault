package com.example.passwordvault.ui.credential.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.model.CredentialInput
import com.example.passwordvault.domain.model.CredentialRecord
import com.example.passwordvault.domain.model.FolderSummary
import com.example.passwordvault.domain.model.PasswordGeneratorOptions
import com.example.passwordvault.domain.model.PasswordStrengthScore
import com.example.passwordvault.domain.repository.VaultRepository
import com.example.passwordvault.domain.usecase.EvaluatePasswordUseCase
import com.example.passwordvault.domain.usecase.GeneratePasswordUseCase
import com.example.passwordvault.domain.usecase.SaveCredentialUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
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
    val folders: List<FolderSummary> = emptyList(),
    val strength: PasswordStrengthScore? = null,
    val isLoading: Boolean = true,
)

class CredentialEditorViewModel(
    private val credentialId: Long?,
    private val initialFolderId: Long?,
    private val vaultRepository: VaultRepository,
    private val saveCredentialUseCase: SaveCredentialUseCase,
    private val generatePasswordUseCase: GeneratePasswordUseCase,
    private val evaluatePasswordUseCase: EvaluatePasswordUseCase,
) : ViewModel() {
    private val editorState = MutableStateFlow(
        CredentialEditorUiState(
            credentialId = credentialId,
            folderId = initialFolderId,
            strength = evaluatePasswordUseCase(""),
        ),
    )

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
        state.copy(
            folders = folders,
            strength = evaluatePasswordUseCase(state.password),
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

    fun updateFolder(folderId: Long?) {
        editorState.value = editorState.value.copy(folderId = folderId)
    }

    fun applyGeneratedPassword(options: PasswordGeneratorOptions): String {
        return generatePasswordUseCase(options)
    }

    fun evaluatePassword(password: String): PasswordStrengthScore {
        return evaluatePasswordUseCase(password)
    }

    fun save() {
        viewModelScope.launch {
            val current = editorState.value
            runCatching {
                require(current.title.isNotBlank()) { "Title is required." }
                require(current.login.isNotBlank()) { "Login is required." }
                require(current.password.isNotBlank()) { "Password is required." }
                saveCredentialUseCase(
                    CredentialInput(
                        id = current.credentialId,
                        folderId = current.folderId,
                        title = current.title,
                        url = current.url.takeIf { it.isNotBlank() },
                        login = current.login,
                        password = current.password,
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
                    else -> R.string.credential_save_failed
                }
                messages.emit(UiText.StringResource(resId))
            }
        }
    }
}
