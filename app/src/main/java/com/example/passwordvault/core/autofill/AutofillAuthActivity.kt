package com.example.passwordvault.core.autofill

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.passwordvault.PasswordVaultApplication
import com.example.passwordvault.R
import com.example.passwordvault.app.AppContainer
import com.example.passwordvault.core.ui.theme.PasswordVaultTheme
import com.example.passwordvault.domain.model.CredentialRecord
import com.example.passwordvault.ui.components.EntryMonogram
import com.example.passwordvault.ui.login.LoginScreen
import com.example.passwordvault.ui.login.LoginViewModel

class AutofillAuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        val usernameId = intent.parcelableExtra<AutofillId>(EXTRA_USERNAME_ID)
        val passwordId = intent.parcelableExtra<AutofillId>(EXTRA_PASSWORD_ID)
        val webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME)

        if (usernameId == null && passwordId == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val app = application as PasswordVaultApplication
        val container = app.container

        setContent {
            val themeMode by container.appPreferences.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by container.appPreferences.dynamicColor.collectAsStateWithLifecycle()
            PasswordVaultTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                AutofillPickerFlow(
                    container = container,
                    webDomain = webDomain,
                    targetPackage = targetPackage,
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onSelect = { credential ->
                        val dataset = NutVaultAutofillService.buildCredentialDataset(
                            packageName = packageName,
                            credential = credential,
                            usernameId = usernameId,
                            passwordId = passwordId,
                        )
                        val reply = Intent().putExtra(
                            AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                            dataset,
                        )
                        setResult(Activity.RESULT_OK, reply)
                        finish()
                    },
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T> Intent.parcelableExtra(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            getParcelableExtra(key) as? T
        }

    companion object {
        const val EXTRA_USERNAME_ID = "extra_username_id"
        const val EXTRA_PASSWORD_ID = "extra_password_id"
        const val EXTRA_WEB_DOMAIN = "extra_web_domain"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutofillPickerFlow(
    container: AppContainer,
    webDomain: String?,
    targetPackage: String?,
    onCancel: () -> Unit,
    onSelect: (CredentialRecord) -> Unit,
) {
    val isUnlocked by container.sessionManager.isUnlocked.collectAsStateWithLifecycle()
    val settings by container.authRepository.observeSettings().collectAsStateWithLifecycle(
        initialValue = com.example.passwordvault.domain.model.AuthSettings(),
    )

    if (settings.protectionEnabled && !isUnlocked) {
        val loginViewModel: LoginViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    LoginViewModel(
                        verifyLoginUseCase = container.verifyLoginUseCase,
                        sessionManager = container.sessionManager,
                        authRepository = container.authRepository,
                        biometricCipherProvider = container.biometricCipherProvider,
                    )
                }
            },
        )
        LoginScreen(
            container = container,
            settings = settings,
            viewModel = loginViewModel,
        )
        return
    }

    var credentials by remember { mutableStateOf<List<CredentialRecord>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val all = container.vaultRepository.getAllCredentialRecords()
        credentials = AutofillStructureParser.rankCredentials(all, webDomain, targetPackage)
    }

    val filtered = remember(credentials, query) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            credentials
        } else {
            credentials.filter {
                it.title.contains(normalized, ignoreCase = true) ||
                    it.login.contains(normalized, ignoreCase = true) ||
                    it.url.orEmpty().contains(normalized, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.autofill_picker_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val hint = webDomain ?: targetPackage
                        if (!hint.isNullOrBlank()) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_entries)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { item ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            EntryMonogram(title = item.title, size = 38.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (item.isFavorite) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(start = 2.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = item.login,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
