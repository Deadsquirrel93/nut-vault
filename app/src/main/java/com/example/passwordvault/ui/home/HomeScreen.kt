package com.example.passwordvault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.domain.model.FolderSummary
import com.example.passwordvault.ui.components.CredentialCard
import com.example.passwordvault.ui.components.DangerConfirmDialog
import com.example.passwordvault.ui.components.EmptyStateCard
import com.example.passwordvault.ui.components.TreeCredentialItem
import com.example.passwordvault.ui.components.TreeSectionCard
import com.example.passwordvault.ui.components.vaultTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenFolder: (Long) -> Unit,
    onOpenCredential: (Long) -> Unit,
    onCreateCredential: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var createMenuExpanded by remember { mutableStateOf(false) }
    var folderDialogState by remember { mutableStateOf<FolderSummary?>(null) }
    var folderDialogName by remember { mutableStateOf("") }
    var deleteFolder by remember { mutableStateOf<FolderSummary?>(null) }
    var expandedFolderIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var standaloneExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF4F8FF), Color(0xFFEAF1FF), Color(0xFFF8FBFF)),
                ),
            ),
    ) {
        Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.open_settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { createMenuExpanded = true },
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_action))
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
                start = 16.dp,
                top = paddingValues.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_entries)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    colors = vaultTextFieldColors(),
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    },
                )
            }

            if (uiState.isSearchActive) {
                item { Text(stringResource(R.string.search_results_title)) }
                if (uiState.searchResults.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(R.string.search_no_results_title),
                            body = stringResource(R.string.search_no_results_body),
                        )
                    }
                } else {
                    items(uiState.searchResults, key = { it.id }) { credential ->
                        CredentialCard(
                            credential = credential,
                            onOpen = { onOpenCredential(credential.id) },
                        )
                    }
                }
                return@LazyColumn
            }

            if (uiState.folders.isEmpty() && uiState.standaloneCredentials.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(R.string.empty_home_title),
                        body = stringResource(R.string.empty_home_body),
                    )
                }
            }

            if (uiState.folders.isNotEmpty()) {
                item { Text(stringResource(R.string.folders_title)) }
                items(uiState.folders, key = { it.id }) { folder ->
                    TreeSectionCard(
                        title = folder.name,
                        subtitle = pluralStringResource(
                            R.plurals.credential_count,
                            folder.credentialCount,
                            folder.credentialCount,
                        ),
                        expanded = expandedFolderIds.contains(folder.id),
                        onToggleExpanded = {
                            expandedFolderIds = if (expandedFolderIds.contains(folder.id)) {
                                expandedFolderIds - folder.id
                            } else {
                                expandedFolderIds + folder.id
                            }
                        },
                        onHeaderClick = { onOpenFolder(folder.id) },
                        actions = {
                            IconButton(onClick = {
                                folderDialogState = folder
                                folderDialogName = folder.name
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.rename_folder_action),
                                )
                            }
                            IconButton(onClick = { deleteFolder = folder }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_folder_action))
                            }
                        },
                    ) {
                        val nestedCredentials = uiState.credentialsByFolderId[folder.id].orEmpty()
                        if (nestedCredentials.isEmpty()) {
                            Text(
                                text = stringResource(R.string.empty_folder_title),
                                color = Color(0xFF5F6F87),
                            )
                        } else {
                            nestedCredentials.forEach { credential ->
                                TreeCredentialItem(
                                    title = credential.title,
                                    subtitle = credential.login.takeIf { it.isNotBlank() } ?: credential.url,
                                    onOpen = { onOpenCredential(credential.id) },
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.standaloneCredentials.isNotEmpty()) {
                item { Text(stringResource(R.string.all_entries_title)) }
                item {
                    TreeSectionCard(
                        title = stringResource(R.string.stored_outside_folders),
                        subtitle = pluralStringResource(
                            R.plurals.credential_count,
                            uiState.standaloneCredentials.size,
                            uiState.standaloneCredentials.size,
                        ),
                        expanded = standaloneExpanded,
                        onToggleExpanded = { standaloneExpanded = !standaloneExpanded },
                    ) {
                        uiState.standaloneCredentials.forEach { credential ->
                            TreeCredentialItem(
                                title = credential.title,
                                subtitle = credential.login.takeIf { it.isNotBlank() } ?: credential.url,
                                onOpen = { onOpenCredential(credential.id) },
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (folderDialogState != null) {
        AlertDialog(
            onDismissRequest = { folderDialogState = null },
            title = { Text(stringResource(if (folderDialogState?.id == 0L) R.string.create_folder else R.string.rename_folder)) },
            text = {
                OutlinedTextField(
                    value = folderDialogName,
                    onValueChange = { folderDialogName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.folder_name)) },
                    singleLine = true,
                    colors = vaultTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveFolder(folderDialogState?.id?.takeIf { it != 0L }, folderDialogName)
                    folderDialogState = null
                }) {
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
                pluralStringResource(R.plurals.delete_folder_with_items_message, folder.credentialCount, folder.credentialCount)
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
