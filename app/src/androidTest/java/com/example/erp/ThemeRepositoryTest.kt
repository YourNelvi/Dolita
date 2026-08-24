package com.example.erp

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.erp.data.ThemeMode
import com.example.erp.data.ThemePreferencesImpl
import com.example.erp.data.ThemePreferences
import com.example.erp.data.ThemeRepository
import com.example.erp.data.ThemeRepositoryImpl
import com.example.erp.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRepositoryTest {

    private fun createTestRepo(): ThemeRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_theme_prefs_${System.nanoTime()}", Context.MODE_PRIVATE)
        val themePrefs = ThemePreferencesImpl(context, prefs)
        return ThemeRepositoryImpl(themePrefs)
    }

    @Test
    fun `default values are returned when no preferences are set`() = runBlocking {
        val repo = createTestRepo()

        assertEquals(AppTheme.AZUL_BANCARIO, repo.theme.first())
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        assertEquals(false, repo.dynamicColorEnabled.first())
    }

    @Test
    fun `setTheme updates the theme flow`() = runBlocking {
        val repo = createTestRepo()

        repo.setTheme(AppTheme.AZUL_BANCARIO)
        assertEquals(AppTheme.AZUL_BANCARIO, repo.theme.first())

        repo.setTheme(AppTheme.VIOLETA_ELEGANTE)
        assertEquals(AppTheme.VIOLETA_ELEGANTE, repo.theme.first())
    }

    @Test
    fun `setThemeMode updates the theme mode flow`() = runBlocking {
        val repo = createTestRepo()

        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.themeMode.first())

        repo.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repo.themeMode.first())

        repo.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
    }

    @Test
    fun `setDynamicColorEnabled updates the flow`() = runBlocking {
        val repo = createTestRepo()

        repo.setDynamicColorEnabled(false)
        assertEquals(false, repo.dynamicColorEnabled.first())

        repo.setDynamicColorEnabled(true)
        assertEquals(true, repo.dynamicColorEnabled.first())
    }
}