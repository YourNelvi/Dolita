package com.example.erp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The differential gap line as a pure function: no composition, no Context, no
 * Android Application. Every word the reader sees is decided here, which is what
 * makes the wording assertable at all.
 */
class DifferentialUnitLabelTest {

    /** The "es-VE" copy the card is written for. */
    private val venezuela = Locale.forLanguageTag("es-VE")

    private fun label(
        percent: Double,
        perUnitBs: Double,
        parallelAbove: Boolean,
        fuente: String,
        locale: Locale = venezuela
    ): String = differentialGapLabel(
        percent = percent,
        perUnitBs = perUnitBs,
        parallelAbove = parallelAbove,
        fuente = fuente,
        locale = locale
    )

    @Test
    fun `the dollar tab states the gap is per dollar`() {
        val text = label(percent = 14.49, perUnitBs = 126.77, parallelAbove = true, fuente = "usd")

        assertEquals("+14,49%  ·  Bs 126,77 más por dólar", text)
    }

    @Test
    fun `the parallel tab states the gap is per dollar, because the gap is measured over the official dollar rate`() {
        // On this tab the selected rate IS the parallel, so officialReference is
        // the official DOLLAR rate (CalculatorCard): both sides are per dollar.
        val text = label(percent = 14.49, perUnitBs = 126.77, parallelAbove = true, fuente = "usdt")

        assertEquals("+14,49%  ·  Bs 126,77 más por dólar", text)
        // The unit follows the currency, never the side the rate sits on.
        assertFalse("must not name the parallel side in: $text", text.contains("paralelo"))
        assertFalse("must not name the ticker in: $text", text.contains("USDT"))
    }

    @Test
    fun `the euro tab states the gap is per euro`() {
        val text = label(percent = 14.49, perUnitBs = 141.89, parallelAbove = true, fuente = "eur")

        assertEquals("+14,49%  ·  Bs 141,89 más por euro", text)
        assertFalse("must not name the dollar on the euro tab: $text", text.contains("dólar"))
    }

    @Test
    fun `a parallel below the official reads menos, and the percent keeps its own sign`() {
        val text = label(percent = -3.2, perUnitBs = 22.1, parallelAbove = false, fuente = "usd")

        assertEquals("-3,20%  ·  Bs 22,10 menos por dólar", text)
    }

    @Test
    fun `the same percentage over two currencies yields two correctly denominated labels`() {
        // The euro parallel is derived from the dollar parallel, so measuring both
        // over their official rate yields the SAME percentage by algebra while the
        // Bs gaps differ — one per euro, one per dollar. Two Bs figures under one
        // percentage read as a wrong calculation unless each names its unit.
        val dollar = label(percent = 14.49, perUnitBs = 126.77, parallelAbove = true, fuente = "usd")
        val euro = label(percent = 14.49, perUnitBs = 141.89, parallelAbove = true, fuente = "eur")

        assertTrue("expected the shared percentage in: $dollar", dollar.contains("14,49%"))
        assertTrue("expected the shared percentage in: $euro", euro.contains("14,49%"))
        assertTrue("expected the per-dollar gap in: $dollar", dollar.contains("Bs 126,77"))
        assertTrue("expected the per-euro gap in: $euro", euro.contains("Bs 141,89"))
        assertTrue("expected the dollar named in: $dollar", dollar.endsWith("por dólar"))
        assertTrue("expected the euro named in: $euro", euro.endsWith("por euro"))
        assertNotEquals(dollar, euro)
    }

    @Test
    fun `the direction word sits with the gap and the unit closes the phrase`() {
        // "Bs 126,77 más por dólar" is one claim; "Bs 126,77 por dólar más" reads
        // as a per-dollar rate that is somehow "more", which names no comparison.
        val text = label(percent = 14.49, perUnitBs = 126.77, parallelAbove = true, fuente = "usd")

        assertTrue("expected the direction before the unit in: $text", text.endsWith("más por dólar"))
    }

    @Test
    fun `the percentage follows the locale it is given`() {
        val text = label(
            percent = 14.49,
            perUnitBs = 126.77,
            parallelAbove = true,
            fuente = "usd",
            locale = Locale.US
        )

        assertEquals("+14.49%  ·  Bs 126,77 más por dólar", text)
    }

    @Test
    fun `an unrecognised source falls back to the dollar`() {
        // Same default as the rest of the card: anything that is not the euro is
        // priced in dollars, so it must not render an unnamed or invented unit.
        val text = label(percent = 5.0, perUnitBs = 40.0, parallelAbove = true, fuente = "otra")

        assertEquals("+5,00%  ·  Bs 40,00 más por dólar", text)
    }

    @Test
    fun `a gap over a thousand keeps the card's grouping while the percentage follows the locale`() {
        // The money half is the app-wide "es-VE" format whatever the locale is, so
        // one line never renders two different number formats side by side.
        val text = label(
            percent = 40.12,
            perUnitBs = 1234.56,
            parallelAbove = true,
            fuente = "eur",
            locale = Locale.US
        )

        assertEquals("+40.12%  ·  Bs 1.234,56 más por euro", text)
    }
}