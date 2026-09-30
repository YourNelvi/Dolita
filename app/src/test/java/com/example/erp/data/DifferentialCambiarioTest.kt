package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * The cambiario differential, using the rates the app actually showed:
 * 1 USDT = 954,06 Bs at P2P against 859,06 Bs for the official dollar.
 */
class DifferentialCambiarioTest {

    private val p2p = 954.06
    private val official = 859.06

    // 100 USDT at P2P is 95.406 Bs.
    private val hundredUsdt = BigDecimal("100")

    @Test
    fun `100 USDT report its official-dollar value and the differential cost`() {
        val d = CurrencyConverter.differentialCambiario(
            entered = hundredUsdt,
            rate = p2p,
            usdRate = official,
            editingBolivares = false
        )!!
        // 100 x 954,06 = 95.406 Bs, and 95.406 / 859,06 = 111,0586 -> 111,06.
        assertEquals(BigDecimal("111.06"), d.officialDollars)
        // Official would have charged 100 x 859,06 = 85.906 Bs.
        assertEquals(BigDecimal("9500.00"), d.costExtraBs.setScale(2, java.math.RoundingMode.HALF_UP))
        assertTrue(d.overpaying)
    }

    @Test
    fun `the amount entered in bolivars reports the same figures as the same money in USDT`() {
        val bs = BigDecimal("95406.00")
        val fromBs = CurrencyConverter.differentialCambiario(bs, p2p, official, editingBolivares = true)!!
        val fromUsdt = CurrencyConverter.differentialCambiario(hundredUsdt, p2p, official, editingBolivares = false)!!

        // The whole point: the numbers describe the MONEY, not the focused field.
        // Deriving them per-field is how one purchase reported two totals.
        assertEquals(fromUsdt.officialDollars, fromBs.officialDollars)
        assertEquals(fromUsdt.costExtraBs.setScale(2, java.math.RoundingMode.HALF_UP), fromBs.costExtraBs.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(fromUsdt.amountInBs.setScale(2, java.math.RoundingMode.HALF_UP), fromBs.amountInBs.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun `an official rate above the parallel one reads as a saving, not a cost`() {
        val d = CurrencyConverter.differentialCambiario(
            entered = hundredUsdt,
            rate = 800.00,
            usdRate = 859.06,
            editingBolivares = false
        )!!
        assertFalse(d.overpaying)
        assertTrue(d.costExtraBs.signum() < 0)
    }

    @Test
    fun `a zero or negative amount produces no differential`() {
        assertNull(CurrencyConverter.differentialCambiario(BigDecimal.ZERO, p2p, official, false))
        assertNull(CurrencyConverter.differentialCambiario(BigDecimal("-10"), p2p, official, false))
    }

    @Test
    fun `an unusable rate produces no differential instead of a fabricated one`() {
        assertNull(CurrencyConverter.differentialCambiario(hundredUsdt, 0.0, official, false))
        assertNull(CurrencyConverter.differentialCambiario(hundredUsdt, p2p, 0.0, false))
        assertNull(CurrencyConverter.differentialCambiario(hundredUsdt, -1.0, official, false))
    }
}
