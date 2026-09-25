package com.example.erp.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp

// CompositionLocal para saber si el tema actual es oscuro
val LocalIsDarkTheme = staticCompositionLocalOf { false }

// The animated palette accent, published by ERPTheme. See accentColor().
val LocalAccentColor = staticCompositionLocalOf { Color(0xFF00E676) }

enum class AppTheme(
    val lightPrimary: Long, val lightSecondary: Long, val lightTertiary: Long,
    val lightOnPrimary: Long, val lightOnSecondary: Long, val lightOnTertiary: Long,
    val darkPrimary: Long, val darkSecondary: Long, val darkTertiary: Long,
    val darkOnPrimary: Long, val darkOnSecondary: Long, val darkOnTertiary: Long,
    /**
     * The accent role is deliberately NOT `primary`. `primary` is a fill color
     * that carries white text, so a palette is free to be dark; the accent
     * paints large numbers, rate labels and focus rings straight onto the card
     * surface, so it must clear 4.5:1 there. Reusing one token for both jobs
     * is what made ROJO_DEGRADADO and GRIS_NEUTRO unreadable.
     */
    val lightAccent: Long, val darkAccent: Long,
    val displayName: String
) {
    DOLAR_VERDE(
        0xFF0F7B46, 0xFF2E7D32, 0xFFFFA000,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFF6FCF97, 0xFFA5D6A7, 0xFFFFE082,
        0xFF000000, 0xFF000000, 0xFF000000,
        0xFF0F7B46, 0xFF6FCF97,
        "Dólar Verde"
    ),
    AZUL_BANCARIO(
        0xFF1565C0, 0xFF1976D2, 0xFFFF8F00,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFF64B5F6, 0xFF90CAF9, 0xFFFFD54F,
        0xFF000000, 0xFF000000, 0xFF000000,
        0xFF1565C0, 0xFF64B5F6,
        "Azul Bancario"
    ),
    VIOLETA_ELEGANTE(
        0xFF6200EE, 0xFF7B1FA2, 0xFFFF6F00,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFFBB86FC, 0xFFCE93D8, 0xFFFFD180,
        0xFF000000, 0xFF000000, 0xFF000000,
        0xFF6200EE, 0xFFBB86FC,
        "Violeta Elegante"
    ),
    ALTO_CONTRASTE(
        0xFF00C853, 0xFF009624, 0xFFFFC107,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFF00E676, 0xFF00C853, 0xFFFFEA00,
        0xFF000000, 0xFF000000, 0xFF000000,
        0xFF1B5E20, 0xFF00E676,
        "Alto Contraste"
    ),
    GRIS_NEUTRO(
        0xFF9E9E9E, 0xFFB0B0B0, 0xFF757575,
        0xFF000000, 0xFF000000, 0xFF000000,
        0xFF616161, 0xFF757575, 0xFF9E9E9E,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
        0xFF616161, 0xFFB0BEC5,
        "Gris Neutro"
    ),
    ROJO_DEGRADADO(
        0xFFFF5252, 0xFFFF1744, 0xFFFF6D00,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFFB71C1C, 0xFF7F0000, 0xFFD84315,
        0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000,
        0xFFD32F2F, 0xFFFF8A80,
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
    // Selected-state containers become accent-tinted instead of M3 tonal fills.
    primaryContainer = Color(t.darkPrimary).copy(alpha = 0.16f),
    onPrimaryContainer = Color(t.darkPrimary),
    secondary = Color(t.darkSecondary),
    onSecondary = Color(t.darkOnSecondary),
    tertiary = Color(t.darkTertiary),
    onTertiary = Color(t.darkOnTertiary),
    tertiaryContainer = FintechSurfaceCapsule,
    onTertiaryContainer = Color(t.darkTertiary),
    background = FintechBackground,
    onBackground = FintechOnSurface,
    surface = FintechSurface,
    onSurface = FintechOnSurface,
    surfaceVariant = FintechSurfaceCapsule,
    onSurfaceVariant = FintechOnSurfaceVariant,
    surfaceContainerLowest = FintechBackground,
    surfaceContainerLow = FintechBackground,
    surfaceContainer = FintechSurface,
    surfaceContainerHigh = FintechSurface,
    surfaceContainerHighest = FintechSurfaceCapsule,
    surfaceBright = FintechSurface,
    surfaceDim = FintechBackground,
    outline = FintechOnSurfaceVariant.copy(alpha = 0.55f),
    outlineVariant = FintechBorder,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

// --- Fintech surface helpers (shared visual tokens for cards, inputs and sheets) ---

/** Card/input container: fintech surface on dark, previous tonal card look on light. */
@Composable
fun cardContainerColor(): Color =
    if (isDarkTheme()) FintechSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

/** Hairline card border: rgba(255,255,255,0.07) on dark, soft dark outline on light. */
@Composable
fun cardBorderColor(): Color =
    if (isDarkTheme()) FintechBorder else Color.Black.copy(alpha = 0.10f)

/** 1dp hairline border stroke for fintech cards. */
@Composable
fun cardBorder(): BorderStroke = BorderStroke(1.dp, cardBorderColor())

/**
 * Accent for primary numeric values / focus rings.
 *
 * The transition between palettes is animated ONCE in `ERPTheme` and published
 * here. Every call site just reads a composition local — when each call site ran
 * its own `animateColorAsState`, every list row spun up its own animator and
 * recomposed the row on each frame of the transition.
 */
@Composable
fun accentColor(): Color = LocalAccentColor.current

/**
 * Semantic positive signal (up deltas, rising series, confirmations):
 * fintech green on dark, palette green on light — never follows the accent,
 * so "green = up" holds regardless of the selected theme.
 */
@Composable
fun positiveColor(): Color = if (isDarkTheme()) FintechAccentGreen else UpGreenLight

/** Quiet pill/badge fill (variation chips, active states): translucent white on dark, soft tint on light. */
@Composable
fun cardBadgeColor(): Color =
    if (isDarkTheme()) Color.White.copy(alpha = 0.07f)
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

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
    // One animator for the whole tree, instead of one per accent call site.
    // The accent comes from its own role, not from `primary`: a palette's
    // primary is a fill that carries white text and is allowed to be dark,
    // while the accent is read straight off the card surface.
    val accentTarget = if (isDark) Color(theme.darkAccent) else Color(theme.lightAccent)
    val animatedAccent by animateColorAsState(
        targetValue = accentTarget,
        animationSpec = tween(durationMillis = 350),
        label = "accentTransition"
    )
    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark,
        LocalAccentColor provides animatedAccent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun isDarkTheme(): Boolean = LocalIsDarkTheme.current