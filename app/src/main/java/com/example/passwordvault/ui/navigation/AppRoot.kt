package com.example.passwordvault.ui.navigation

import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.passwordvault.R
import com.example.passwordvault.app.AppContainer
import com.example.passwordvault.app.AppViewModel
import com.example.passwordvault.ui.components.findActivity
import com.example.passwordvault.ui.credential.detail.CredentialDetailScreen
import com.example.passwordvault.ui.credential.detail.CredentialDetailViewModel
import com.example.passwordvault.ui.credential.editor.CredentialEditorScreen
import com.example.passwordvault.ui.credential.editor.CredentialEditorViewModel
import com.example.passwordvault.ui.folder.FolderScreen
import com.example.passwordvault.ui.folder.FolderViewModel
import com.example.passwordvault.ui.home.HomeScreen
import com.example.passwordvault.ui.home.HomeViewModel
import com.example.passwordvault.ui.login.LoginScreen
import com.example.passwordvault.ui.login.LoginViewModel
import com.example.passwordvault.ui.settings.SettingsScreen
import com.example.passwordvault.ui.settings.SettingsViewModel
import com.example.passwordvault.ui.setup.SetupScreen
import com.example.passwordvault.ui.setup.SetupViewModel

@Composable
fun AppRoot(
    container: AppContainer,
) {
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AppViewModel(container) }
        },
    )
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context.findActivity()

    if (appState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (appState.requiresSetup) {
        val setupViewModel: SetupViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    SetupViewModel(
                        enableProtectionUseCase = container.enableProtectionUseCase,
                        sessionManager = container.sessionManager,
                    )
                }
            },
        )
        SetupScreen(viewModel = setupViewModel)
        return
    }

    if (appState.requiresLogin) {
        val loginViewModel: LoginViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    LoginViewModel(
                        verifyLoginUseCase = container.verifyLoginUseCase,
                        sessionManager = container.sessionManager,
                        authRepository = container.authRepository,
                    )
                }
            },
        )
        LoginScreen(
            container = container,
            settings = appState.settings,
            viewModel = loginViewModel,
        )
        return
    }

    NavHost(navController = navController, startDestination = Routes.home) {
        composable(Routes.home) {
            val vm: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        HomeViewModel(
                            vaultRepository = container.vaultRepository,
                            saveFolderUseCase = container.saveFolderUseCase,
                        )
                    }
                },
            )
            HomeScreen(
                viewModel = vm,
                onOpenFolder = { navController.navigate(Routes.folder(it)) },
                onOpenCredential = { navController.navigate(Routes.credential(it)) },
                onCreateCredential = { navController.navigate(Routes.createCredential()) },
                onOpenSettings = { navController.navigate(Routes.settings) },
            )
        }

        composable(Routes.settings) {
            val vm: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        SettingsViewModel(
                            authRepository = container.authRepository,
                            vaultRepository = container.vaultRepository,
                            enableProtectionUseCase = container.enableProtectionUseCase,
                            changeMasterPasswordUseCase = container.changeMasterPasswordUseCase,
                            exportVaultUseCase = container.exportVaultUseCase,
                            importVaultUseCase = container.importVaultUseCase,
                            sessionManager = container.sessionManager,
                            localeManager = container.localeManager,
                        )
                    }
                },
            )
            SettingsScreen(
                container = container,
                viewModel = vm,
                onBack = { navController.navigateUp() },
            )
        }

        composable(
            route = Routes.folder,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            val vm: FolderViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        FolderViewModel(
                            folderId = folderId,
                            vaultRepository = container.vaultRepository,
                            saveFolderUseCase = container.saveFolderUseCase,
                        )
                    }
                },
            )
            FolderScreen(
                viewModel = vm,
                onBack = { navController.navigateUp() },
                onOpenCredential = { navController.navigate(Routes.credential(it)) },
                onCreateCredential = { navController.navigate(Routes.createCredential(folderId)) },
            )
        }

        composable(
            route = Routes.credential,
            arguments = listOf(navArgument("credentialId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val credentialId = backStackEntry.arguments?.getLong("credentialId") ?: return@composable
            val vm: CredentialDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        CredentialDetailViewModel(
                            credentialId = credentialId,
                            vaultRepository = container.vaultRepository,
                        )
                    }
                },
            )
            CredentialDetailScreen(
                viewModel = vm,
                onBack = { navController.navigateUp() },
                onEdit = { id, folderId ->
                    navController.navigate(Routes.editCredential(id, folderId))
                },
            )
        }

        composable(
            route = Routes.editor,
            arguments = listOf(
                navArgument("credentialId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("folderId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val credentialId = backStackEntry.arguments?.getLong("credentialId")?.takeIf { it != -1L }
            val folderId = backStackEntry.arguments?.getLong("folderId")?.takeIf { it != -1L }
            val vm: CredentialEditorViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        CredentialEditorViewModel(
                            credentialId = credentialId,
                            initialFolderId = folderId,
                            vaultRepository = container.vaultRepository,
                            saveCredentialUseCase = container.saveCredentialUseCase,
                            generatePasswordUseCase = container.generatePasswordUseCase,
                            evaluatePasswordUseCase = container.evaluatePasswordUseCase,
                        )
                    }
                },
            )
            CredentialEditorScreen(
                viewModel = vm,
                onBack = { navController.navigateUp() },
                onSaved = { savedId ->
                    navController.navigate(Routes.credential(savedId)) {
                        popUpTo(backStackEntry.destination.route ?: Routes.editor) { inclusive = true }
                    }
                },
            )
        }
    }

    val canOfferBiometricEnrollment = appState.shouldOfferBiometricEnrollment &&
        activity != null &&
        container.biometricCipherProvider.biometricAvailability(context) == BiometricManager.BIOMETRIC_SUCCESS

    LaunchedEffect(appState.shouldOfferBiometricEnrollment, canOfferBiometricEnrollment) {
        if (appState.shouldOfferBiometricEnrollment && !canOfferBiometricEnrollment) {
            appViewModel.dismissBiometricEnrollmentOffer()
        }
    }

    if (canOfferBiometricEnrollment) {
        AlertDialog(
            onDismissRequest = appViewModel::dismissBiometricEnrollmentOffer,
            title = { Text(stringResource(R.string.biometric_offer_title)) },
            text = { Text(stringResource(R.string.biometric_offer_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cipher = container.biometricCipherProvider.createEncryptionCipher()
                        val prompt = BiometricPrompt(
                            requireNotNull(activity),
                            ContextCompat.getMainExecutor(context),
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    val finalCipher = result.cryptoObject?.cipher ?: return
                                    val ciphertext = Base64.encodeToString(
                                        finalCipher.doFinal(container.biometricCipherProvider.verifierPlaintext()),
                                        Base64.NO_WRAP,
                                    )
                                    val iv = Base64.encodeToString(finalCipher.iv, Base64.NO_WRAP)
                                    appViewModel.enableBiometric(iv, ciphertext)
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    appViewModel.dismissBiometricEnrollmentOffer()
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
                    },
                ) {
                    Text(stringResource(R.string.enable_biometric_now))
                }
            },
            dismissButton = {
                TextButton(onClick = appViewModel::dismissBiometricEnrollmentOffer) {
                    Text(stringResource(R.string.biometric_offer_later))
                }
            },
        )
    }
}
