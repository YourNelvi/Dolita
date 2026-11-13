package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The update check's decisions, pinned.
 *
 * The network call itself is a thin OkHttp wrapper; the parts that can silently
 * mislead a reader are pure: whether a tag is actually newer, which asset is the
 * APK, whether a check is due, and how the release notes read once the markdown
 * is gone. Those are the pieces below.
 */
class UpdateCheckerTest {

    private val latestReleaseJson = """
        {
          "tag_name": "v1.31.0",
          "html_url": "https://github.com/YourNelvi/Dolita/releases/tag/v1.31.0",
          "body": "## Qué cambia\n\n### Calculadora\n\n- Agrega `algo`.\n- Corrige **un detalle**.\n\n\n",
          "assets": [
            {
              "name": "checksums.txt",
              "content_type": "text/plain",
              "size": 128
            },
            {
              "name": "Dolita-1.31.0.apk",
              "content_type": "application/vnd.android.package-archive",
              "size": 48669750,
              "browser_download_url": "https://github.com/YourNelvi/Dolita/releases/download/v1.31.0/Dolita-1.31.0.apk",
              "digest": "sha256:a198025f107815c7a65143f8f5dc3d525c814be9c078be9b16e6d6f11d45bf12"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `newer versions are detected by number, not by text`() {
        assertTrue(isNewerVersion("1.31.0", "1.30.0"))
        assertTrue(isNewerVersion("v1.9.0", "1.8.0"))
        assertFalse("1.9.0 is older than 1.10.0", isNewerVersion("1.9.0", "1.10.0"))
        assertFalse(isNewerVersion("1.30.0", "1.30.0"))
        assertFalse(isNewerVersion("v1.30.0", "1.30.0"))
    }

    @Test
    fun `a version suffix does not outrank the base version`() {
        assertTrue(isNewerVersion("1.30.1", "1.30.0"))
        assertFalse(isNewerVersion("1.30.1-beta.1", "1.30.1"))
        assertFalse(isNewerVersion("1.30.0+build.7", "1.30.0"))
    }

    @Test
    fun `unparseable versions never claim an update`() {
        assertFalse(isNewerVersion("latest", "1.30.0"))
        assertFalse(isNewerVersion("1.31.0", "not-a-version"))
        assertNull(parseSemver(""))
        assertNull(parseSemver("1.x.0"))
    }

    @Test
    fun `the latest release is read from the GitHub payload`() {
        val release = parseLatestRelease(latestReleaseJson)!!

        assertEquals("1.31.0", release.versionName)
        assertEquals(
            "https://github.com/YourNelvi/Dolita/releases/tag/v1.31.0",
            release.releaseUrl
        )
        assertEquals("the APK asset, not the checksum file", 48_669_750L, release.apkSizeBytes)
        assertEquals(
            "https://github.com/YourNelvi/Dolita/releases/download/v1.31.0/Dolita-1.31.0.apk",
            release.apkUrl
        )
        assertEquals(
            "sha256:a198025f107815c7a65143f8f5dc3d525c814be9c078be9b16e6d6f11d45bf12",
            release.apkDigest
        )
        assertEquals("Dolita-1.31.0.apk", release.apkName)
        assertEquals(
            "Qué cambia\n\nCalculadora\n\n- Agrega algo.\n- Corrige un detalle.",
            release.releaseNotes
        )
    }

    @Test
    fun `a release without an apk still opens its page`() {
        val json = """
            {
              "tag_name": "v1.31.0",
              "html_url": "https://github.com/YourNelvi/Dolita/releases/tag/v1.31.0",
              "body": "",
              "assets": []
            }
        """.trimIndent()

        val release = parseLatestRelease(json)!!

        assertNull(release.apkSizeBytes)
        assertNull(release.apkUrl)
        assertNull(release.apkDigest)
        assertEquals("Sin notas de la versión.", release.releaseNotes)
    }

    @Test
    fun `an apk is only offered when github publishes its sha256`() {
        val update = AvailableUpdate(
            versionName = "1.31.0",
            releaseNotes = "",
            releaseUrl = "https://github.com/YourNelvi/Dolita/releases/tag/v1.31.0",
            apkSizeBytes = 48_669_750L,
            apkUrl = "https://github.com/YourNelvi/Dolita/releases/download/v1.31.0/Dolita-1.31.0.apk",
            apkDigest = "sha256:a198025f107815c7a65143f8f5dc3d525c814be9c078be9b16e6d6f11d45bf12",
            apkName = "Dolita-1.31.0.apk"
        )

        assertTrue(update.downloadable)
        assertFalse("no hash, no install", update.copy(apkDigest = null).downloadable)
        assertFalse("no url, nothing to download", update.copy(apkUrl = null).downloadable)
        assertFalse(
            "a digest from another algorithm cannot be checked here",
            update.copy(apkDigest = "md5:a198025f107815c7a65143f8f5dc3d52").downloadable
        )
    }

    @Test
    fun `malformed github responses are rejected`() {
        assertNull(parseLatestRelease(""))
        assertNull(parseLatestRelease("{}"))
        assertNull(parseLatestRelease("""{"tag_name": ""}"""))
        assertNull(parseLatestRelease("""{"tag_name": "v"}"""))
    }

    @Test
    fun `a launch check runs once the chosen interval has passed`() {
        val now = 1_000_000_000_000L
        val window = UpdateCheckInterval.FREQUENT.intervalMs!!
        val week = UpdateCheckInterval.WEEKLY.intervalMs!!

        assertTrue(shouldCheckForUpdate(0L, now, force = false))
        assertFalse(shouldCheckForUpdate(now, now + window - 1, force = false))
        assertTrue(shouldCheckForUpdate(now, now + window, force = false))
        assertTrue(shouldCheckForUpdate(now, now + window - 1, force = true))
        assertTrue(
            "a clock that moved backwards must not freeze checks",
            shouldCheckForUpdate(now, now - 1, force = false)
        )

        assertFalse(
            "a weekly interval does not fire after the frequent window",
            shouldCheckForUpdate(
                now,
                now + window,
                force = false,
                interval = UpdateCheckInterval.WEEKLY
            )
        )
        assertFalse(
            shouldCheckForUpdate(
                now,
                now + week - 1,
                force = false,
                interval = UpdateCheckInterval.WEEKLY
            )
        )
        assertTrue(
            shouldCheckForUpdate(
                now,
                now + week,
                force = false,
                interval = UpdateCheckInterval.WEEKLY
            )
        )
    }

    @Test
    fun `manual only never checks on its own`() {
        val now = 1_000_000_000_000L
        val years = 10L * 365L * 24L * 60L * 60L * 1000L

        assertFalse(
            "a fresh install must not check behind a manual-only reader",
            shouldCheckForUpdate(
                0L,
                now,
                force = false,
                interval = UpdateCheckInterval.NEVER
            )
        )
        assertFalse(
            shouldCheckForUpdate(
                now,
                now + years,
                force = false,
                interval = UpdateCheckInterval.NEVER
            )
        )
        assertTrue(
            "the button always runs",
            shouldCheckForUpdate(
                now,
                now,
                force = true,
                interval = UpdateCheckInterval.NEVER
            )
        )
    }

    @Test
    fun `an unreadable stored interval falls back to daily`() {
        assertEquals(UpdateCheckInterval.FREQUENT, UpdateCheckInterval.fromStorage(null))
        assertEquals(UpdateCheckInterval.FREQUENT, UpdateCheckInterval.fromStorage(""))
        assertEquals(UpdateCheckInterval.FREQUENT, UpdateCheckInterval.fromStorage("hourly"))
        assertEquals(UpdateCheckInterval.FREQUENT, UpdateCheckInterval.fromStorage("Cada día"))
        assertEquals(UpdateCheckInterval.WEEKLY, UpdateCheckInterval.fromStorage("weekly"))
        assertEquals(UpdateCheckInterval.NEVER, UpdateCheckInterval.fromStorage("never"))
    }

    @Test
    fun `the apk size is shown in decimal megabytes`() {
        assertEquals("48.7 MB", formatApkSize(48_669_750L, Locale.US))
        assertEquals("", formatApkSize(0L, Locale.US))
    }

    @Test
    fun `the frequent window is two hours`() {
        assertEquals(
            2L * 60L * 60L * 1000L,
            UpdateCheckInterval.FREQUENT.intervalMs
        )
    }

    @Test
    fun `an automatic check offers a release the reader did not dismiss`() {
        assertTrue(
            shouldShowUpdateDialog(
                releaseVersion = "1.31.1",
                currentVersion = "1.31.0",
                dismissedVersion = null,
                manual = false
            )
        )
    }

    @Test
    fun `a dismissed release does not come back on its own`() {
        assertFalse(
            shouldShowUpdateDialog(
                releaseVersion = "1.31.1",
                currentVersion = "1.31.0",
                dismissedVersion = "1.31.1",
                manual = false
            )
        )
    }

    @Test
    fun `a manual check shows the release even if it was dismissed`() {
        assertTrue(
            shouldShowUpdateDialog(
                releaseVersion = "1.31.1",
                currentVersion = "1.31.0",
                dismissedVersion = "1.31.1",
                manual = true
            )
        )
    }

    @Test
    fun `a release that is not newer is never offered`() {
        assertFalse(
            shouldShowUpdateDialog(
                releaseVersion = "1.31.0",
                currentVersion = "1.31.0",
                dismissedVersion = null,
                manual = true
            )
        )
    }

    @Test
    fun `a newer release is news again`() {
        assertTrue(
            shouldShowUpdateDialog(
                releaseVersion = "1.31.2",
                currentVersion = "1.31.0",
                dismissedVersion = "1.31.1",
                manual = false
            )
        )
    }

    @Test
    fun `the redirect of the releases page carries the published version`() {
        assertEquals(
            "1.31.1",
            versionFromReleaseRedirect(
                "https://github.com/YourNelvi/Dolita/releases/tag/v1.31.1"
            )
        )
        assertEquals(
            "1.31.1",
            versionFromReleaseRedirect(
                "https://github.com/YourNelvi/Dolita/releases/tag/V1.31.1"
            )
        )
    }

    @Test
    fun `a redirect without a tag means there is nothing published`() {
        assertEquals("", versionFromReleaseRedirect(null))
        assertEquals("", versionFromReleaseRedirect(""))
        assertEquals(
            "",
            versionFromReleaseRedirect("https://github.com/YourNelvi/Dolita/releases")
        )
    }
}
