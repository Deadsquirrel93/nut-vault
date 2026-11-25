package com.example.passwordvault.core.auth

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

class PasswordHasher(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun generateSalt(): String {
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(password: String, saltBase64: String, iterations: Int): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verify(
        password: String,
        saltBase64: String,
        iterations: Int,
        expectedHashBase64: String,
    ): Boolean {
        val calculated = hash(password, saltBase64, iterations)
        return MessageDigest.isEqual(
            calculated.toByteArray(StandardCharsets.UTF_8),
            expectedHashBase64.toByteArray(StandardCharsets.UTF_8),
        )
    }
}

class SessionManager(
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()
    private val _shouldOfferBiometricEnrollment = MutableStateFlow(false)
    val shouldOfferBiometricEnrollment: StateFlow<Boolean> = _shouldOfferBiometricEnrollment.asStateFlow()

    private var backgroundedAtMillis: Long? = null
    private var pendingLockJob: Job? = null

    fun unlock() {
        pendingLockJob?.cancel()
        pendingLockJob = null
        backgroundedAtMillis = null
        _isUnlocked.value = true
    }

    fun lock() {
        pendingLockJob?.cancel()
        pendingLockJob = null
        backgroundedAtMillis = null
        _isUnlocked.value = false
    }

    fun onAppBackgrounded(timeoutMillis: Long, scope: CoroutineScope) {
        if (!_isUnlocked.value) return
        if (timeoutMillis <= 0L) {
            lock()
            return
        }
        backgroundedAtMillis = clockMillis()
        pendingLockJob?.cancel()
        pendingLockJob = scope.launch {
            delay(timeoutMillis)
            lock()
        }
    }

    fun onAppForegrounded(timeoutMillis: Long) {
        pendingLockJob?.cancel()
        pendingLockJob = null
        val backgroundedAt = backgroundedAtMillis ?: return
        backgroundedAtMillis = null
        if (_isUnlocked.value && (clockMillis() - backgroundedAt) >= timeoutMillis) {
            _isUnlocked.value = false
        }
    }

    fun requestBiometricEnrollmentOffer() {
        _shouldOfferBiometricEnrollment.value = true
    }

    fun clearBiometricEnrollmentOffer() {
        _shouldOfferBiometricEnrollment.value = false
    }
}

class BiometricCipherProvider {
    // Keep alias unchanged for compatibility with existing enrolled users.
    private val alias = "password_vault_biometric_unlock"
    private val keyStoreProvider = "AndroidKeyStore"

    fun createEncryptionCipher(): Result<Cipher> = runCatching {
        try {
            initEncryptionCipher()
        } catch (error: KeyPermanentlyInvalidatedException) {
            deleteKey()
            initEncryptionCipher()
        }
    }

    fun createDecryptionCipher(ivBase64: String): Result<Cipher> = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getExistingKey() ?: throw KeyPermanentlyInvalidatedException(),
            GCMParameterSpec(128, Base64.decode(ivBase64, Base64.NO_WRAP)),
        )
        cipher
    }

    fun deleteKey() {
        runCatching {
            val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        }
    }

    fun biometricAvailability(context: Context): Int {
        return BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        )
    }

    fun verifierPlaintext(): ByteArray = "password-vault-biometric-verifier"
        .toByteArray(StandardCharsets.UTF_8)

    private fun initEncryptionCipher(): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher
    }

    private fun getExistingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }
        return (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun getOrCreateKey(): SecretKey {
        getExistingKey()?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        generator.init(builder.build())
        return generator.generateKey()
    }
}
