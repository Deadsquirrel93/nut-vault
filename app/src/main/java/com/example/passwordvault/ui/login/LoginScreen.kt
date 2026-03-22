package com.example.passwordvault.ui.login

import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.passwordvault.R
import com.example.passwordvault.app.AppContainer
import com.example.passwordvault.domain.model.AuthSettings
import com.example.passwordvault.ui.components.findActivity
import com.example.passwordvault.ui.components.vaultTextFieldColors

@Composable
fun LoginScreen(
    container: AppContainer,
    settings: AuthSettings,
    viewModel: LoginViewModel,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var login by remember { mutableStateOf(settings.login) }
    var password by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var biometricPromptAttempted by rememberSaveable { mutableStateOf(false) }

    val canUseBiometric = settings.biometricEnabled &&
        settings.biometricIv != null &&
        settings.biometricCiphertext != null &&
        activity != null &&
        container.biometricCipherProvider.biometricAvailability(context) == BiometricManager.BIOMETRIC_SUCCESS

    fun launchBiometricPrompt() {
        val decryptCipher = container.biometricCipherProvider.createDecryptionCipher(requireNotNull(settings.biometricIv))
        val prompt = BiometricPrompt(
            requireNotNull(activity),
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val finalCipher = result.cryptoObject?.cipher ?: return
                    val clear = finalCipher.doFinal(
                        Base64.decode(settings.biometricCiphertext, Base64.NO_WRAP),
                    )
                    if (clear.contentEquals(container.biometricCipherProvider.verifierPlaintext())) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF163A74), Color(0xFF234C8C), Color(0xFF1E3359)),
                ),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.unlock_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.unlock_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login)) },
                    singleLine = true,
                    colors = vaultTextFieldColors(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.master_password)) },
                    colors = vaultTextFieldColors(),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = { viewModel.login(login, password) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.unlock_button))
                }
                if (canUseBiometric) {
                    TextButton(
                        onClick = ::launchBiometricPrompt,
                    ) {
                        Text(stringResource(R.string.unlock_biometric_button))
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
