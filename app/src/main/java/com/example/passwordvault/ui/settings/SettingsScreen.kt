package com.example.passwordvault.ui.settings

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.app.AppContainer
import com.example.passwordvault.core.localization.LocaleManager
import com.example.passwordvault.ui.components.DangerConfirmDialog
import com.example.passwordvault.ui.components.findActivity
import com.example.passwordvault.ui.components.vaultTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val languageCode by viewModel.languageCode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    var login by remember { mutableStateOf("") }
    var masterPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentMasterPassword by remember { mutableStateOf("") }
    var newMasterPassword by remember { mutableStateOf("") }
    var confirmNewMasterPassword by remember { mutableStateOf("") }
    var exportPassphrase by remember { mutableStateOf("") }
    var importPassphrase by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteCredentials by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val passphrase = pendingExportPassphrase
        if (uri != null && passphrase != null) {
            viewModel.exportVault(uri, passphrase)
        }
        pendingExportPassphrase = null
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF4F8FF), Color(0xFFEAF1FF), Color(0xFFF8FBFF)))),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsCard(icon = { Icon(Icons.Default.Security, contentDescription = null) }) {
                    Text(stringResource(R.string.protection_title), style = MaterialTheme.typography.titleMedium)
                    if (!settings.protectionEnabled) {
                        Text(stringResource(R.string.protection_disabled_body))
                        OutlinedTextField(
                            value = login,
                            onValueChange = { login = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.login)) },
                            singleLine = true,
                            colors = vaultTextFieldColors(),
                        )
                        OutlinedTextField(
                            value = masterPassword,
                            onValueChange = { masterPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.master_password)) },
                            colors = vaultTextFieldColors(),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.confirm_master_password)) },
                            colors = vaultTextFieldColors(),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        Button(
                            onClick = { viewModel.enableProtection(login, masterPassword, confirmPassword) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.enable_protection))
                        }
                    } else {
                        Text(stringResource(R.string.protection_enabled_body))
                        Text("${stringResource(R.string.login)}: ${settings.login}")
                        RowToggle(
                            label = stringResource(R.string.unlock_with_biometric),
                            checked = settings.biometricEnabled,
                            onCheckedChange = { enabled: Boolean ->
                                if (!enabled) {
                                    viewModel.disableBiometric()
                                } else if (activity != null) {
                                    val cipher = container.biometricCipherProvider.createEncryptionCipher()
                                    val prompt = BiometricPrompt(
                                        activity,
                                        ContextCompat.getMainExecutor(context),
                                        object : BiometricPrompt.AuthenticationCallback() {
                                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                val finalCipher = result.cryptoObject?.cipher ?: return
                                                val ciphertext = Base64.encodeToString(
                                                    finalCipher.doFinal(container.biometricCipherProvider.verifierPlaintext()),
                                                    Base64.NO_WRAP,
                                                )
                                                val iv = Base64.encodeToString(finalCipher.iv, Base64.NO_WRAP)
                                                viewModel.enableBiometric(iv, ciphertext)
                                            }
                                        },
                                    )
                                    prompt.authenticate(
                                        BiometricPrompt.PromptInfo.Builder()
                                            .setTitle(context.getString(R.string.enable_biometric_prompt_title))
                                            .setSubtitle(context.getString(R.string.enable_biometric_prompt_subtitle))
                                            .setNegativeButtonText(context.getString(R.string.cancel))
                                            .build(),
                                        BiometricPrompt.CryptoObject(cipher),
                                    )
                                }
                            },
                        )
                        Text(
                            stringResource(R.string.change_master_password),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        OutlinedTextField(
                            value = currentMasterPassword,
                            onValueChange = { currentMasterPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.current_master_password)) },
                            colors = vaultTextFieldColors(),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        OutlinedTextField(
                            value = newMasterPassword,
                            onValueChange = { newMasterPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.new_master_password)) },
                            colors = vaultTextFieldColors(),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        OutlinedTextField(
                            value = confirmNewMasterPassword,
                            onValueChange = { confirmNewMasterPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.confirm_new_master_password)) },
                            colors = vaultTextFieldColors(),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        Button(
                            onClick = {
                                viewModel.changeMasterPassword(
                                    currentPassword = currentMasterPassword,
                                    newPassword = newMasterPassword,
                                    confirmNewPassword = confirmNewMasterPassword,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.change_password_button))
                        }
                    }
                }

                SettingsCard(icon = { Icon(Icons.Default.Language, contentDescription = null) }) {
                    Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.language_body))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = languageCode == LocaleManager.RUSSIAN,
                            onClick = { viewModel.setLanguage(LocaleManager.RUSSIAN) },
                            label = { Text(stringResource(R.string.language_russian)) },
                        )
                        FilterChip(
                            selected = languageCode == LocaleManager.ENGLISH,
                            onClick = { viewModel.setLanguage(LocaleManager.ENGLISH) },
                            label = { Text(stringResource(R.string.language_english)) },
                        )
                    }
                }

                SettingsCard(icon = { Icon(Icons.Default.FileDownload, contentDescription = null) }) {
                    Text(stringResource(R.string.transfer_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.transfer_body))
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Text(stringResource(R.string.export_encrypted_vault), modifier = Modifier.padding(start = 8.dp))
                    }
                    Button(
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Text(stringResource(R.string.import_encrypted_vault), modifier = Modifier.padding(start = 8.dp))
                    }
                }

                SettingsCard(icon = { Icon(Icons.Default.Warning, contentDescription = null) }) {
                    Text(stringResource(R.string.danger_zone_title), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showDeleteCredentials = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Text(stringResource(R.string.delete_all_credentials), modifier = Modifier.padding(start = 8.dp))
                    }
                    Button(onClick = { showDeleteAll = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Text(stringResource(R.string.delete_all_credentials_and_folders), modifier = Modifier.padding(start = 8.dp))
                    }
                    if (settings.protectionEnabled) {
                        Button(onClick = viewModel::lockApp, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Text(stringResource(R.string.lock_app_now), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_encrypted_vault)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.export_dialog_body))
                    OutlinedTextField(
                        value = exportPassphrase,
                        onValueChange = { exportPassphrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.export_passphrase)) },
                        colors = vaultTextFieldColors(),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingExportPassphrase = exportPassphrase
                    showExportDialog = false
                    createDocumentLauncher.launch("password-vault-${System.currentTimeMillis()}.vault")
                }) { Text(stringResource(R.string.choose_file)) }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showImportDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.import_encrypted_vault)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.import_dialog_body))
                    OutlinedTextField(
                        value = importPassphrase,
                        onValueChange = { importPassphrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.import_passphrase)) },
                        colors = vaultTextFieldColors(),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importVault(requireNotNull(pendingImportUri), importPassphrase)
                    pendingImportUri = null
                    showImportDialog = false
                }) { Text(stringResource(R.string.import_and_replace)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    showImportDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showDeleteCredentials) {
        DangerConfirmDialog(
            title = stringResource(R.string.delete_all_credentials),
            message = stringResource(R.string.delete_all_credentials_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteAllCredentials()
                showDeleteCredentials = false
            },
            onDismiss = { showDeleteCredentials = false },
        )
    }

    if (showDeleteAll) {
        DangerConfirmDialog(
            title = stringResource(R.string.delete_all_credentials_and_folders),
            message = stringResource(R.string.delete_all_data_message),
            confirmLabel = stringResource(R.string.delete_everything),
            onConfirm = {
                viewModel.deleteAllData()
                showDeleteAll = false
            },
            onDismiss = { showDeleteAll = false },
        )
    }
}

@Composable
private fun SettingsCard(
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                icon()
            }
            content()
        }
    }
}

@Composable
private fun RowToggle(
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
