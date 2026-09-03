package com.example.erp.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DownloadState(
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val downloadId: Long = -1,
    val isComplete: Boolean = false,
    val error: String? = null
)

object UpdateDownloader {
    private const val TAG = "UpdateDownloader"
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var downloadManager: DownloadManager? = null
    private var receiver: BroadcastReceiver? = null

    fun startDownload(context: Context, url: String, versionName: String) {
        downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Descargando Dolita v$versionName")
            .setDescription("Actualización en progreso...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Dolita-v$versionName.apk"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager!!.enqueue(request)
        _downloadState.value = DownloadState(isDownloading = true, downloadId = downloadId)

        Log.d(TAG, "Download started: $downloadId")

        // Register receiver for download complete
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    handleDownloadComplete(context, downloadId)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        // Start monitoring progress
        monitorProgress(downloadId)
    }

    private fun monitorProgress(downloadId: Long) {
        Thread {
            while (_downloadState.value.isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor? = downloadManager?.query(query)

                cursor?.use {
                    if (it.moveToFirst()) {
                        val bytesDownloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val totalSize = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                        if (totalSize > 0) {
                            val progress = ((bytesDownloaded * 100) / totalSize).toInt()
                            _downloadState.value = _downloadState.value.copy(progress = progress)
                        }
                    }
                }

                Thread.sleep(500)
            }
        }.start()
    }

    private fun handleDownloadComplete(context: Context, downloadId: Long) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager?.query(query)

        cursor?.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    _downloadState.value = _downloadState.value.copy(
                        isDownloading = false,
                        isComplete = true,
                        progress = 100
                    )
                    Log.d(TAG, "Download complete: $uri")

                    // Install the APK
                    installApk(context, Uri.parse(uri))
                } else {
                    _downloadState.value = _downloadState.value.copy(
                        isDownloading = false,
                        error = "Descarga fallida"
                    )
                    Log.e(TAG, "Download failed with status: $status")
                }
            }
        }

        // Unregister receiver
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
    }

    private fun installApk(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK: ${e.message}")
            _downloadState.value = _downloadState.value.copy(
                error = "Error al instalar: ${e.message}"
            )
        }
    }

    fun reset() {
        _downloadState.value = DownloadState()
    }
}
