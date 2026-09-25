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
import com.example.erp.data.ThemeMode
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.cardBadgeColor
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.cardContainerColor

@ExperimentalMaterial3Api
@Composable
fun ThemeBottomSheetContent(
    currentTheme: AppTheme,
    currentMode: ThemeMode,
    currentDynamicColor: Boolean,
    currentHighPrecision: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHighPrecisionChange: (Boolean) -> Unit,
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
            text = "Tema y apariencia",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
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
