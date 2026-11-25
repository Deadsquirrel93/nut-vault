package com.example.passwordvault.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.core.password.PasswordHealthAnalyzer
import com.example.passwordvault.domain.model.PasswordHealthReport
import com.example.passwordvault.domain.repository.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PasswordHealthViewModel(
    vaultRepository: VaultRepository,
    analyzer: PasswordHealthAnalyzer,
) : ViewModel() {
    val report: StateFlow<PasswordHealthReport> = vaultRepository
        .observeAllCredentialRecords()
        .map { analyzer.analyze(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PasswordHealthReport(
                totalCount = 0,
                scorePercent = 100,
                weakCredentials = emptyList(),
                mediumCredentials = emptyList(),
                reusedGroups = emptyList(),
                totpEnabledCount = 0,
            ),
        )
}
