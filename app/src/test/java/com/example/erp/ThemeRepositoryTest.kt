package com.example.erp

import androidx.datastore.preferences.testing.createTestPreferencesDataStore
import com.example.erp.data.ThemeMode
import com.example.erp.data.ThemePreferencesImpl
import com.example.erp.data.ThemeRepository
import com.example.erp.data.ThemeRepositoryImpl
import com.example.erp.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRepositoryTest {

    @Test
    fun `default values are returned when no preferences are set`() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferencesImpl(context)
        val repo = ThemeRepositoryImpl(prefs)

        assertEquals(AppTheme.DOLAR_VERDE, repo.theme.first())
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        assertEquals(true, repo.dynamicColorEnabled.first())
    }

    @Test
    fun `setTheme updates the theme flow`() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferencesImpl(context)
        val repo = ThemeRepositoryImpl(prefs)

        repo.setTheme(AppTheme.AZUL_BANCARIO)
        assertEquals(AppTheme.AZUL_BANCARIO, repo.theme.first())

        repo.setTheme(AppTheme.VIOLETA_ELEGANTE)
        assertEquals(AppTheme.VIOLETA_ELEGANTE, repo.theme.first())
    }

    @Test
    fun `setThemeMode updates the theme mode flow`() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferencesImpl(context)
        val repo = ThemeRepositoryImpl(prefs)

        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.themeMode.first())

        repo.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repo.themeMode.first())

        repo.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
    }

    @Test
    fun `setDynamicColorEnabled updates the flow`() = runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferencesImpl(context)
        val repo = ThemeRepositoryImpl(prefs)

        repo.setDynamicColorEnabled(false)
        assertEquals(false, repo.dynamicColorEnabled.first())

        repo.setDynamicColorEnabled(true)
        assertEquals(true, repo.dynamicColorEnabled.first())
    }
}