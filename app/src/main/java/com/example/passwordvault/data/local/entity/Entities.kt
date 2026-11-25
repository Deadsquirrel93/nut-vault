package com.example.passwordvault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val encryptedName: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "credentials",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folderId")],
)
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val folderId: Long?,
    val encryptedTitle: String,
    val encryptedUrl: String?,
    val encryptedLogin: String,
    val encryptedPassword: String,
    val encryptedNotes: String? = null,
    val encryptedTotpSecret: String? = null,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "auth_settings")
data class AuthSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val protectionEnabled: Boolean = false,
    val login: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val kdfIterations: Int = 210_000,
    val biometricEnabled: Boolean = false,
    val biometricCiphertext: String? = null,
    val biometricIv: String? = null,
    val failedAttempts: Int = 0,
    val lockoutUntilEpochMillis: Long? = null,
)

data class FolderSummaryProjection(
    val id: Long,
    val encryptedName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val credentialCount: Int,
)
