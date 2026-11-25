package com.example.passwordvault.ui.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.ui.components.CompactCredentialRow
import com.example.passwordvault.ui.components.DangerConfirmDialog
import com.example.passwordvault.ui.components.EmptyStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    onBack: () -> Unit,
    onOpenCredential: (Long) -> Unit,
    onCreateCredential: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showRename by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }

    val loginLabel = stringResource(R.string.login)
    val passwordLabel = stringResource(R.string.password)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    val folder = uiState.folder

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
                            text = folder?.name ?: stringResource(R.string.folder),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        folder?.let {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.credential_count,
                                    it.credentialCount,
                                    it.credentialCount,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (folder != null) {
                        IconButton(
                            onClick = {
                                folderName = folder.name
                                showRename = true
                            },
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename_folder_action))
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_folder_action))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateCredential,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_credential))
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
            item(key = "folder_search") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_in_folder),
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

            if (uiState.filteredCredentials.isEmpty() && !uiState.isLoading) {
                item(key = "folder_empty") {
                    EmptyStateCard(
                        title = if (uiState.isSearchActive) {
                            stringResource(R.string.search_no_results_title)
                        } else {
                            stringResource(R.string.empty_folder_title)
                        },
                        body = if (uiState.isSearchActive) {
                            stringResource(R.string.search_no_results_body)
                        } else {
                            stringResource(R.string.empty_folder_body)
                        },
                    )
                }
            }

            items(uiState.filteredCredentials, key = { it.id }) { credential ->
                CompactCredentialRow(
                    credential = credential,
                    onOpen = { onOpenCredential(credential.id) },
                    onCopyLogin = { viewModel.copyLogin(credential.id, loginLabel) },
                    onCopyPassword = { viewModel.copyPassword(credential.id, passwordLabel) },
                    showFolderBadge = false,
                )
            }
        }
    }

    if (showRename && folder != null) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(stringResource(R.string.rename_folder)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.folder_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameFolder(folder.id, folderName)
                        showRename = false
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showDelete && folder != null) {
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
                viewModel.deleteFolder(folder.id, onBack)
                showDelete = false
            },
            onDismiss = { showDelete = false },
        )
    }
}
