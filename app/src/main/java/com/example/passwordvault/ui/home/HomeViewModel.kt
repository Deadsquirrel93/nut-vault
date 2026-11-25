package com.example.passwordvault.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.R
import com.example.passwordvault.core.auth.SessionManager
import com.example.passwordvault.core.clipboard.ClipboardHelper
import com.example.passwordvault.core.password.PasswordHealthAnalyzer
import com.example.passwordvault.core.preferences.AppPreferences
import com.example.passwordvault.core.preferences.VaultSortOption
import com.example.passwordvault.core.ui.UiText
import com.example.passwordvault.domain.model.CredentialSummary
import com.example.passwordvault.domain.model.FolderSummary
import com.example.passwordvault.domain.model.PasswordGeneratorOptions
import com.example.passwordvault.domain.model.PasswordStrengthScore
import com.example.passwordvault.domain.repository.VaultRepository
import com.example.passwordvault.domain.usecase.EvaluatePasswordUseCase
import com.example.passwordvault.domain.usecase.GeneratePasswordUseCase
import com.example.passwordvault.domain.usecase.SaveFolderUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeFilter {
    ALL,
    FAVORITES,
    TOTP,
}

data class HomeUiState(
    val folders: List<FolderSummary> = emptyList(),
    val visibleCredentials: List<CredentialSummary> = emptyList(),
    val totalCredentialCount: Int = 0,
    val favoriteCount: Int = 0,
    val totpCount: Int = 0,
    val healthIssueCount: Int = 0,
    val searchQuery: String = "",
    val activeFilter: HomeFilter = HomeFilter.ALL,
    val sortOption: VaultSortOption = VaultSortOption.UPDATED_DESC,
    val isLoading: Boolean = true,
) {
    val isSearchActive: Boolean
        get() = searchQuery.isNotBlank()
}

class HomeViewModel(
    private val vaultRepository: VaultRepository,
    private val saveFolderUseCase: SaveFolderUseCase,
    private val sessionManager: SessionManager,
    private val clipboardHelper: ClipboardHelper,
    private val appPreferences: AppPreferences,
    private val passwordHealthAnalyzer: PasswordHealthAnalyzer,
    private val generatePasswordUseCase: GeneratePasswordUseCase,
    private val evaluatePasswordUseCase: EvaluatePasswordUseCase,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val activeFilter = MutableStateFlow(HomeFilter.ALL)

    val generatorOptions: StateFlow<PasswordGeneratorOptions> = appPreferences.generatorOptions

    val uiState: StateFlow<HomeUiState> = combine(
        vaultRepository.observeFolders(),
        vaultRepository.observeAllCredentialRecords(),
        searchQuery,
        activeFilter,
        appPreferences.sortOption,
    ) { folders, allRecords, query, filter, sort ->
        val folderNamesById = folders.associateBy({ it.id }, { it.name })
        val summaries = allRecords.map { record ->
            CredentialSummary(
                id = record.id,
                folderId = record.folderId,
                folderName = record.folderId?.let(folderNamesById::get),
                title = record.title,
                url = record.url,
                login = record.login,
                isFavorite = record.isFavorite,
                hasTotp = !record.totpSecret.isNullOrBlank(),
                updatedAt = record.updatedAt,
            )
        }

        val normalizedQuery = query.trim()
        val filtered = summaries.filter { item ->
            val matchesFilter = when (filter) {
                HomeFilter.ALL -> true
                HomeFilter.FAVORITES -> item.isFavorite
                HomeFilter.TOTP -> item.hasTotp
            }
            val matchesQuery = normalizedQuery.isBlank() ||
                item.title.contains(normalizedQuery, ignoreCase = true) ||
                item.login.contains(normalizedQuery, ignoreCase = true) ||
                item.url.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                item.folderName.orEmpty().contains(normalizedQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        val sorted = when (sort) {
            VaultSortOption.UPDATED_DESC -> filtered.sortedByDescending { it.updatedAt }
            VaultSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            VaultSortOption.FAVORITES_FIRST -> filtered.sortedWith(
                compareByDescending<CredentialSummary> { it.isFavorite }
                    .thenByDescending { it.updatedAt },
            )
        }

        val healthReport = passwordHealthAnalyzer.analyze(allRecords)
        val issueCount = healthReport.weakCredentials.size + healthReport.reusedGroups.size

        HomeUiState(
            folders = folders.sortedBy { it.name.lowercase() },
            visibleCredentials = sorted,
            totalCredentialCount = summaries.size,
            favoriteCount = summaries.count { it.isFavorite },
            totpCount = summaries.count { it.hasTotp },
            healthIssueCount = issueCount,
            searchQuery = query,
            activeFilter = filter,
            sortOption = sort,
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

    fun selectFilter(filter: HomeFilter) {
        activeFilter.value = filter
    }

    fun cycleSortOption() {
        val next = when (appPreferences.sortOption.value) {
            VaultSortOption.UPDATED_DESC -> VaultSortOption.TITLE_ASC
            VaultSortOption.TITLE_ASC -> VaultSortOption.FAVORITES_FIRST
            VaultSortOption.FAVORITES_FIRST -> VaultSortOption.UPDATED_DESC
        }
        appPreferences.setSortOption(next)
    }

    fun copyLogin(credentialId: Long, label: String) {
        viewModelScope.launch {
            val record = vaultRepository.getCredentialRecord(credentialId) ?: return@launch
            clipboardHelper.copy(label = label, value = record.login, isSensitive = false)
            messages.emit(UiText.StringResource(R.string.login_copied))
        }
    }

    fun copyPassword(credentialId: Long, label: String) {
        viewModelScope.launch {
            val record = vaultRepository.getCredentialRecord(credentialId) ?: return@launch
            clipboardHelper.copy(label = label, value = record.password, isSensitive = true)
            messages.emit(UiText.StringResource(R.string.password_copied))
        }
    }

    fun copyGeneratedPassword(password: String, label: String) {
        viewModelScope.launch {
            clipboardHelper.copy(label = label, value = password, isSensitive = true)
            messages.emit(UiText.StringResource(R.string.password_copied))
        }
    }

    fun saveGeneratorOptions(options: PasswordGeneratorOptions) {
        appPreferences.setGeneratorOptions(options)
    }

    fun generatePassword(options: PasswordGeneratorOptions): String =
        generatePasswordUseCase(options)

    fun evaluatePassword(password: String): PasswordStrengthScore =
        evaluatePasswordUseCase(password)

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

    fun lockVault() {
        sessionManager.lock()
    }
}
