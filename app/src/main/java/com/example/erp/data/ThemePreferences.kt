package com.example.erp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.erp.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val SELECTED_THEME_KEY = "selected_theme"
private const val THEME_MODE_KEY = "theme_mode"
private const val DYNAMIC_COLOR_ENABLED_KEY = "dynamic_color_enabled"
private const val HIGH_PRECISION_KEY = "high_precision_enabled"
private const val PREFS_NAME = "theme_prefs"

/**
 * ThemeMode represents the app's theme mode preference.
 * 0 = System (follows system setting), 1 = Light, 2 = Dark
 */
enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromValue(value: Int): ThemeMode = values().firstOrNull { it.value == value } ?: SYSTEM
    }
}

interface ThemePreferences {
    val selectedTheme: Flow<String>
    val themeMode: Flow<ThemeMode>
    val dynamicColorEnabled: Flow<Boolean>
    val highPrecisionEnabled: Flow<Boolean>

    suspend fun setSelectedTheme(themeName: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setHighPrecisionEnabled(enabled: Boolean)
}

class ThemePreferencesImpl(
    private val context: Context,
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
) : ThemePreferences {

    private val _selectedTheme = MutableStateFlow(
        prefs.getString(SELECTED_THEME_KEY, AppTheme.AZUL_BANCARIO.name) ?: AppTheme.AZUL_BANCARIO.name
    )
    override val selectedTheme: Flow<String> = _selectedTheme.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromValue(prefs.getInt(THEME_MODE_KEY, ThemeMode.SYSTEM.value))
    )
    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(prefs.getBoolean(DYNAMIC_COLOR_ENABLED_KEY, false))
    override val dynamicColorEnabled: Flow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _highPrecisionEnabled = MutableStateFlow(prefs.getBoolean(HIGH_PRECISION_KEY, false))
    override val highPrecisionEnabled: Flow<Boolean> = _highPrecisionEnabled.asStateFlow()

    override suspend fun setSelectedTheme(themeName: String) {
        prefs.edit().putString(SELECTED_THEME_KEY, themeName).apply()
        _selectedTheme.value = themeName
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(THEME_MODE_KEY, mode.value).apply()
        _themeMode.value = mode
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(DYNAMIC_COLOR_ENABLED_KEY, enabled).apply()
        _dynamicColorEnabled.value = enabled
    }

    override suspend fun setHighPrecisionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(HIGH_PRECISION_KEY, enabled).apply()
        _highPrecisionEnabled.value = enabled
    }
}