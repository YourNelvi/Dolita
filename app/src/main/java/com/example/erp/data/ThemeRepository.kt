package com.example.erp.data

import com.example.erp.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

interface ThemeRepository {
    val theme: Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)

    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    val dynamicColorEnabled: Flow<Boolean>
    suspend fun setDynamicColorEnabled(enabled: Boolean)

    val highPrecisionEnabled: Flow<Boolean>
    suspend fun setHighPrecisionEnabled(enabled: Boolean)
}

class ThemeRepositoryImpl(
    private val themePreferences: ThemePreferences
) : ThemeRepository {

    override val theme: Flow<AppTheme> = themePreferences.selectedTheme
        .map { name ->
            AppTheme.values().firstOrNull { it.name == name } ?: AppTheme.DOLAR_VERDE
        }
        .distinctUntilChanged()

    override suspend fun setTheme(theme: AppTheme) {
        themePreferences.setSelectedTheme(theme.name)
    }

    override val themeMode: Flow<ThemeMode> = themePreferences.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    override val dynamicColorEnabled: Flow<Boolean> = themePreferences.dynamicColorEnabled

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        themePreferences.setDynamicColorEnabled(enabled)
    }

    override val highPrecisionEnabled: Flow<Boolean> = themePreferences.highPrecisionEnabled

    override suspend fun setHighPrecisionEnabled(enabled: Boolean) {
        themePreferences.setHighPrecisionEnabled(enabled)
    }
}