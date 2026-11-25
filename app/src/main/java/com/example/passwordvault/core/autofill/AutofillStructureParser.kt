package com.example.passwordvault.core.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import com.example.passwordvault.domain.model.CredentialRecord
import java.net.URI
import java.util.Locale

data class ParsedAutofillRequest(
    val usernameId: AutofillId?,
    val passwordId: AutofillId?,
    val webDomain: String?,
    val packageName: String?,
) {
    val hasTargets: Boolean
        get() = usernameId != null || passwordId != null
}

object AutofillStructureParser {
    fun parse(structure: AssistStructure): ParsedAutofillRequest {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var webDomain: String? = null
        var packageName: String? = structure.activityComponent?.packageName

        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode ?: continue
            traverse(root) { node ->
                if (webDomain == null && !node.webDomain.isNullOrBlank()) {
                    webDomain = node.webDomain
                }
                if (packageName == null && !node.idPackage.isNullOrBlank()) {
                    packageName = node.idPackage
                }

                val autofillId = node.autofillId ?: return@traverse
                if (node.autofillType == View.AUTOFILL_TYPE_NONE) return@traverse

                val hints = node.autofillHints?.map { it.lowercase(Locale.US) }.orEmpty()
                val htmlAttrs = node.htmlInfo?.attributes
                    ?.associate { it.first.lowercase(Locale.US) to it.second.lowercase(Locale.US) }
                    .orEmpty()
                val hintText = listOfNotNull(
                    node.hint,
                    node.idEntry,
                    node.contentDescription?.toString(),
                    htmlAttrs["name"],
                    htmlAttrs["id"],
                    htmlAttrs["autocomplete"],
                    htmlAttrs["placeholder"],
                ).joinToString(" ").lowercase(Locale.US)

                val isPasswordInput = isPasswordNode(node, hints, htmlAttrs, hintText)
                val isUsernameInput = !isPasswordInput && isUsernameNode(node, hints, htmlAttrs, hintText)

                if (isPasswordInput && passwordId == null) {
                    passwordId = autofillId
                } else if (isUsernameInput && usernameId == null) {
                    usernameId = autofillId
                }
            }
        }

        return ParsedAutofillRequest(
            usernameId = usernameId,
            passwordId = passwordId,
            webDomain = webDomain,
            packageName = packageName,
        )
    }

    fun rankCredentials(
        credentials: List<CredentialRecord>,
        webDomain: String?,
        packageName: String?,
    ): List<CredentialRecord> {
        val domainToken = normalizeHost(webDomain)
        val pkgTokens = packageName
            ?.lowercase(Locale.US)
            ?.split('.')
            ?.filter { it.length >= 3 && it !in IGNORED_PACKAGE_TOKENS }
            .orEmpty()

        return credentials.sortedWith(
            compareByDescending<CredentialRecord> {
                matchScore(it, domainToken, pkgTokens)
            }.thenByDescending { it.isFavorite }
                .thenByDescending { it.updatedAt },
        )
    }

    fun matchScore(
        credential: CredentialRecord,
        webDomain: String?,
        packageName: String?,
    ): Int {
        val domainToken = normalizeHost(webDomain)
        val pkgTokens = packageName
            ?.lowercase(Locale.US)
            ?.split('.')
            ?.filter { it.length >= 3 && it !in IGNORED_PACKAGE_TOKENS }
            .orEmpty()
        return matchScore(credential, domainToken, pkgTokens)
    }

    private fun matchScore(
        credential: CredentialRecord,
        domainToken: String?,
        pkgTokens: List<String>,
    ): Int {
        val credHost = normalizeHost(credential.url)
        val titleLower = credential.title.lowercase(Locale.US)
        val urlLower = credential.url.orEmpty().lowercase(Locale.US)

        if (!domainToken.isNullOrBlank()) {
            if (credHost == domainToken) return 100
            if (credHost != null && (credHost.endsWith(".$domainToken") || domainToken.endsWith(".$credHost"))) {
                return 80
            }
            val baseDomain = domainToken.substringBeforeLast('.').substringAfterLast('.')
            if (baseDomain.length >= 3 && (titleLower.contains(baseDomain) || urlLower.contains(baseDomain))) {
                return 60
            }
        }

        for (token in pkgTokens) {
            if (titleLower.contains(token) || urlLower.contains(token)) {
                return 45
            }
        }
        return 0
    }

    internal fun normalizeHost(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        val host = runCatching { URI(withScheme).host }.getOrNull()
            ?.lowercase(Locale.US)
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
        return host ?: trimmed.lowercase(Locale.US).removePrefix("www.").substringBefore('/')
    }

    private fun isPasswordNode(
        node: AssistStructure.ViewNode,
        hints: List<String>,
        htmlAttrs: Map<String, String>,
        hintText: String,
    ): Boolean {
        if (hints.any { it.contains("password") }) return true
        if (htmlAttrs["type"] == "password") return true
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        val isPasswordVariation = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        if ((node.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT && isPasswordVariation) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            (node.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER &&
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            return true
        }
        return hintText.contains("password") || hintText.contains("passwort") || hintText.contains("парол")
    }

    private fun isUsernameNode(
        node: AssistStructure.ViewNode,
        hints: List<String>,
        htmlAttrs: Map<String, String>,
        hintText: String,
    ): Boolean {
        if (hints.any {
                it.contains("username") || it.contains("email") || it.contains("login") || it.contains("phone")
            }
        ) {
            return true
        }
        if (htmlAttrs["type"] == "email") return true
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        if ((node.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
            (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
        ) {
            return true
        }
        val editable = node.isFocusable && (node.inputType != 0 || htmlAttrs["type"] in setOf("text", "email", "tel"))
        if (!editable) return false
        return USERNAME_KEYWORDS.any { hintText.contains(it) }
    }

    private fun traverse(
        node: AssistStructure.ViewNode,
        visitor: (AssistStructure.ViewNode) -> Unit,
    ) {
        visitor(node)
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { traverse(it, visitor) }
        }
    }

    private val USERNAME_KEYWORDS = listOf(
        "username",
        "user_name",
        "userid",
        "email",
        "e-mail",
        "login",
        "account",
        "логин",
        "почт",
    )

    private val IGNORED_PACKAGE_TOKENS = setOf(
        "com",
        "org",
        "net",
        "app",
        "android",
        "mobile",
        "client",
        "browser",
        "chrome",
        "firefox",
        "google",
    )
}
