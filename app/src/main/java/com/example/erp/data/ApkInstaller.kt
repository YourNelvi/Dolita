package com.example.erp.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed interface ApkInstallResult {
    data object Started : ApkInstallResult
    data object NeedsPermission : ApkInstallResult
    data class Failed(val message: String) : ApkInstallResult
}

/**
 * Hands a verified APK to Android's package installer.
 *
 * There is no silent path here: only a system app or a device owner can install
 * without asking. What this object can do is make the prerequisite explicit —
 * without the per-app "install unknown apps" grant, the installer activity
 * refuses to open at all.
 */
object ApkInstaller {

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun install(context: Context, apk: File): ApkInstallResult {
        if (!apk.isFile || apk.length() <= 0L) {
            return ApkInstallResult.Failed("El APK descargado ya no está disponible.")
        }

        if (!canInstall(context)) return ApkInstallResult.NeedsPermission

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )

            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, APK_MIME_TYPE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )

            ApkInstallResult.Started
        } catch (e: Exception) {
            ApkInstallResult.Failed("No se pudo abrir el instalador de Android.")
        }
    }

    /**
     * Sends the user straight to Dolita's "install unknown apps" switch, not to
     * the generic list of every app that asked for it.
     */
    fun openInstallPermissionSettings(context: Context): Boolean = runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
        )
    }.isSuccess
}
