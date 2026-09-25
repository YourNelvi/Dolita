package com.example.erp

import com.example.erp.data.CurrencyConverter
import com.example.erp.data.CurrencyConverter.divToVes
import com.example.erp.data.CurrencyConverter.format
import com.example.erp.data.CurrencyConverter.parseDigits
import com.example.erp.data.CurrencyConverter.toDigits
import com.example.erp.data.CurrencyConverter.vesToDiv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CurrencyConverterTest {

    // Rates the card actually shows: a BCV/USD bolivar rate and the kind of
    // round number that makes the rounding boundaries easy to read.
    private val bcvRate = 36.5644
    private val neatRate = 3.0

    @Test
    fun `parseDigits treats the last two digits as the decimals`() {
        assertEquals(0, BigDecimal("0.00").compareTo(parseDigits("0")!!))
        assertEquals(0, BigDecimal("0.01").compareTo(parseDigits("1")!!))
        assertEquals(0, BigDecimal("0.09").compareTo(parseDigits("9")!!))
        assertEquals(0, BigDecimal("0.12").compareTo(parseDigits("12")!!))
        assertEquals(0, BigDecimal("0.99").compareTo(parseDigits("99")!!))
        assertEquals(0, BigDecimal("1.00").compareTo(parseDigits("100")!!))
        assertEquals(0, BigDecimal("1.05").compareTo(parseDigits("105")!!))
        assertEquals(0, BigDecimal("9.99").compareTo(parseDigits("999")!!))
        assertEquals(0, BigDecimal("12.34").compareTo(parseDigits("1234")!!))
    }

    @Test
    fun `parseDigits returns null only for an empty field`() {
        assertNull(parseDigits(""))
    }

    @Test
    fun `parseDigits rejects non numeric input by throwing`() {
        // The UI filters the field down to digits before ever calling this, so
        // the failure mode is a crash, not a silent zero.
        assertThrows(NumberFormatException::class.java) { parseDigits("a") }
        assertThrows(NumberFormatException::class.java) { parseDigits("abc") }
        assertThrows(NumberFormatException::class.java) { parseDigits("1a2") }
        assertThrows(NumberFormatException::class.java) { parseDigits("12a") }
        assertThrows(NumberFormatException::class.java) { parseDigits("-1") }
        assertThrows(NumberFormatException::class.java) { parseDigits("1.5") }
    }

    @Test
    fun `format groups thousands and always writes two decimals`() {
        assertEquals("0,00", format(BigDecimal("0")))
        assertEquals("1,00", format(BigDecimal("1")))
        assertEquals("1,50", format(BigDecimal("1.5")))
        assertEquals("1.000,00", format(BigDecimal("1000")))
        assertEquals("1.234,57", format(BigDecimal("1234.567")))
        assertEquals("12.345.678,91", format(BigDecimal("12345678.905")))
    }

    @Test
    fun `format rounds half up not half even`() {
        // 0.005 is the tie that separates the two modes: HALF_EVEN keeps 0,00.
        assertEquals("0,01", format(BigDecimal("0.005")))
        assertEquals("0,02", format(BigDecimal("0.015")))
        // 2.345 would go down to 2,34 under HALF_EVEN (4 is even).
        assertEquals("2,35", format(BigDecimal("2.345")))
        assertEquals("2,36", format(BigDecimal("2.355")))
        assertEquals("0,00", format(BigDecimal("0.004")))
        assertEquals("0,01", format(BigDecimal("0.006")))
    }

    @Test
    fun `toDigits is the inverse of format for the field encoding`() {
        assertEquals("123456", toDigits("1.234,56"))
        assertEquals("100", toDigits("1,00"))
        assertEquals("1000", toDigits("10,00"))
        assertEquals("10000", toDigits("100,00"))
        assertEquals("100000", toDigits("1.000,00"))
        assertEquals("1234567890", toDigits("12.345.678,90"))
        assertEquals("01", toDigits("0,01"))
        assertEquals("50", toDigits("0,50"))
    }

    @Test
    fun `toDigits keeps the zero field as 00 and not 0`() {
        // The `?: "0"` fallback never fires: padding guarantees at least two
        // digits, so 0,00 encodes as "00" and still reads back as 0,00.
        assertEquals("00", toDigits("0,00"))
        assertEquals("0,00", format(parseDigits(toDigits("0,00"))!!))
    }

    @Test
    fun `toDigits truncates the fraction instead of rounding it`() {
        assertEquals("156", toDigits("1,567"))
        assertEquals(0, BigDecimal("1.56").compareTo(parseDigits(toDigits("1,567"))!!))
    }

    @Test
    fun `toDigits pads a missing fraction to the right`() {
        // No comma at all: the value is read as if "00" were its decimals, so
        // "5" encodes 500 (5,00) rather than 5.
        assertEquals("500", toDigits("5"))
        assertEquals("150", toDigits("1,5"))
    }

    @Test
    fun `vesToDiv divides the amount and rounds to two decimals`() {
        assertEquals("100", vesToDiv("3656", bcvRate))
        assertEquals("338", vesToDiv("12345", bcvRate))
        assertEquals("34", vesToDiv("1234", bcvRate))
        assertEquals("6173", vesToDiv("12345", 2.0))
    }

    @Test
    fun `vesToDiv collapses an amount under half a cent to zero`() {
        // 0,01 Bs at 36,5644 is 0,000273 USD, which rounds down to 0,00.
        assertEquals("00", vesToDiv("1", bcvRate))
        // 1,00 USD at 773,31 is 0,0013 USD, also under the tie.
        assertEquals("00", vesToDiv("100", 773.31))
        // 1,00 Bs at 36,5644 is 0,0273 USD: above the tie, so it survives.
        assertEquals("03", vesToDiv("100", bcvRate))
    }

    @Test
    fun `divToVes multiplies the amount and rounds to two decimals`() {
        assertEquals("3656", divToVes("100", bcvRate))
        assertEquals("133444", divToVes("3656", 36.5))
        assertEquals("77331", divToVes("100", 773.31))
    }

    @Test
    fun `both conversions answer empty for an empty field or an unusable rate`() {
        assertEquals("", vesToDiv("", bcvRate))
        assertEquals("", divToVes("", bcvRate))
        assertEquals("", vesToDiv("100", 0.0))
        assertEquals("", divToVes("100", 0.0))
        assertEquals("", vesToDiv("100", -5.0))
        assertEquals("", divToVes("100", -5.0))
    }

    @Test
    fun `both conversions throw on a non finite rate`() {
        // `rate <= 0.0` is false for NaN, so the guard lets it through and
        // BigDecimal.valueOf rejects it. Latent, not reachable from the API
        // today, but the guard is not actually total.
        assertThrows(NumberFormatException::class.java) { vesToDiv("100", Double.NaN) }
        assertThrows(NumberFormatException::class.java) { divToVes("100", Double.NaN) }
        assertThrows(NumberFormatException::class.java) { vesToDiv("100", Double.POSITIVE_INFINITY) }
    }

    @Test
    fun `round trip is exact when the division lands on two decimals`() {
        // 123,45 / 3 is 41,15 with nothing lost, so the way back is 123,45.
        val div = vesToDiv("12345", neatRate)
        assertEquals("4115", div)
        assertEquals("12345", divToVes(div, neatRate))
    }

    @Test
    fun `round trip stays within half a cent of the divisa times the rate`() {
        val ves = "12345"
        val original = parseDigits(ves)!!
        val div = vesToDiv(ves, bcvRate)
        val back = parseDigits(divToVes(div, bcvRate))!!
        // The divisa side keeps 2 decimals, so the worst case is half a cent
        // re-multiplied by the rate.
        val tolerance = BigDecimal("0.005") * BigDecimal.valueOf(bcvRate)
        val drift = back.subtract(original).abs()
        assertTrue("drift $drift exceeded tolerance $tolerance", drift <= tolerance)
    }

    @Test
    fun `round trip is lossy when the divisa side is rounded`() {
        // 12,34 / 36,5644 is 0,3375... -> 0,34, and 0,34 back is 12,43.
        val div = vesToDiv("1234", bcvRate)
        assertEquals("34", div)
        assertEquals("1243", divToVes(div, bcvRate))
    }

    @Test
    fun `the rate prefill round trips through the same encoding`() {
        // What the composable does on load: toDigits(format(BigDecimal.valueOf(rate)))
        assertEquals("3656", toDigits(format(BigDecimal.valueOf(bcvRate))))
        assertEquals("77331", toDigits(format(BigDecimal.valueOf(773.31))))
        assertEquals("10550", toDigits(format(BigDecimal.valueOf(105.5))))
    }

    @Test
    fun `a large divisa amount produces more digits than the field accepts`() {
        // The fields clamp typed input to 10 digits, but the conversion output
        // is not clamped: 12.345.678,90 USD becomes a 12 digit VES field.
        val ves = divToVes("1234567890", 773.31)
        assertEquals("954703695016", ves)
        assertEquals(12, ves.length)
    }
}
