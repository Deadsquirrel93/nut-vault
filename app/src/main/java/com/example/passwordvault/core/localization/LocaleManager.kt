package com.example.passwordvault.core.localization

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocaleManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("locale_settings", Context.MODE_PRIVATE)

    private val _languageCode = MutableStateFlow<String?>(readCurrentLanguage())
    val languageCode: StateFlow<String?> = _languageCode.asStateFlow()

    init {
        val stored = prefs.getString(KEY_LANGUAGE_CODE, null)
        if (stored != null && stored != SYSTEM_SENTINEL) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(stored))
        }
        refresh()
    }

    fun refresh() {
        _languageCode.value = readCurrentLanguage()
    }

    fun setLanguage(languageCode: String?) {
        _languageCode.value = languageCode
        if (languageCode == null) {
            prefs.edit().putString(KEY_LANGUAGE_CODE, SYSTEM_SENTINEL).apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            prefs.edit().putString(KEY_LANGUAGE_CODE, languageCode).apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
    }

    private fun readCurrentLanguage(): String? {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            return appLocales[0]?.language
        }
        val stored = prefs.getString(KEY_LANGUAGE_CODE, null)
        return when (stored) {
            null, SYSTEM_SENTINEL -> null
            else -> stored
        }
    }

    companion object {
        const val ENGLISH = "en"
        const val RUSSIAN = "ru"

        private const val KEY_LANGUAGE_CODE = "language_code"
        private const val SYSTEM_SENTINEL = "system"
    }
}
