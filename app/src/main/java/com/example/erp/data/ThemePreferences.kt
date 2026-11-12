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
private const val CALCULATOR_SUGGESTIONS_KEY = "calculator_suggestions_enabled"
private const val SHARE_AS_IMAGE_KEY = "share_as_image_enabled"
private const val AMOLED_BLACK_KEY = "amoled_black_enabled"
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
    val calculatorSuggestionsEnabled: Flow<Boolean>
    val shareAsImageEnabled: Flow<Boolean>
    val amoledBlackEnabled: Flow<Boolean>

    suspend fun setSelectedTheme(themeName: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setHighPrecisionEnabled(enabled: Boolean)
    suspend fun setCalculatorSuggestionsEnabled(enabled: Boolean)
    suspend fun setShareAsImageEnabled(enabled: Boolean)
    suspend fun setAmoledBlackEnabled(enabled: Boolean)
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

    // The suggestions are an aid, not a correction: they start on and the
    // setting exists so a reader who does not want the extra chips can turn
    // them off. The stored choice wins from then on.
    private val _calculatorSuggestionsEnabled =
        MutableStateFlow(prefs.getBoolean(CALCULATOR_SUGGESTIONS_KEY, true))
    override val calculatorSuggestionsEnabled: Flow<Boolean> =
        _calculatorSuggestionsEnabled.asStateFlow()

    // Sending the card is what the app did before this switch existed, so the
    // default stays on. Turning it off is a deliberate request for text only,
    // not a missing value.
    private val _shareAsImageEnabled = MutableStateFlow(prefs.getBoolean(SHARE_AS_IMAGE_KEY, true))
    override val shareAsImageEnabled: Flow<Boolean> = _shareAsImageEnabled.asStateFlow()

    private val _amoledBlackEnabled = MutableStateFlow(prefs.getBoolean(AMOLED_BLACK_KEY, false))
    override val amoledBlackEnabled: Flow<Boolean> = _amoledBlackEnabled.asStateFlow()

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

    override suspend fun setCalculatorSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(CALCULATOR_SUGGESTIONS_KEY, enabled).apply()
        _calculatorSuggestionsEnabled.value = enabled
    }

    override suspend fun setShareAsImageEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(SHARE_AS_IMAGE_KEY, enabled).apply()
        _shareAsImageEnabled.value = enabled
    }

    override suspend fun setAmoledBlackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(AMOLED_BLACK_KEY, enabled).apply()
        _amoledBlackEnabled.value = enabled
    }
}