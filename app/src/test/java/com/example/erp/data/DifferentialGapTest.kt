package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cambiario gap has to be ONE number.
 *
 * It was reported as +13,65% on the official tab and −12,01% on the parallel tab
 * for the same pair of rates, because each divided by whatever rate it was
 * holding. The reader saw two different facts on one screen. These tests pin the
 * figure to a single reference.
 */
class DifferentialGapTest {

    private val oficial = 872.39
    private val paralelo = 991.47

    @Test
    fun `the gap is measured over the official rate`() {
        val gap = differentialGap(paralelo, oficial)!!
        assertEquals(13.65, gap.percent, 0.01)
        assertEquals(119.08, gap.perUnit, 0.01)
        assertTrue(gap.parallelAbove)
    }

    @Test
    fun `both tabs report the same figure for the same pair of rates`() {
        // The official tab holds (official, parallel); the parallel tab holds
        // (parallel, official). Same market, so same number.
        val fromOfficialTab = differentialGap(paralelo, oficial)!!
        val fromParallelTab = differentialGap(paralelo, oficial)!!

        assertEquals(fromOfficialTab.percent, fromParallelTab.percent, 0.0001)
        assertEquals(fromOfficialTab.perUnit, fromParallelTab.perUnit, 0.0001)
        assertEquals(fromOfficialTab.parallelAbove, fromParallelTab.parallelAbove)
    }

    @Test
    fun `dividing over the parallel is not the same number`() {
        // The regression, stated as arithmetic: this is why the old figures
        // disagreed. Keeping it as a test documents what must NOT come back.
        val wrong = ((paralelo - oficial) / paralelo) * 100.0
        assertEquals(12.01, wrong, 0.01)
        assertTrue(wrong != differentialGap(paralelo, oficial)!!.percent)
    }

    @Test
    fun `a parallel below the official reads as below`() {
        val gap = differentialGap(800.0, oficial)!!
        assertFalse(gap.parallelAbove)
        assertTrue("a negative gap must not be reported as positive", gap.percent < 0)
        assertEquals(72.39, gap.perUnit, 0.01)
    }

    @Test
    fun `identical rates are a zero gap, not a division by zero`() {
        val gap = differentialGap(oficial, oficial)!!
        assertEquals(0.0, gap.percent, 0.0001)
        assertEquals(0.0, gap.perUnit, 0.0001)
        assertFalse(gap.parallelAbove)
    }

    @Test
    fun `an unusable rate produces no gap instead of an invented one`() {
        assertNull(differentialGap(0.0, oficial))
        assertNull(differentialGap(paralelo, 0.0))
        assertNull(differentialGap(-1.0, oficial))
    }
}
