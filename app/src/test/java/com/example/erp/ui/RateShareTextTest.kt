package com.example.erp.ui

import com.example.erp.data.DolarQuote
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/**
 * The WhatsApp share payload as a pure function: no Intent, no Context, no
 * Android Application. Everything the reader receives is decided here, so it can
 * be asserted on a plain JVM.
 */
class RateShareTextTest {

    private val usLocale: Locale = Locale.US
    private val caracas: ZoneId = ZoneId.of("America/Caracas")

    /** 2026-08-18T14:32:00-04:00. */
    private val nowMillis = 1_787_077_920_000L

    private val quote = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 773.3112,
        anterior = 772.54,
        variacion = 0.099,
        fechaActualizacion = "2026-08-18"
    )

    private fun build(highPrecision: Boolean, target: DolarQuote? = quote): String? =
        RateShareText.build(
            quote = target,
            highPrecision = highPrecision,
            zoneId = caracas,
            locale = usLocale,
            nowMillis = nowMillis
        )

    @Test
    fun `the shared text names the rate`() {
        val text = build(highPrecision = false)

        assertTrue("expected the rate name in: $text", text!!.contains("Dólar (BCV)"))
    }

    @Test
    fun `the shared text states the rate in bolivars`() {
        val text = build(highPrecision = false)

        assertTrue("expected the Bs label in: $text", text!!.contains("Bs"))
        assertTrue("expected two decimals in: $text", text.contains("773.31"))
    }

    @Test
    fun `high precision renders four decimals`() {
        val text = build(highPrecision = true)

        assertTrue("expected four decimals in: $text", text!!.contains("773.3112"))
    }

    @Test
    fun `the shared text carries the quote day and the current time`() {
        val text = build(highPrecision = false)

        assertTrue("expected the quote day in: $text", text!!.contains("18/08/2026"))
        assertTrue("expected the current time in: $text", text.contains("14:32"))
    }

    @Test
    fun `an unreadable quote date still shares the name and the rate`() {
        // The day is a nice-to-have; dropping the whole message because one
        // field would not parse would be a worse answer than an unspecific day.
        val odd = quote.copy(fechaActualizacion = "sin fecha")

        val text = build(highPrecision = false, target = odd)

        assertTrue("expected the rate name in: $text", text!!.contains("Dólar (BCV)"))
        assertTrue("expected the rate in: $text", text.contains("773.31"))
        assertTrue("expected the raw value passed through in: $text", text.contains("sin fecha"))
    }

    @Test
    fun `an unavailable quote yields no text instead of a malformed share`() {
        // An empty share sheet is a dead control, so the builder answers null
        // and the caller never launches anything.
        assertNull(build(highPrecision = false, target = null))
    }

    @Test
    fun `a quote with a full timestamp keeps its day`() {
        val usdt = quote.copy(
            fuente = "usdt",
            nombre = "USDT (P2P)",
            promedio = 889.5,
            anterior = null,
            variacion = null,
            fechaActualizacion = "2026-08-17T10:30:00"
        )

        val text = build(highPrecision = false, target = usdt)

        assertTrue("expected the quote day in: $text", text!!.contains("17/08/2026"))
        assertTrue("expected the rate name in: $text", text.contains("USDT (P2P)"))
        assertTrue("expected two decimals in: $text", text.contains("889.50"))
    }
}