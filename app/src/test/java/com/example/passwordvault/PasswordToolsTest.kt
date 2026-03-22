package com.example.passwordvault

import com.example.passwordvault.core.password.PasswordGenerator
import com.example.passwordvault.core.password.PasswordStrengthEvaluator
import com.example.passwordvault.domain.model.PasswordGeneratorOptions
import com.example.passwordvault.domain.model.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordToolsTest {
    private val generator = PasswordGenerator()
    private val evaluator = PasswordStrengthEvaluator()

    @Test
    fun generatedPasswordRespectsEnabledCharacterGroups() {
        val password = generator.generate(
            PasswordGeneratorOptions(
                length = 24,
                includeUppercase = true,
                includeLowercase = true,
                includeDigits = true,
                includeSymbols = true,
            ),
        )

        assertEquals(24, password.length)
        assertTrue(password.any(Char::isUpperCase))
        assertTrue(password.any(Char::isLowerCase))
        assertTrue(password.any(Char::isDigit))
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun generatedPasswordOmitsDisabledCharacterGroups() {
        val password = generator.generate(
            PasswordGeneratorOptions(
                length = 18,
                includeUppercase = false,
                includeLowercase = true,
                includeDigits = false,
                includeSymbols = false,
            ),
        )

        assertEquals(18, password.length)
        assertTrue(password.all(Char::isLowerCase))
        assertFalse(password.any(Char::isUpperCase))
        assertFalse(password.any(Char::isDigit))
        assertFalse(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun strengthEvaluatorFlagsWeakPasswords() {
        val score = evaluator.evaluate("abc123")

        assertEquals(PasswordStrength.WEAK, score.strength)
        assertTrue(score.entropyBits < 40)
    }

    @Test
    fun strengthEvaluatorFlagsVeryStrongPasswords() {
        val score = evaluator.evaluate("7Q!nR2@fT9#kL4%zX8^mC6&v")

        assertEquals(PasswordStrength.VERY_STRONG, score.strength)
        assertTrue(score.entropyBits >= 96)
    }
}
