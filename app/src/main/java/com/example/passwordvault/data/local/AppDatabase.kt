package com.example.passwordvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.passwordvault.data.local.dao.AuthSettingsDao
import com.example.passwordvault.data.local.dao.CredentialDao
import com.example.passwordvault.data.local.dao.FolderDao
import com.example.passwordvault.data.local.entity.AuthSettingsEntity
import com.example.passwordvault.data.local.entity.CredentialEntity
import com.example.passwordvault.data.local.entity.FolderEntity

@Database(
    entities = [
        FolderEntity::class,
        CredentialEntity::class,
        AuthSettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun credentialDao(): CredentialDao
    abstract fun authSettingsDao(): AuthSettingsDao
}
