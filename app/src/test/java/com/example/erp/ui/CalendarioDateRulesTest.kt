package com.example.erp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The calendar's selectable-day rule as a pure function: no DatePicker, no
 * Android Application, no system clock. The DatePicker and this test call the
 * same object on purpose — a rule copied into the composable is a rule that
 * drifts from what was verified.
 */
class CalendarioDateRulesTest {

    // 2026-08-18 is a Tuesday.
    private val today: LocalDate = LocalDate.of(2026, 8, 18)

    private fun utcMillisOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `a weekday inside the window is selectable`() {
        // Monday, one day back.
        assertTrue(CalendarioDateRules.isSelectable(LocalDate.of(2026, 8, 17), today))
        // Today itself is still in range.
        assertTrue(CalendarioDateRules.isSelectable(today, today))
    }

    @Test
    fun `saturday is not a selectable day`() {
        // BCV does not publish on weekends: a Saturday is not a day without a
        // rate, it is not a day at all.
        assertEquals(
            CalendarioRejection.WEEKEND,
            CalendarioDateRules.rejection(LocalDate.of(2026, 8, 15), today)
        )
        assertFalse(CalendarioDateRules.isSelectable(LocalDate.of(2026, 8, 15), today))
    }

    @Test
    fun `sunday is not a selectable day`() {
        assertEquals(
            CalendarioRejection.WEEKEND,
            CalendarioDateRules.rejection(LocalDate.of(2026, 8, 16), today)
        )
        assertFalse(CalendarioDateRules.isSelectable(LocalDate.of(2026, 8, 16), today))
    }

    @Test
    fun `a weekday older than three months is rejected for its age alone`() {
        // 2026-05-15 is a Friday, so only the window can reject it.
        val tooOld = LocalDate.of(2026, 5, 15)
        assertEquals(
            CalendarioRejection.TOO_OLD,
            CalendarioDateRules.rejection(tooOld, today)
        )
        assertFalse(CalendarioDateRules.isSelectable(tooOld, today))
    }

    @Test
    fun `the three month bound is inclusive`() {
        // 2026-05-18 is a Monday, exactly three months back.
        val boundary = LocalDate.of(2026, 5, 18)
        assertEquals(boundary, today.minusMonths(3))
        assertEquals(CalendarioRejection.NONE, CalendarioDateRules.rejection(boundary, today))
        assertTrue(CalendarioDateRules.isSelectable(boundary, today))
    }

    @Test
    fun `a future date is rejected`() {
        val tomorrow = LocalDate.of(2026, 8, 19)
        assertEquals(
            CalendarioRejection.AFTER_TODAY,
            CalendarioDateRules.rejection(tomorrow, today)
        )
        assertFalse(CalendarioDateRules.isSelectable(tomorrow, today))
    }

    @Test
    fun `a selectable day reports no rejection at all`() {
        assertEquals(
            CalendarioRejection.NONE,
            CalendarioDateRules.rejection(LocalDate.of(2026, 8, 17), today)
        )
    }

    @Test
    fun `the future bound is reported ahead of the weekend rule`() {
        // 2026-08-22 is a Saturday in the future: the pinned order keeps the
        // "not after today" message the one the reader already knows.
        assertEquals(
            CalendarioRejection.AFTER_TODAY,
            CalendarioDateRules.rejection(LocalDate.of(2026, 8, 22), today)
        )
    }

    @Test
    fun `the utc millis rule agrees with the date rule`() {
        // The DatePicker hands out UTC midnights, so the rule the composable
        // uses has to read the same day the test states.
        assertTrue(CalendarioDateRules.isSelectableUtc(utcMillisOf(LocalDate.of(2026, 8, 17)), today))
        assertFalse(CalendarioDateRules.isSelectableUtc(utcMillisOf(LocalDate.of(2026, 8, 15)), today))
        assertFalse(CalendarioDateRules.isSelectableUtc(utcMillisOf(LocalDate.of(2026, 5, 15)), today))
        assertFalse(CalendarioDateRules.isSelectableUtc(utcMillisOf(LocalDate.of(2026, 8, 19)), today))
    }
}