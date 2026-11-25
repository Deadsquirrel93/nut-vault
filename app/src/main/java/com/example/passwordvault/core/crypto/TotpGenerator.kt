package com.example.passwordvault.core.crypto

import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

data class TotpConfig(
    val secret: String,
    val digits: Int = 6,
    val periodSeconds: Int = 30,
    val algorithm: String = "HmacSHA1",
)

data class TotpCode(
    val code: String,
    val remainingSeconds: Int,
    val periodSeconds: Int,
) {
    val formatted: String
        get() = if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
}

object TotpGenerator {
    fun parseInput(rawInput: String): TotpConfig? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
            val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
            val params = parseQueryParams(uri.rawQuery)
            val secret = params["secret"]?.trim().orEmpty()
            if (decodeBase32(secret) == null) return null
            val digits = params["digits"]?.toIntOrNull()?.takeIf { it in 6..8 } ?: 6
            val period = params["period"]?.toIntOrNull()?.takeIf { it in 10..120 } ?: 30
            val algorithm = when (params["algorithm"]?.uppercase(Locale.US)) {
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> "HmacSHA1"
            }
            return TotpConfig(
                secret = normalizeSecret(secret),
                digits = digits,
                periodSeconds = period,
                algorithm = algorithm,
            )
        }

        val normalized = normalizeSecret(trimmed)
        if (decodeBase32(normalized) == null) return null
        return TotpConfig(secret = normalized)
    }

    fun isValidInput(rawInput: String): Boolean = parseInput(rawInput) != null

    fun generate(
        rawInput: String,
        epochSeconds: Long = System.currentTimeMillis() / 1000L,
    ): TotpCode? {
        val config = parseInput(rawInput) ?: return null
        val keyBytes = decodeBase32(config.secret) ?: return null
        if (keyBytes.isEmpty()) return null

        val counter = epochSeconds / config.periodSeconds
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance(config.algorithm).apply {
            init(SecretKeySpec(keyBytes, config.algorithm))
        }
        val hash = mac.doFinal(counterBytes)
        val offset = hash[hash.lastIndex].toInt() and 0x0F
        val binary =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val modulo = 10.0.pow(config.digits.toDouble()).toInt()
        val otp = (binary % modulo).toString().padStart(config.digits, '0')
        val elapsed = (epochSeconds % config.periodSeconds).toInt()
        val remaining = config.periodSeconds - elapsed
        return TotpCode(
            code = otp,
            remainingSeconds = remaining,
            periodSeconds = config.periodSeconds,
        )
    }

    private fun parseQueryParams(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return buildMap {
            rawQuery.split('&').forEach { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.size == 2) {
                    val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()).lowercase(Locale.US)
                    val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
                    put(key, value)
                }
            }
        }
    }

    internal fun normalizeSecret(secret: String): String =
        secret.uppercase(Locale.US).replace(Regex("[\\s-]"), "")

    internal fun decodeBase32(input: String): ByteArray? {
        val clean = normalizeSecret(input).trimEnd('=')
        if (clean.isEmpty()) return null

        val out = ByteArray((clean.length * 5) / 8)
        var buffer = 0
        var bitsLeft = 0
        var count = 0

        for (char in clean) {
            val value = when (char) {
                in 'A'..'Z' -> char - 'A'
                in '2'..'7' -> char - '2' + 26
                else -> return null
            }
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out[count++] = ((buffer shr (bitsLeft - 8)) and 0xFF).toByte()
                bitsLeft -= 8
            }
        }
        return if (count == 0) null else out.copyOf(count)
    }
}
