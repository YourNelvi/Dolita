package com.example.erp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.erp.data.PaymentProfile
import com.example.erp.data.PaymentProfileType

/**
 * Asks which saved payment profile rides along with a share.
 *
 * The dialog only exists when the user has profiles; without them the share
 * goes out exactly as it did before, because an empty picker is a step that
 * cannot change the answer.
 *
 * Profiles are grouped by type and every row names the account by its holder
 * and summary line, never by decoding the fields again: the same
 * [PaymentProfile.shareLines] the client receives decides what is listed.
 */
@Composable
fun PaymentProfilePickerDialog(
    profiles: List<PaymentProfile>,
    onSelect: (PaymentProfile?) -> Unit,
    onDismiss: () -> Unit,
    onAddProfile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "¿Incluir datos de pago?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            if (profiles.isEmpty()) {
                Text(
                    text = "No tienes perfiles guardados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Many profiles on a small phone: bound the list and let it
                // scroll instead of stretching the dialog past the screen.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    PickerRow(
                        title = "No incluir",
                        onClick = { onSelect(null) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    PaymentProfileType.entries.forEach { type ->
                        val group = profiles.filter { it.type == type }
                        if (group.isEmpty()) return@forEach

                        Text(
                            text = group.first().typeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp)
                        )
                        group.forEach { profile ->
                            PickerRow(
                                title = profile.nombre.ifBlank { profile.typeLabel },
                                subtitle = profile.summaryLine,
                                onClick = { onSelect(profile) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (profiles.isEmpty()) {
                TextButton(onClick = onAddProfile) {
                    Text("Agregar perfil")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
