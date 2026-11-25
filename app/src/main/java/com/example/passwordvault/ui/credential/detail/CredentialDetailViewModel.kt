package com.example.passwordvault.ui.credential.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.clipboard.ClipboardHelper
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.model.CredentialRecord
import com.example.passwordvault.domain.model.PasswordStrengthScore
import com.example.passwordvault.domain.repository.VaultRepository
import com.example.passwordvault.domain.usecase.EvaluatePasswordUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CredentialDetailUiState(
    val credential: CredentialRecord? = null,
    val folderName: String? = null,
    val passwordStrength: PasswordStrengthScore? = null,
    val isLoading: Boolean = true,
)

class CredentialDetailViewModel(
    credentialId: Long,
    private val vaultRepository: VaultRepository,
    private val clipboardHelper: ClipboardHelper,
    private val evaluatePasswordUseCase: EvaluatePasswordUseCase,
) : ViewModel() {
    val uiState: StateFlow<CredentialDetailUiState> = combine(
        vaultRepository.observeCredential(credentialId),
        vaultRepository.observeFolders(),
    ) { credential, folders ->
        val folderName = credential?.folderId?.let { id ->
            folders.firstOrNull { it.id == id }?.name
        }
        CredentialDetailUiState(
            credential = credential,
            folderName = folderName,
            passwordStrength = credential?.let { evaluatePasswordUseCase(it.password) },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CredentialDetailUiState(),
    )

    val messages = MutableSharedFlow<UiText>()

    fun copyField(label: String, value: String, isSensitive: Boolean, feedbackRes: Int) {
        viewModelScope.launch {
            clipboardHelper.copy(label = label, value = value, isSensitive = isSensitive)
            messages.emit(UiText.StringResource(feedbackRes))
        }
    }

    fun toggleFavorite() {
        val current = uiState.value.credential ?: return
        val next = !current.isFavorite
        viewModelScope.launch {
            runCatching { vaultRepository.setFavorite(current.id, next) }
                .onSuccess {
                    messages.emit(
                        UiText.StringResource(
                            if (next) R.string.favorite_added else R.string.favorite_removed,
                        ),
                    )
                }
        }
    }

    fun deleteCredential(credentialId: Long, afterDelete: () -> Unit) {
        viewModelScope.launch {
            runCatching { vaultRepository.deleteCredential(credentialId) }
                .onSuccess {
                    messages.emit(UiText.StringResource(R.string.credential_deleted))
                    afterDelete()
                }
                .onFailure { messages.emit(UiText.StringResource(R.string.credential_delete_failed)) }
        }
    }
}
