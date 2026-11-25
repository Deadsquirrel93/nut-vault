package com.example.passwordvault.ui.credential.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.ui.components.PasswordGeneratorSheet
import com.example.passwordvault.ui.components.PasswordStrengthBar
import com.example.passwordvault.ui.components.PasswordTextField
import com.example.passwordvault.ui.components.VaultSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialEditorScreen(
    viewModel: CredentialEditorViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val generatorOptions by viewModel.generatorOptions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val passwordLabel = stringResource(R.string.password)

    var folderMenuExpanded by remember { mutableStateOf(false) }
    var showGenerator by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }
    LaunchedEffect(Unit) {
        viewModel.savedCredentialIds.collect { onSaved(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Text(
                        stringResource(
                            if (uiState.credentialId == null) R.string.new_credential else R.string.edit_credential,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.updateFavorite(!uiState.isFavorite) }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(R.string.favorite_toggle),
                            tint = if (uiState.isFavorite) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 14.dp,
                    top = paddingValues.calculateTopPadding() + 4.dp,
                    end = 14.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                )
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VaultSectionCard(contentPadding = 14.dp) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.title)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.small,
                )
                OutlinedTextField(
                    value = uiState.login,
                    onValueChange = viewModel::updateLogin,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.small,
                )
                PasswordTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = passwordLabel,
                    imeAction = ImeAction.Next,
                    extraTrailingAction = {
                        IconButton(onClick = { showGenerator = true }) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = stringResource(R.string.open_generator),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
                if (uiState.password.isNotEmpty()) {
                    uiState.strength?.let { PasswordStrengthBar(score = it) }
                }
            }

            VaultSectionCard(contentPadding = 14.dp) {
                OutlinedTextField(
                    value = uiState.url,
                    onValueChange = viewModel::updateUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.url_optional)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    shape = MaterialTheme.shapes.small,
                )

                val selectedFolderName = uiState.folders
                    .firstOrNull { it.id == uiState.folderId }
                    ?.name
                    ?: stringResource(R.string.stored_outside_folders)

                ExposedDropdownMenuBox(
                    expanded = folderMenuExpanded,
                    onExpandedChange = { folderMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedFolderName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.folder)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderMenuExpanded)
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = folderMenuExpanded,
                        onDismissRequest = { folderMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stored_outside_folders)) },
                            onClick = {
                                viewModel.updateFolder(null)
                                folderMenuExpanded = false
                            },
                        )
                        uiState.folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    viewModel.updateFolder(folder.id)
                                    folderMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.totpSecret,
                    onValueChange = viewModel::updateTotpSecret,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.totp_secret_optional)) },
                    singleLine = true,
                    isError = !uiState.isTotpValid,
                    supportingText = if (!uiState.isTotpValid) {
                        { Text(stringResource(R.string.totp_invalid_secret)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.small,
                )

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateNotes,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.notes_optional)) },
                    minLines = 2,
                    maxLines = 5,
                    shape = MaterialTheme.shapes.small,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.favorite_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = uiState.isFavorite,
                        onCheckedChange = viewModel::updateFavorite,
                    )
                }
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_credential))
            }
        }
    }

    if (showGenerator) {
        PasswordGeneratorSheet(
            initialOptions = generatorOptions,
            onOptionsChanged = viewModel::saveGeneratorOptions,
            generatePassword = viewModel::applyGeneratedPassword,
            evaluatePassword = viewModel::evaluatePassword,
            onCopy = { viewModel.copyGeneratedPassword(it, passwordLabel) },
            onDismiss = { showGenerator = false },
            onUsePassword = { viewModel.updatePassword(it) },
        )
    }
}
