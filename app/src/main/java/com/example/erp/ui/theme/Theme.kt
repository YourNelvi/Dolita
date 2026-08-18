package com.example.erp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(
    val lightPrimary: Long, val lightSecondary: Long, val lightTertiary: Long,
    val darkPrimary: Long, val darkSecondary: Long, val darkTertiary: Long,
    val displayName: String
) {
    DOLAR_VERDE(
        0xFF0F7B46, 0xFF2E7D32, 0xFFFFA000,
        0xFF6FCF97, 0xFFA5D6A7, 0xFFFFE082,
        "Dólar Verde"
    ),
    AZUL_BANCARIO(
        0xFF1565C0, 0xFF1976D2, 0xFFFF8F00,
        0xFF64B5F6, 0xFF90CAF9, 0xFFFFD54F,
        "Azul Bancario"
    ),
    VIOLETA_ELEGANTE(
        0xFF6200EE, 0xFF7B1FA2, 0xFFFF6F00,
        0xFFBB86FC, 0xFFCE93D8, 0xFFFFD180,
        "Violeta Elegante"
    ),
    ALTO_CONTRASTE(
        0xFF00C853, 0xFF009624, 0xFFFFC107,
        0xFF00E676, 0xFF00C853, 0xFFFFEA00,
        "Alto Contraste"
    )
}

private fun lightScheme(t: AppTheme) = lightColorScheme(
    primary = Color(t.lightPrimary),
    secondary = Color(t.lightSecondary),
    tertiary = Color(t.lightTertiary)
)

private fun darkScheme(t: AppTheme) = darkColorScheme(
    primary = Color(t.darkPrimary),
    secondary = Color(t.darkSecondary),
    tertiary = Color(t.darkTertiary)
)

@Composable
fun ERPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    theme: AppTheme = AppTheme.DOLAR_VERDE,
    content: @Composable () -> Unit
) {
    val selectedTheme = remember { mutableStateOf(theme) }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme(selectedTheme.value)
        else -> lightScheme(selectedTheme.value)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}