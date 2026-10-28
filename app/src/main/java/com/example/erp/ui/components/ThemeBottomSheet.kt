package com.example.erp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.erp.R
import com.example.erp.data.ThemeMode
import com.example.erp.ui.versionLabel
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.cardBadgeColor
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.cardContainerColor

/**
 * Where the app's public links live.
 *
 * Both are constants rather than scattered literals so changing a handle is a
 * one-line edit in one place, and so a link can never be wrong in one spot and
 * right in another.
 */
object AppLinks {
    const val GITHUB = "https://github.com/YourNelvi/Dolita"
    const val INSTAGRAM = "https://www.instagram.com/yournelvi/"
}

/**
 * Open [url] outside the app.
 *
 * Guarded: a device with no browser, or one where the handler is missing,
 * throws rather than crashing. A dead link must not take the settings sheet
 * down with it.
 */
private fun openExternal(context: android.content.Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            )
        )
    }
}

/**
 * GitHub and Instagram, at the bottom of the settings sheet.
 *
 * Two rows and nothing else: these are the only destinations the app has, and
 * a section padded out with platforms it does not use would read as a directory
 * rather than a set of links.
 */
@Composable
private fun SocialLinksSection() {
    val context = LocalContext.current

    val destinos = listOf(
        SocialLink("GitHub", AppLinks.GITHUB, R.drawable.ic_github, tintToTheme = true),
        SocialLink("Instagram", AppLinks.INSTAGRAM, R.drawable.ic_instagram, tintToTheme = false)
    )

    if (destinos.isEmpty()) return

    Spacer(modifier = Modifier.height(24.dp))
    androidx.compose.material3.HorizontalDivider(color = cardBorderColor())
    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "REDES",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    destinos.forEach { link ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { openExternal(context, link.url) }
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(link.icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (link.tintToTheme) {
                    androidx.compose.material3.LocalContentColor.current
                } else {
                    Color.Unspecified
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = link.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Abrir",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * One destination: what it is called, where it goes, which mark shows it, and
 * whether that mark should follow the theme.
 *
 * A monochrome mark (GitHub) has to be tinted or it disappears on one of the two
 * backgrounds; a mark that carries its own brand colors (Instagram) must never be
 * tinted, or the gradient collapses into a flat silhouette.
 */
private data class SocialLink(
    val nombre: String,
    val url: String,
    val icon: Int,
    val tintToTheme: Boolean
)

@ExperimentalMaterial3Api
@Composable
fun ThemeBottomSheetContent(
    currentTheme: AppTheme,
    currentMode: ThemeMode,
    currentDynamicColor: Boolean,
    currentHighPrecision: Boolean,
    currentAmoledBlack: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHighPrecisionChange: (Boolean) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    isDynamicColorAvailable: Boolean,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val horizontalPadding = if (screenWidthDp < 360) 16.dp else 24.dp

    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The settings list outgrew a phone screen. Without a scroll the
            // bottom of it was simply unreachable.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        // Handle bar + Close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, cardBorderColor(), CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "APARIENCIA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic Color Toggle
        if (isDynamicColorAvailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Color dinámico (Material You)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Usa los colores del fondo de pantalla del sistema",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = currentDynamicColor,
                    onCheckedChange = { onDynamicColorChange(!currentDynamicColor) }
                )
            }
            androidx.compose.material3.HorizontalDivider(
                color = cardBorderColor()
            )
        }

        // AMOLED pure black toggle. No auto-detection: Android exposes no API that
        // reports whether the panel is AMOLED, and the Build.MODEL allow-lists that
        // some apps use go stale the day a new phone ships. The user knows their
        // screen; the app only has to honour the choice.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Negro puro (AMOLED)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Fondo negro real en modo oscuro. En AMOLED esos píxeles se apagan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = currentAmoledBlack,
                onCheckedChange = { onAmoledBlackChange(!currentAmoledBlack) }
            )
        }
        androidx.compose.material3.HorizontalDivider(
            color = cardBorderColor()
        )

        // High Precision Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alta precisión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Muestra 4 decimales en la tasa (ej: 798,3260)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = currentHighPrecision,
                onCheckedChange = { onHighPrecisionChange(!currentHighPrecision) }
            )
        }
        androidx.compose.material3.HorizontalDivider(
            color = cardBorderColor()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Theme Mode Selector
        Text(
            text = "Modo de tema",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val modeInteractionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, cardBorderColor(), RoundedCornerShape(50))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = currentMode == mode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) cardBadgeColor() else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.outline
                            else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(
                            interactionSource = modeInteractionSource,
                            indication = null
                        ) { onModeChange(mode) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Subtle active-state indicator, grown rather than toggled.
                    val modeIndicator by animateDpAsState(
                        targetValue = if (isSelected) 16.dp else 0.dp,
                        animationSpec = tween(MotionDurations.BASE, easing = Emphasized),
                        label = "modeIndicator"
                    )
                    Box(
                        modifier = Modifier
                            .width(modeIndicator)
                            .height(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accentColor())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.material3.HorizontalDivider(
            color = cardBorderColor()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Theme Selector — button that opens a dialog
        Text(
            text = "Tema de color",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val lightPrimary = Color(currentTheme.lightPrimary)
        val darkPrimary = Color(currentTheme.darkPrimary)

        // Current theme preview button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardContainerColor())
                .border(1.dp, cardBorderColor(), RoundedCornerShape(16.dp))
                .clickable { showThemeDialog = true }
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.size(56.dp, 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(lightPrimary, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(darkPrimary, CircleShape)
                    )
                }
                Text(
                    text = currentTheme.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Cambiar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        androidx.compose.material3.HorizontalDivider(
            color = cardBorderColor()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PREFERENCIAS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        PriceAlertSection()

        SocialLinksSection()

        // Build identity at the very bottom. It is the first thing a bug report
        // needs and the last thing a reader wants to scroll past, so it sits
        // under the links rather than in a section of its own.
        versionLabel(com.example.erp.BuildConfig.VERSION_NAME)?.let { label ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = {
                onThemeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

/**
 * "Avisame cuando el paralelo pase de 900."
 *
 * The threshold is a plain number field, not a slider: a slider implies
 * precision the rate does not have and makes an exact target fiddly to hit.
 */
@Composable
private fun PriceAlertSection() {
    val context = LocalContext.current
    val store = remember(context) { com.example.erp.data.PriceAlertStore(context) }
    var config by remember { mutableStateOf(store.load()) }
    var thresholdText by remember(config.threshold) {
        mutableStateOf(if (config.threshold > 0) config.threshold.toString() else "")
    }

    fun persist(next: com.example.erp.data.PriceAlertStore.Config) {
        config = next
        store.save(next)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Avisarme cuando cruce un precio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Un aviso por cruce, no cada hora",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = { persist(config.copy(enabled = it)) }
            )
        }

        if (config.enabled) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                com.example.erp.data.PriceAlert.Fuente.entries.forEach { fuente ->
                    val isSelected = config.fuente == fuente
                    val label = when (fuente) {
                        com.example.erp.data.PriceAlert.Fuente.USD -> "BCV"
                        com.example.erp.data.PriceAlert.Fuente.EUR -> "Euro"
                        com.example.erp.data.PriceAlert.Fuente.PARALELO -> "Paralelo"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) accentColor() else Color.Transparent)
                            .border(1.dp, cardBorderColor(), RoundedCornerShape(50))
                            .clickable { persist(config.copy(fuente = fuente)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                androidx.compose.ui.graphics.Color.Black
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "Sube de" to com.example.erp.data.PriceAlert.Direccion.ARRIBA,
                    "Baja de" to com.example.erp.data.PriceAlert.Direccion.ABAJO
                ).forEach { (label, direction) ->
                    val isSelected = config.direction == direction
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) accentColor() else Color.Transparent)
                            .border(1.dp, cardBorderColor(), RoundedCornerShape(50))
                            .clickable { persist(config.copy(direction = direction)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                androidx.compose.ui.graphics.Color.Black
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = thresholdText,
                onValueChange = { entered ->
                    val digits = entered.filter { it.isDigit() || it == '.' }
                    thresholdText = digits
                    digits.toDoubleOrNull()?.let { value ->
                        persist(config.copy(threshold = value))
                    }
                },
                label = { Text("Precio en bolívares") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Seleccionar tema",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    val isSelected = currentTheme == theme
                    val lightPrimary = Color(theme.lightPrimary)
                    val darkPrimary = Color(theme.darkPrimary)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.outline
                                else cardBorderColor(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onThemeSelected(theme) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color preview circles
                        Row(
                            modifier = Modifier.size(56.dp, 28.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(lightPrimary, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(darkPrimary, CircleShape)
                            )
                        }

                        Text(
                            text = theme.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Seleccionado",
                                // Active-state indicator -> accent.
                                tint = accentColor(),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private val ThemeMode.displayName: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }
