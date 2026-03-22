package com.example.passwordvault.ui.credential.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.domain.model.PasswordGeneratorOptions
import com.example.passwordvault.ui.components.PasswordStrengthChip
import com.example.passwordvault.ui.components.vaultTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialEditorScreen(
    viewModel: CredentialEditorViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var showGenerator by remember { mutableStateOf(false) }
    var generatorOptions by remember { mutableStateOf(PasswordGeneratorOptions()) }
    var generatedPassword by remember { mutableStateOf("") }
    val generatorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }
    LaunchedEffect(Unit) {
        viewModel.savedCredentialIds.collect { onSaved(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF4F8FF), Color(0xFFEAF1FF), Color(0xFFF8FBFF)))),
    ) {
        Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.credentialId == null) R.string.new_credential else R.string.edit_credential)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    start = 16.dp,
                    top = paddingValues.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.title)) },
                singleLine = true,
                colors = vaultTextFieldColors(),
            )
            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::updateUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.url_optional)) },
                singleLine = true,
                colors = vaultTextFieldColors(),
            )
            OutlinedTextField(
                value = uiState.login,
                onValueChange = viewModel::updateLogin,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.login)) },
                singleLine = true,
                colors = vaultTextFieldColors(),
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.password)) },
                colors = vaultTextFieldColors(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                            )
                        }
                        IconButton(onClick = { showGenerator = true }) {
                            Icon(Icons.Default.Autorenew, contentDescription = stringResource(R.string.open_generator))
                        }
                    }
                },
            )
            uiState.strength?.let { PasswordStrengthChip(it) }

            Box {
                OutlinedButton(onClick = { folderMenuExpanded = true }) {
                    Text(
                        text = uiState.folders.firstOrNull { it.id == uiState.folderId }?.name ?: stringResource(R.string.stored_outside_folders),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = folderMenuExpanded,
                    onDismissRequest = { folderMenuExpanded = false },
                    modifier = Modifier.widthIn(min = 220.dp),
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

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7FB4FF),
                    contentColor = Color(0xFF0E2347),
                ),
            ) {
                Text(stringResource(R.string.save_credential))
            }
        }
        }
    }

    if (showGenerator) {
        ModalBottomSheet(
            onDismissRequest = { showGenerator = false },
            sheetState = generatorSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.password_generator), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.password_length_format, generatorOptions.length))
                Slider(
                    value = generatorOptions.length.toFloat(),
                    onValueChange = { generatorOptions = generatorOptions.copy(length = it.toInt()) },
                    valueRange = 8f..48f,
                )
                SettingToggleRow(stringResource(R.string.uppercase_label), generatorOptions.includeUppercase) {
                    generatorOptions = generatorOptions.copy(includeUppercase = it)
                }
                SettingToggleRow(stringResource(R.string.lowercase_label), generatorOptions.includeLowercase) {
                    generatorOptions = generatorOptions.copy(includeLowercase = it)
                }
                SettingToggleRow(stringResource(R.string.digits_label), generatorOptions.includeDigits) {
                    generatorOptions = generatorOptions.copy(includeDigits = it)
                }
                SettingToggleRow(stringResource(R.string.symbols_label), generatorOptions.includeSymbols) {
                    generatorOptions = generatorOptions.copy(includeSymbols = it)
                }
                Button(
                    onClick = { generatedPassword = viewModel.applyGeneratedPassword(generatorOptions) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (generatedPassword.isBlank()) R.string.generate_password else R.string.regenerate_password))
                }
                if (generatedPassword.isNotBlank()) {
                    Text(generatedPassword, style = MaterialTheme.typography.bodyLarge)
                    PasswordStrengthChip(viewModel.evaluatePassword(generatedPassword))
                    OutlinedButton(
                        onClick = {
                            viewModel.updatePassword(generatedPassword)
                            showGenerator = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.use_password))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
