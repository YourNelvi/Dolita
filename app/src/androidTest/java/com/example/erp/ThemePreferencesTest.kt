package com.example.erp

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.erp.data.ThemeMode
import com.example.erp.data.ThemePreferences
import com.example.erp.data.ThemePreferencesImpl
import com.example.erp.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesTest {

    private fun createTestPrefs(): ThemePreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use a unique prefs name per test to avoid interference
        val prefs = context.getSharedPreferences("test_theme_prefs_${System.nanoTime()}", Context.MODE_PRIVATE)
        return ThemePreferencesImpl(context, prefs)
    }

    @Test
    fun `default values are returned when no preferences are set`() = runBlocking {
        val prefs = createTestPrefs()

        assertEquals(AppTheme.AZUL_BANCARIO.name, prefs.selectedTheme.first())
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.first())
        assertEquals(false, prefs.dynamicColorEnabled.first())
    }

    @Test
    fun `setSelectedTheme updates the theme flow`() = runBlocking {
        val prefs = createTestPrefs()

        prefs.setSelectedTheme(AppTheme.AZUL_BANCARIO.name)

        assertEquals(AppTheme.AZUL_BANCARIO.name, prefs.selectedTheme.first())
    }

    @Test
    fun `setThemeMode updates the theme mode flow`() = runBlocking {
        val prefs = createTestPrefs()

        prefs.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, prefs.themeMode.first())

        prefs.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, prefs.themeMode.first())

        prefs.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.first())
    }

    @Test
    fun `setDynamicColorEnabled updates the flow`() = runBlocking {
        val prefs = createTestPrefs()

        prefs.setDynamicColorEnabled(false)
        assertEquals(false, prefs.dynamicColorEnabled.first())

        prefs.setDynamicColorEnabled(true)
        assertEquals(true, prefs.dynamicColorEnabled.first())
    }

    @Test
    fun `ThemeMode fromValue returns correct enum`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(0))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromValue(1))
        assertEquals(ThemeMode.DARK, ThemeMode.fromValue(2))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(99)) // invalid falls back to SYSTEM
    }
}