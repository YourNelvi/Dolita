package com.example.erp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/** Directory under `filesDir` that holds the downloaded, verified APK. */
internal const val UPDATE_DIR = "updates"

/** How many bytes may pass between two progress reports. */
private const val PROGRESS_STEP_BYTES = 256L * 1024L
private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
private const val HEX_DIGITS = "0123456789abcdef"
private const val SHA256_PREFIX = "sha256:"

sealed interface ApkDownloadResult {
    data class Success(val file: File) : ApkDownloadResult
    data class Failure(val message: String) : ApkDownloadResult
}

/**
 * Downloads the published APK and proves it is the published artifact before
 * anyone gets a chance to run it.
 *
 * The SHA-256 is computed from the same bytes that are written to disk, in one
 * pass: re-reading a 48 MB file just to hash it would double the work and leave
 * a window where the hash and the file could disagree. A mismatch deletes the
 * file instead of returning it, so no install path ever sees unverified bytes.
 */
class ApkDownloader(
    private val directory: File,
    private val client: OkHttpClient = sharedHttpClient
) {
    constructor(context: Context, client: OkHttpClient = sharedHttpClient) :
        this(File(context.applicationContext.filesDir, UPDATE_DIR), client)

    /** Removes leftovers: a stale APK is 48 MB nobody asked to keep. */
    fun clearPending() {
        directory.listFiles()?.forEach { it.delete() }
    }

    /**
     * Streams [url] into a new file and verifies it against [expectedSha256].
     *
     * [onProgress] is called at most once every 256 KB and once at the end. It
     * runs on the download dispatcher, so a caller that updates UI state has to
     * hop to the main thread itself.
     */
    suspend fun download(
        url: String,
        expectedSha256: String,
        fileName: String,
        onProgress: suspend (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): ApkDownloadResult = withContext(Dispatchers.IO) {
        directory.mkdirs()
        // A previous attempt left either a partial file or an older release.
        // Neither one should be installable.
        clearPending()

        // A unique partial name: a cancelled attempt deletes by path in its
        // `finally`, and it must never delete the file a newer attempt is
        // writing under the same name.
        val partial = File(directory, "$fileName.${UUID.randomUUID()}.part")
        val target = File(directory, fileName)
        val call = client.newCall(Request.Builder().url(url).build())

        // Cancel the socket as soon as the job is cancelled. Without this, a
        // blocked read keeps the download alive until the 15 s read timeout,
        // and "Cancelar" looks broken for that long.
        currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }

        try {
            currentCoroutineContext().ensureActive()
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ApkDownloadResult.Failure(
                        "La descarga falló (HTTP ${response.code})."
                    )
                }

                val body = response.body
                    ?: return@withContext ApkDownloadResult.Failure(
                        "La descarga llegó vacía."
                    )

                val totalBytes = body.contentLength()
                val digest = MessageDigest.getInstance("SHA-256")
                var bytesRead = 0L
                var reportedAt = 0L

                body.byteStream().use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break

                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            bytesRead += count

                            val stepReached = bytesRead - reportedAt >= PROGRESS_STEP_BYTES
                            val downloadFinished = totalBytes > 0L && bytesRead >= totalBytes
                            if (stepReached || downloadFinished) {
                                reportedAt = bytesRead
                                onProgress(bytesRead, totalBytes)
                            }
                        }
                    }
                }

                val actualSha256 = digest.digest().toHexString()
                if (!actualSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
                    partial.delete()
                    return@withContext ApkDownloadResult.Failure(
                        "El APK no coincide con el SHA-256 publicado. Se descartó sin instalarlo."
                    )
                }

                // `renameTo` is atomic inside the same directory, so a reader
                // never observes a half-written APK under the final name.
                target.delete()
                if (!partial.renameTo(target)) {
                    partial.delete()
                    return@withContext ApkDownloadResult.Failure(
                        "No se pudo guardar el APK descargado."
                    )
                }

                ApkDownloadResult.Success(target)
            }
        } catch (e: IOException) {
            partial.delete()
            ApkDownloadResult.Failure("Sin conexión. Revisá tu red e intentá de nuevo.")
        } finally {
            if (!currentCoroutineContext().isActive) {
                // Cancelling the coroutine closes the socket; otherwise a
                // blocked read would keep the download alive after the dialog
                // is gone.
                call.cancel()
                partial.delete()
            }
        }
    }
}

/**
 * Reads the hex payload of a GitHub asset `digest` field.
 *
 * GitHub reports `sha256:<64 hex>`. Anything else — a different algorithm, a
 * truncated hash, a value that is not a digest at all — returns null, and a
 * null digest is what disables the in-app download.
 */
internal fun parseSha256Digest(digest: String?): String? {
    val value = digest?.trim().orEmpty()
    if (value.length <= SHA256_PREFIX.length) return null
    if (!value.startsWith(SHA256_PREFIX, ignoreCase = true)) return null

    val hex = value.substring(SHA256_PREFIX.length)
    if (hex.length != 64) return null
    if (!hex.all { it.isHexDigit() }) return null

    return hex.lowercase()
}

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()

/**
 * The file name an update is stored under.
 *
 * The version comes from a release tag, which is remote input: every character
 * that is not safe in a file name becomes an underscore, so a tag can never
 * steer the download outside `filesDir/updates/`.
 */
internal fun updateApkFileName(versionName: String): String {
    val safeVersion = versionName.trim()
        .map { character ->
            when {
                character in 'a'..'z' || character in 'A'..'Z' -> character
                character in '0'..'9' -> character
                character == '.' || character == '-' || character == '_' -> character
                else -> '_'
            }
        }
        .joinToString("")
        .trim('.', '-', '_')

    return "Dolita-${safeVersion.ifEmpty { "update" }}.apk"
}

/**
 * Download progress as a whole percentage, or null when the total is unknown
 * and the bar has to stay indeterminate.
 */
internal fun downloadProgressPercent(bytesRead: Long, totalBytes: Long): Int? {
    if (totalBytes <= 0L || bytesRead < 0L) return null
    val fraction = bytesRead.toDouble() / totalBytes.toDouble()
    return (fraction.coerceIn(0.0, 1.0) * 100).toInt()
}

internal fun ByteArray.toHexString(): String = buildString(size * 2) {
    for (byte in this@toHexString) {
        val value = byte.toInt() and 0xFF
        append(HEX_DIGITS[value ushr 4])
        append(HEX_DIGITS[value and 0x0F])
    }
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
