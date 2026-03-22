package com.example.passwordvault.data.local

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.example.passwordvault.core.auth.PasswordHasher
import com.example.passwordvault.core.crypto.DeviceCryptoManager
import com.example.passwordvault.core.crypto.VaultFileCodec
import com.example.passwordvault.data.local.dao.AuthSettingsDao
import com.example.passwordvault.data.local.dao.CredentialDao
import com.example.passwordvault.data.local.dao.FolderDao
import com.example.passwordvault.data.local.entity.AuthSettingsEntity
import com.example.passwordvault.data.local.entity.CredentialEntity
import com.example.passwordvault.data.local.entity.FolderEntity
import com.example.passwordvault.data.local.entity.FolderSummaryProjection
import com.example.passwordvault.domain.model.AuthSettings
import com.example.passwordvault.domain.model.ChangePasswordResult
import com.example.passwordvault.domain.model.CredentialInput
import com.example.passwordvault.domain.model.CredentialRecord
import com.example.passwordvault.domain.model.CredentialSummary
import com.example.passwordvault.domain.model.ExportCredential
import com.example.passwordvault.domain.model.ExportFolder
import com.example.passwordvault.domain.model.FolderSummary
import com.example.passwordvault.domain.model.LoginResult
import com.example.passwordvault.domain.model.LoginFailureReason
import com.example.passwordvault.domain.model.VaultSnapshot
import com.example.passwordvault.domain.repository.AuthRepository
import com.example.passwordvault.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultRepositoryImpl(
    private val database: AppDatabase,
    private val folderDao: FolderDao,
    private val credentialDao: CredentialDao,
    private val cryptoManager: DeviceCryptoManager,
) : VaultRepository {

    override fun observeFolders(): Flow<List<FolderSummary>> =
        folderDao.observeFolders().map { items -> items.map(::mapFolderSummary) }

    override fun observeAllCredentials(): Flow<List<CredentialSummary>> =
        credentialDao.observeAllCredentials().map { items -> items.map(::mapCredentialSummary) }

    override fun observeRootCredentials(): Flow<List<CredentialSummary>> =
        credentialDao.observeRootCredentials().map { items -> items.map(::mapCredentialSummary) }

    override fun observeFolder(folderId: Long): Flow<FolderSummary?> =
        folderDao.observeFolder(folderId).map { it?.let(::mapFolderSummary) }

    override fun observeCredentialsInFolder(folderId: Long): Flow<List<CredentialSummary>> =
        credentialDao.observeCredentialsInFolder(folderId).map { items -> items.map(::mapCredentialSummary) }

    override fun observeCredential(credentialId: Long): Flow<CredentialRecord?> =
        credentialDao.observeCredential(credentialId).map { it?.let(::mapCredentialRecord) }

    override suspend fun getAllFolders(): List<FolderSummary> =
        folderDao.getFolders().map {
            FolderSummary(
                id = it.id,
                name = cryptoManager.decrypt(it.encryptedName),
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                credentialCount = folderDao.getCredentialCount(it.id),
            )
        }

    override suspend fun getFolderCount(folderId: Long): Int = folderDao.getCredentialCount(folderId)

    override suspend fun upsertFolder(folderId: Long?, name: String): Long {
        val now = System.currentTimeMillis()
        val encryptedName = cryptoManager.encrypt(name.trim())
        return if (folderId == null) {
            folderDao.insert(
                FolderEntity(
                    encryptedName = encryptedName,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val existing = folderDao.getFolder(folderId) ?: error("Folder not found.")
            folderDao.update(existing.copy(encryptedName = encryptedName, updatedAt = now))
            folderId
        }
    }

    override suspend fun deleteFolder(folderId: Long) {
        folderDao.deleteById(folderId)
    }

    override suspend fun saveCredential(input: CredentialInput): Long {
        val now = System.currentTimeMillis()
        return if (input.id == null) {
            credentialDao.insert(
                CredentialEntity(
                    folderId = input.folderId,
                    encryptedTitle = cryptoManager.encrypt(input.title.trim()),
                    encryptedUrl = input.url?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                    encryptedLogin = cryptoManager.encrypt(input.login.trim()),
                    encryptedPassword = cryptoManager.encrypt(input.password),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val existing = credentialDao.getById(input.id) ?: error("Credential not found.")
            credentialDao.update(
                existing.copy(
                    folderId = input.folderId,
                    encryptedTitle = cryptoManager.encrypt(input.title.trim()),
                    encryptedUrl = input.url?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                    encryptedLogin = cryptoManager.encrypt(input.login.trim()),
                    encryptedPassword = cryptoManager.encrypt(input.password),
                    updatedAt = now,
                ),
            )
            input.id
        }
    }

    override suspend fun deleteCredential(credentialId: Long) {
        credentialDao.deleteById(credentialId)
    }

    override suspend fun deleteAllCredentials() {
        credentialDao.deleteAll()
    }

    override suspend fun deleteAllData() {
        database.withTransaction {
            credentialDao.deleteAll()
            folderDao.deleteAll()
        }
    }

    override suspend fun exportSnapshot(): VaultSnapshot {
        val folders = folderDao.getFolders().map {
            ExportFolder(
                externalId = it.id,
                name = cryptoManager.decrypt(it.encryptedName),
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        val credentials = credentialDao.getAll().map {
            ExportCredential(
                externalId = it.id,
                folderExternalId = it.folderId,
                title = cryptoManager.decrypt(it.encryptedTitle),
                url = it.encryptedUrl?.let(cryptoManager::decrypt),
                login = cryptoManager.decrypt(it.encryptedLogin),
                password = cryptoManager.decrypt(it.encryptedPassword),
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        return VaultSnapshot(folders = folders, credentials = credentials)
    }

    override suspend fun replaceWith(snapshot: VaultSnapshot) {
        database.withTransaction {
            credentialDao.deleteAll()
            folderDao.deleteAll()

            val folderIdMap = mutableMapOf<Long, Long>()
            snapshot.folders.forEach { folder ->
                val newId = folderDao.insert(
                    FolderEntity(
                        encryptedName = cryptoManager.encrypt(folder.name),
                        createdAt = folder.createdAt,
                        updatedAt = folder.updatedAt,
                    ),
                )
                folderIdMap[folder.externalId] = newId
            }

            snapshot.credentials.forEach { credential ->
                credentialDao.insert(
                    CredentialEntity(
                        folderId = credential.folderExternalId?.let(folderIdMap::get),
                        encryptedTitle = cryptoManager.encrypt(credential.title),
                        encryptedUrl = credential.url?.let(cryptoManager::encrypt),
                        encryptedLogin = cryptoManager.encrypt(credential.login),
                        encryptedPassword = cryptoManager.encrypt(credential.password),
                        createdAt = credential.createdAt,
                        updatedAt = credential.updatedAt,
                    ),
                )
            }
        }
    }

    private fun mapFolderSummary(item: FolderSummaryProjection): FolderSummary =
        FolderSummary(
            id = item.id,
            name = cryptoManager.decrypt(item.encryptedName),
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
            credentialCount = item.credentialCount,
        )

    private fun mapCredentialSummary(item: CredentialEntity): CredentialSummary =
        CredentialSummary(
            id = item.id,
            folderId = item.folderId,
            folderName = null,
            title = cryptoManager.decrypt(item.encryptedTitle),
            url = item.encryptedUrl?.let(cryptoManager::decrypt),
            login = cryptoManager.decrypt(item.encryptedLogin),
            updatedAt = item.updatedAt,
        )

    private fun mapCredentialRecord(item: CredentialEntity): CredentialRecord =
        CredentialRecord(
            id = item.id,
            folderId = item.folderId,
            title = cryptoManager.decrypt(item.encryptedTitle),
            url = item.encryptedUrl?.let(cryptoManager::decrypt),
            login = cryptoManager.decrypt(item.encryptedLogin),
            password = cryptoManager.decrypt(item.encryptedPassword),
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        )
}

class AuthRepositoryImpl(
    private val authSettingsDao: AuthSettingsDao,
    private val passwordHasher: PasswordHasher,
) : AuthRepository {
    private val maxAttemptsBeforeLock = 5

    override fun observeSettings(): Flow<AuthSettings> =
        authSettingsDao.observeSettings().map { (it ?: AuthSettingsEntity()).toModel() }

    override suspend fun getSettings(): AuthSettings =
        (authSettingsDao.getSettings() ?: AuthSettingsEntity()).toModel()

    override suspend fun enableProtection(login: String, password: String) {
        val current = authSettingsDao.getSettings() ?: AuthSettingsEntity()
        val salt = passwordHasher.generateSalt()
        val iterations = 210_000
        authSettingsDao.upsert(
            current.copy(
                protectionEnabled = true,
                login = login.trim(),
                passwordHash = passwordHasher.hash(password, salt, iterations),
                passwordSalt = salt,
                kdfIterations = iterations,
                biometricEnabled = false,
                biometricCiphertext = null,
                biometricIv = null,
                failedAttempts = 0,
                lockoutUntilEpochMillis = null,
            ),
        )
    }

    override suspend fun changeMasterPassword(
        currentPassword: String,
        newPassword: String,
    ): ChangePasswordResult {
        val current = authSettingsDao.getSettings() ?: AuthSettingsEntity()
        if (!current.protectionEnabled) {
            return ChangePasswordResult.PROTECTION_NOT_ENABLED
        }

        val currentHash = passwordHasher.hash(
            currentPassword,
            current.passwordSalt,
            current.kdfIterations,
        )
        if (currentHash != current.passwordHash) {
            return ChangePasswordResult.INVALID_CURRENT_PASSWORD
        }

        val newSalt = passwordHasher.generateSalt()
        val iterations = current.kdfIterations.coerceAtLeast(210_000)
        authSettingsDao.upsert(
            current.copy(
                passwordHash = passwordHasher.hash(newPassword, newSalt, iterations),
                passwordSalt = newSalt,
                kdfIterations = iterations,
                failedAttempts = 0,
                lockoutUntilEpochMillis = null,
            ),
        )
        return ChangePasswordResult.SUCCESS
    }

    override suspend fun verifyLogin(login: String, password: String): LoginResult {
        val current = authSettingsDao.getSettings() ?: AuthSettingsEntity()
        if (!current.protectionEnabled) {
            return LoginResult(success = true)
        }

        val now = System.currentTimeMillis()
        val lockoutUntil = current.lockoutUntilEpochMillis
        if (lockoutUntil != null && lockoutUntil > now) {
            return LoginResult(
                success = false,
                reason = LoginFailureReason.TEMPORARILY_LOCKED,
                lockoutRemainingMillis = lockoutUntil - now,
            )
        }

        val calculatedHash = passwordHasher.hash(password, current.passwordSalt, current.kdfIterations)
        val success = current.login == login.trim() && current.passwordHash == calculatedHash
        if (success) {
            authSettingsDao.upsert(
                current.copy(
                    failedAttempts = 0,
                    lockoutUntilEpochMillis = null,
                ),
            )
            return LoginResult(success = true)
        }

        val failedAttempts = current.failedAttempts + 1
        val delayStep = (failedAttempts / maxAttemptsBeforeLock).coerceAtLeast(0)
        val lockDuration = if (failedAttempts >= maxAttemptsBeforeLock) {
            (30_000L * (1 shl (delayStep - 1).coerceAtMost(3))).coerceAtMost(300_000L)
        } else {
            0L
        }

        authSettingsDao.upsert(
            current.copy(
                failedAttempts = failedAttempts,
                lockoutUntilEpochMillis = (now + lockDuration).takeIf { lockDuration > 0 },
            ),
        )
        return LoginResult(
            success = false,
            reason = if (lockDuration > 0) {
                LoginFailureReason.TEMPORARILY_LOCKED
            } else {
                LoginFailureReason.INVALID_CREDENTIALS
            },
            lockoutRemainingMillis = lockDuration,
        )
    }

    override suspend fun saveBiometricSecret(iv: String, ciphertext: String) {
        val current = authSettingsDao.getSettings() ?: AuthSettingsEntity()
        authSettingsDao.upsert(
            current.copy(
                biometricEnabled = true,
                biometricIv = iv,
                biometricCiphertext = ciphertext,
            ),
        )
    }

    override suspend fun disableBiometric() {
        val current = authSettingsDao.getSettings() ?: AuthSettingsEntity()
        authSettingsDao.upsert(
            current.copy(
                biometricEnabled = false,
                biometricIv = null,
                biometricCiphertext = null,
            ),
        )
    }

    private fun AuthSettingsEntity.toModel(): AuthSettings =
        AuthSettings(
            protectionEnabled = protectionEnabled,
            login = login,
            passwordHash = passwordHash,
            passwordSalt = passwordSalt,
            kdfIterations = kdfIterations,
            biometricEnabled = biometricEnabled,
            biometricCiphertext = biometricCiphertext,
            biometricIv = biometricIv,
            failedAttempts = failedAttempts,
            lockoutUntilEpochMillis = lockoutUntilEpochMillis,
        )
}

class VaultTransferManager(
    private val contentResolver: ContentResolver,
    private val vaultRepository: VaultRepository,
    private val codec: VaultFileCodec,
) {
    suspend fun exportTo(uri: Uri, passphrase: String) {
        require(passphrase.length >= 8) { "Export passphrase must be at least 8 characters." }
        val snapshot = vaultRepository.exportSnapshot()
        val encoded = codec.encode(snapshot, passphrase)
        contentResolver.openOutputStream(uri)?.use { it.write(encoded) }
            ?: error("Unable to open destination file.")
    }

    suspend fun importFrom(uri: Uri, passphrase: String) {
        require(passphrase.length >= 8) { "Import passphrase must be at least 8 characters." }
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read import file.")
        val snapshot = codec.decode(bytes, passphrase)
        vaultRepository.replaceWith(snapshot)
    }
}
