package com.example.passwordvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun credentialDao(): CredentialDao
    abstract fun authSettingsDao(): AuthSettingsDao

    companion object {
        // Non-destructive v1 -> v2 migration preserving all existing encrypted rows.
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE credentials ADD COLUMN encryptedNotes TEXT")
                db.execSQL("ALTER TABLE credentials ADD COLUMN encryptedTotpSecret TEXT")
                db.execSQL("ALTER TABLE credentials ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
