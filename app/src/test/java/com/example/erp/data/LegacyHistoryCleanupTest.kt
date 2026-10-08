package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The one-time sweep of the legacy flat history location.
 *
 * History used to be written straight into `filesDir` as `rates-YYYY.json`.
 * Every reader and writer now uses `filesDir/rate_history/`, so those files are
 * dead weight — verified on a real device as 4 files, ~174 KB, that no code path
 * opens. It cannot be cleaned by hand on one phone because every updated install
 * carries its own copy, so the app has to remove them itself.
 *
 * The blast radius is what makes this delicate. `filesDir` is the app's private
 * root, not a dedicated folder: it holds whatever else the app stores there, and
 * the canonical history directory is a subdirectory of it. So the match is
 * anchored to the exact `rates-<4 digits>.json` shape, nothing recurses, and a
 * sibling that merely starts with "rates" has to survive. Deleting a file this
 * app does not understand is a far worse outcome than leaving a few bytes behind.
 */
class LegacyHistoryCleanupTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** The legacy directory: `filesDir` itself. */
    private val legacy: File get() = tmp.root

    private fun file(dir: File, name: String, content: String = "{}"): File =
        File(dir, name).apply {
            parentFile?.mkdirs()
            writeText(content)
        }

    private fun namesIn(dir: File): List<String> = (dir.list() ?: emptyArray()).sorted()

    @Test
    fun `every legacy year file is deleted and reported`() {
        listOf("rates-2024.json", "rates-2025.json", "rates-2026.json").forEach {
            file(legacy, it, """{"anio":2026,"samples":[]}""")
        }

        val removed = deleteLegacyHistoryFiles(legacy)

        assertEquals(
            "the function must report exactly what it deleted",
            listOf("rates-2024.json", "rates-2025.json", "rates-2026.json"),
            removed
        )
        assertEquals("no legacy file may be left behind", emptyList<String>(), namesIn(legacy))
    }

    @Test
    fun `a sibling that merely starts with rates survives`() {
        // Every one of these could plausibly sit next to the real thing, and
        // deleting any of them would be destroying data this function never wrote.
        //
        // "rates-2026.JSON" is deliberately NOT here: the sweep matches lowercase
        // only, and on a case-insensitive host filesystem that file IS
        // rates-2026.json, so including it would make this test assert whatever the
        // developer's filesystem happened to do. Android's is case-sensitive, but
        // this contract is not expressible from a JVM test.
        val mustSurvive = listOf(
            "rates.json",              // no year at all
            "rates-.json",             // empty year
            "rates-202.json",          // three digits
            "rates-20266.json",        // five digits
            "rates-2026.json.tmp",     // a stranded temp file from a failed write
            "rates-2026.json.bak",     // someone's backup
            "my-rates-2026.json",      // not anchored to the start
            "xrates-2026.json",        // not anchored to the start
            "shares-2026.json",        // a different name that merely contains "rates"
        )
        mustSurvive.forEach { file(legacy, it) }
        val real = file(legacy, "rates-2026.json")
        val bystander = file(legacy, "rate_history_quarters.json")

        val removed = deleteLegacyHistoryFiles(legacy)

        assertEquals(listOf("rates-2026.json"), removed)
        assertEquals(
            "only the exact rates-YYYY.json shape may be deleted",
            (mustSurvive + "rate_history_quarters.json").sorted(),
            namesIn(legacy).filter { it != "rates-2026.json" }
        )
        assertFalse("the real legacy file must be gone", real.exists())
        assertTrue(bystander.exists())
    }

    @Test
    fun `the canonical directory and its contents are untouched`() {
        // The canonical location is a SUBDIRECTORY of the directory being swept,
        // and its year files carry the very same names. Matching by name without
        // refusing to recurse would delete the live history of an install that
        // had already been repopulated.
        val canonical = file(legacy, "$RATE_HISTORY_DIR_NAME/rates-2026.json", """{"anio":2026,"samples":[{"precio":36.58}]}""")
        file(legacy, "rates-2024.json")

        deleteLegacyHistoryFiles(legacy)

        assertTrue("the canonical directory must survive the sweep", canonical.parentFile!!.exists())
        assertTrue("the canonical history must survive the sweep", canonical.exists())
        assertEquals(
            "the canonical series must not be touched",
            """{"anio":2026,"samples":[{"precio":36.58}]}""",
            canonical.readText()
        )
    }

    @Test
    fun `it does not recurse into an unrelated subdirectory`() {
        val nested = file(legacy, "old/rates-2020.json", """{"anio":2020,"samples":[]}""")

        val removed = deleteLegacyHistoryFiles(legacy)

        assertEquals(emptyList<String>(), removed)
        assertTrue("a nested directory must be left alone entirely", nested.exists())
        assertTrue("nothing inside the nested directory may be removed", nested.readText().isNotEmpty())
    }

    @Test
    fun `a year-named directory is neither emptied nor reported as deleted`() {
        // A directory matching the name cannot be unlinked with delete(), and
        // emptying it would mean recursing. Both are worse than leaving it: this
        // function is allowed to report a deletion it did not perform.
        val occupied = File(legacy, "rates-2026.json").apply { mkdirs() }
        file(occupied, "occupant", "in the way")

        val removed = deleteLegacyHistoryFiles(legacy)

        assertTrue("the directory must still be there", occupied.exists())
        assertTrue("its contents must not have been emptied", File(occupied, "occupant").exists())
        assertEquals(
            "a deletion that did not happen must not be reported",
            emptyList<String>(),
            removed
        )
    }

    @Test
    fun `handed the canonical directory it deletes nothing at all`() {
        // THE WORST CASE. A caller that passed the path one level too low hands this
        // function the live history directory, and every file in it matches the exact
        // shape it exists to remove. Recognised by name, before anything is listed.
        val canonicalDir = File(legacy, RATE_HISTORY_DIR_NAME)
        val live = file(canonicalDir, "rates-2026.json", """{"anio":2026,"samples":[{"precio":36.58}]}""")
        val prior = file(canonicalDir, "rates-2025.json")

        val removed = deleteLegacyHistoryFiles(canonicalDir)

        assertEquals(
            "the live series must survive even when the caller gets the path wrong",
            emptyList<String>(),
            removed
        )
        assertEquals(
            "the live series must survive even when the caller gets the path wrong",
            listOf("rates-2025.json", "rates-2026.json"),
            namesIn(canonicalDir)
        )
        assertTrue(live.exists())
        assertTrue(prior.exists())
    }

    @Test
    fun `a missing directory is an empty result, not a failure`() {
        val absent = File(tmp.root, "never-created")

        val removed = deleteLegacyHistoryFiles(absent)

        assertEquals(emptyList<String>(), removed)
    }

    @Test
    fun `running again finds nothing left to delete`() {
        file(legacy, "rates-2025.json")

        assertEquals(listOf("rates-2025.json"), deleteLegacyHistoryFiles(legacy))
        assertEquals(
            "the sweep is idempotent, so startup can call it unconditionally",
            emptyList<String>(),
            deleteLegacyHistoryFiles(legacy)
        )
    }

    @Test
    fun `the same shape in the canonical directory is what the writer produces`() {
        // Pins the contract between the name the writer uses and the shape the
        // sweep removes: if rateYearName changed, this is what would silently
        // stop being collected.
        val produced = rateYearName(1_787_133_600_000L)
        file(legacy, produced)

        assertEquals(listOf(produced), deleteLegacyHistoryFiles(legacy))
    }
}