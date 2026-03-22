package com.example.passwordvault.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.model.CredentialSummary
import com.example.passwordvault.domain.model.FolderSummary
import com.example.passwordvault.domain.repository.VaultRepository
import com.example.passwordvault.domain.usecase.SaveFolderUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderUiState(
    val folder: FolderSummary? = null,
    val credentials: List<CredentialSummary> = emptyList(),
    val searchQuery: String = "",
    val filteredCredentials: List<CredentialSummary> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isSearchActive: Boolean
        get() = searchQuery.isNotBlank()
}

class FolderViewModel(
    folderId: Long,
    private val vaultRepository: VaultRepository,
    private val saveFolderUseCase: SaveFolderUseCase,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<FolderUiState> = combine(
        vaultRepository.observeFolder(folderId),
        vaultRepository.observeCredentialsInFolder(folderId),
        searchQuery,
    ) { folder, credentials, query ->
        val normalizedQuery = query.trim()
        val filteredCredentials = if (normalizedQuery.isBlank()) {
            credentials
        } else {
            credentials.filter { credential ->
                credential.title.contains(normalizedQuery, ignoreCase = true) ||
                    credential.login.contains(normalizedQuery, ignoreCase = true) ||
                    credential.url.orEmpty().contains(normalizedQuery, ignoreCase = true)
            }
        }
        FolderUiState(
            folder = folder,
            credentials = credentials,
            searchQuery = query,
            filteredCredentials = filteredCredentials,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FolderUiState(),
    )

    val messages = MutableSharedFlow<UiText>()

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun renameFolder(folderId: Long, name: String) {
        viewModelScope.launch {
            runCatching {
                require(name.isNotBlank()) { "Folder name cannot be empty." }
                saveFolderUseCase(folderId, name)
            }.onSuccess {
                messages.emit(UiText.StringResource(R.string.folder_renamed))
            }.onFailure {
                messages.emit(
                    UiText.StringResource(
                        if (it is IllegalArgumentException) R.string.folder_name_required else R.string.folder_save_failed,
                    ),
                )
            }
        }
    }

    fun deleteFolder(folderId: Long, afterDelete: () -> Unit) {
        viewModelScope.launch {
            runCatching { vaultRepository.deleteFolder(folderId) }
                .onSuccess {
                    messages.emit(UiText.StringResource(R.string.folder_deleted))
                    afterDelete()
                }
                .onFailure { messages.emit(UiText.StringResource(R.string.folder_delete_failed)) }
        }
    }
}
