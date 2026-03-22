package com.example.passwordvault.core.crypto

import android.net.Uri
import android.util.Base64
import com.example.passwordvault.domain.model.ExportCredential
import com.example.passwordvault.domain.model.ExportFolder
import com.example.passwordvault.domain.model.VaultSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

data class PackedCipherText(
    val ivBase64: String,
    val cipherTextBase64: String,
) {
    fun pack(): String = "$ivBase64:$cipherTextBase64"

    companion object {
        fun unpack(packed: String): PackedCipherText {
            val parts = packed.split(':', limit = 2)
            require(parts.size == 2) { "Invalid encrypted payload format." }
            return PackedCipherText(parts[0], parts[1])
        }
    }
}

class DeviceCryptoManager {
    private val keyAlias = "password_vault_local_key"
    private val keyStoreProvider = "AndroidKeyStore"

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return PackedCipherText(
            ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            cipherTextBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
        ).pack()
    }

    fun decrypt(packed: String): String {
        val payload = PackedCipherText.unpack(packed)
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, Base64.decode(payload.ivBase64, Base64.NO_WRAP)),
        )
        val clear = cipher.doFinal(Base64.decode(payload.cipherTextBase64, Base64.NO_WRAP))
        return clear.toString(StandardCharsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }
        val existing = (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val AES_MODE = "AES/GCM/NoPadding"
    }
}

class VaultFileCodec(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun encode(snapshot: VaultSnapshot, passphrase: String): ByteArray {
        val snapshotJson = JSONObject().apply {
            put("folders", JSONArray().apply {
                snapshot.folders.forEach { folder ->
                    put(JSONObject().apply {
                        put("id", folder.externalId)
                        put("name", folder.name)
                        put("createdAt", folder.createdAt)
                        put("updatedAt", folder.updatedAt)
                    })
                }
            })
            put("credentials", JSONArray().apply {
                snapshot.credentials.forEach { credential ->
                    put(JSONObject().apply {
                        put("id", credential.externalId)
                        put("folderId", credential.folderExternalId)
                        put("title", credential.title)
                        put("url", credential.url)
                        put("login", credential.login)
                        put("password", credential.password)
                        put("createdAt", credential.createdAt)
                        put("updatedAt", credential.updatedAt)
                    })
                }
            })
        }.toString().toByteArray(StandardCharsets.UTF_8)

        val salt = ByteArray(16).also(secureRandom::nextBytes)
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val iterations = 210_000
        val secret = deriveSecretKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(snapshotJson)

        val envelope = JSONObject().apply {
            put("version", 1)
            put("kdf", JSONObject().apply {
                put("algorithm", "PBKDF2WithHmacSHA256")
                put("iterations", iterations)
                put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            })
            put("cipher", JSONObject().apply {
                put("algorithm", "AES/GCM/NoPadding")
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            })
            put("payload", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
        return envelope.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun decode(raw: ByteArray, passphrase: String): VaultSnapshot {
        val envelope = JSONObject(raw.toString(StandardCharsets.UTF_8))
        require(envelope.optInt("version") == 1) { "Unsupported vault version." }

        val kdf = envelope.getJSONObject("kdf")
        val iterations = kdf.getInt("iterations")
        val salt = Base64.decode(kdf.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(envelope.getJSONObject("cipher").getString("iv"), Base64.NO_WRAP)
        val payload = Base64.decode(envelope.getString("payload"), Base64.NO_WRAP)

        val secret = deriveSecretKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, iv))
        val clear = cipher.doFinal(payload).toString(StandardCharsets.UTF_8)
        val snapshotJson = JSONObject(clear)

        val folders = snapshotJson.getJSONArray("folders").toFolders()
        val credentials = snapshotJson.getJSONArray("credentials").toCredentials()
        return VaultSnapshot(folders = folders, credentials = credentials)
    }

    private fun deriveSecretKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)
        val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return javax.crypto.spec.SecretKeySpec(secret, "AES")
    }

    private fun JSONArray.toFolders(): List<ExportFolder> = buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                ExportFolder(
                    externalId = item.getLong("id"),
                    name = item.getString("name"),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                ),
            )
        }
    }

    private fun JSONArray.toCredentials(): List<ExportCredential> = buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                ExportCredential(
                    externalId = item.getLong("id"),
                    folderExternalId = item.optLong("folderId").takeIf { !item.isNull("folderId") },
                    title = item.getString("title"),
                    url = item.optString("url").takeIf { !item.isNull("url") },
                    login = item.getString("login"),
                    password = item.getString("password"),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                ),
            )
        }
    }
}
