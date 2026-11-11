package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The pairwise rate differences, the cambiario differential, and the zero-key
 * trap.
 *
 * A reader comparing the quoted rates needs the real difference between them,
 * not a parallel estimate: on the dollar tab the euro row is EUR - USD, and on
 * the USDT tab it is EUR - USDT. The differential tests below cover the other
 * question: what a purchase overpaid as money, not as a percentage.
 */
class CalculatorZerosAndDifferentialTest {

    // Live shapes from the real endpoint on 12/10/2026.
    private val usd = 875.65
    private val eur = 980.61
    private val usdt = 1008.26

    // ---------------------------------------------------------------- zeros

    @Test
    fun `a run of zeros collapses to one so the key never occupies space`() {
        assertEquals("0", CurrencyConverter.normalizeDigits("0"))
        assertEquals("0", CurrencyConverter.normalizeDigits("00"))
        assertEquals("0", CurrencyConverter.normalizeDigits("0000000000"))
    }

    @Test
    fun `the collapsed zero still means the same amount as the long run did`() {
        val long = CurrencyConverter.parseDigits("0000000000")!!
        val short = CurrencyConverter.parseDigits(CurrencyConverter.normalizeDigits("0000000000"))!!
        assertEquals(long, short)
    }

    @Test
    fun `a real number is left exactly as typed`() {
        assertEquals("500", CurrencyConverter.normalizeDigits("500"))
        assertEquals("1005", CurrencyConverter.normalizeDigits("1005"))
        assertEquals("10", CurrencyConverter.normalizeDigits("10"))
    }

    @Test
    fun `an interior zero is not collapsed`() {
        // Only a run of zeros is a trap. 1005 is 10,05 and must survive intact:
        // collapsing it would change the amount the reader typed.
        assertEquals("1005", CurrencyConverter.normalizeDigits("1005"))
        assertEquals("10001", CurrencyConverter.normalizeDigits("10001"))
    }

    @Test
    fun `leading zeros never reach the reader`() {
        // The zero that survives from an earlier keystroke must not end up in
        // the formatted amount: "0500000" and "5000000" are the same 50.000,00,
        // and showing the first one tells the reader a number they did not type.
        assertEquals("5000000", CurrencyConverter.normalizeDigits("05000000"))
        assertEquals("500", CurrencyConverter.normalizeDigits("0500"))
        assertEquals("5", CurrencyConverter.normalizeDigits("005"))
    }

    @Test
    fun `a leading zero does not change the amount`() {
        val withZero = CurrencyConverter.parseDigits(CurrencyConverter.normalizeDigits("05000000"))!!
        val without = CurrencyConverter.parseDigits("5000000")!!
        assertEquals(without, withZero)
        assertEquals("50.000,00", CurrencyConverter.format(withZero))
    }

    @Test
    fun `typing after a zero keeps working`() {
        // The trap is only a LEADING zero. Once there is a real digit, a zero
        // is an ordinary digit and must keep being accepted.
        assertEquals("500", CurrencyConverter.normalizeDigits("50" + "0"))
    }

    @Test
    fun `non digits are dropped and the length is capped`() {
        assertEquals("123", CurrencyConverter.normalizeDigits("1a2b3"))
        assertEquals("1234567890", CurrencyConverter.normalizeDigits("123456789012345"))
    }

    @Test
    fun `an empty field stays empty`() {
        assertEquals("", CurrencyConverter.normalizeDigits(""))
    }

    // --------------------------------------------------------- differential

    @Test
    fun `the dollar tab reports the real euro and usdt differences`() {
        val d = diferenciasPorMoneda(
            selectedFuente = "usd",
            usdRate = usd,
            usdtRate = usdt,
            eurRate = eur
        )
        // 980,61 - 875,65 = 104,96, not the old synthetic 148,51.
        assertEquals(BigDecimal("104.96"), d.euroBs)
        // 1.008,26 - 875,65 = 132,61.
        assertEquals(BigDecimal("132.61"), d.usdtBs)
        assertNull(d.dolarBs)
    }

