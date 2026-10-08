package com.example.erp.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.DayOfWeek
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

    private fun namesIn(dir: File): List<String> = (dir.list() ?: emptyArray()).sorted()

    /** A real Saturday (or any weekday) inside the store's current-year file. */
    private fun weekdayInThisYear(dayOfWeek: DayOfWeek): LocalDate =
        generateSequence(LocalDate.of(thisYear, 1, 1)) { it.plusDays(1) }
            .first { it.dayOfWeek == dayOfWeek }

    private fun thisYearFile(dir: File): File = File(dir, "rates-$thisYear.json")

    /** Writes straight to disk, bypassing the merge, to set up a repair scenario. */
    private fun seedFile(dir: File, samples: List<RateSample>) {
        FileHistoryStore(dir, zone).writeAtomically(thisYearFile(dir), RateHistoryCodec.encode(samples))
    }

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

    // ---------------------------------------------------------------------------------
    // The merge rule and the on-disk repair.
    //
    // Every other test here picks a different date per sample, because that is the
    // only way to exercise append's merge without the same-day rule interfering.
    // Which leaves the same-day rule itself untested, and it is the rule that
    // decides what a corrected rate for a day does to the series.
    // ---------------------------------------------------------------------------------

    @Test
    fun `a second sample for the same day replaces the first`() = runTest {
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val store = FileHistoryStore(dir, zone)
        val day = LocalDate.of(thisYear, 5, 11)

        store.append(listOf(sample("usd", 866.56, day)))
        store.append(listOf(sample("usd", 871.37, day)))

        val stored = store.readCurrentYear()
        assertEquals(
            "a day may hold one sample per source, never two",
            1,
            stored.size
        )
        assertEquals("the newer value is the one that survives", 871.37, stored.single().precio, 0.0)
    }

    @Test
    fun `one day keeps a sample for each source`() = runTest {
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val store = FileHistoryStore(dir, zone)
        val day = LocalDate.of(thisYear, 5, 11)

        store.append(
            listOf(
                sample("usd", 866.56, day),
                sample("eur", 4.0315, day),
                sample("usdt", 889.5, day)
            )
        )

        assertEquals(
            "the dedupe is per source, so same-day sources must not collapse",
            listOf("usd", "eur", "usdt"),
            store.readCurrentYear().map { it.fuente }
        )
    }

    @Test
    fun `the day after a corrected rate is untouched by the replacement`() = runTest {
        // Guards the previous test from passing for the wrong reason: a merge that
        // dropped the whole file would also leave one sample behind.
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val store = FileHistoryStore(dir, zone)
        val corrected = LocalDate.of(thisYear, 5, 11)
        val next = LocalDate.of(thisYear, 5, 12)

        store.append(listOf(sample("usd", 866.56, corrected)))
        store.append(listOf(sample("usd", 871.37, corrected)))
        store.append(listOf(sample("usd", 872.10, next)))

        assertEquals(
            listOf(871.37, 872.10),
            store.readCurrentYear().map { it.precio }
        )
    }

    // ---------------------------------------------------------------------------------
    // The on-disk repair.
    //
    // BcvSampleOnlyOnChangeTest pins the POLICY that stopped new weekend samples.
    // The damage is already on disk for whoever shipped the older build, and only
    // the store can undo it: those points stay in the file, get drawn, and flatten
    // the series. Nothing tests that pass.
    //
    // The decision itself is asserted through [bcvWeekendSamples] rather than by
    // driving ensureSeeded and watching the file change. The repair path ends in an
    // android.util.Log call, which is an unmocked stub on the JVM and throws before
    // the rewrite — so a test that exercised it here would fail on the log rather
    // than on the behaviour. The pure function IS that decision; these assert it.
    // ---------------------------------------------------------------------------------

    @Test
    fun `the BCV weekend points already on disk are recognised as having no published rate`() {
        val friday = weekdayInThisYear(DayOfWeek.FRIDAY)
        val saturday = friday.plusDays(1)
        val sunday = friday.plusDays(2)
        val monday = friday.plusDays(3)
        // Friday's rate re-stored under Saturday and Sunday: the BCV serves it
        // both days without publishing anything new.
        val stored = listOf(
            sample("usd", 866.56, friday),
            sample("usd", 866.56, saturday),
            sample("usd", 866.56, sunday),
            sample("usd", 871.37, monday)
        )

        val dropped = bcvWeekendSamples(stored, zone)

        assertEquals(
            "only the weekend duplicates are points where nothing was published",
            listOf(saturday, sunday),
            dropped.map { localDateOf(it.timestampEpochMillis, zone) }
        )
        assertEquals(
            "the published days are what the chart is left drawing",
            listOf(866.56, 871.37),
            stored.filterNot { it in dropped }.map { it.precio }
        )
    }

    @Test
    fun `a source that does move on weekends is never treated as an artefact`() {
        val saturday = weekdayInThisYear(DayOfWeek.SATURDAY)
        val sunday = saturday.plusDays(1)
        // The P2P market moves on weekends, so purging these would delete real
        // history rather than an artefact of the official rate being re-served.
        val stored = listOf(
            sample("usdt", 889.5, saturday),
            sample("usdt", 891.25, sunday),
            sample("usd", 866.56, saturday),
            sample("eur", 4.0315, sunday)
        )

        val dropped = bcvWeekendSamples(stored, zone)

        assertEquals(listOf("usd", "eur"), dropped.map { it.fuente })
    }

    @Test
    fun `a clean series has nothing to repair`() {
        val tuesday = weekdayInThisYear(DayOfWeek.TUESDAY)
        val wednesday = tuesday.plusDays(1)

        assertEquals(
            emptyList<RateSample>(),
            bcvWeekendSamples(listOf(sample("usd", 866.56, tuesday), sample("eur", 4.0315, wednesday)), zone)
        )
    }

    @Test
    fun `ensureSeeded writes no fake seed into an empty store`() = runTest {
        // The store used to invent a history so the chart was never blank. Fabricated
        // points are worse than an empty chart: they are indistinguishable from
        // published rates once they are in the series.
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        seedFile(dir, emptyList())
        val store = FileHistoryStore(dir, zone)

        store.ensureSeeded()

        assertEquals(emptyList<RateSample>(), store.readCurrentYear())
        assertEquals(0, RateHistoryCodec.decode(thisYearFile(dir).readText()).size)
        assertEquals(emptyList<String>(), tempFilesIn(dir))
    }

    @Test
    fun `ensureSeeded creates nothing when the year file is absent`() = runTest {
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val store = FileHistoryStore(dir, zone)

        store.ensureSeeded()

        assertFalse(
            "a fresh install has no history yet, and that is not a problem to fix",
            thisYearFile(dir).exists()
        )
        assertEquals(emptyList<String>(), namesIn(dir))
    }

    @Test
    fun `ensureSeeded is idempotent on an already-clean file`() = runTest {
        val dir = File(tmp.root, RATE_HISTORY_DIR_NAME)
        val tuesday = weekdayInThisYear(DayOfWeek.TUESDAY)
        seedFile(dir, listOf(sample("usd", 866.56, tuesday)))
        val store = FileHistoryStore(dir, zone)
        val afterFirst = store.let {
            it.ensureSeeded()
            it.readCurrentYear()
        }

        store.ensureSeeded()

        assertEquals(afterFirst, store.readCurrentYear())
        assertEquals(emptyList<String>(), tempFilesIn(dir))
    }
}
