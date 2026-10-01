package com.example.erp.data

import com.example.erp.ui.HISTORICO_MUESTRAS_HORARIAS
import com.example.erp.ui.historicoTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * The backfill gate and the chart header, which are two halves of one promise:
 * the header claims a window, the gate is what makes the claim come true.
 *
 * Fixed at 2026-10-01 to match the device this was diagnosed on.
 */
class HistoryDepthTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val zone = ZoneId.of("America/Caracas")

    private fun days(vararg offsets: Long) = offsets.map { today.minusDays(it) }

    // --- gate -------------------------------------------------------------

    @Test
    fun `a deep series reaching yesterday needs no backfill`() {
        // 16 days back, fresh: the state a healthy install settles into, and the
        // one that keeps the historical fetch from running on every app open.
        assertFalse(needsHistoricalBackfill(days(15, 8, 1, 0), today))
    }

    @Test
    fun `a single recent sample is not enough to skip the backfill`() {
        // The regression: recency alone passed this forever, so a wiped store
        // stayed three days long and never recovered.
        assertTrue(needsHistoricalBackfill(days(0), today))
        assertTrue(needsHistoricalBackfill(days(1, 0), today))
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
        // Stale AND short: the common case after the app sat closed for a week.
        assertTrue(needsHistoricalBackfill(days(15, 8, 3), today))
    }

    @Test
    fun `the required window is configurable so a test cannot drift from it`() {
        assertFalse(needsHistoricalBackfill(days(5, 0), today, requiredDays = 5))
        assertTrue(needsHistoricalBackfill(days(5, 0), today, requiredDays = 6))
    }

    // --- honest header ----------------------------------------------------

    private fun samples(fuente: String, offsets: List<Long>) = offsets.map {
        RateSample(
            fuente = fuente,
            nombre = fuente.uppercase(),
            precio = 860.0,
            timestampEpochMillis = today.minusDays(it).atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }

    @Test
    fun `a full window keeps the full-window wording`() {
        val title = historicoTitle(samples("usd", listOf(14, 7, 0)), zone)
        assertEquals("Últimos 15 días (toca un punto para ver precio)", title)
    }

    @Test
    fun `a short series says how short it is instead of claiming 15 days`() {
        // 29/09 -> 01/10 is the exact span seen on the device after the wipe.
        val title = historicoTitle(samples("usd", listOf(2, 1, 0)), zone)
        assertEquals("Últimos 3 días · histórico en carga (toca un punto para ver precio)", title)
    }

    @Test
    fun `a single day never claims 15 days`() {
        val title = historicoTitle(samples("usd", listOf(0)), zone)
        assertEquals("Últimos 1 día · histórico en carga (toca un punto para ver precio)", title)
    }

    @Test
    fun `weekend gaps count as span, not as missing samples`() {
        // BCV does not publish on weekends. Counting distinct samples would report
        // fewer days than the axis actually draws.
        val title = historicoTitle(samples("usd", listOf(14, 13, 11, 9, 0)), zone)
        assertTrue(title, title.startsWith("Últimos 15 días"))
    }

    @Test
    fun `an hourly series reports samples, not days`() {
        val short = historicoTitle(samples("usdt", listOf(0)), zone)
        assertEquals(
            "Muestreo horario: 1 de $HISTORICO_MUESTRAS_HORARIAS (toca un punto para ver precio)",
            short
        )

        val full = historicoTitle(samples("usdt", (0 until 48).map { it.toLong() }), zone)
        assertEquals(
            "Últimas $HISTORICO_MUESTRAS_HORARIAS muestras horarias (toca un punto para ver precio)",
            full
        )
    }

    @Test
    fun `an empty series does not claim any coverage`() {
        assertFalse(historicoTitle(emptyList(), zone).contains("15"))
    }
}