    @Test
    fun `the euro tab compares the official dollar against the selected euro`() {
        val d = diferenciasPorMoneda(
            selectedFuente = "eur",
            usdRate = usd,
            usdtRate = usdt,
            eurRate = eur
        )
        // 875,65 - 980,61 = -104,96; the card shows the absolute value.
        assertEquals(BigDecimal("-104.96"), d.dolarBs)
        // 1.008,26 - 980,61 = 27,65.
        assertEquals(BigDecimal("27.65"), d.usdtBs)
        assertNull(d.euroBs)
    }

    @Test
    fun `the usdt tab compares both official rates against the selected usdt`() {
        val d = diferenciasPorMoneda(
            selectedFuente = "usdt",
            usdRate = usd,
            usdtRate = usdt,
            eurRate = eur
        )
        // 875,65 - 1.008,26 = -132,61.
        assertEquals(BigDecimal("-132.61"), d.dolarBs)
        // 980,61 - 1.008,26 = -27,65, not the old synthetic 148,51.
        assertEquals(BigDecimal("-27.65"), d.euroBs)
        assertNull(d.usdtBs)
    }

    @Test
    fun `live BCV rates report the real euro and euro-usdt differences`() {
        // The rates the app showed on 09/11/2026: USD 897,5383,
        // EUR 1.001,18602288 and USDT 1.041,0683.
        val liveUsd = 897.5383
        val liveEur = 1001.18602288
        val liveUsdt = 1041.0683

        val fromUsd = diferenciasPorMoneda(
            selectedFuente = "usd",
            usdRate = liveUsd,
            usdtRate = liveUsdt,
            eurRate = liveEur
        )
        // EUR - USD = 103,65, the number the reader expects.
        assertEquals(BigDecimal("103.65"), fromUsd.euroBs)
        // USDT - USD = 143,53.
        assertEquals(BigDecimal("143.53"), fromUsd.usdtBs)

        val fromUsdt = diferenciasPorMoneda(
            selectedFuente = "usdt",
            usdRate = liveUsd,
            usdtRate = liveUsdt,
            eurRate = liveEur
        )
        // EUR - USDT = -39,88, not the old synthetic 160,10.
        assertEquals(BigDecimal("-39.88"), fromUsdt.euroBs)
        assertEquals(BigDecimal("-143.53"), fromUsdt.dolarBs)
    }

    @Test
    fun `a missing rate yields no figure rather than a gap of zero`() {
        val d = diferenciasPorMoneda(
            selectedFuente = "usd",
            usdRate = usd,
            usdtRate = usdt,
            eurRate = null
        )
        assertNull(d.euroBs)
        assertEquals(BigDecimal("132.61"), d.usdtBs)
        assertNull(d.dolarBs)
    }

    @Test
    fun `an unusable selected rate yields nothing at all`() {
        val d = diferenciasPorMoneda(
            selectedFuente = "usd",
            usdRate = 0.0,
            usdtRate = usdt,
            eurRate = eur
        )
        assertNull(d.dolarBs)
        assertNull(d.euroBs)
        assertNull(d.usdtBs)
    }

    @Test
    fun `an unusable other rate yields no row for that currency`() {
        val d = diferenciasPorMoneda(
            selectedFuente = "usd",
            usdRate = usd,
            usdtRate = 0.0,
            eurRate = eur
        )
        assertEquals(BigDecimal("104.96"), d.euroBs)
        assertNull(d.usdtBs)
    }

    @Test
    fun `identical rates mean no difference`() {
        val d = diferenciasPorMoneda(
            selectedFuente = "usd",
            usdRate = usd,
            usdtRate = usd,
            eurRate = usd
        )
        assertEquals(BigDecimal("0.00"), d.euroBs)
        assertEquals(BigDecimal("0.00"), d.usdtBs)
    }

    @Test
    fun `a rate below the selected one is reported as a negative, not a fabricated zero`() {
        val d = diferenciasPorMoneda(
            selectedFuente = "usd",
            usdRate = usd,
            usdtRate = 800.0,
            eurRate = eur
        )
        assertEquals(BigDecimal("-75.65"), d.usdtBs)
        assertTrue(
            "a rate under the selected one is a negative difference",
            d.usdtBs!! < BigDecimal.ZERO
        )
    }
}
