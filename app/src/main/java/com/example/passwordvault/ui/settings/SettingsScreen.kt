package com.example.passwordvault.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.BuildConfig
import com.example.passwordvault.R
import com.example.passwordvault.app.AppContainer
import com.example.passwordvault.core.localization.LocaleManager
import com.example.passwordvault.core.preferences.AutoLockTimeout
import com.example.passwordvault.core.preferences.ThemeMode
import com.example.passwordvault.domain.model.ImportMode
import com.example.passwordvault.ui.components.DangerConfirmDialog
import com.example.passwordvault.ui.components.PasswordTextField
import com.example.passwordvault.ui.components.VaultSectionCard
import com.example.passwordvault.ui.components.findActivity

private const val GITHUB_RELEASES_URL =
    "https://github.com/Deadsquirrel93/nut-vault/releases/latest"
private const val OBTAINIUM_ADD_URL =
    "obtainium://add/https://github.com/Deadsquirrel93/nut-vault"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val languageCode by viewModel.languageCode.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val autoLockTimeout by viewModel.autoLockTimeout.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    val autofillManager = remember(context) { context.getSystemService(AutofillManager::class.java) }
    var isAutofillEnabled by remember {
        mutableStateOf(autofillManager?.hasEnabledAutofillServices() == true)
    }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentMasterPassword by remember { mutableStateOf("") }
    var newMasterPassword by remember { mutableStateOf("") }
    var confirmNewMasterPassword by remember { mutableStateOf("") }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf("") }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importPassphrase by remember { mutableStateOf("") }
    var importMode by remember { mutableStateOf(ImportMode.MERGE) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    var showDeleteCredentials by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshLocale()
        isAutofillEnabled = autofillManager?.hasEnabledAutofillServices() == true
        onPauseOrDispose { }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val passphrase = pendingExportPassphrase
        if (uri != null && passphrase != null) {
            viewModel.exportVault(uri, passphrase)
        }
        pendingExportPassphrase = null
        exportPassphrase = ""
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            importPassphrase = ""
            importMode = ImportMode.MERGE
            showImportDialog = true
        }
    }

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
                    start = 14.dp,
                    top = paddingValues.calculateTopPadding() + 4.dp,
                    end = 14.dp,
                    bottom = paddingValues.calculateBottomPadding() + 20.dp,
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Security section
            VaultSectionCard {
                SectionHeader(Icons.Default.Security, stringResource(R.string.protection_title))
                if (settings.protectionEnabled) {
                    Text(
                        text = stringResource(R.string.protection_enabled_body, settings.login),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.unlock_with_biometric),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = settings.biometricEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    viewModel.disableBiometric()
                                } else {
                                    val available = container.biometricCipherProvider.biometricAvailability(context)
                                    if (activity == null || available != BiometricManager.BIOMETRIC_SUCCESS) {
                                        viewModel.reportBiometricUnavailable()
                                    } else {
                                        val cipher = container.biometricCipherProvider.createEncryptionCipher()
                                            .getOrElse {
                                                viewModel.reportBiometricEnableFailed()
                                                return@Switch
                                            }
                                        val prompt = BiometricPrompt(
                                            activity,
                                            ContextCompat.getMainExecutor(context),
                                            object : BiometricPrompt.AuthenticationCallback() {
                                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                    val finalCipher = result.cryptoObject?.cipher ?: return
                                                    runCatching {
                                                        val ciphertext = Base64.encodeToString(
                                                            finalCipher.doFinal(
                                                                container.biometricCipherProvider.verifierPlaintext(),
                                                            ),
                                                            Base64.NO_WRAP,
                                                        )
                                                        val iv = Base64.encodeToString(finalCipher.iv, Base64.NO_WRAP)
                                                        viewModel.enableBiometric(iv, ciphertext)
                                                    }.onFailure {
                                                        viewModel.reportBiometricEnableFailed()
                                                    }
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
                                }
                            },
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = stringResource(R.string.autolock_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val timeouts = listOf(
                            AutoLockTimeout.IMMEDIATE to R.string.autolock_immediate,
                            AutoLockTimeout.SECONDS_30 to R.string.autolock_30s,
                            AutoLockTimeout.MINUTE_1 to R.string.autolock_1m,
                            AutoLockTimeout.MINUTES_5 to R.string.autolock_5m,
                        )
                        timeouts.forEach { (option, labelRes) ->
                            FilterChip(
                                selected = autoLockTimeout == option,
                                onClick = { viewModel.setAutoLockTimeout(option) },
                                label = { Text(stringResource(labelRes)) },
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            currentMasterPassword = ""
                            newMasterPassword = ""
                            confirmNewMasterPassword = ""
                            showChangePasswordDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.change_master_password))
                    }
                }
            }

            // Android Autofill section
            VaultSectionCard {
                SectionHeader(Icons.Default.Password, stringResource(R.string.autofill_title))
                Text(
                    text = stringResource(
                        if (isAutofillEnabled) R.string.autofill_enabled_body else R.string.autofill_disabled_body,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAutofillEnabled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                val autofillUnavailable = stringResource(R.string.autofill_unavailable)
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, autofillUnavailable, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.autofill_configure))
                }
            }

            // Appearance & Language section
            VaultSectionCard {
                SectionHeader(Icons.Default.Palette, stringResource(R.string.appearance_title))
                Text(
                    text = stringResource(R.string.theme_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text(stringResource(R.string.theme_system)) },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text(stringResource(R.string.theme_light)) },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        label = { Text(stringResource(R.string.theme_dark)) },
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dynamic_color_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(R.string.dynamic_color_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = stringResource(R.string.language_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = languageCode == null,
                        onClick = { viewModel.setLanguage(null) },
                        label = { Text(stringResource(R.string.language_system)) },
                    )
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

            // Backup & Transfer section
            VaultSectionCard {
                SectionHeader(Icons.Default.FileDownload, stringResource(R.string.transfer_title))
                Text(
                    text = stringResource(R.string.transfer_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            exportPassphrase = ""
                            showExportDialog = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.export_encrypted_vault))
                    }
                    OutlinedButton(
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.import_encrypted_vault))
                    }
                }
            }

            // Updates section (offline, GitHub Releases + Obtainium — matches Squirrel Detective)
            VaultSectionCard {
                SectionHeader(Icons.Default.SystemUpdate, stringResource(R.string.updates_title))
                Text(
                    text = stringResource(R.string.updates_current_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.updates_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val openUrlFailed = stringResource(R.string.updates_open_url_failed)
                val obtainiumMissing = stringResource(R.string.updates_obtainium_missing)
                OutlinedButton(
                    onClick = { openExternalUrl(context, GITHUB_RELEASES_URL, openUrlFailed) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.updates_open_github))
                }
                OutlinedButton(
                    onClick = { openExternalUrl(context, OBTAINIUM_ADD_URL, obtainiumMissing) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.updates_add_obtainium))
                }
            }

            // About section
            VaultSectionCard {
                SectionHeader(Icons.Default.Info, stringResource(R.string.opensource_title))
                Text(
                    text = stringResource(R.string.opensource_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Danger Zone section
            VaultSectionCard {
                SectionHeader(
                    icon = Icons.Default.Warning,
                    title = stringResource(R.string.danger_zone_title),
                    tint = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(
                    onClick = { showDeleteCredentials = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete_all_credentials))
                }
                Button(
                    onClick = { showDeleteAll = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.delete_all_credentials_and_folders))
                }
            }
        }
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePasswordDialog = false
                currentMasterPassword = ""
                newMasterPassword = ""
                confirmNewMasterPassword = ""
            },
            title = { Text(stringResource(R.string.change_master_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PasswordTextField(
                        value = currentMasterPassword,
                        onValueChange = { currentMasterPassword = it },
                        label = stringResource(R.string.current_master_password),
                        imeAction = ImeAction.Next,
                    )
                    PasswordTextField(
                        value = newMasterPassword,
                        onValueChange = { newMasterPassword = it },
                        label = stringResource(R.string.new_master_password),
                        imeAction = ImeAction.Next,
                    )
                    PasswordTextField(
                        value = confirmNewMasterPassword,
                        onValueChange = { confirmNewMasterPassword = it },
                        label = stringResource(R.string.confirm_new_master_password),
                        imeAction = ImeAction.Done,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.changeMasterPassword(
                            currentPassword = currentMasterPassword,
                            newPassword = newMasterPassword,
                            confirmNewPassword = confirmNewMasterPassword,
                            onSuccess = {
                                showChangePasswordDialog = false
                                currentMasterPassword = ""
                                newMasterPassword = ""
                                confirmNewMasterPassword = ""
                            },
                        )
                    },
                ) { Text(stringResource(R.string.change_password_button)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangePasswordDialog = false
                        currentMasterPassword = ""
                        newMasterPassword = ""
                        confirmNewMasterPassword = ""
                    },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportPassphrase = ""
            },
            title = { Text(stringResource(R.string.export_encrypted_vault)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.export_dialog_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    PasswordTextField(
                        value = exportPassphrase,
                        onValueChange = { exportPassphrase = it },
                        label = stringResource(R.string.export_passphrase),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingExportPassphrase = exportPassphrase
                        showExportDialog = false
                        createDocumentLauncher.launch("nutvault-${System.currentTimeMillis()}.vault")
                    },
                ) { Text(stringResource(R.string.choose_file)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        exportPassphrase = ""
                    },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showImportDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportUri = null
                importPassphrase = ""
            },
            title = { Text(stringResource(R.string.import_encrypted_vault)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.import_dialog_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = importMode == ImportMode.MERGE,
                            onClick = { importMode = ImportMode.MERGE },
                            label = { Text(stringResource(R.string.import_mode_merge)) },
                        )
                        FilterChip(
                            selected = importMode == ImportMode.REPLACE,
                            onClick = { importMode = ImportMode.REPLACE },
                            label = { Text(stringResource(R.string.import_mode_replace)) },
                        )
                    }
                    if (importMode == ImportMode.REPLACE) {
                        Text(
                            text = stringResource(R.string.import_replace_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    PasswordTextField(
                        value = importPassphrase,
                        onValueChange = { importPassphrase = it },
                        label = stringResource(R.string.import_passphrase),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importVault(requireNotNull(pendingImportUri), importPassphrase, importMode)
                        pendingImportUri = null
                        importPassphrase = ""
                        showImportDialog = false
                    },
                ) {
                    Text(
                        stringResource(
                            if (importMode == ImportMode.MERGE) {
                                R.string.import_and_merge
                            } else {
                                R.string.import_and_replace
                            },
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        importPassphrase = ""
                        showImportDialog = false
                    },
                ) { Text(stringResource(R.string.cancel)) }
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
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun openExternalUrl(context: Context, url: String, errorMessage: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}
