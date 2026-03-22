package com.example.passwordvault.app

import android.content.Context
import androidx.room.Room
import com.example.passwordvault.core.auth.BiometricCipherProvider
import com.example.passwordvault.core.auth.PasswordHasher
import com.example.passwordvault.core.auth.SessionManager
import com.example.passwordvault.core.crypto.DeviceCryptoManager
import com.example.passwordvault.core.crypto.VaultFileCodec
import com.example.passwordvault.core.localization.LocaleManager
import com.example.passwordvault.core.password.PasswordGenerator
import com.example.passwordvault.core.password.PasswordStrengthEvaluator
import com.example.passwordvault.data.local.AppDatabase
import com.example.passwordvault.data.local.AuthRepositoryImpl
import com.example.passwordvault.data.local.VaultRepositoryImpl
import com.example.passwordvault.data.local.VaultTransferManager
import com.example.passwordvault.domain.repository.AuthRepository
import com.example.passwordvault.domain.repository.VaultRepository
import com.example.passwordvault.domain.usecase.EnableProtectionUseCase
import com.example.passwordvault.domain.usecase.ChangeMasterPasswordUseCase
import com.example.passwordvault.domain.usecase.EvaluatePasswordUseCase
import com.example.passwordvault.domain.usecase.ExportVaultUseCase
import com.example.passwordvault.domain.usecase.GeneratePasswordUseCase
import com.example.passwordvault.domain.usecase.ImportVaultUseCase
import com.example.passwordvault.domain.usecase.SaveCredentialUseCase
import com.example.passwordvault.domain.usecase.SaveFolderUseCase
import com.example.passwordvault.domain.usecase.VerifyLoginUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appDatabase: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "password-vault.db",
    ).build()

    val deviceCryptoManager = DeviceCryptoManager()
    val passwordHasher = PasswordHasher()
    val passwordGenerator = PasswordGenerator()
    val passwordStrengthEvaluator = PasswordStrengthEvaluator()
    val biometricCipherProvider = BiometricCipherProvider()
    val sessionManager = SessionManager()
    val localeManager = LocaleManager(context)

    val vaultRepository: VaultRepository = VaultRepositoryImpl(
        database = appDatabase,
        folderDao = appDatabase.folderDao(),
        credentialDao = appDatabase.credentialDao(),
        cryptoManager = deviceCryptoManager,
    )

    val authRepository: AuthRepository = AuthRepositoryImpl(
        authSettingsDao = appDatabase.authSettingsDao(),
        passwordHasher = passwordHasher,
    )

    private val vaultTransferManager = VaultTransferManager(
        contentResolver = context.contentResolver,
        vaultRepository = vaultRepository,
        codec = VaultFileCodec(),
    )

    val saveCredentialUseCase = SaveCredentialUseCase(vaultRepository)
    val saveFolderUseCase = SaveFolderUseCase(vaultRepository)
    val enableProtectionUseCase = EnableProtectionUseCase(authRepository)
    val changeMasterPasswordUseCase = ChangeMasterPasswordUseCase(authRepository)
    val verifyLoginUseCase = VerifyLoginUseCase(authRepository)
    val generatePasswordUseCase = GeneratePasswordUseCase(passwordGenerator)
    val evaluatePasswordUseCase = EvaluatePasswordUseCase(passwordStrengthEvaluator)
    val exportVaultUseCase = ExportVaultUseCase(vaultTransferManager)
    val importVaultUseCase = ImportVaultUseCase(vaultTransferManager)
}
