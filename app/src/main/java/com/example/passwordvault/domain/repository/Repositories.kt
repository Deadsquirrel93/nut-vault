package com.example.passwordvault.domain.repository

import com.example.passwordvault.domain.model.AuthSettings
import com.example.passwordvault.domain.model.ChangePasswordResult
import com.example.passwordvault.domain.model.CredentialInput
import com.example.passwordvault.domain.model.CredentialRecord
import com.example.passwordvault.domain.model.CredentialSummary
import com.example.passwordvault.domain.model.FolderSummary
import com.example.passwordvault.domain.model.ImportSummary
import com.example.passwordvault.domain.model.LoginResult
import com.example.passwordvault.domain.model.VaultSnapshot
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun observeFolders(): Flow<List<FolderSummary>>
    fun observeAllCredentials(): Flow<List<CredentialSummary>>
    fun observeAllCredentialRecords(): Flow<List<CredentialRecord>>
    fun observeRootCredentials(): Flow<List<CredentialSummary>>
    fun observeFolder(folderId: Long): Flow<FolderSummary?>
    fun observeCredentialsInFolder(folderId: Long): Flow<List<CredentialSummary>>
    fun observeCredential(credentialId: Long): Flow<CredentialRecord?>

    suspend fun getAllFolders(): List<FolderSummary>
    suspend fun getAllCredentialRecords(): List<CredentialRecord>
    suspend fun getCredentialRecord(credentialId: Long): CredentialRecord?
    suspend fun getFolderCount(folderId: Long): Int
    suspend fun upsertFolder(folderId: Long?, name: String): Long
    suspend fun deleteFolder(folderId: Long)

    suspend fun saveCredential(input: CredentialInput): Long
    suspend fun setFavorite(credentialId: Long, isFavorite: Boolean)
    suspend fun deleteCredential(credentialId: Long)
    suspend fun deleteAllCredentials()
    suspend fun deleteAllData()

    suspend fun exportSnapshot(): VaultSnapshot
    suspend fun replaceWith(snapshot: VaultSnapshot): ImportSummary
    suspend fun mergeWith(snapshot: VaultSnapshot): ImportSummary
}

interface AuthRepository {
    fun observeSettings(): Flow<AuthSettings>
    suspend fun getSettings(): AuthSettings
    suspend fun enableProtection(login: String, password: String)
    suspend fun changeMasterPassword(currentPassword: String, newPassword: String): ChangePasswordResult
    suspend fun verifyLogin(login: String, password: String): LoginResult
    suspend fun saveBiometricSecret(iv: String, ciphertext: String)
    suspend fun disableBiometric()
}
