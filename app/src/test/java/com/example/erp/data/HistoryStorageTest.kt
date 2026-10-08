package com.example.erp.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * The storage seam itself: what the store wrote, what it reads back, and what it
 * leaves behind on disk.
 *
 * The suite covered the pure helpers around this file — year derivation, the
 * sampling policy, the codec — but never the disk round-trip. A store that
 * wrote something nothing could read, or that scattered temp files on every
 * failed write, passed everything.
 */
class HistoryStorageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val zone: ZoneId = ZoneId.of("America/Caracas")

    private val thisYear: Int = LocalDate.now(zone).year
    private val priorYear: Int = thisYear - 1

    private fun epochMillisAt(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun sample(fuente: String, precio: Double, date: LocalDate) = RateSample(
        fuente = fuente,
        nombre = "Fuente $fuente",
        precio = precio,
        timestampEpochMillis = epochMillisAt(date),
        anterior = precio - 0.05,
        variacion = 0.12
    )

    private fun tempFilesIn(dir: File): List<String> =
        (dir.list() ?: emptyArray()).filter { it.endsWith(".tmp") }.sorted()

    @Test
    fun `a sample either side of 31 December lands in its own year file`() = runTest {
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val store = FileHistoryStore(dir, zone)

        val lastDayOfPriorYear = LocalDate.of(priorYear, 12, 31)
        val firstDayOfThisYear = LocalDate.of(thisYear, 1, 1)
        store.append(
            listOf(
                sample("usd", 36.72, lastDayOfPriorYear),
                sample("usd", 36.80, firstDayOfThisYear)
            )
        )

        val priorFile = File(dir, rateYearName(epochMillisAt(lastDayOfPriorYear), zone))
        val thisFile = File(dir, rateYearName(epochMillisAt(firstDayOfThisYear), zone))
        assertTrue("$priorFile must exist", priorFile.exists())
        assertTrue("$thisFile must exist", thisFile.exists())

        val priorDecoded = RateHistoryCodec.decode(priorFile.readText())
        assertEquals(1, priorDecoded.size)
        assertEquals(lastDayOfPriorYear, localDateOf(priorDecoded.single().timestampEpochMillis, zone))

        val thisDecoded = RateHistoryCodec.decode(thisFile.readText())
        assertEquals(1, thisDecoded.size)
        assertEquals(firstDayOfThisYear, localDateOf(thisDecoded.single().timestampEpochMillis, zone))

        // The boundary split, not a merge into one file: readCurrentYear is
        // documented as "this calendar year" and the widget reads it as such.
        assertEquals(
            listOf(firstDayOfThisYear),
            store.readCurrentYear().map { localDateOf(it.timestampEpochMillis, zone) }
        )
        assertEquals("neither write may leave a temp file behind", emptyList<String>(), tempFilesIn(dir))
    }

    @Test
    fun `what was written reads back with every field intact`() = runTest {
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val store = FileHistoryStore(dir, zone)
        val original = sample("eur", 4.0281, LocalDate.of(thisYear, 2, 3))

        store.append(listOf(original))

        assertEquals(listOf(original), store.readCurrentYear())
    }

    @Test
    fun `writeAtomically leaves no temp file behind on success`() {
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val target = File(dir, "rates-$thisYear.json")
        val store = FileHistoryStore(dir, zone)

        store.writeAtomically(target, """{"anio":$thisYear,"samples":[]}""")

        assertTrue("the target must exist after a successful write", target.exists())
        assertEquals(emptyList<String>(), tempFilesIn(dir))
    }

    @Test
    fun `writeAtomically leaves no temp file behind when the move fails`() {
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val target = File(dir, "rates-$thisYear.json")
        // A non-empty directory standing where the year file has to land. The
        // temp file is written first, so the move is the step that fails.
        assertTrue("precondition: the target path must be occupied", target.mkdirs())
        File(target, "occupant").writeText("in the way")

        val store = FileHistoryStore(dir, zone)
        val thrown = runCatching {
            store.writeAtomically(target, """{"anio":$thisYear,"samples":[]}""")
        }.exceptionOrNull()

        assertNotNull("precondition: the move has to fail for this to test cleanup", thrown)
        assertEquals(
            "a failed write must not leave its temp file on disk",
            emptyList<String>(),
            tempFilesIn(dir)
        )
    }
}
