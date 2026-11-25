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
import com.example.passwordvault.domain.model.ImportMode
import com.example.passwordvault.domain.model.ImportSummary
import com.example.passwordvault.domain.model.LoginFailureReason
import com.example.passwordvault.domain.model.LoginResult
import com.example.passwordvault.domain.model.VaultSnapshot
import com.example.passwordvault.domain.repository.AuthRepository
import com.example.passwordvault.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VaultRepositoryImpl(
    private val database: AppDatabase,
    private val folderDao: FolderDao,
    private val credentialDao: CredentialDao,
    private val cryptoManager: DeviceCryptoManager,
) : VaultRepository {

    override fun observeFolders(): Flow<List<FolderSummary>> =
        folderDao.observeFolders()
            .map { items -> items.map(::mapFolderSummary) }
            .flowOn(Dispatchers.IO)

    override fun observeAllCredentials(): Flow<List<CredentialSummary>> =
        credentialDao.observeAllCredentials()
            .map { items -> items.map(::mapCredentialSummary) }
            .flowOn(Dispatchers.IO)

    override fun observeAllCredentialRecords(): Flow<List<CredentialRecord>> =
        credentialDao.observeAllCredentials()
            .map { items -> items.map(::mapCredentialRecord) }
            .flowOn(Dispatchers.IO)

    override fun observeRootCredentials(): Flow<List<CredentialSummary>> =
        credentialDao.observeRootCredentials()
            .map { items -> items.map(::mapCredentialSummary) }
            .flowOn(Dispatchers.IO)

    override fun observeFolder(folderId: Long): Flow<FolderSummary?> =
        folderDao.observeFolder(folderId)
            .map { it?.let(::mapFolderSummary) }
            .flowOn(Dispatchers.IO)

    override fun observeCredentialsInFolder(folderId: Long): Flow<List<CredentialSummary>> =
        credentialDao.observeCredentialsInFolder(folderId)
            .map { items -> items.map(::mapCredentialSummary) }
            .flowOn(Dispatchers.IO)

    override fun observeCredential(credentialId: Long): Flow<CredentialRecord?> =
        credentialDao.observeCredential(credentialId)
            .map { it?.let(::mapCredentialRecord) }
            .flowOn(Dispatchers.IO)

    override suspend fun getAllFolders(): List<FolderSummary> = withContext(Dispatchers.IO) {
        folderDao.getFolders().map {
            FolderSummary(
                id = it.id,
                name = cryptoManager.decrypt(it.encryptedName),
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                credentialCount = folderDao.getCredentialCount(it.id),
            )
        }
    }

    override suspend fun getAllCredentialRecords(): List<CredentialRecord> = withContext(Dispatchers.IO) {
        credentialDao.getAll().map(::mapCredentialRecord)
    }

    override suspend fun getCredentialRecord(credentialId: Long): CredentialRecord? = withContext(Dispatchers.IO) {
        credentialDao.getById(credentialId)?.let(::mapCredentialRecord)
    }

    override suspend fun getFolderCount(folderId: Long): Int = withContext(Dispatchers.IO) {
        folderDao.getCredentialCount(folderId)
    }

    override suspend fun upsertFolder(folderId: Long?, name: String): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val encryptedName = cryptoManager.encrypt(name.trim())
        if (folderId == null) {
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

    override suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
        folderDao.deleteById(folderId)
    }

    override suspend fun saveCredential(input: CredentialInput): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val encryptedNotes = input.notes?.trim()?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt)
        val encryptedTotp = input.totpSecret?.trim()?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt)
        if (input.id == null) {
            credentialDao.insert(
                CredentialEntity(
                    folderId = input.folderId,
                    encryptedTitle = cryptoManager.encrypt(input.title.trim()),
                    encryptedUrl = input.url?.trim()?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                    encryptedLogin = cryptoManager.encrypt(input.login.trim()),
                    encryptedPassword = cryptoManager.encrypt(input.password),
                    encryptedNotes = encryptedNotes,
                    encryptedTotpSecret = encryptedTotp,
                    isFavorite = input.isFavorite,
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
                    encryptedUrl = input.url?.trim()?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                    encryptedLogin = cryptoManager.encrypt(input.login.trim()),
                    encryptedPassword = cryptoManager.encrypt(input.password),
                    encryptedNotes = encryptedNotes,
                    encryptedTotpSecret = encryptedTotp,
                    isFavorite = input.isFavorite,
                    updatedAt = now,
                ),
            )
            input.id
        }
    }

    override suspend fun setFavorite(credentialId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        credentialDao.setFavorite(
            credentialId = credentialId,
            isFavorite = isFavorite,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun deleteCredential(credentialId: Long) = withContext(Dispatchers.IO) {
        credentialDao.deleteById(credentialId)
    }

    override suspend fun deleteAllCredentials() = withContext(Dispatchers.IO) {
        credentialDao.deleteAll()
    }

    override suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            credentialDao.deleteAll()
            folderDao.deleteAll()
        }
    }

    override suspend fun exportSnapshot(): VaultSnapshot = withContext(Dispatchers.IO) {
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
                notes = it.encryptedNotes?.let(cryptoManager::decrypt),
                totpSecret = it.encryptedTotpSecret?.let(cryptoManager::decrypt),
                isFavorite = it.isFavorite,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        VaultSnapshot(folders = folders, credentials = credentials)
    }

    override suspend fun replaceWith(snapshot: VaultSnapshot): ImportSummary = withContext(Dispatchers.IO) {
        var addedFolders = 0
        var addedCredentials = 0
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
                addedFolders++
            }

            snapshot.credentials.forEach { credential ->
                credentialDao.insert(
                    CredentialEntity(
                        folderId = credential.folderExternalId?.let(folderIdMap::get),
                        encryptedTitle = cryptoManager.encrypt(credential.title),
                        encryptedUrl = credential.url?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                        encryptedLogin = cryptoManager.encrypt(credential.login),
                        encryptedPassword = cryptoManager.encrypt(credential.password),
                        encryptedNotes = credential.notes?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                        encryptedTotpSecret = credential.totpSecret?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                        isFavorite = credential.isFavorite,
                        createdAt = credential.createdAt,
                        updatedAt = credential.updatedAt,
                    ),
                )
                addedCredentials++
            }
        }
        ImportSummary(
            addedFolders = addedFolders,
            addedCredentials = addedCredentials,
            skippedDuplicates = 0,
        )
    }

    override suspend fun mergeWith(snapshot: VaultSnapshot): ImportSummary = withContext(Dispatchers.IO) {
        var addedFolders = 0
        var addedCredentials = 0
        var skippedDuplicates = 0

        database.withTransaction {
            val existingFoldersByName = folderDao.getFolders().associateBy(
                keySelector = { cryptoManager.decrypt(it.encryptedName).trim().lowercase() },
                valueTransform = { it.id },
            ).toMutableMap()

            val folderIdMap = mutableMapOf<Long, Long>()
            snapshot.folders.forEach { folder ->
                val key = folder.name.trim().lowercase()
                val existingId = existingFoldersByName[key]
                if (existingId != null) {
                    folderIdMap[folder.externalId] = existingId
                } else {
                    val newId = folderDao.insert(
                        FolderEntity(
                            encryptedName = cryptoManager.encrypt(folder.name.trim()),
                            createdAt = folder.createdAt,
                            updatedAt = folder.updatedAt,
                        ),
                    )
                    existingFoldersByName[key] = newId
                    folderIdMap[folder.externalId] = newId
                    addedFolders++
                }
            }

            data class CredentialKey(
                val folderId: Long?,
                val title: String,
                val login: String,
                val password: String,
            )

            val existingKeys = credentialDao.getAll().mapTo(mutableSetOf()) { entity ->
                CredentialKey(
                    folderId = entity.folderId,
                    title = cryptoManager.decrypt(entity.encryptedTitle).trim().lowercase(),
                    login = cryptoManager.decrypt(entity.encryptedLogin).trim().lowercase(),
                    password = cryptoManager.decrypt(entity.encryptedPassword),
                )
            }

            snapshot.credentials.forEach { credential ->
                val mappedFolderId = credential.folderExternalId?.let(folderIdMap::get)
                val key = CredentialKey(
                    folderId = mappedFolderId,
                    title = credential.title.trim().lowercase(),
                    login = credential.login.trim().lowercase(),
                    password = credential.password,
                )
                if (existingKeys.add(key)) {
                    credentialDao.insert(
                        CredentialEntity(
                            folderId = mappedFolderId,
                            encryptedTitle = cryptoManager.encrypt(credential.title),
                            encryptedUrl = credential.url?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                            encryptedLogin = cryptoManager.encrypt(credential.login),
                            encryptedPassword = cryptoManager.encrypt(credential.password),
                            encryptedNotes = credential.notes?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                            encryptedTotpSecret = credential.totpSecret?.takeIf { it.isNotBlank() }?.let(cryptoManager::encrypt),
                            isFavorite = credential.isFavorite,
                            createdAt = credential.createdAt,
                            updatedAt = credential.updatedAt,
                        ),
                    )
                    addedCredentials++
                } else {
                    skippedDuplicates++
                }
            }
        }

        ImportSummary(
            addedFolders = addedFolders,
            addedCredentials = addedCredentials,
            skippedDuplicates = skippedDuplicates,
        )
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
            isFavorite = item.isFavorite,
            hasTotp = !item.encryptedTotpSecret.isNullOrBlank(),
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
            notes = item.encryptedNotes?.let(cryptoManager::decrypt),
            totpSecret = item.encryptedTotpSecret?.let(cryptoManager::decrypt),
            isFavorite = item.isFavorite,
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
        authSettingsDao.observeSettings()
            .map { (it ?: AuthSettingsEntity()).toModel() }
            .flowOn(Dispatchers.IO)

    override suspend fun getSettings(): AuthSettings = withContext(Dispatchers.IO) {
        (authSettingsDao.getSettings() ?: AuthSettingsEntity()).toModel()
    }

    override suspend fun enableProtection(login: String, password: String) = withContext(Dispatchers.IO) {
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
    ): ChangePasswordResult = withContext(Dispatchers.IO) {
        val current = authSettingsDao.getSettings() ?: AuthSettingsEntity()
        if (!current.protectionEnabled) {
            return@withContext ChangePasswordResult.PROTECTION_NOT_ENABLED
        }

        val matches = passwordHasher.verify(
            password = currentPassword,
            saltBase64 = current.passwordSalt,
            iterations = current.kdfIterations,
            expectedHashBase64 = current.passwordHash,
        )
        if (!matches) {
            return@withContext ChangePasswordResult.INVALID_CURRENT_PASSWORD
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
        ChangePasswordResult.SUCCESS
    }

    override suspend fun verifyLogin(login: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        val current = authSettingsDao.getSettings() ?: AuthSettingsEntity()
        if (!current.protectionEnabled) {
            return@withContext LoginResult(success = true)
        }

        val now = System.currentTimeMillis()
        val lockoutUntil = current.lockoutUntilEpochMillis
        if (lockoutUntil != null && lockoutUntil > now) {
            return@withContext LoginResult(
                success = false,
                reason = LoginFailureReason.TEMPORARILY_LOCKED,
                lockoutRemainingMillis = lockoutUntil - now,
            )
        }

        val passwordValid = passwordHasher.verify(
            password = password,
            saltBase64 = current.passwordSalt,
            iterations = current.kdfIterations,
            expectedHashBase64 = current.passwordHash,
        )
        val success = current.login == login.trim() && passwordValid
        if (success) {
            authSettingsDao.upsert(
                current.copy(
                    failedAttempts = 0,
                    lockoutUntilEpochMillis = null,
                ),
            )
            return@withContext LoginResult(success = true)
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
        LoginResult(
            success = false,
            reason = if (lockDuration > 0) {
                LoginFailureReason.TEMPORARILY_LOCKED
            } else {
                LoginFailureReason.INVALID_CREDENTIALS
            },
            lockoutRemainingMillis = lockDuration,
        )
    }

    override suspend fun saveBiometricSecret(iv: String, ciphertext: String) = withContext(Dispatchers.IO) {
        val current = authSettingsDao.getSettings() ?: AuthSettingsEntity()
        authSettingsDao.upsert(
            current.copy(
                biometricEnabled = true,
                biometricIv = iv,
                biometricCiphertext = ciphertext,
            ),
        )
    }

    override suspend fun disableBiometric() = withContext(Dispatchers.IO) {
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
    suspend fun exportTo(uri: Uri, passphrase: String) = withContext(Dispatchers.IO) {
        require(passphrase.length >= 8) { "Export passphrase must be at least 8 characters." }
        val snapshot = vaultRepository.exportSnapshot()
        val encoded = codec.encode(snapshot, passphrase)
        contentResolver.openOutputStream(uri)?.use { it.write(encoded) }
            ?: error("Unable to open destination file.")
    }

    suspend fun importFrom(uri: Uri, passphrase: String, mode: ImportMode): ImportSummary = withContext(Dispatchers.IO) {
        require(passphrase.length >= 8) { "Import passphrase must be at least 8 characters." }
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read import file.")
        val snapshot = codec.decode(bytes, passphrase)
        when (mode) {
            ImportMode.REPLACE -> vaultRepository.replaceWith(snapshot)
            ImportMode.MERGE -> vaultRepository.mergeWith(snapshot)
        }
    }
}
