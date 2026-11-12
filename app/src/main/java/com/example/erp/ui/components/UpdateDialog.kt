package com.example.erp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.erp.data.AvailableUpdate
import com.example.erp.data.downloadProgressPercent
import com.example.erp.data.formatApkSize
import com.example.erp.ui.theme.ERPTheme
import com.example.erp.ui.theme.cardBorderColor

/** What the update dialog is doing with the APK right now. */
sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : UpdateDownloadState
    data class Failed(val message: String) : UpdateDownloadState
}

/**
 * The update notice: name the version, show what changed, how big the APK is,
 * and either download it or open the release page.
 *
 * Level 2 only appears when the release publishes both an APK and a SHA-256.
 * Without the hash there is no way to prove the downloaded bytes are the
 * published ones, so the dialog quietly stays at Level 1 instead of offering an
 * install it cannot verify.
 */
@Composable
fun UpdateDialog(
    update: AvailableUpdate,
    installedVersion: String,
    downloadState: UpdateDownloadState,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onOpenRelease: () -> Unit,
    onDismiss: () -> Unit
) {
    val downloading = downloadState is UpdateDownloadState.Downloading

    AlertDialog(
        onDismissRequest = onDismiss,
        // AMOLED turns every surface pure black, so the dialog needs the same
        // hairline the other cards use or it vanishes into the scrim.
        modifier = Modifier.border(1.dp, cardBorderColor(), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Actualización disponible",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dolita ${update.versionName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tenés instalada la versión $installedVersion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                update.apkSizeBytes?.let { bytes ->
                    val size = formatApkSize(bytes)
                    if (size.isNotEmpty()) {
                        Text(
                            text = "Tamaño del APK: $size",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (update.downloadable) {
                    Text(
                        text = "Se verifica el SHA-256 antes de instalar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        DownloadProgress(state = downloadState)
                    }

                    is UpdateDownloadState.Failed -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    UpdateDownloadState.Idle -> Unit
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = update.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        // A release can be longer than a small phone's dialog.
                        // A bounded, scrollable block keeps every line reachable
                        // without stretching the dialog past the screen.
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenRelease) {
                    Text("Ver release")
                }

                if (update.downloadable) {
                    TextButton(onClick = if (downloading) onCancelDownload else onDownload) {
                        Text(if (downloading) "Cancelar" else "Descargar")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Más tarde")
            }
        }
    )
}

@Composable
private fun DownloadProgress(state: UpdateDownloadState.Downloading) {
    val percent = downloadProgressPercent(state.bytesRead, state.totalBytes)

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = if (percent != null) "Descargando… $percent %" else "Descargando…",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))

    if (percent != null) {
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        // Without a Content-Length there is no honest percentage; a bar that
        // invents one would look stuck at a number that never matches reality.
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Explains the one prerequisite Android will not let the app grant itself.
 *
 * "Ya lo habilité" re-checks instead of trusting the user: a setting screen can
 * be left without granting anything, and the install would fail silently after
 * the download already happened.
 */
@Composable
fun InstallPermissionDialog(
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, cardBorderColor(), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Permiso necesario",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Android no permite instalar una app descargada fuera de la " +
                    "tienda sin tu permiso.\n\nHabilitá «Instalar apps desconocidas» " +
                    "para Dolita y volvé a intentarlo.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenSettings) {
                    Text("Abrir ajustes")
                }
                TextButton(onClick = onRetry) {
                    Text("Ya lo habilité")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Más tarde")
            }
        }
    )
}

private const val PREVIEW_SHA256 =
    "sha256:a198025f107815c7a65143f8f5dc3d525c814be9c078be9b16e6d6f11d45bf12"

@Preview(showBackground = true)
@Composable
private fun UpdateDialogPreview() {
    ERPTheme {
        UpdateDialog(
            update = AvailableUpdate(
                versionName = "1.31.1",
                releaseNotes = "Qué cambia\n\nCalculadora\n\n- Agrega algo.\n- Corrige un detalle.",
                releaseUrl = "https://github.com/YourNelvi/Dolita/releases/latest",
                apkSizeBytes = 48_669_750L,
                apkUrl = "https://github.com/YourNelvi/Dolita/releases/download/v1.31.1/Dolita-1.31.1.apk",
                apkDigest = PREVIEW_SHA256,
                apkName = "Dolita-1.31.1.apk"
            ),
            installedVersion = "1.31.0",
            downloadState = UpdateDownloadState.Idle,
            onDownload = {},
            onCancelDownload = {},
            onOpenRelease = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateDialogDownloadingPreview() {
    ERPTheme {
        UpdateDialog(
            update = AvailableUpdate(
                versionName = "1.31.1",
                releaseNotes = "Qué cambia\n\nCalculadora\n\n- Agrega algo.",
                releaseUrl = "https://github.com/YourNelvi/Dolita/releases/latest",
                apkSizeBytes = 48_669_750L,
                apkUrl = "https://github.com/YourNelvi/Dolita/releases/download/v1.31.1/Dolita-1.31.1.apk",
                apkDigest = PREVIEW_SHA256,
                apkName = "Dolita-1.31.1.apk"
            ),
            installedVersion = "1.31.0",
            downloadState = UpdateDownloadState.Downloading(
                bytesRead = 24_334_875L,
                totalBytes = 48_669_750L
            ),
            onDownload = {},
            onCancelDownload = {},
            onOpenRelease = {},
            onDismiss = {}
        )
    }
}
