package com.example.passwordvault

import com.example.passwordvault.core.auth.SessionManager
import com.example.passwordvault.core.autofill.AutofillStructureParser
import com.example.passwordvault.core.password.PasswordHealthAnalyzer
import com.example.passwordvault.domain.model.CredentialRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordHealthAndSessionTest {
    private val analyzer = PasswordHealthAnalyzer()

    @Test
    fun analyzerDetectsWeakReusedAndTotpEntries() {
        val records = listOf(
            sampleRecord(1, "Site A", "abc123", totp = "JBSWY3DPEHPK3PXP"),
            sampleRecord(2, "Site B", "abc123"),
            sampleRecord(3, "Site C", "7Q!nR2@fT9#kL4%zX8^mC6&v"),
        )

        val report = analyzer.analyze(records)
        assertEquals(3, report.totalCount)
        assertEquals(2, report.weakCredentials.size)
        assertEquals(1, report.reusedGroups.size)
        assertEquals(2, report.reusedGroups.first().credentials.size)
        assertEquals(1, report.totpEnabledCount)
        assertTrue(report.scorePercent in 1..60)
    }

    @Test
    fun sessionManagerRespectsAutoLockTimeout() = runTest {
        var now = 1_000L
        val manager = SessionManager(clockMillis = { now })

        manager.unlock()
        assertTrue(manager.isUnlocked.value)

        // Background with 30s timeout and return after 10s -> stays unlocked
        manager.onAppBackgrounded(timeoutMillis = 30_000L, scope = backgroundScope)
        now += 10_000L
        manager.onAppForegrounded(timeoutMillis = 30_000L)
        assertTrue(manager.isUnlocked.value)

        // Background with 30s timeout and let coroutine timer expire -> locks
        manager.onAppBackgrounded(timeoutMillis = 30_000L, scope = backgroundScope)
        advanceTimeBy(31_000L)
        assertFalse(manager.isUnlocked.value)

        // Immediate timeout locks right away
        manager.unlock()
        manager.onAppBackgrounded(timeoutMillis = 0L, scope = backgroundScope)
        assertFalse(manager.isUnlocked.value)
    }

    @Test
    fun autofillRankingPrioritizesExactDomainMatch() {
        val github = sampleRecord(1, "GitHub", "secret", url = "https://github.com/login")
        val gitlab = sampleRecord(2, "GitLab", "secret", url = "https://gitlab.com")
        val bank = sampleRecord(3, "MyBank", "secret", url = "https://bank.example.org")

        val ranked = AutofillStructureParser.rankCredentials(
            credentials = listOf(bank, gitlab, github),
            webDomain = "www.github.com",
            packageName = "com.android.chrome",
        )
        assertEquals(github.id, ranked.first().id)
    }

    private fun sampleRecord(
        id: Long,
        title: String,
        password: String,
        url: String? = null,
        totp: String? = null,
    ) = CredentialRecord(
        id = id,
        folderId = null,
        title = title,
        url = url,
        login = "user@example.com",
        password = password,
        totpSecret = totp,
        createdAt = 100L,
        updatedAt = 200L,
    )
}
