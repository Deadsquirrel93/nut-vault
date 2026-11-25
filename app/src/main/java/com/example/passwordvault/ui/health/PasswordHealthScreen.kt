package com.example.passwordvault.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.domain.model.CredentialRecord
import com.example.passwordvault.domain.model.CredentialSummary
import com.example.passwordvault.ui.components.CompactCredentialRow
import com.example.passwordvault.ui.components.EmptyStateCard
import com.example.passwordvault.ui.components.VaultSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordHealthScreen(
    viewModel: PasswordHealthViewModel,
    onBack: () -> Unit,
    onOpenCredential: (Long) -> Unit,
) {
    val report by viewModel.report.collectAsStateWithLifecycle()

    val scoreColor = when {
        report.scorePercent >= 80 -> MaterialTheme.colorScheme.secondary
        report.scorePercent >= 55 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = { Text(stringResource(R.string.health_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                top = padding.calculateTopPadding() + 4.dp,
                end = 14.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "score_card") {
                VaultSectionCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { report.scorePercent / 100f },
                                modifier = Modifier.size(64.dp),
                                color = scoreColor,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                strokeWidth = 6.dp,
                                strokeCap = StrokeCap.Round,
                            )
                            Text(
                                text = "${report.scorePercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.health_score_label),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StatPill(
                                    label = stringResource(R.string.health_stat_total),
                                    value = report.totalCount.toString(),
                                    modifier = Modifier.weight(1f),
                                )
                                StatPill(
                                    label = stringResource(R.string.health_stat_weak),
                                    value = report.weakCredentials.size.toString(),
                                    tint = if (report.weakCredentials.isNotEmpty()) MaterialTheme.colorScheme.error else null,
                                    modifier = Modifier.weight(1f),
                                )
                                StatPill(
                                    label = stringResource(R.string.health_stat_reused),
                                    value = report.reusedGroups.sumOf { it.credentials.size }.toString(),
                                    tint = if (report.reusedGroups.isNotEmpty()) MaterialTheme.colorScheme.tertiary else null,
                                    modifier = Modifier.weight(1f),
                                )
                                StatPill(
                                    label = stringResource(R.string.health_stat_totp),
                                    value = report.totpEnabledCount.toString(),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            val hasIssues = report.weakCredentials.isNotEmpty() || report.reusedGroups.isNotEmpty()
            if (!hasIssues) {
                item(key = "all_good") {
                    EmptyStateCard(
                        title = stringResource(R.string.health_all_good_title),
                        body = stringResource(R.string.health_all_good_body),
                    )
                }
            }

            if (report.weakCredentials.isNotEmpty()) {
                item(key = "weak_header") {
                    Text(
                        text = stringResource(R.string.health_weak_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                    )
                }
                items(report.weakCredentials, key = { "weak_${it.id}" }) { record ->
                    CompactCredentialRow(
                        credential = record.toSummary(),
                        onOpen = { onOpenCredential(record.id) },
                        showFolderBadge = false,
                    )
                }
            }

            if (report.reusedGroups.isNotEmpty()) {
                item(key = "reused_header") {
                    Text(
                        text = stringResource(R.string.health_reused_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                    )
                }
                report.reusedGroups.forEachIndexed { idx, group ->
                    item(key = "reused_group_$idx") {
                        VaultSectionCard(contentPadding = 12.dp) {
                            Text(
                                text = stringResource(R.string.health_reused_group_label, group.credentials.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            group.credentials.forEach { record ->
                                CompactCredentialRow(
                                    credential = record.toSummary(),
                                    onOpen = { onOpenCredential(record.id) },
                                    showFolderBadge = false,
                                )
                            }
                        }
                    }
                }
            }

            if (report.mediumCredentials.isNotEmpty()) {
                item(key = "medium_header") {
                    Text(
                        text = stringResource(R.string.health_medium_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                    )
                }
                items(report.mediumCredentials, key = { "medium_${it.id}" }) { record ->
                    CompactCredentialRow(
                        credential = record.toSummary(),
                        onOpen = { onOpenCredential(record.id) },
                        showFolderBadge = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tint ?: MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun CredentialRecord.toSummary(): CredentialSummary = CredentialSummary(
    id = id,
    folderId = folderId,
    title = title,
    url = url,
    login = login,
    isFavorite = isFavorite,
    hasTotp = !totpSecret.isNullOrBlank(),
    updatedAt = updatedAt,
)
