package com.example.passwordvault

import com.example.passwordvault.core.crypto.TotpGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpGeneratorTest {
    // RFC 6238 SHA1 secret "12345678901234567890" encoded in Base32
    private val rfcSecret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    @Test
    fun generatesRfc6238Sha1Vector() {
        val result = TotpGenerator.generate(rfcSecret, epochSeconds = 59L)
        assertNotNull(result)
        assertEquals("287082", result?.code)
        assertEquals("287 082", result?.formatted)
        assertEquals(1, result?.remainingSeconds)
    }

    @Test
    fun parsesFormattedBase32AndOtpauthUri() {
        val spaced = "gezd gnbv-gy3t qojq gezd gnbv gy3t qojq"
        assertEquals(
            "287082",
            TotpGenerator.generate(spaced, epochSeconds = 59L)?.code,
        )

        val uri = "otpauth://totp/NutVault:user@example.com?secret=$rfcSecret&issuer=NutVault&digits=6&period=30"
        assertTrue(TotpGenerator.isValidInput(uri))
        assertEquals("287082", TotpGenerator.generate(uri, epochSeconds = 59L)?.code)
    }

    @Test
    fun rejectsInvalidSecrets() {
        assertFalse(TotpGenerator.isValidInput(""))
        assertFalse(TotpGenerator.isValidInput("1890!@#"))
        assertNull(TotpGenerator.generate("not-valid-base32!"))
    }
}
