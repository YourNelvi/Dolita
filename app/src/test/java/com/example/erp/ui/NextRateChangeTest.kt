package com.example.erp.ui

import com.example.erp.data.DolarQuote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/**
 * The change a next-day rate reports, as a pure function: no ViewModel, no
 * composition, no Android Application.
 *
 * The cases that matter most here are the empty ones. A rate published without
 * a previous value has no delta, and the failure this guards against is a card
 * that prints a confident `+0,00 Bs` — or worse, a percentage — for a rate the
 * API never said anything about.
 */
class NextRateChangeTest {

    private val usLocale: Locale = Locale.US

    private val future = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 780.10,
        anterior = 773.31,
        variacion = 0.878,
        fechaActualizacion = "2026-08-19"
    )

    @Test
    fun `the change subtracts the previous rate`() {
        val change = rateChangeOf(future)

        assertNotNull(change)
        assertEquals(6.79, change!!.bolivaresDelta!!, 0.0001)
    }

    @Test
    fun `the change carries the published percentage verbatim`() {
        val change = rateChangeOf(future)

        // Taken from the quote, never recomputed from a base the quote may not
        // have: the API's own variation is the only one guaranteed coherent.
        assertEquals(0.878, rateChangeOf(future)!!.percent!!, 0.0001)
    }

    @Test
    fun `a rise is UP`() {
        assertEquals(RateChangeDirection.UP, rateChangeOf(future)!!.direction)
    }

    @Test
    fun `a fall subtracts to a negative delta and reads DOWN`() {
        val falling = future.copy(promedio = 765.31, variacion = -1.03)

        val change = rateChangeOf(falling)

        assertEquals(-8.0, change!!.bolivaresDelta!!, 0.0001)
        assertEquals(RateChangeDirection.DOWN, change.direction)
    }

    @Test
    fun `an unchanged rate is FLAT, not a rise`() {
        val flat = future.copy(promedio = 773.31, variacion = 0.0)

        val change = rateChangeOf(flat)

        assertEquals(0.0, change!!.bolivaresDelta!!, 0.0001)
        // Zero must not be painted green: a flat rate is a fact, not a gain.
        assertEquals(RateChangeDirection.FLAT, change.direction)
    }

    @Test
    fun `a quote with no previous rate reports no money delta and keeps its percentage`() {
        val noAnterior = future.copy(anterior = null)

        val change = rateChangeOf(noAnterior)

        assertNotNull(change)
        assertNull("a missing anterior must not become a fabricated +0,00 Bs", change!!.bolivaresDelta)
        assertEquals(0.878, change.percent!!, 0.0001)
        assertEquals(RateChangeDirection.UP, change.direction)
    }

    @Test
    fun `a quote with no published variation reports no percentage`() {
        // The USDT/P2P shape: a rate with no previous day attached.
        val noVariacion = future.copy(variacion = null)

        val change = rateChangeOf(noVariacion)

        assertNotNull(change)
        assertNull(change!!.percent)
        assertEquals(6.79, change.bolivaresDelta!!, 0.0001)
        // The colour still follows the money when the percentage is missing,
        // rather than collapsing to a neutral that contradicts the number.
        assertEquals(RateChangeDirection.UP, change.direction)
    }

    @Test
    fun `a quote with neither half reports both as absent and stays FLAT`() {
        val bare = future.copy(anterior = null, variacion = null)

        val change = rateChangeOf(bare)

        assertNotNull(change)
        assertNull(change!!.bolivaresDelta)
        assertNull(change.percent)
        assertEquals(RateChangeDirection.FLAT, change.direction)
    }

    @Test
    fun `a money-only fall still reads DOWN`() {
        val fallingMoneyOnly = future.copy(anterior = 800.0, variacion = null)

        assertEquals(RateChangeDirection.DOWN, rateChangeOf(fallingMoneyOnly)!!.direction)
    }

    @Test
    fun `an absent quote yields no change at all`() {
        assertNull(rateChangeOf(null))
    }

    @Test
    fun `a rise is signed explicitly and labelled in bolivares`() {
        assertEquals("+1.47 Bs", formatBolivaresDelta(1.47, usLocale))
    }

    @Test
    fun `a fall keeps the minus sign`() {
        assertEquals("-0.88 Bs", formatBolivaresDelta(-0.88, usLocale))
    }

    @Test
    fun `a percentage is signed and keeps the space before its unit`() {
        assertEquals("+0.17 %", formatRatePercent(0.17, usLocale))
        assertEquals("-1.03 %", formatRatePercent(-1.03, usLocale))
    }

    @Test
    fun `the change renders at two decimals in both locales`() {
        // The screen renders this next to a rate that may carry four decimals;
        // a third decimal on the delta is noise, and it must not drift into a
        // three-decimal percentage that the card never claimed.
        assertEquals("+6.79 Bs", formatBolivaresDelta(6.79, usLocale))
        assertEquals("+0.88 %", formatRatePercent(0.878, usLocale))
    }
}
