package com.example.passwordvault.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.core.preferences.VaultSortOption
import com.example.passwordvault.domain.model.FolderSummary
import com.example.passwordvault.ui.components.CompactCredentialRow
import com.example.passwordvault.ui.components.CompactFolderRow
import com.example.passwordvault.ui.components.DangerConfirmDialog
import com.example.passwordvault.ui.components.EmptyStateCard
import com.example.passwordvault.ui.components.PasswordGeneratorSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenFolder: (Long) -> Unit,
    onOpenCredential: (Long) -> Unit,
    onCreateCredential: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val generatorOptions by viewModel.generatorOptions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var createMenuExpanded by remember { mutableStateOf(false) }
    var showGeneratorSheet by remember { mutableStateOf(false) }
    var folderDialogState by remember { mutableStateOf<FolderSummary?>(null) }
    var folderDialogName by remember { mutableStateOf("") }
    var deleteFolder by remember { mutableStateOf<FolderSummary?>(null) }

    val loginLabel = stringResource(R.string.login)
    val passwordLabel = stringResource(R.string.password)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (!uiState.isLoading) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.credential_count,
                                    uiState.totalCredentialCount,
                                    uiState.totalCredentialCount,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showGeneratorSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Password,
                            contentDescription = stringResource(R.string.password_generator),
                        )
                    }
                    IconButton(onClick = onOpenHealth) {
                        BadgedBox(
                            badge = {
                                if (uiState.healthIssueCount > 0) {
                                    Badge { Text(uiState.healthIssueCount.coerceAtMost(99).toString()) }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = stringResource(R.string.health_title),
                            )
                        }
                    }
                    IconButton(onClick = viewModel::lockVault) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.lock_app_now),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.open_settings),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { createMenuExpanded = true },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_action))
                }
                DropdownMenu(
                    expanded = createMenuExpanded,
                    onDismissRequest = { createMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_credential)) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        onClick = {
                            createMenuExpanded = false
                            onCreateCredential()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_folder)) },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            createMenuExpanded = false
                            folderDialogState = FolderSummary(0L, "", 0L, 0L, 0)
                            folderDialogName = ""
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                top = paddingValues.calculateTopPadding() + 4.dp,
                end = 14.dp,
                bottom = paddingValues.calculateBottomPadding() + 84.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "search_bar") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_entries),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear_search),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                )
            }

            item(key = "filter_chips") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        FilterChip(
                            selected = uiState.activeFilter == HomeFilter.ALL,
                            onClick = { viewModel.selectFilter(HomeFilter.ALL) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.activeFilter == HomeFilter.FAVORITES,
                            onClick = { viewModel.selectFilter(HomeFilter.FAVORITES) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = {
                                val text = if (uiState.favoriteCount > 0) {
                                    "${stringResource(R.string.filter_favorites)} (${uiState.favoriteCount})"
                                } else {
                                    stringResource(R.string.filter_favorites)
                                }
                                Text(text)
                            },
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.activeFilter == HomeFilter.TOTP,
                            onClick = { viewModel.selectFilter(HomeFilter.TOTP) },
                            label = {
                                val text = if (uiState.totpCount > 0) {
                                    "${stringResource(R.string.filter_totp)} (${uiState.totpCount})"
                                } else {
                                    stringResource(R.string.filter_totp)
                                }
                                Text(text)
                            },
                        )
                    }
                    item {
                        val sortLabel = when (uiState.sortOption) {
                            VaultSortOption.UPDATED_DESC -> R.string.sort_recent
                            VaultSortOption.TITLE_ASC -> R.string.sort_title
                            VaultSortOption.FAVORITES_FIRST -> R.string.sort_favorites
                        }
                        FilterChip(
                            selected = false,
                            onClick = viewModel::cycleSortOption,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = { Text(stringResource(sortLabel)) },
                        )
                    }
                }
            }

            if (!uiState.isSearchActive && uiState.activeFilter == HomeFilter.ALL && uiState.folders.isNotEmpty()) {
                item(key = "folders_header") {
                    Text(
                        text = stringResource(R.string.folders_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                    )
                }
                items(uiState.folders, key = { "folder_${it.id}" }) { folder ->
                    CompactFolderRow(
                        folder = folder,
                        onOpen = { onOpenFolder(folder.id) },
                        onRename = {
                            folderDialogState = folder
                            folderDialogName = folder.name
                        },
                        onDelete = { deleteFolder = folder },
                    )
                }
            }

            if (uiState.visibleCredentials.isNotEmpty()) {
                item(key = "entries_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, start = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (uiState.isSearchActive) {
                                stringResource(R.string.search_results_title)
                            } else {
                                stringResource(R.string.all_entries_title)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = uiState.visibleCredentials.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(uiState.visibleCredentials, key = { "cred_${it.id}" }) { credential ->
                    CompactCredentialRow(
                        credential = credential,
                        onOpen = { onOpenCredential(credential.id) },
                        onCopyLogin = { viewModel.copyLogin(credential.id, loginLabel) },
                        onCopyPassword = { viewModel.copyPassword(credential.id, passwordLabel) },
                    )
                }
            } else if (!uiState.isLoading) {
                item(key = "empty_state") {
                    val (titleRes, bodyRes) = when {
                        uiState.isSearchActive ->
                            R.string.search_no_results_title to R.string.search_no_results_body
                        uiState.activeFilter == HomeFilter.FAVORITES ->
                            R.string.empty_favorites_title to R.string.empty_favorites_body
                        uiState.activeFilter == HomeFilter.TOTP ->
                            R.string.empty_totp_title to R.string.empty_totp_body
                        uiState.folders.isEmpty() ->
                            R.string.empty_home_title to R.string.empty_home_body
                        else -> null to null
                    }
                    if (titleRes != null && bodyRes != null) {
                        EmptyStateCard(
                            title = stringResource(titleRes),
                            body = stringResource(bodyRes),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }

    if (showGeneratorSheet) {
        PasswordGeneratorSheet(
            initialOptions = generatorOptions,
            onOptionsChanged = viewModel::saveGeneratorOptions,
            generatePassword = viewModel::generatePassword,
            evaluatePassword = viewModel::evaluatePassword,
            onCopy = { generated ->
                viewModel.copyGeneratedPassword(generated, passwordLabel)
                showGeneratorSheet = false
            },
            onDismiss = { showGeneratorSheet = false },
        )
    }

    if (folderDialogState != null) {
        AlertDialog(
            onDismissRequest = { folderDialogState = null },
            title = {
                Text(
                    stringResource(
                        if (folderDialogState?.id == 0L) R.string.create_folder else R.string.rename_folder,
                    ),
                )
            },
            text = {
                OutlinedTextField(
                    value = folderDialogName,
                    onValueChange = { folderDialogName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.folder_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveFolder(folderDialogState?.id?.takeIf { it != 0L }, folderDialogName)
                        folderDialogState = null
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { folderDialogState = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    deleteFolder?.let { folder ->
        DangerConfirmDialog(
            title = stringResource(R.string.delete_folder_title),
            message = if (folder.credentialCount > 0) {
                pluralStringResource(
                    R.plurals.delete_folder_with_items_message,
                    folder.credentialCount,
                    folder.credentialCount,
                )
            } else {
                stringResource(R.string.delete_folder_empty_message)
            },
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteFolder(folder.id)
                deleteFolder = null
            },
            onDismiss = { deleteFolder = null },
        )
    }
}
