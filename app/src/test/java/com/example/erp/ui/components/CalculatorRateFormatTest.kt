package com.example.erp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rate line in the calculator obeys the "Alta precisión" switch the same
 * way the quote card does: two decimals by default, four when the reader asks
 * for them.
 *
 * The switch was wired to the quote card and to both share texts, but the
 * calculator kept printing cents: a reader who turned it on to see the rate
 * with four decimals found the same rounded number one screen below.
 */
class CalculatorRateFormatTest {

    @Test
    fun `the rate shows two decimals by default`() {
        assertEquals("914,02", formatCalc(914.0218, fracDigits = 2))
    }

    @Test
    fun `high precision shows four decimals`() {
        assertEquals("914,0218", formatCalc(914.0218, fracDigits = 4))
    }

    @Test
    fun `four decimals round up from the fifth`() {
        assertEquals("914,5678", formatCalc(914.56784, fracDigits = 4))
    }

    @Test
    fun `thousands keep their separator`() {
        assertEquals("1.234,5678", formatCalc(1234.5678, fracDigits = 4))
        assertEquals("1.234,57", formatCalc(1234.5678, fracDigits = 2))
    }
}
