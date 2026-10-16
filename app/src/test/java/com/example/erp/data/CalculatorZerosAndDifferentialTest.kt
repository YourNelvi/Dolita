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
    fun `the overpayment is stated in bolivarares, dollars, USDT and euros`() {
        val result = CurrencyConverter.differentialEnMonedas(
            amountInBs = BigDecimal("50000"),
            usdtRate = usdt,
            usdRate = usd,
            eurRate = eur
        )!!

        assertTrue("the parallel rate is above the official one here", result.overpaying)

        // Bs 50.000 buys 49,590383 USDT at the parallel. Those same USDT would
        // have cost 43.423,82 at the official rate, so the purchase overpaid
        // Bs 6.576,18 — which is 7,51 USD, 6,52 USDT or 6,71 EUR of the same money.
        assertEquals(BigDecimal("6576.18"), result.extraBs.setScale(2, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("7.51"), result.extraUsd)
        assertEquals(BigDecimal("6.52"), result.extraUsdt)
        assertEquals(BigDecimal("6.71"), result.extraEur)
    }

    @Test
    fun `every reading describes the same money within the rounding of a cent`() {
        val r = CurrencyConverter.differentialEnMonedas(
            amountInBs = BigDecimal("50000"),
            usdtRate = usdt,
            usdRate = usd,
            eurRate = eur
        )!!
        val target = r.extraBs.setScale(2, RoundingMode.HALF_UP)

        // Each reading is rounded to cents INDEPENDENTLY, so multiplying one back
        // by its rate cannot reproduce the Bs figure exactly — half a cent of USD
        // is several bolivarares. The bound is that half cent times the rate,
        // and anything wider would mean a reading is describing different money.
        assertAgrees("USD", r.extraUsd, usd, target)
        assertAgrees("USDT", r.extraUsdt, usdt, target)
        assertAgrees("EUR", r.extraEur!!, eur, target)
    }

    private fun assertAgrees(label: String, reading: BigDecimal, rate: Double, target: BigDecimal) {
        val back = reading.multiply(BigDecimal.valueOf(rate)).abs()
        val tolerance = BigDecimal.valueOf(0.005).multiply(BigDecimal.valueOf(rate))
        val drift = back.subtract(target).abs()
        assertTrue(
            "$label reading $back drifts from $target by $drift, over the tolerance $tolerance",
            drift <= tolerance
        )
    }

    @Test
    fun `the euro reading is omitted rather than invented when there is no euro rate`() {
        val r = CurrencyConverter.differentialEnMonedas(
            amountInBs = BigDecimal("50000"),
            usdtRate = usdt,
            usdRate = usd,
            eurRate = null
        )!!
        assertNull("no euro rate means no euro figure, not a zero", r.extraEur)
    }

    @Test
    fun `a zero euro rate is treated as no rate`() {
        val r = CurrencyConverter.differentialEnMonedas(
            amountInBs = BigDecimal("50000"),
            usdtRate = usdt,
            usdRate = usd,
            eurRate = 0.0
        )!!
        assertNull(r.extraEur)
    }

    @Test
    fun `an unusable rate yields nothing rather than a fabricated overpayment`() {
        assertNull(
            CurrencyConverter.differentialEnMonedas(
                BigDecimal("50000"), usdtRate = 0.0, usdRate = usd, eurRate = eur
            )
        )
        assertNull(
            CurrencyConverter.differentialEnMonedas(
                BigDecimal("50000"), usdtRate = usdt, usdRate = 0.0, eurRate = eur
            )
        )
        assertNull(
            CurrencyConverter.differentialEnMonedas(
                BigDecimal.ZERO, usdtRate = usdt, usdRate = usd, eurRate = eur
            )
        )
    }

    @Test
    fun `a saving is reported as a saving, not coloured as a loss`() {
        // A parallel rate BELOW the official one means the reader saved. The
        // sign has to survive the trip, because the colour follows it.
        val r = CurrencyConverter.differentialEnMonedas(
            amountInBs = BigDecimal("50000"),
            usdtRate = 800.0,   // below the official
            usdRate = usd,
            eurRate = eur
        )!!
        assertTrue("expected a saving", !r.overpaying)
        assertTrue("a saving is a negative cost in Bs", r.extraBs.signum() < 0)
    }

    @Test
    fun `identical rates produce no differential at all`() {
        assertNull(
            CurrencyConverter.differentialEnMonedas(
                amountInBs = BigDecimal("50000"),
                usdtRate = usd,
                usdRate = usd,
                eurRate = eur
            )
        )
    }
}