package com.example.erp

import androidx.datastore.preferences.testing.createTestPreferencesDataStore
import com.example.erp.data.ThemeMode
import com.example.erp.data.ThemePreferences
import com.example.erp.data.ThemePreferencesImpl
import com.example.erp.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesTest {

    @Test
    fun `default values are returned when no preferences are set`() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dataStore = createTestPreferencesDataStore(context, "test_theme_preferences")
        val prefs = ThemePreferencesImpl(context)

        // We can't easily inject the test datastore into ThemePreferencesImpl
        // because it creates its own. Instead, we test the behavior indirectly
        // by using the actual implementation and checking defaults
        assertEquals(AppTheme.DOLAR_VERDE.name, prefs.selectedTheme.first())
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.first())
        assertEquals(true, prefs.dynamicColorEnabled.first())
    }

    @Test
    fun `setSelectedTheme updates the theme flow`() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferencesImpl(context)

        // Set a new theme
        prefs.setSelectedTheme(AppTheme.AZUL_BANCARIO.name)

        // Verify the flow emits the new value
        assertEquals(AppTheme.AZUL_BANCARIO.name, prefs.selectedTheme.first())
    }

    @Test
    fun `setThemeMode updates the theme mode flow`() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferencesImpl(context)

        prefs.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, prefs.themeMode.first())

        prefs.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, prefs.themeMode.first())

        prefs.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.first())
    }

    @Test
    fun `setDynamicColorEnabled updates the flow`() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferencesImpl(context)

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