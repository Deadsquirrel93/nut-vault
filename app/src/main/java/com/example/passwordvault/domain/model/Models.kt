package com.example.passwordvault.domain.model

data class FolderSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val credentialCount: Int,
)

data class CredentialSummary(
    val id: Long,
    val folderId: Long?,
    val folderName: String? = null,
    val title: String,
    val url: String?,
    val login: String,
    val isFavorite: Boolean = false,
    val hasTotp: Boolean = false,
    val updatedAt: Long,
)

data class CredentialRecord(
    val id: Long,
    val folderId: Long?,
    val title: String,
    val url: String?,
    val login: String,
    val password: String,
    val notes: String? = null,
    val totpSecret: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CredentialInput(
    val id: Long? = null,
    val folderId: Long?,
    val title: String,
    val url: String?,
    val login: String,
    val password: String,
    val notes: String? = null,
    val totpSecret: String? = null,
    val isFavorite: Boolean = false,
)

data class AuthSettings(
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

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG,
    VERY_STRONG,
}

data class PasswordStrengthScore(
    val strength: PasswordStrength,
    val entropyBits: Int,
)

data class ReusedPasswordGroup(
    val credentials: List<CredentialRecord>,
)

data class PasswordHealthReport(
    val totalCount: Int,
    val scorePercent: Int,
    val weakCredentials: List<CredentialRecord>,
    val mediumCredentials: List<CredentialRecord>,
    val reusedGroups: List<ReusedPasswordGroup>,
    val totpEnabledCount: Int,
)

data class PasswordGeneratorOptions(
    val length: Int = 20,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
) {
    val activeGroupCount: Int
        get() = listOf(includeUppercase, includeLowercase, includeDigits, includeSymbols).count { it }
}

data class ExportFolder(
    val externalId: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ExportCredential(
    val externalId: Long,
    val folderExternalId: Long?,
    val title: String,
    val url: String?,
    val login: String,
    val password: String,
    val notes: String? = null,
    val totpSecret: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

data class VaultSnapshot(
    val folders: List<ExportFolder>,
    val credentials: List<ExportCredential>,
)

enum class ImportMode {
    REPLACE,
    MERGE,
}

data class ImportSummary(
    val addedFolders: Int,
    val addedCredentials: Int,
    val skippedDuplicates: Int,
)

data class LoginResult(
    val success: Boolean,
    val reason: LoginFailureReason? = null,
    val lockoutRemainingMillis: Long = 0L,
)

enum class LoginFailureReason {
    INVALID_CREDENTIALS,
    TEMPORARILY_LOCKED,
}

enum class ChangePasswordResult {
    SUCCESS,
    INVALID_CURRENT_PASSWORD,
    PROTECTION_NOT_ENABLED,
}
