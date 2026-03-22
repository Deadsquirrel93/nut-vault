package com.example.passwordvault.ui.home

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

data class HomeUiState(
    val folders: List<FolderSummary> = emptyList(),
    val standaloneCredentials: List<CredentialSummary> = emptyList(),
    val credentialsByFolderId: Map<Long, List<CredentialSummary>> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<CredentialSummary> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isSearchActive: Boolean
        get() = searchQuery.isNotBlank()
}

class HomeViewModel(
    private val vaultRepository: VaultRepository,
    private val saveFolderUseCase: SaveFolderUseCase,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        vaultRepository.observeFolders(),
        vaultRepository.observeRootCredentials(),
        vaultRepository.observeAllCredentials(),
        searchQuery,
    ) { folders, standaloneCredentials, allCredentials, query ->
        val folderNamesById = folders.associateBy({ it.id }, { it.name })
        val allCredentialsWithFolders = allCredentials.map { credential ->
            credential.copy(folderName = credential.folderId?.let(folderNamesById::get))
        }
        val normalizedQuery = query.trim()
        val searchResults = if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            allCredentialsWithFolders
                .filter { credential ->
                    credential.title.contains(normalizedQuery, ignoreCase = true) ||
                        credential.login.contains(normalizedQuery, ignoreCase = true) ||
                        credential.url.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                        credential.folderName.orEmpty().contains(normalizedQuery, ignoreCase = true)
                }
        }
        HomeUiState(
            folders = folders,
            standaloneCredentials = standaloneCredentials,
            credentialsByFolderId = allCredentialsWithFolders
                .filter { it.folderId != null }
                .groupBy { requireNotNull(it.folderId) },
            searchQuery = query,
            searchResults = searchResults,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    val messages = MutableSharedFlow<UiText>()

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun saveFolder(folderId: Long?, name: String) {
        viewModelScope.launch {
            runCatching {
                require(name.isNotBlank()) { "Folder name cannot be empty." }
                saveFolderUseCase(folderId, name)
            }.onSuccess {
                messages.emit(
                    UiText.StringResource(
                        if (folderId == null) R.string.folder_created else R.string.folder_renamed,
                    ),
                )
            }.onFailure {
                messages.emit(
                    UiText.StringResource(
                        if (it is IllegalArgumentException) R.string.folder_name_required else R.string.folder_save_failed,
                    ),
                )
            }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            runCatching { vaultRepository.deleteFolder(folderId) }
                .onSuccess { messages.emit(UiText.StringResource(R.string.folder_deleted)) }
                .onFailure { messages.emit(UiText.StringResource(R.string.folder_delete_failed)) }
        }
    }
}
