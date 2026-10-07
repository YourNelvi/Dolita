package com.example.erp.ui

import com.example.erp.data.DolarQuote
import com.example.erp.data.RateSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/**
 * The calculator's share payload as a pure function: no Intent, no Context, no
 * composition. The reader of a shared message has no way to ask what the app
 * meant, so everything that leaves the device is asserted here.
 */
class CalculatorShareTextTest {

    private val usLocale: Locale = Locale.US
    private val caracas: ZoneId = ZoneId.of("America/Caracas")

    /** 2026-08-18T14:32:00-04:00. */
    private val nowMillis = 1_787_077_920_000L

    private val today = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 773.31,
        anterior = 772.54,
        variacion = 0.099,
        fechaActualizacion = "2026-08-18"
    )

    private val future = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 780.10,
        anterior = 773.31,
        variacion = 0.878,
        fechaActualizacion = "2026-08-19"
    )

    private val sample = RateSample(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        precio = 750.42,
        timestampEpochMillis = 1_787_155_200_000L,
        anterior = 749.10,
        variacion = 0.176
    )

    /** "123456" is 1.234,56 — the same digit string the Bolívares field holds. */
    private val bolivares = "123456"
    private val divisa = "158"

    private fun build(
        quote: DolarQuote?,
        bolivares: String = this.bolivares,
        divisa: String = this.divisa,
        highPrecision: Boolean = false
    ): String? = CalculatorShareText.build(
        quote = quote,
        bolivaresDigits = bolivares,
        divisaDigits = divisa,
        highPrecision = highPrecision,
        zoneId = caracas,
        locale = usLocale,
        nowMillis = nowMillis
    )

    @Test
    fun `both amounts travel in the message`() {
        val text = build(today)

        // The whole point of the control: not just the rate, but the money the
        // user typed. One without the other is a different feature.
        assertTrue("expected the bolivares amount in: $text", text!!.contains("Bs 1.234,56"))
        assertTrue("expected the divisa amount in: $text", text.contains("USD 1,58"))
    }

    @Test
    fun `the shared text names the rate and states it in bolivars`() {
        val text = build(today)

        assertTrue("expected the rate name in: $text", text!!.contains("Dólar (BCV)"))
        assertTrue("expected the rate in Bs in: $text", text.contains("Bs 773.31"))
    }

    @Test
    fun `high precision renders four decimals`() {
        val text = build(today, highPrecision = true)

        assertTrue("expected four decimals in: $text", text!!.contains("Bs 773.3100"))
    }

    @Test
    fun `the shared text carries the rate day and the moment it was sent`() {
        val text = build(today)

        assertTrue("expected the quote day in: $text", text!!.contains("18/08/2026"))
        assertTrue("expected the current time in: $text", text.contains("14:32"))
    }

    @Test
    fun `an unavailable quote yields no text`() {
        assertNull(build(null))
    }

    @Test
    fun `two empty amounts yield no text`() {
        // The rate alone is already shareable from the hero: a message that
        // carries a rate and two blank amounts teaches the reader nothing.
        assertNull(build(today, bolivares = "", divisa = ""))
    }

    @Test
    fun `a next-day rate reports its own day and never today's`() {
        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = future,
            futureSource = CalculatorRateSource.NEXT_RATE,
            selectedSample = null,
            selectedLabel = null,
            dateSource = CalculatorRateSource.TODAY
        )

        val text = build(resolved)

        // The distinction the reader cannot recover on their own: this was NOT
        // today's rate.
        assertTrue("expected the future day in: $text", text!!.contains("19/08/2026"))
        assertTrue(
            "expected today not to be claimed in: $text",
            !text.contains("18/08/2026")
        )
        assertTrue("expected the future rate name in: $text", text.contains("(futura)"))
        assertTrue("expected the future rate in: $text", text.contains("Bs 780.10"))
    }

    @Test
    fun `a date-selected rate reports the selected day and never today's`() {
        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = null,
            futureSource = CalculatorRateSource.TODAY,
            selectedSample = sample,
            selectedLabel = "17/08/2026",
            dateSource = CalculatorRateSource.SELECTED_DATE
        )

        val text = build(resolved)

        assertTrue("expected the selected day in: $text", text!!.contains("17/08/2026"))
        assertTrue(
            "expected today not to be claimed in: $text",
            !text.contains("18/08/2026")
        )
        assertTrue("expected the selected rate in: $text", text.contains("Bs 750.42"))
    }

    @Test
    fun `an unreadable quote date still shares the amounts and the rate`() {
        val odd = today.copy(fechaActualizacion = "sin fecha")

        val text = build(odd)

        assertTrue("expected the amount line in: $text", text!!.contains("Bs 1.234,56"))
        assertTrue("expected the rate in: $text", text.contains("Bs 773.31"))
        assertTrue("expected the raw value passed through in: $text", text.contains("sin fecha"))
    }

    @Test
    fun `a single typed amount is shared without an empty half`() {
        val text = build(today, divisa = "")

        // The conversion line is assembled from the sides that exist, so one
        // typed amount must not come out as "Bs 1.234,56 = " with a hole on the
        // end — and the rate line below it is unaffected by the missing side.
        val lines = text!!.lines()
        assertEquals("Bs 1.234,56", lines[1])
        assertEquals("1 USD = Bs 773.31", lines[2])
    }

    @Test
    fun `both typed amounts share as one conversion line`() {
        val text = build(today)

        assertEquals("Bs 1.234,56 = USD 1,58", text!!.lines()[1])
    }
}
