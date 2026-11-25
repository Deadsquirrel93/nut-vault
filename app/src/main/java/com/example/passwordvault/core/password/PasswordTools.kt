package com.example.passwordvault.core.password

import com.example.passwordvault.domain.model.CredentialRecord
import com.example.passwordvault.domain.model.PasswordGeneratorOptions
import com.example.passwordvault.domain.model.PasswordHealthReport
import com.example.passwordvault.domain.model.PasswordStrength
import com.example.passwordvault.domain.model.PasswordStrengthScore
import com.example.passwordvault.domain.model.ReusedPasswordGroup
import java.security.SecureRandom
import kotlin.math.log2

class PasswordGenerator(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    private val uppercase = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val lowercase = "abcdefghijkmnopqrstuvwxyz"
    private val digits = "23456789"
    private val symbols = "!@#\$%^&*()-_=+[]{};:,.?/|"

    fun generate(options: PasswordGeneratorOptions): String {
        val groups = buildList {
            if (options.includeUppercase) add(uppercase)
            if (options.includeLowercase) add(lowercase)
            if (options.includeDigits) add(digits)
            if (options.includeSymbols) add(symbols)
        }
        require(groups.isNotEmpty()) { "At least one character set must be selected." }

        val length = options.length.coerceIn(6, 64)
        val passwordChars = mutableListOf<Char>()
        groups.forEach { group ->
            passwordChars += group.random(secureRandom)
        }

        val fullAlphabet = groups.joinToString("")
        while (passwordChars.size < length) {
            passwordChars += fullAlphabet.random(secureRandom)
        }

        passwordChars.shuffle(secureRandom)
        return passwordChars.joinToString("")
    }

    private fun String.random(random: SecureRandom): Char = this[random.nextInt(length)]

    private fun <T> MutableList<T>.shuffle(random: SecureRandom) {
        for (i in indices.reversed()) {
            val index = random.nextInt(i + 1)
            val tmp = this[i]
            this[i] = this[index]
            this[index] = tmp
        }
    }
}

class PasswordStrengthEvaluator {
    fun evaluate(password: String): PasswordStrengthScore {
        if (password.isBlank()) {
            return PasswordStrengthScore(
                strength = PasswordStrength.WEAK,
                entropyBits = 0,
            )
        }

        var poolSize = 0
        if (password.any(Char::isUpperCase)) poolSize += 26
        if (password.any(Char::isLowerCase)) poolSize += 26
        if (password.any(Char::isDigit)) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32
        poolSize = poolSize.coerceAtLeast(1)

        val repeatedPenalty = password.zipWithNext().count { (a, b) -> a == b } * 2
        val sequencePenalty = detectSequencePenalty(password)
        val entropy = ((password.length * log2(poolSize.toDouble())) - repeatedPenalty - sequencePenalty)
            .coerceAtLeast(0.0)
            .toInt()

        val strength = when {
            entropy < 40 -> PasswordStrength.WEAK
            entropy < 64 -> PasswordStrength.MEDIUM
            entropy < 96 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }

        return PasswordStrengthScore(strength = strength, entropyBits = entropy)
    }

    private fun detectSequencePenalty(password: String): Int {
        if (password.length < 3) return 0
        var penalty = 0
        for (index in 0 until password.length - 2) {
            val a = password[index].code
            val b = password[index + 1].code
            val c = password[index + 2].code
            if ((b == a + 1 && c == b + 1) || (b == a - 1 && c == b - 1)) {
                penalty += 3
            }
        }
        return penalty
    }
}

class PasswordHealthAnalyzer(
    private val evaluator: PasswordStrengthEvaluator = PasswordStrengthEvaluator(),
) {
    fun analyze(credentials: List<CredentialRecord>): PasswordHealthReport {
        if (credentials.isEmpty()) {
            return PasswordHealthReport(
                totalCount = 0,
                scorePercent = 100,
                weakCredentials = emptyList(),
                mediumCredentials = emptyList(),
                reusedGroups = emptyList(),
                totpEnabledCount = 0,
            )
        }

        val scored = credentials.map { it to evaluator.evaluate(it.password) }
        val weak = scored.filter { it.second.strength == PasswordStrength.WEAK }.map { it.first }
        val medium = scored.filter { it.second.strength == PasswordStrength.MEDIUM }.map { it.first }
        val reusedGroups = credentials
            .filter { it.password.isNotBlank() }
            .groupBy { it.password }
            .values
            .filter { it.size > 1 }
            .sortedByDescending { it.size }
            .map { ReusedPasswordGroup(credentials = it) }

        val reusedIds = reusedGroups.flatMap { it.credentials }.map { it.id }.toSet()
        val points = scored.sumOf { (cred, score) ->
            val base = when (score.strength) {
                PasswordStrength.WEAK -> 20
                PasswordStrength.MEDIUM -> 65
                PasswordStrength.STRONG -> 92
                PasswordStrength.VERY_STRONG -> 100
            }
            if (cred.id in reusedIds) (base - 30).coerceAtLeast(10) else base
        }

        return PasswordHealthReport(
            totalCount = credentials.size,
            scorePercent = (points / credentials.size).coerceIn(0, 100),
            weakCredentials = weak,
            mediumCredentials = medium,
            reusedGroups = reusedGroups,
            totpEnabledCount = credentials.count { !it.totpSecret.isNullOrBlank() },
        )
    }
}
