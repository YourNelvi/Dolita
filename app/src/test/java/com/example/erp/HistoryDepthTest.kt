package com.example.erp.data

import com.example.erp.data.HISTORICO_PUNTOS
import com.example.erp.data.RateSample
import com.example.erp.data.needsHistoricalBackfill
import com.example.erp.ui.HISTORICO_MUESTRAS_HORARIAS
import com.example.erp.ui.historicoTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * The backfill gate and the chart header: two halves of one promise. The gate is
 * what makes the series deep enough to draw, and the header has to describe the
 * range the chart actually draws.
 *
 * Fixed at 2026-10-01 (a Thursday), the date this was diagnosed on.
 */
class HistoryDepthTest {

    private val today = LocalDate.of(2026, 10, 1)
    

    /** Consecutive days ending today, the way a daily BCV series accumulates. */
    private fun days(vararg offsets: Int) = offsets.map { today.minusDays(it.toLong()) }

    /**
     * Offsets of the last [count] PUBLISHED days ending today, skipping
     * weekends the way BCV does. Oct 2026: 01 is Thursday, so offset 4 is Sunday.
     */
    private fun publishedDayOffsets(count: Int): List<Int> {
        val out = mutableListOf<Int>()
        var offset = 0
        while (out.size < count) {
            if (today.minusDays(offset.toLong()).dayOfWeek != DayOfWeek.SATURDAY &&
                today.minusDays(offset.toLong()).dayOfWeek != DayOfWeek.SUNDAY
            ) out.add(offset)
            offset++
        }
        return out
    }

    // --- gate -------------------------------------------------------------

    @Test
    fun `a full chart that reaches yesterday needs no backfill`() {
        // The state a healthy install settles into, and the one that keeps the
        // historical fetch from running on every app open.
        val offsets = publishedDayOffsets(HISTORICO_PUNTOS)
        assertFalse(needsHistoricalBackfill(days(*offsets.toIntArray()), today))
    }

    @Test
    fun `a single recent sample is not enough to skip the backfill`() {
        // The regression: recency alone passed this forever, so a wiped store
        // stayed three days long and never recovered.
        assertTrue(needsHistoricalBackfill(days(0), today))
        assertTrue(needsHistoricalBackfill(days(1, 0), today))
    }

    @Test
    fun `too few samples backfills even when the newest is today`() {
        // Deep enough to look current, too thin to fill the chart: 11 published
        // days is a fresh October, and the chart would draw a stub.
        val offsets = publishedDayOffsets(HISTORICO_PUNTOS - 4)
        assertTrue(needsHistoricalBackfill(days(*offsets.toIntArray()), today))
    }

    @Test
    fun `an empty store needs a backfill`() {
        assertTrue(needsHistoricalBackfill(emptyList(), today))
    }

    @Test
    fun `a future-dated sample alone is not treated as depth`() {
        // The next rate is stored as a sample. Counting it would both fake depth
        // and claim completeness for a rate that has not been published yet.
        assertTrue(needsHistoricalBackfill(days(-1), today))
    }

    @Test
    fun `a deep series that stops before yesterday still backfills`() {
        // Stale AND full: the common case after the app sat closed for a week.
        assertTrue(needsHistoricalBackfill(days(*publishedDayOffsets(HISTORICO_PUNTOS).toIntArray()), today.minusDays(7)))
    }

    @Test
    fun `the required point count is configurable so a test cannot drift from it`() {
        assertFalse(needsHistoricalBackfill(days(0, 1, 2, 3, 4), today, requiredSamples = 5))
        assertTrue(needsHistoricalBackfill(days(0, 1, 2, 3, 4), today, requiredSamples = 6))
    }

    // --- honest header ----------------------------------------------------

    private fun samples(fuente: String, offsets: List<Int>) = offsets.map {
        RateSample(
            fuente = fuente,
            nombre = fuente.uppercase(),
            precio = 860.0,
            timestampEpochMillis = today.minusDays(it.toLong()).atStartOfDay(ZoneId.of("America/Caracas")).toInstant().toEpochMilli()
        )
    }

    @Test
    fun `a full chart reports published days, not elapsed calendar time`() {
        // THE REGRESSION, both ways. Fifteen BCV samples skip weekends and cover
        // 21+ calendar days, but a Saturday is not a day without a rate here --
        // it is not a day at all. Counting elapsed time called a complete chart
        // "22 días" and read as missing data when nothing was missing.
        val offsets = publishedDayOffsets(HISTORICO_PUNTOS)

        val title = historicoTitle(samples("usd", offsets))
        assertEquals(
            "Últimos $HISTORICO_PUNTOS días hábiles (toca un punto para ver precio)",
            title
        )
        // The elapsed window really is wider; the header must not adopt it.
        assertFalse(title, title.contains("21"))
        assertFalse(title, title.contains("22"))
    }

    @Test
    fun `the business-day unit is named so the wider axis is not a contradiction`() {
        val title = historicoTitle(samples("usd", publishedDayOffsets(HISTORICO_PUNTOS)))
        assertTrue(title, title.contains("días hábiles"))
    }

    @Test
    fun `a short series says how short it is instead of claiming a full chart`() {
        // 29/09 -> 01/10 is the exact span seen on the device after the wipe.
        val title = historicoTitle(samples("usd", listOf(2, 1, 0)))
        assertEquals("Últimos 3 días hábiles · histórico en carga (toca un punto para ver precio)", title)
    }

    @Test
    fun `a single day never claims a full chart`() {
        val title = historicoTitle(samples("usd", listOf(0)))
        assertEquals("Último día hábil · histórico en carga (toca un punto para ver precio)", title)
    }

    @Test
    fun `only the last window of samples is described`() {
        // The header must mirror the chart's takeLast, or it describes a longer
        // history than the one drawn underneath it.
        val offsets = (0 until 40).toList()
        val title = historicoTitle(samples("usd", offsets))
        // takeLast(15) of 40 stored days: the header must say 15, not 40, or it
        // describes a longer history than the line drawn underneath it.
        assertEquals("Últimos 15 días hábiles (toca un punto para ver precio)", title)
    }

    @Test
    fun `an hourly series reports samples, not days`() {
        val short = historicoTitle(samples("usdt", listOf(0)))
        assertEquals(
            "Muestreo horario: 1 de $HISTORICO_MUESTRAS_HORARIAS (toca un punto para ver precio)",
            short
        )

        val full = historicoTitle(samples("usdt", (0 until HISTORICO_MUESTRAS_HORARIAS).toList()))
        assertEquals(
            "Últimas $HISTORICO_MUESTRAS_HORARIAS muestras horarias (toca un punto para ver precio)",
            full
        )
    }

    @Test
    fun `an empty series does not claim any coverage`() {
        assertFalse(historicoTitle(emptyList()).contains("días hábiles"))
    }
}