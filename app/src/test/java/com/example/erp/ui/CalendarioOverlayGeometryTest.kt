package com.example.erp.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Regression: the published-day circle rendered on the WRONG cell.
 *
 * The offset into the calendar grid was derived from the marked date's own day
 * of the week instead of the month's first day. That is wrong by however many
 * days separate them, and it fails silently — the result is a perfectly valid
 * coordinate that lands on a neighbouring day, which looks like a working
 * feature. 206 tests were green while the dot sat under the 6th instead of the
 * 7th.
 *
 * These pin the arithmetic that the previous suite could not see, because it
 * lived inside a composable next to a pile of Dp constants.
 */
class CalendarioOverlayGeometryTest {

    private val october2026 = LocalDate.of(2026, 10, 1)

    /** The Spanish/Venezuelan locale starts the week on Sunday, as the app does. */
    private val sundayFirst = DayOfWeek.SUNDAY
    private val mondayFirst = DayOfWeek.MONDAY

    /**
     * The exact case that shipped wrong. 1 October 2026 is a Thursday, so in a
     * Sunday-first grid it occupies column 4 and the 7th occupies column 3.
     * Deriving the offset from the 7th (a Wednesday) instead gave column 2 —
     * the 6th — which is what the circle actually rendered on.
     */
    @Test
    fun `october 2026 puts the seventh in the fourth column of a sunday-first grid`() {
        val marked = LocalDate.of(2026, 10, 7)
        val cell = cellIndexOf(marked, october2026, sundayFirst)

        // cell 10 = row 1, column 3 (Sunday=0), which is where Wednesday sits.
        assertEquals("row", 1, cell / 7)
        assertEquals("column", 3, cell % 7)
    }

    @Test
    fun `the first of the month lands in the column its weekday implies`() {
        // 1 October 2026 is a Thursday, four days after Sunday, so column 4.
        assertEquals(
            "Thursday is column 4 of a Sunday-first grid",
            4,
            cellIndexOf(october2026, october2026, sundayFirst) % 7
        )
        // The same day in a Monday-first grid is column 3, one further left.
        assertEquals(
            "Thursday is column 3 of a Monday-first grid",
            3,
            cellIndexOf(october2026, october2026, mondayFirst) % 7
        )
    }

    @Test
    fun `a month starting exactly on the first day of the week begins at column zero`() {
        // 1 November 2026 is a Sunday.
        val november = LocalDate.of(2026, 11, 1)
        assertEquals(DayOfWeek.SUNDAY, november.dayOfWeek)
        assertEquals(
            "no offset when the month starts on the locale's first day",
            0,
            cellIndexOf(november, november, sundayFirst) % 7
        )
    }

    @Test
    fun `consecutive days are consecutive cells across a row boundary`() {
        // The 3rd through the 9th of October straddle a row, so this catches an
        // off-by-one that a single in-row day would hide.
        (3..9).forEach { day ->
            val cell = cellIndexOf(LocalDate.of(2026, 10, day), october2026, sundayFirst)
            assertEquals("day $day must be cell ${day + 3}", day + 3, cell)
        }
    }

    @Test
    fun `the last day of the month stays inside the grid`() {
        val octoberLast = LocalDate.of(2026, 10, 31)
        val cell = cellIndexOf(octoberLast, october2026, sundayFirst)

        assertEquals("31 October 2026 is a Saturday, column 6", 6, cell % 7)
        assertTrue("must not spill into a seventh row", cell / 7 < 6)
    }

    @Test
    fun `a marked day from another month resolves to the wrong weekday, so the month guard is load-bearing`() {
        // The composable returns before calling this when the marked day is not
        // in the displayed month. This pins WHY that guard exists: the arithmetic
        // does not reject a foreign day, it silently returns a plausible cell for
        // it. December's 15th is a Tuesday; addressed in October's grid the same
        // index points at a Thursday, so without the guard the circle would sit
        // under a valid-looking wrong day after every month change.
        val december15 = LocalDate.of(2026, 12, 15)
        assertEquals("15 December 2026 is a Tuesday", DayOfWeek.TUESDAY, december15.dayOfWeek)

        val column = cellIndexOf(december15, october2026, sundayFirst) % 7
        val weekdayAtThatColumn = sundayFirst.plus(column.toLong())

        assertTrue(
            "the guard is what prevents a wrong weekday being marked",
            weekdayAtThatColumn != december15.dayOfWeek
        )
    }

    private fun assertTrue(message: String, condition: Boolean) =
        org.junit.Assert.assertTrue(message, condition)
}
