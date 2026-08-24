package com.example.erp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// CompositionLocal para saber si el tema actual es oscuro
val LocalIsDarkTheme = staticCompositionLocalOf { false }

enum class AppTheme(
    val lightPrimary: Long, val lightSecondary: Long, val lightTertiary: Long,
    val lightOnPrimary: Long, val lightOnSecondary: Long, val lightOnTertiary: Long,
    val darkPrimary: Long, val darkSecondary: Long, val darkTertiary: Long,
    val darkOnPrimary: Long, val darkOnSecondary: Long, val darkOnTertiary: Long,
    val displayName: String
) {
    DOLAR_VERDE(
        0xFF0F7B46, 0xFF2E7D32, 0xFFFFA000,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFF6FCF97, 0xFFA5D6A7, 0xFFFFE082,
        0xFF000000, 0xFF000000, 0xFF000000,
        "Dólar Verde"
    ),
    AZUL_BANCARIO(
        0xFF1565C0, 0xFF1976D2, 0xFFFF8F00,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFF64B5F6, 0xFF90CAF9, 0xFFFFD54F,
        0xFF000000, 0xFF000000, 0xFF000000,
        "Azul Bancario"
    ),
    VIOLETA_ELEGANTE(
        0xFF6200EE, 0xFF7B1FA2, 0xFFFF6F00,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFFBB86FC, 0xFFCE93D8, 0xFFFFD180,
        0xFF000000, 0xFF000000, 0xFF000000,
        "Violeta Elegante"
    ),
    ALTO_CONTRASTE(
        0xFF00C853, 0xFF009624, 0xFFFFC107,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFF00E676, 0xFF00C853, 0xFFFFEA00,
        0xFF000000, 0xFF000000, 0xFF000000,
        "Alto Contraste"
    ),
    GRIS_NEUTRO(
        0xFF9E9E9E, 0xFFB0B0B0, 0xFF757575,
        0xFF000000, 0xFF000000, 0xFF000000,
        0xFF616161, 0xFF757575, 0xFF9E9E9E,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
        "Gris Neutro"
    ),
    ROJO_DEGRADADO(
        0xFFFF5252, 0xFFFF1744, 0xFFFF6D00,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFFB71C1C, 0xFF7F0000, 0xFFD84315,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        "Rojo Degradado"
    )
}

private fun lightScheme(t: AppTheme) = lightColorScheme(
    primary = Color(t.lightPrimary),
    onPrimary = Color(t.lightOnPrimary),
    secondary = Color(t.lightSecondary),
    onSecondary = Color(t.lightOnSecondary),
    tertiary = Color(t.lightTertiary),
    onTertiary = Color(t.lightOnTertiary),
    surface = Color.White,
    onSurface = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    error = Color(0xFFB00020),
    onError = Color.White
)

private fun darkScheme(t: AppTheme) = darkColorScheme(
    primary = Color(t.darkPrimary),
    onPrimary = Color(t.darkOnPrimary),
    secondary = Color(t.darkSecondary),
    onSecondary = Color(t.darkOnSecondary),
    tertiary = Color(t.darkTertiary),
    onTertiary = Color(t.darkOnTertiary),
    surface = Color(0xFF121212),
    onSurface = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun ERPTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = true,
    theme: AppTheme = AppTheme.DOLAR_VERDE,
    content: @Composable () -> Unit
) {
    val effectiveDarkTheme = darkTheme ?: isSystemInDarkTheme()
    val selectedTheme = remember(theme) { mutableStateOf(theme) }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDarkTheme -> darkScheme(selectedTheme.value)
        else -> lightScheme(selectedTheme.value)
    }

    val isDark = effectiveDarkTheme
    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun isDarkTheme(): Boolean = LocalIsDarkTheme.current