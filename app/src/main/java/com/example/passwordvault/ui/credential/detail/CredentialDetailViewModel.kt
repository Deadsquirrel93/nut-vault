package com.example.passwordvault.ui.credential.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.model.CredentialRecord
import com.example.passwordvault.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CredentialDetailUiState(
    val credential: CredentialRecord? = null,
    val isLoading: Boolean = true,
)

class CredentialDetailViewModel(
    credentialId: Long,
    private val vaultRepository: VaultRepository,
) : ViewModel() {
    val uiState: StateFlow<CredentialDetailUiState> = vaultRepository.observeCredential(credentialId)
        .map { credential ->
            CredentialDetailUiState(
                credential = credential,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CredentialDetailUiState(),
        )

    val messages = MutableSharedFlow<UiText>()

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
