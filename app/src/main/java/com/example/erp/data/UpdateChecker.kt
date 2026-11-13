package com.example.erp.data

import android.content.Context
import com.example.erp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

/** A published release newer than the installed build. */
data class AvailableUpdate(
    val versionName: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val apkSizeBytes: Long?,
    val apkUrl: String? = null,
    val apkDigest: String? = null,
    val apkName: String? = null
) {
    /**
     * Whether the APK can be downloaded at all.
     *
     * A URL alone is not enough: without a published SHA-256 there is no way to
     * prove that the bytes came from the release, so the dialog falls back to
     * opening the release page.
     */
    val downloadable: Boolean
        get() = !apkUrl.isNullOrBlank() && parseSha256Digest(apkDigest) != null
}

/** The outcome of one update check. */
sealed interface UpdateCheckResult {
    data class Available(val update: AvailableUpdate) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Skipped : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

/** The subset of the GitHub release payload the update check needs. */
internal data class LatestRelease(
    val versionName: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val apkSizeBytes: Long?,
    val apkUrl: String?,
    val apkDigest: String?,
    val apkName: String?
)

internal data class Semver(val major: Int, val minor: Int, val patch: Int)

internal const val GITHUB_RELEASES_LATEST_URL =
    "https://api.github.com/repos/YourNelvi/Dolita/releases/latest"

/**
 * The releases page, which answers "which version is published?" with a
 * redirect instead of a JSON body — and without spending the API quota.
 */
internal const val GITHUB_RELEASES_LATEST_PAGE =
    "https://github.com/YourNelvi/Dolita/releases/latest"

private const val FALLBACK_RELEASE_URL =
    "https://github.com/YourNelvi/Dolita/releases/latest"

/**
 * Level 1 update check: ask GitHub for the latest release, compare its tag with
 * the installed version, and hand the release back to the UI.
 *
 * This class only reports what is published. Downloading and installing the
 * artifact are [ApkDownloader] and [ApkInstaller]'s job, and only when the
 * release carries a digest this app can verify.
 *
 * The successful-check timestamp is persisted so an app launch tries at most
 * once per the interval chosen in Settings. A manual check passes
 * `force = true` and skips that throttle.
 */
class UpdateChecker(
    context: Context,
    private val currentVersion: String = BuildConfig.VERSION_NAME,
    private val client: OkHttpClient = sharedHttpClient,
    private val clock: () -> Long = System::currentTimeMillis,
    private val preferences: UpdatePreferences = UpdatePreferences(context)
) {
    suspend fun check(force: Boolean = false): UpdateCheckResult {
        val now = clock()
        val due = shouldCheckForUpdate(
            lastCheckEpochMs = preferences.lastSuccessfulCheckEpochMs,
            nowEpochMs = now,
            force = force,
            interval = preferences.interval
        )
        if (!due) {
            return UpdateCheckResult.Skipped
        }

        return try {
            // The cheap question comes first: which version is published?
            // The releases page answers it with a redirect, and the web front
            // end does not spend the API quota that every Dolita reader
            // behind the same carrier shares.
            val publishedVersion = fetchLatestPublishedVersion()
            if (!isNewerVersion(publishedVersion, currentVersion)) {
                preferences.lastSuccessfulCheckEpochMs = now
                return UpdateCheckResult.UpToDate
            }

            // Something newer exists, so now one API call buys the notes, the
            // APK and the digest. It is spent only when there is news.
            val release = parseLatestRelease(fetchLatestReleaseJson())
                ?: return UpdateCheckResult.Failed("No se pudo leer la respuesta de GitHub.")

            // Mark only a successful, parsed response. A network failure should
            // not make the app wait a whole interval before trying again.
            preferences.lastSuccessfulCheckEpochMs = now

            if (isNewerVersion(release.versionName, currentVersion)) {
                UpdateCheckResult.Available(
                    AvailableUpdate(
                        versionName = release.versionName,
                        releaseNotes = release.releaseNotes,
                        releaseUrl = release.releaseUrl,
                        apkSizeBytes = release.apkSizeBytes,
                        apkUrl = release.apkUrl,
                        apkDigest = release.apkDigest,
                        apkName = release.apkName
                    )
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: UpdateHttpException) {
            UpdateCheckResult.Failed(e.message ?: "No se pudo buscar la actualización.")
        } catch (e: IOException) {
            UpdateCheckResult.Failed("Sin conexión. Revisá tu red e intentá de nuevo.")
        } catch (e: Exception) {
            UpdateCheckResult.Failed("No se pudo buscar la actualización.")
        }
    }

    /**
     * Reads the version of the newest published release without spending API
     * quota.
     *
     * The releases page answers with a redirect to the newest release, and the
     * web front end is not the API: its quota is orders of magnitude bigger
     * and it is not shared per carrier the way the API's sixty requests per
     * hour are. This is the check that can safely run on every launch.
     */
    private suspend fun fetchLatestPublishedVersion(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(GITHUB_RELEASES_LATEST_PAGE)
            .header("User-Agent", "Dolita/$currentVersion")
            .build()

        // The redirect IS the answer. Following it would download the whole
        // release page to read one word.
        client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
            .newCall(request)
            .execute()
            .use { response ->
                when {
                    response.code == 404 ->
                        throw UpdateHttpException("No hay releases publicadas.")

                    response.code == 403 || response.code == 429 ->
                        throw UpdateHttpException("GitHub limitó las consultas. Probá más tarde.")

                    response.isRedirect -> versionFromReleaseRedirect(response.header("Location"))

                    response.isSuccessful -> ""

                    else -> throw UpdateHttpException("GitHub respondió HTTP ${response.code}.")
                }
            }
    }

    private suspend fun fetchLatestReleaseJson(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(GITHUB_RELEASES_LATEST_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Dolita/$currentVersion")
            .build()

        client.newCall(request).execute().use { response ->
            when {
                response.isSuccessful -> response.body?.string()
                    ?: throw IOException("GitHub returned an empty body")

                response.code == 403 || response.code == 429 ->
                    throw UpdateHttpException("GitHub limitó las consultas. Probá más tarde.")

                response.code == 404 ->
                    throw UpdateHttpException("No hay releases publicadas.")

                else ->
                    throw UpdateHttpException("GitHub respondió HTTP ${response.code}.")
            }
        }
    }
}

/**
 * Reads the version out of the redirect the releases page answers with.
 *
 * `https://github.com/owner/repo/releases/latest` answers with a 302 to
 * `.../releases/tag/v1.31.1`, so the published version is in the last path
 * segment. An empty answer means there is nothing published to point at.
 */
internal fun versionFromReleaseRedirect(location: String?): String {
    val target = location?.substringAfterLast("/releases/tag/", "")?.trim().orEmpty()
    if (target.isEmpty()) return ""
    return target.removePrefix("v").removePrefix("V").trim()
}

private class UpdateHttpException(message: String) : IOException(message)

/**
 * Reads `major.minor.patch` from a tag or version name.
 *
 * A leading `v` is accepted, and a prerelease/build suffix is ignored: the app
 * only compares the numeric release line. Anything that is not a number in the
 * first three segments is not a version this app can reason about.
 */
internal fun parseSemver(value: String): Semver? {
    val cleaned = value.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .substringBefore('+')
        .trim()

    if (cleaned.isEmpty()) return null

    val parts = cleaned.split('.')
    if (parts.size !in 1..3) return null

    val numbers = parts.map { part ->
        val number = part.toIntOrNull() ?: return null
        if (number < 0) return null
        number
    }

    return Semver(
        major = numbers.getOrElse(0) { 0 },
        minor = numbers.getOrElse(1) { 0 },
        patch = numbers.getOrElse(2) { 0 }
    )
}

/**
 * Whether the update dialog should be shown for the release that was found.
 *
 * The automatic check can run on every launch now, so a reader who dismissed
 * the notice with "Más tarde" would be greeted by the same dialog every time
 * they open the app. The dismissal is remembered for that version only: a
 * newer release is news again, and a manual check always shows it, because
 * that is the reader asking for it.
 */
internal fun shouldShowUpdateDialog(
    releaseVersion: String,
    currentVersion: String,
    dismissedVersion: String?,
    manual: Boolean
): Boolean {
    if (!isNewerVersion(releaseVersion, currentVersion)) return false
    if (manual) return true
    return dismissedVersion != releaseVersion
}

/** True only when [latest] is a strictly newer numeric version than [current]. */
internal fun isNewerVersion(latest: String, current: String): Boolean {
    val latestVersion = parseSemver(latest) ?: return false
    val currentVersion = parseSemver(current) ?: return false

    return when {
        latestVersion.major != currentVersion.major ->
            latestVersion.major > currentVersion.major

        latestVersion.minor != currentVersion.minor ->
            latestVersion.minor > currentVersion.minor

        else ->
            latestVersion.patch > currentVersion.patch
    }
}

/**
 * Turns the release body into text the dialog can show.
 *
 * GitHub writes changelogs in markdown; a dialog is not a markdown renderer.
 * Heading markers and code fences would be read as noise, so they are removed.
 */
internal fun cleanReleaseNotes(body: String): String {
    val cleaned = body
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .joinToString("\n") { line ->
            line
                .replace(Regex("^\\s{0,3}#{1,6}\\s*"), "")
                .replace(Regex("\\*\\*(.+?)\\*\\*")) { it.groupValues[1] }
                .replace(Regex("__(.+?)__")) { it.groupValues[1] }
                .replace("`", "")
        }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    return cleaned.ifBlank { "Sin notas de la versión." }
}

/**
 * Parses the subset of `GET /releases/latest` this app needs.
 *
 * The APK data is taken from the first Android package asset. A release with no
 * APK is still valid: its page can be opened, it just has nothing to download.
 */
internal fun parseLatestRelease(json: String): LatestRelease? {
    val root = runCatching { JSONObject(json) }.getOrNull() ?: return null

    val tagName = (root.opt("tag_name") as? String)?.trim().orEmpty()
    val versionName = tagName.removePrefix("v").removePrefix("V").trim()
    if (versionName.isEmpty()) return null

    val releaseUrl = (root.opt("html_url") as? String)?.trim()
        .takeUnless { it.isNullOrEmpty() }
        ?: FALLBACK_RELEASE_URL

    val apkAsset = root.optJSONArray("assets")?.let(::selectApkAsset)
    val apkSizeBytes = apkAsset?.optLong("size", 0L)?.takeIf { it > 0L }

    return LatestRelease(
        versionName = versionName,
        releaseNotes = cleanReleaseNotes((root.opt("body") as? String).orEmpty()),
        releaseUrl = releaseUrl,
        apkSizeBytes = apkSizeBytes,
        apkUrl = apkAsset?.optString("browser_download_url")?.trim()
            ?.takeIf { it.isNotEmpty() },
        apkDigest = apkAsset?.optString("digest")?.trim()?.takeIf { it.isNotEmpty() },
        apkName = apkAsset?.optString("name")?.trim()?.takeIf { it.isNotEmpty() }
    )
}

private fun selectApkAsset(assets: JSONArray): JSONObject? {
    for (index in 0 until assets.length()) {
        val asset = assets.optJSONObject(index) ?: continue
        val contentType = asset.optString("content_type")
        val name = asset.optString("name")

        if (contentType == "application/vnd.android.package-archive" ||
            name.endsWith(".apk", ignoreCase = true)
        ) {
            return asset
        }
    }
    return null
}

/**
 * A launch check is due when the chosen interval has elapsed, when none has
 * succeeded yet, or when the clock moved backwards. A manual check always runs,
 * and "only manual" forbids every automatic one.
 */
internal fun shouldCheckForUpdate(
    lastCheckEpochMs: Long,
    nowEpochMs: Long,
    force: Boolean,
    interval: UpdateCheckInterval = UpdateCheckInterval.FREQUENT
): Boolean {
    if (force) return true
    val intervalMs = interval.intervalMs ?: return false
    if (lastCheckEpochMs <= 0L) return true
    if (nowEpochMs < lastCheckEpochMs) return true
    return nowEpochMs - lastCheckEpochMs >= intervalMs
}

/** Decimal megabytes, matching how APK sizes are usually quoted. */
internal fun formatApkSize(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes <= 0L) return ""

    val formatter = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

    return "${formatter.format(bytes / 1_000_000.0)} MB"
}
