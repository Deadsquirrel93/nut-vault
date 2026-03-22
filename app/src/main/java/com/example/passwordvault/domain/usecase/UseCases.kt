package com.example.passwordvault.domain.usecase

import android.net.Uri
import com.example.passwordvault.core.password.PasswordGenerator
import com.example.passwordvault.core.password.PasswordStrengthEvaluator
import com.example.passwordvault.data.local.VaultTransferManager
import com.example.passwordvault.domain.model.CredentialInput
import com.example.passwordvault.domain.model.ChangePasswordResult
import com.example.passwordvault.domain.model.LoginResult
import com.example.passwordvault.domain.model.PasswordGeneratorOptions
import com.example.passwordvault.domain.model.PasswordStrengthScore
import com.example.passwordvault.domain.repository.AuthRepository
import com.example.passwordvault.domain.repository.VaultRepository

class SaveCredentialUseCase(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke(input: CredentialInput): Long = vaultRepository.saveCredential(input)
}

class SaveFolderUseCase(
    private val vaultRepository: VaultRepository,
) {
    suspend operator fun invoke(folderId: Long?, name: String): Long = vaultRepository.upsertFolder(folderId, name)
}

class EnableProtectionUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(login: String, password: String) {
        authRepository.enableProtection(login, password)
    }
}

class VerifyLoginUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(login: String, password: String): LoginResult =
        authRepository.verifyLogin(login, password)
}

class ChangeMasterPasswordUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): ChangePasswordResult =
        authRepository.changeMasterPassword(currentPassword, newPassword)
}

class GeneratePasswordUseCase(
    private val passwordGenerator: PasswordGenerator,
) {
    operator fun invoke(options: PasswordGeneratorOptions): String = passwordGenerator.generate(options)
}

class EvaluatePasswordUseCase(
    private val passwordStrengthEvaluator: PasswordStrengthEvaluator,
) {
    operator fun invoke(password: String): PasswordStrengthScore =
        passwordStrengthEvaluator.evaluate(password)
}

class ExportVaultUseCase(
    private val vaultTransferManager: VaultTransferManager,
) {
    suspend operator fun invoke(uri: Uri, passphrase: String) {
        vaultTransferManager.exportTo(uri, passphrase)
    }
}

class ImportVaultUseCase(
    private val vaultTransferManager: VaultTransferManager,
) {
    suspend operator fun invoke(uri: Uri, passphrase: String) {
        vaultTransferManager.importFrom(uri, passphrase)
    }
}
