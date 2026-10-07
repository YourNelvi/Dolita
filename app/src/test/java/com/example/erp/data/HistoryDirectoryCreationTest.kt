package com.example.erp.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Regression: the canonical history directory did not exist on a fresh install.
 *
 * Unifying every reader and writer on filesDir/rate_history introduced a
 * subdirectory that Android never creates for you. The first write then threw
 * FileNotFoundException on the .tmp file, an uncaught crash on the main thread
 * during ViewModel init -- the app died on launch.
 *
 * Every other test in this suite uses a temp dir the fixture already made, so
 * the whole suite stayed green while the app was unlaunchable. A directory
 * nobody created, written to by a store handed its path by five call sites, is
 * exactly the thing unit tests do not see.
 */
class HistoryDirectoryCreationTest {

    private fun millisFor(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun freshMissingDir(tag: String): File =
        File(System.getProperty("java.io.tmpdir"), "hist-$tag-${System.nanoTime()}")
            .let { File(it, "rate_history") }

    private fun sample(rate: Double, millis: Long) = RateSample(
        fuente = "usd",
        nombre = "Dolar",
        precio = rate,
        timestampEpochMillis = millis
    )

    @Test
    fun `append succeeds when the parent directory does not exist yet`() = runTest {
        // No mkdir here on purpose: this is the fresh-install state.
        val dir = freshMissingDir("fresh")

        assertTrue(
            "precondition: the subdirectory must not exist before the store writes",
            !dir.exists()
        )

        val store = FileHistoryStore(dir = dir, zoneId = ZoneId.of("UTC"))
        store.append(listOf(sample(3.318, millisFor(LocalDate.of(2026, 10, 6), ZoneId.of("UTC")))))

        val written = File(dir, "rates-2026.json")
        assertTrue("the year file must exist after appending", written.exists())
        assertTrue("the file must not be empty", written.length() > 0L)

        dir.parentFile?.deleteRecursively()
    }

    @Test
    fun `two stores over the same recreated path both succeed`() = runTest {
        val dir = freshMissingDir("shared")
        val zone = ZoneId.of("UTC")

        // The ViewModel and the USDT worker each construct a store over the same
        // path. The second must find the directory the first one created.
        val first = FileHistoryStore(dir = dir, zoneId = zone)
        val second = FileHistoryStore(dir = dir, zoneId = zone)

        // Distinct days on purpose: the store keeps one official sample per day,
        // so three prices on one date collapse to a single entry and would make
        // this assertion pass or fail for a reason unrelated to the directory.
        first.append(listOf(sample(3.318, millisFor(LocalDate.of(2026, 10, 5), zone))))
        second.append(
            listOf(
                sample(3.400, millisFor(LocalDate.of(2026, 10, 6), zone)),
                sample(3.500, millisFor(LocalDate.of(2026, 10, 7), zone))
            )
        )

        assertTrue(
            "the directory created by the first store must be reusable by the second",
            dir.exists()
        )
        assertTrue(
            "expected three days of samples across both stores, got ${second.readCurrentYear().size}",
            second.readCurrentYear().size == 3
        )

        dir.parentFile?.deleteRecursively()
    }
}
