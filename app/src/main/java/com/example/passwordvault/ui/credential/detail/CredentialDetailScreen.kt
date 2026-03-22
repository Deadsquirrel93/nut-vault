package com.example.passwordvault.ui.credential.detail

import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.ui.components.DangerConfirmDialog
import com.example.passwordvault.ui.components.DetailFieldRow
import com.example.passwordvault.ui.components.copyToClipboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailScreen(
    viewModel: CredentialDetailViewModel,
    onBack: () -> Unit,
    onEdit: (credentialId: Long, folderId: Long?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(ClipboardManager::class.java)
    }
    val titleLabel = stringResource(R.string.title)
    val urlLabel = stringResource(R.string.url)
    val loginLabel = stringResource(R.string.login)
    val passwordLabel = stringResource(R.string.password)
    var passwordVisible by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    val credential = uiState.credential ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF4F8FF), Color(0xFFEAF1FF), Color(0xFFF8FBFF)))),
    ) {
        Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(credential.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
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
                    start = 16.dp,
                    top = paddingValues.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailFieldRow(
                label = titleLabel,
                value = credential.title,
                onCopy = {
                    copyToClipboard(clipboardManager, titleLabel, credential.title, coroutineScope)
                    coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.title_copied)) }
                },
            )
            DetailFieldRow(
                label = urlLabel,
                value = credential.url.orEmpty(),
                onCopy = {
                    copyToClipboard(clipboardManager, urlLabel, credential.url.orEmpty(), coroutineScope)
                    coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.url_copied)) }
                },
            )
            DetailFieldRow(
                label = loginLabel,
                value = credential.login,
                onCopy = {
                    copyToClipboard(clipboardManager, loginLabel, credential.login, coroutineScope)
                    coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.login_copied)) }
                },
            )
            DetailFieldRow(
                label = passwordLabel,
                value = credential.password,
                hidden = !passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                onCopy = {
                    copyToClipboard(
                        clipboardManager = clipboardManager,
                        label = passwordLabel,
                        value = credential.password,
                        coroutineScope = coroutineScope,
                        clearAfterMillis = 30_000L,
                    )
                    coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.password_copied)) }
                },
            )
        }
        }
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
