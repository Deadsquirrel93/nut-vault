package com.example.passwordvault.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.passwordvault.domain.model.PasswordGeneratorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class AutoLockTimeout(val key: String, val durationMillis: Long) {
    IMMEDIATE("immediate", 0L),
    SECONDS_30("30s", 30_000L),
    MINUTE_1("1m", 60_000L),
    MINUTES_5("5m", 300_000L),
    ;

    companion object {
        fun fromKey(key: String?): AutoLockTimeout =
            entries.firstOrNull { it.key == key } ?: SECONDS_30
    }
}

enum class VaultSortOption(val key: String) {
    UPDATED_DESC("updated_desc"),
    TITLE_ASC("title_asc"),
    FAVORITES_FIRST("favorites_first"),
    ;

    companion object {
        fun fromKey(key: String?): VaultSortOption =
            entries.firstOrNull { it.key == key } ?: UPDATED_DESC
    }
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, null)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _autoLockTimeout = MutableStateFlow(
        AutoLockTimeout.fromKey(prefs.getString(KEY_AUTO_LOCK_TIMEOUT, null)),
    )
    val autoLockTimeout: StateFlow<AutoLockTimeout> = _autoLockTimeout.asStateFlow()

    private val _sortOption = MutableStateFlow(
        VaultSortOption.fromKey(prefs.getString(KEY_SORT_OPTION, null)),
    )
    val sortOption: StateFlow<VaultSortOption> = _sortOption.asStateFlow()

    private val _generatorOptions = MutableStateFlow(readGeneratorOptions())
    val generatorOptions: StateFlow<PasswordGeneratorOptions> = _generatorOptions.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.key).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        _autoLockTimeout.value = timeout
        prefs.edit().putString(KEY_AUTO_LOCK_TIMEOUT, timeout.key).apply()
    }

    fun setSortOption(option: VaultSortOption) {
        _sortOption.value = option
        prefs.edit().putString(KEY_SORT_OPTION, option.key).apply()
    }

    fun setGeneratorOptions(options: PasswordGeneratorOptions) {
        _generatorOptions.value = options
        prefs.edit()
            .putInt(KEY_GEN_LENGTH, options.length)
            .putBoolean(KEY_GEN_UPPER, options.includeUppercase)
            .putBoolean(KEY_GEN_LOWER, options.includeLowercase)
            .putBoolean(KEY_GEN_DIGITS, options.includeDigits)
            .putBoolean(KEY_GEN_SYMBOLS, options.includeSymbols)
            .apply()
    }

    private fun readGeneratorOptions(): PasswordGeneratorOptions {
        val upper = prefs.getBoolean(KEY_GEN_UPPER, true)
        val lower = prefs.getBoolean(KEY_GEN_LOWER, true)
        val digits = prefs.getBoolean(KEY_GEN_DIGITS, true)
        val symbols = prefs.getBoolean(KEY_GEN_SYMBOLS, true)
        val anySelected = upper || lower || digits || symbols
        return PasswordGeneratorOptions(
            length = prefs.getInt(KEY_GEN_LENGTH, 20).coerceIn(8, 48),
            includeUppercase = if (anySelected) upper else true,
            includeLowercase = if (anySelected) lower else true,
            includeDigits = if (anySelected) digits else true,
            includeSymbols = if (anySelected) symbols else true,
        )
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_GEN_LENGTH = "gen_length"
        private const val KEY_GEN_UPPER = "gen_upper"
        private const val KEY_GEN_LOWER = "gen_lower"
        private const val KEY_GEN_DIGITS = "gen_digits"
        private const val KEY_GEN_SYMBOLS = "gen_symbols"
    }
}
