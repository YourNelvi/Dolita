package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The differential in every currency, and the zero-key trap.
 *
 * Both were asked for as one thing: a reader on the parallel tab with an amount
 * in bolivares needs to know what the purchase overpaid as money, not as a
 * percentage. A percentage cannot be spent.
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
    fun `the dollar gap is the parallel against the official, in bolivares`() {
        val d = diferenciasPorMoneda(usdRate = usd, usdtRate = usdt, eurRate = eur)
        // 1008,26 - 875,65 = 132,61
        assertEquals(BigDecimal("132.61"), d.dolarBs)
    }

    @Test
    fun `the euro gap is its own number, larger than the dollar's`() {
        val d = diferenciasPorMoneda(usdRate = usd, usdtRate = usdt, eurRate = eur)
        // The euro parallel is reached THROUGH the dollar: (980,61/875,65) *
        // 1008,26 = 1.129,115330, so the euro gap is Bs 148,51 against the
        // dollar's Bs 132,61. Reporting one figure for both would hide that
        // euro is the wider one.
        assertEquals(BigDecimal("148.51"), d.euroBs)
        assertTrue(
            "the euro spread should be its own, larger number",
            d.euroBs!! > d.dolarBs!!
        )
    }

    @Test
    fun `a missing rate yields no figure rather than a gap of zero`() {
        // "Bs 0,00" reads as "no premium exists", which is the opposite of
        // "this rate is unknown".
        val d = diferenciasPorMoneda(usdRate = usd, usdtRate = usdt, eurRate = null)
        assertNull(d.euroBs)
        assertEquals(BigDecimal("132.61"), d.dolarBs)
    }

    @Test
    fun `an unusable rate yields nothing instead of a negative premium`() {
        assertNull(diferenciasPorMoneda(0.0, usdt, eur).dolarBs)
        assertNull(diferenciasPorMoneda(usd, 0.0, eur).dolarBs)
        assertNull(diferenciasPorMoneda(0.0, usdt, eur).euroBs)
    }

    @Test
    fun `identical rates mean no premium on either currency`() {
        val d = diferenciasPorMoneda(usdRate = usd, usdtRate = usd, eurRate = eur)
        assertEquals(BigDecimal("0.00"), d.dolarBs)
        assertEquals(BigDecimal("0.00"), d.euroBs)
    }

    @Test
    fun `a parallel below the official is reported as a negative, not a fabricated zero`() {
        val d = diferenciasPorMoneda(usdRate = usd, usdtRate = 800.0, eurRate = eur)
        assertTrue("a parallel under the official is a saving", d.dolarBs!! < BigDecimal.ZERO)
    }
}