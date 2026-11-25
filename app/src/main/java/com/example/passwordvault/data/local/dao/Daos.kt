package com.example.passwordvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.passwordvault.data.local.entity.AuthSettingsEntity
import com.example.passwordvault.data.local.entity.CredentialEntity
import com.example.passwordvault.data.local.entity.FolderEntity
import com.example.passwordvault.data.local.entity.FolderSummaryProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query(
        """
        SELECT folders.id, folders.encryptedName, folders.createdAt, folders.updatedAt, COUNT(credentials.id) AS credentialCount
        FROM folders
        LEFT JOIN credentials ON folders.id = credentials.folderId
        GROUP BY folders.id
        ORDER BY folders.updatedAt DESC
        """,
    )
    fun observeFolders(): Flow<List<FolderSummaryProjection>>

    @Query(
        """
        SELECT folders.id, folders.encryptedName, folders.createdAt, folders.updatedAt, COUNT(credentials.id) AS credentialCount
        FROM folders
        LEFT JOIN credentials ON folders.id = credentials.folderId
        WHERE folders.id = :folderId
        GROUP BY folders.id
        """,
    )
    fun observeFolder(folderId: Long): Flow<FolderSummaryProjection?>

    @Query("SELECT * FROM folders ORDER BY updatedAt DESC")
    suspend fun getFolders(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolder(folderId: Long): FolderEntity?

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteById(folderId: Long)

    @Query("DELETE FROM folders")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM credentials WHERE folderId = :folderId")
    suspend fun getCredentialCount(folderId: Long): Int
}

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials ORDER BY updatedAt DESC")
    fun observeAllCredentials(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE folderId IS NULL ORDER BY updatedAt DESC")
    fun observeRootCredentials(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun observeCredentialsInFolder(folderId: Long): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE id = :credentialId")
    fun observeCredential(credentialId: Long): Flow<CredentialEntity?>

    @Query("SELECT * FROM credentials ORDER BY updatedAt DESC")
    suspend fun getAll(): List<CredentialEntity>

    @Query("SELECT * FROM credentials WHERE id = :credentialId")
    suspend fun getById(credentialId: Long): CredentialEntity?

    @Insert
    suspend fun insert(credential: CredentialEntity): Long

    @Update
    suspend fun update(credential: CredentialEntity)

    @Query("UPDATE credentials SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :credentialId")
    suspend fun setFavorite(credentialId: Long, isFavorite: Boolean, updatedAt: Long)

    @Query("DELETE FROM credentials WHERE id = :credentialId")
    suspend fun deleteById(credentialId: Long)

    @Query("DELETE FROM credentials")
    suspend fun deleteAll()
}

@Dao
interface AuthSettingsDao {
    @Query("SELECT * FROM auth_settings WHERE id = 0")
    fun observeSettings(): Flow<AuthSettingsEntity?>

    @Query("SELECT * FROM auth_settings WHERE id = 0")
    suspend fun getSettings(): AuthSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AuthSettingsEntity)
}
