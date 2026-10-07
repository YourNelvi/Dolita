package com.example.erp.ui

import com.example.erp.data.DolarQuote
import com.example.erp.data.localDateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * The day the calendar marks as published, resolved into a real rate — as pure
 * functions, because the ViewModel that holds them needs an `Application` and a
 * coroutine scope and asserting the lookup through it would assert the wiring,
 * not the rule.
 *
 * The gap this covers is concrete: the published next-day rate lives in
 * `uiState.futureQuote`, never in `historyStore`. A lookup that only searches
 * history therefore answered "Sin datos para el …" for exactly the day the
 * calendar had just marked as published — a green circle promising a rate the
 * lookup could not produce.
 */
class CalendarioLookupResolutionTest {

    private val usLocale: Locale = Locale.US
    private val caracas: ZoneId = ZoneId.of("America/Caracas")

    private val today: LocalDate = LocalDate.of(2026, 8, 18)
    private val publishedDay: LocalDate = LocalDate.of(2026, 8, 19)

    private val todayQuote = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 773.31,
        anterior = 772.54,
        variacion = 0.099,
        fechaActualizacion = "2026-08-18"
    )

    private val futureQuote = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 780.10,
        anterior = 773.31,
        variacion = 0.878,
        fechaActualizacion = "2026-08-19"
    )

    // -----------------------------------------------------------------------
    // Which day the quote publishes
    // -----------------------------------------------------------------------

    @Test
    fun `the published day is the quote's own fechaActualizacion`() {
        assertEquals(publishedDay, publishedDayOf(futureQuote))
        // Not "tomorrow": BCV's next-day quote is for the next day it actually
        // publishes, which is not literally today + 1 when that lands on a
        // weekend. The quote is the only honest source for this date.
        assertEquals(today, publishedDayOf(todayQuote))
    }

    @Test
    fun `an absent or unreadable quote publishes no day`() {
        assertNull(publishedDayOf(null))
        assertNull(publishedDayOf(todayQuote.copy(fechaActualizacion = "sin fecha")))
        // A published day is what makes a future day selectable, so a day read
        // out of something unparseable must not become one.
        assertNull(publishedDayOf(todayQuote.copy(fechaActualizacion = "")))
    }

    // -----------------------------------------------------------------------
    // Resolving the marked day into a sample
    // -----------------------------------------------------------------------

    @Test
    fun `the published day resolves to that day's own rate`() {
        val sample = nextDaySampleOf(futureQuote, "usd", publishedDay, caracas)

        assertNotNull(sample)
        assertEquals(780.10, sample!!.precio, 0.0001)
        assertEquals("usd", sample.fuente)
        assertEquals("Dólar (BCV)", sample.nombre)
        // The published pair travels with the rate: a delta computed against a
        // different day's rate is the bug the NEXT_RATE branch already fixed.
        assertEquals(773.31, sample.anterior!!, 0.0001)
        assertEquals(0.878, sample.variacion!!, 0.0001)
        // And the timestamp reads back as the marked day, so the history-side
        // identity of the sample agrees with the day that was tapped.
        assertEquals(publishedDay, localDateOf(sample.timestampEpochMillis, caracas))
    }

    @Test
    fun `the match is exact, never a neighbouring day`() {
        // The day before and the day after the published one both exist on the
        // calendar. Neither may borrow the published rate: that would print a
        // number under a date the BCV never said it belonged to.
        assertNull(nextDaySampleOf(futureQuote, "usd", publishedDay.minusDays(1), caracas))
        assertNull(nextDaySampleOf(futureQuote, "usd", publishedDay.plusDays(1), caracas))
        assertNull(nextDaySampleOf(futureQuote, "usd", today, caracas))
    }

    @Test
    fun `another source's published day is not this source's day`() {
        val eur = futureQuote.copy(fuente = "eur", nombre = "Euro (BCV)")

        assertNull(nextDaySampleOf(eur, "usd", publishedDay, caracas))
        assertNotNull(nextDaySampleOf(eur, "eur", publishedDay, caracas))
    }

    @Test
    fun `no published quote resolves nothing`() {
        assertNull(nextDaySampleOf(null, "usd", publishedDay, caracas))
    }

    @Test
    fun `a quote with an unreadable day resolves nothing rather than guessing`() {
        assertNull(
            nextDaySampleOf(
                futureQuote.copy(fechaActualizacion = "sin fecha"),
                "usd",
                publishedDay,
                caracas
            )
        )
    }

    // -----------------------------------------------------------------------
    // End to end: the marked day must reach the calculator and the share text
    // as the MARKED day, never as today.
    // -----------------------------------------------------------------------

    @Test
    fun `the marked day reaches the calculator as the marked day, not today`() {
        // This is the whole feature in order: the calendar offers the published
        // day, the lookup resolves it, the reader taps the selection bar, and the
        // calculator converts at THAT day's rate.
        val sample = nextDaySampleOf(futureQuote, "usd", publishedDay, caracas)
        val label = CalendarioDateRules.lookupLabel(publishedDay)

        val resolved = CalculatorRateResolver.resolve(
            today = todayQuote,
            future = futureQuote,
            futureSource = CalculatorRateSource.TODAY,
            selectedSample = sample,
            selectedLabel = label,
            dateSource = CalculatorRateSource.SELECTED_DATE
        )

        assertNotNull(resolved)
        assertEquals(780.10, resolved!!.promedio, 0.0001)
        // The marked day, both in the quote's day field and in the name the
        // calculator renders next to the amount.
        assertEquals("19/08/2026", resolved.fechaActualizacion)
        assertEquals("Dólar (BCV) (19/08/2026)", resolved.nombre)
        assertTrue(
            "the calculator must not claim today for the marked day",
            !resolved.fechaActualizacion.contains("18/08/2026")
        )
    }

    @Test
    fun `the shared result names the marked day, never today`() {
        val sample = nextDaySampleOf(futureQuote, "usd", publishedDay, caracas)
        val resolved = CalculatorRateResolver.resolve(
            today = todayQuote,
            future = futureQuote,
            futureSource = CalculatorRateSource.TODAY,
            selectedSample = sample,
            selectedLabel = CalendarioDateRules.lookupLabel(publishedDay),
            dateSource = CalculatorRateSource.SELECTED_DATE
        )

        val text = CalculatorShareText.build(
            quote = resolved,
            bolivaresDigits = "123456",
            divisaDigits = "158",
            highPrecision = false,
            zoneId = caracas,
            locale = usLocale,
            nowMillis = 1_787_077_920_000L
        )

        assertNotNull(text)
        assertTrue("expected the marked day in: $text", text!!.contains("19/08/2026"))
        assertTrue(
            "expected today not to be claimed in: $text",
            !text.contains("18/08/2026")
        )
        assertTrue("expected the marked rate in: $text", text.contains("Bs 780.10"))
    }

    @Test
    fun `the marked day and the next-rate toggle stay two different things`() {
        // Both resolve to the same published rate, but they must not be
        // indistinguishable in the reader's output: one day was picked from the
        // calendar, the other is the next-rate card.
        val sample = nextDaySampleOf(futureQuote, "usd", publishedDay, caracas)

        val fromCalendar = CalculatorRateResolver.resolve(
            today = todayQuote,
            future = futureQuote,
            futureSource = CalculatorRateSource.TODAY,
            selectedSample = sample,
            selectedLabel = CalendarioDateRules.lookupLabel(publishedDay),
            dateSource = CalculatorRateSource.SELECTED_DATE
        )
        val fromNextRateBar = CalculatorRateResolver.resolve(
            today = todayQuote,
            future = futureQuote,
            futureSource = CalculatorRateSource.NEXT_RATE,
            selectedSample = null,
            selectedLabel = null,
            dateSource = CalculatorRateSource.TODAY
        )

        assertEquals(fromCalendar!!.promedio, fromNextRateBar!!.promedio, 0.0001)
        assertEquals("19/08/2026", fromCalendar.fechaActualizacion)
        assertTrue(fromNextRateBar.nombre.endsWith("(futura)"))
        assertTrue(!fromCalendar.nombre.contains("(futura)"))
    }

    @Test
    fun `a day with genuinely no rate stays an honest no-data result`() {
        // The exemption exists for the published day only: a future day nobody
        // published still resolves to nothing, and the screen's honest
        // "Sin datos para el …" row is the correct answer for it.
        assertNull(nextDaySampleOf(futureQuote, "usd", publishedDay.plusDays(1), caracas))
        assertFalse(
            "a future day nobody published must not be selectable",
            CalendarioDateRules.isSelectable(publishedDay.plusDays(1), today, publishedDay)
        )
    }
}