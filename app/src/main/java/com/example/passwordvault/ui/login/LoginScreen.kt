package com.example.passwordvault.ui.login

import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.passwordvault.R
import com.example.passwordvault.app.AppContainer
import com.example.passwordvault.domain.model.AuthSettings
import com.example.passwordvault.ui.components.PasswordTextField
import com.example.passwordvault.ui.components.VaultSectionCard
import com.example.passwordvault.ui.components.findActivity
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    container: AppContainer,
    settings: AuthSettings,
    viewModel: LoginViewModel,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var login by remember(settings.login) { mutableStateOf(settings.login) }
    var password by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var biometricPromptAttempted by rememberSaveable { mutableStateOf(false) }

    var remainingLockSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(settings.lockoutUntilEpochMillis) {
        val until = settings.lockoutUntilEpochMillis ?: 0L
        while (true) {
            val remainingMs = until - System.currentTimeMillis()
            if (remainingMs <= 0L) {
                remainingLockSeconds = 0L
                break
            }
            remainingLockSeconds = (remainingMs + 999L) / 1000L
            delay(500L)
        }
    }

    val canUseBiometric = settings.biometricEnabled &&
        settings.biometricIv != null &&
        settings.biometricCiphertext != null &&
        activity != null &&
        container.biometricCipherProvider.biometricAvailability(context) == BiometricManager.BIOMETRIC_SUCCESS

    fun launchBiometricPrompt() {
        val iv = settings.biometricIv ?: return
        val ciphertext = settings.biometricCiphertext ?: return
        val currentActivity = activity ?: return

        val cipherResult = container.biometricCipherProvider.createDecryptionCipher(iv)
        val decryptCipher = cipherResult.getOrElse {
            viewModel.handleInvalidatedBiometric()
            return
        }

        val prompt = BiometricPrompt(
            currentActivity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val finalCipher = result.cryptoObject?.cipher ?: return
                    val verified = runCatching {
                        val clear = finalCipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
                        clear.contentEquals(container.biometricCipherProvider.verifierPlaintext())
                    }.getOrElse {
                        viewModel.handleInvalidatedBiometric()
                        false
                    }
                    if (verified) {
                        viewModel.completeBiometricUnlock()
                    }
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.unlock_biometric_prompt_title))
                .setSubtitle(context.getString(R.string.unlock_biometric_prompt_subtitle))
                .setNegativeButtonText(context.getString(R.string.cancel))
                .build(),
            BiometricPrompt.CryptoObject(decryptCipher),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric && !biometricPromptAttempted) {
            biometricPromptAttempted = true
            launchBiometricPrompt()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.app_icon_preview),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp)),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.unlock_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            VaultSectionCard(contentPadding = 16.dp) {
                if (remainingLockSeconds > 0L) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.lockout_countdown_format, remainingLockSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.small,
                )
                PasswordTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.master_password),
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (remainingLockSeconds <= 0L) {
                            viewModel.login(login, password)
                        }
                    },
                )
                Button(
                    onClick = { viewModel.login(login, password) },
                    enabled = remainingLockSeconds <= 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                ) {
                    Text(stringResource(R.string.unlock_button))
                }
                if (canUseBiometric) {
                    OutlinedButton(
                        onClick = ::launchBiometricPrompt,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.unlock_biometric_button))
                    }
                }
            }
        }
    }
}
