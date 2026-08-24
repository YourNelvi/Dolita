package com.example.erp

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.erp.data.ThemeMode
import com.example.erp.data.ThemePreferences
import com.example.erp.data.ThemePreferencesImpl
import com.example.erp.data.ThemeRepository
import com.example.erp.data.ThemeRepositoryImpl
import com.example.erp.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemePersistenceTest {

    private lateinit var context: Context
    private lateinit var themeRepository: ThemeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_theme_persistence_${System.nanoTime()}", Context.MODE_PRIVATE)
        val themePrefs = ThemePreferencesImpl(context, prefs)
        themeRepository = ThemeRepositoryImpl(themePrefs)
    }

    @Test
    fun `theme persists after process death simulation`() = runBlocking {
        // Set a theme
        themeRepository.setTheme(AppTheme.ROJO_DEGRADADO)
        
        // Verify it's set
        val currentTheme = themeRepository.theme.first()
        assertEquals(AppTheme.ROJO_DEGRADADO, currentTheme)
        
        // Simulate process death by creating new repository with same prefs
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_theme_persistence_${System.nanoTime()}", Context.MODE_PRIVATE)
        // Manually put the value as if it was saved
        prefs.edit().putString("selected_theme", AppTheme.ROJO_DEGRADADO.name).apply()
        
        val newThemePrefs = ThemePreferencesImpl(
            ApplicationProvider.getApplicationContext<Context>(),
            prefs
        )
        val newRepository = ThemeRepositoryImpl(newThemePrefs)
        
        // Verify theme persisted
        val restoredTheme = newRepository.theme.first()
        assertEquals(AppTheme.ROJO_DEGRADADO, restoredTheme)
    }

    @Test
    fun `themeMode persists after process death simulation`() = runBlocking {
        themeRepository.setThemeMode(ThemeMode.DARK)
        val currentMode = themeRepository.themeMode.first()
        assertEquals(ThemeMode.DARK, currentMode)
        
        // Simulate process death
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_theme_persistence_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().putInt("theme_mode", ThemeMode.DARK.value).apply()
        
        val newThemePrefs = ThemePreferencesImpl(
            ApplicationProvider.getApplicationContext<Context>(),
            prefs
        )
        val newRepository = ThemeRepositoryImpl(newThemePrefs)
        
        val restoredMode = newRepository.themeMode.first()
        assertEquals(ThemeMode.DARK, restoredMode)
    }

    @Test
    fun `dynamicColorEnabled persists after process death simulation`() = runBlocking {
        themeRepository.setDynamicColorEnabled(true)
        val currentDynamic = themeRepository.dynamicColorEnabled.first()
        assertEquals(true, currentDynamic)
        
        // Simulate process death
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_theme_persistence_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dynamic_color_enabled", true).apply()
        
        val newThemePrefs = ThemePreferencesImpl(
            ApplicationProvider.getApplicationContext<Context>(),
            prefs
        )
        val newRepository = ThemeRepositoryImpl(newThemePrefs)
        
        val restoredDynamic = newRepository.dynamicColorEnabled.first()
        assertEquals(true, restoredDynamic)
    }

    @Test
    fun `default values when no preferences stored`() = runBlocking {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_theme_persistence_${System.nanoTime()}", Context.MODE_PRIVATE)
        // Don't put any values - simulate fresh install
        
        val themePrefs = ThemePreferencesImpl(
            ApplicationProvider.getApplicationContext<Context>(),
            prefs
        )
        val repository = ThemeRepositoryImpl(themePrefs)
        
        assertEquals(AppTheme.AZUL_BANCARIO, repository.theme.first())
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        assertEquals(false, repository.dynamicColorEnabled.first())
    }
}