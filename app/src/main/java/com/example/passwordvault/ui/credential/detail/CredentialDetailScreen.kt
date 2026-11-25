package com.example.passwordvault.ui.credential.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.ui.components.DangerConfirmDialog
import com.example.passwordvault.ui.components.EntryMonogram
import com.example.passwordvault.ui.components.PasswordStrengthBar
import com.example.passwordvault.ui.components.TotpLiveCard
import com.example.passwordvault.ui.components.VaultSectionCard
import com.example.passwordvault.ui.components.colorizedPassword
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailScreen(
    viewModel: CredentialDetailViewModel,
    onBack: () -> Unit,
    onEdit: (credentialId: Long, folderId: Long?) -> Unit,
    onOpenFolder: (folderId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val titleLabel = stringResource(R.string.title)
    val urlLabel = stringResource(R.string.url)
    val loginLabel = stringResource(R.string.login)
    val passwordLabel = stringResource(R.string.password)
    val notesLabel = stringResource(R.string.notes)
    val totpLabel = stringResource(R.string.totp_label)

    var passwordVisible by remember { mutableStateOf(false) }
    var showLargePassword by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    val credential = uiState.credential ?: return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Text(
                        text = credential.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (credential.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(R.string.favorite_toggle),
                            tint = if (credential.isFavorite) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(onClick = { onEdit(credential.id, credential.folderId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_credential_action))
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_credential_action))
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VaultSectionCard(contentPadding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EntryMonogram(title = credential.title, size = 46.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = credential.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val folderId = credential.folderId
                        val folderName = uiState.folderName
                        if (folderId != null && !folderName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable { onOpenFolder(folderId) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = folderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.copyField(titleLabel, credential.title, false, R.string.title_copied)
                        },
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy_value),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }

            if (!credential.totpSecret.isNullOrBlank()) {
                TotpLiveCard(
                    totpSecret = credential.totpSecret,
                    onCopy = { code ->
                        viewModel.copyField(totpLabel, code, true, R.string.totp_copied)
                    },
                )
            }

            VaultSectionCard(contentPadding = 14.dp) {
                CompactDetailField(
                    label = loginLabel,
                    value = credential.login,
                    onCopy = {
                        viewModel.copyField(loginLabel, credential.login, false, R.string.login_copied)
                    },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = passwordLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (passwordVisible) {
                            Text(
                                text = colorizedPassword(credential.password),
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Text(
                                text = "••••••••••••",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = stringResource(
                                        if (passwordVisible) R.string.hide_password else R.string.show_password,
                                    ),
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            IconButton(
                                onClick = { showLargePassword = true },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = stringResource(R.string.large_password_view),
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.copyField(passwordLabel, credential.password, true, R.string.password_copied)
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.copy_value),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                    uiState.passwordStrength?.let { PasswordStrengthBar(score = it) }
                }

                if (!credential.url.isNullOrBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    val rawUrl = credential.url
                    CompactDetailField(
                        label = urlLabel,
                        value = rawUrl,
                        valueColor = MaterialTheme.colorScheme.primary,
                        extraAction = {
                            IconButton(
                                onClick = {
                                    val target = if ("://" in rawUrl) rawUrl else "https://$rawUrl"
                                    val intent = Intent(Intent.ACTION_VIEW, target.toUri())
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.open_url_failed))
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = stringResource(R.string.open_url),
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        },
                        onCopy = {
                            viewModel.copyField(urlLabel, rawUrl, false, R.string.url_copied)
                        },
                    )
                }
            }

            if (!credential.notes.isNullOrBlank()) {
                VaultSectionCard(contentPadding = 14.dp) {
                    CompactDetailField(
                        label = notesLabel,
                        value = credential.notes,
                        onCopy = {
                            viewModel.copyField(notesLabel, credential.notes, false, R.string.notes_copied)
                        },
                    )
                }
            }

            val dateFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.updated_at_format, dateFormatter.format(Date(credential.updatedAt))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.created_at_format, dateFormatter.format(Date(credential.createdAt))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showLargePassword) {
        AlertDialog(
            onDismissRequest = { showLargePassword = false },
            title = { Text(credential.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = colorizedPassword(credential.password),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            letterSpacing = 1.5.sp,
                        ),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.password_length_format, credential.password.length),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLargePassword = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (showDelete) {
        DangerConfirmDialog(
            title = stringResource(R.string.delete_credential_title),
            message = stringResource(R.string.delete_credential_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteCredential(credential.id, onBack)
                showDelete = false
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun CompactDetailField(
    label: String,
    value: String,
    onCopy: () -> Unit,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    extraAction: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value.ifBlank { stringResource(R.string.field_not_set) },
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                extraAction?.invoke()
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_value),
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}
