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

    // -----------------------------------------------------------------------
    // The published next-day rate
    //
    // "Not after today" is a rule about what COULD exist. Once the API has
    // published tomorrow's rate, tomorrow is a day that exists, so it is the
    // one future date the calendar offers — and it is still exactly one day.
    // -----------------------------------------------------------------------

    /** 2026-08-19, a Wednesday: the day BCV published tomorrow's rate for. */
    private val publishedDay: LocalDate = LocalDate.of(2026, 8, 19)

    @Test
    fun `the published day is selectable even though it is in the future`() {
        assertEquals(
            CalendarioRejection.NONE,
            CalendarioDateRules.rejection(publishedDay, today, publishedDay)
        )
        assertTrue(CalendarioDateRules.isSelectable(publishedDay, today, publishedDay))
    }

    @Test
    fun `only the published day escapes the future rejection`() {
        // 2026-08-20 is the day AFTER the published one. Publishing tomorrow's
        // rate says nothing about the day after, so the old bound stands.
        val dayAfter = LocalDate.of(2026, 8, 20)
        assertEquals(
            CalendarioRejection.AFTER_TODAY,
            CalendarioDateRules.rejection(dayAfter, today, publishedDay)
        )
        assertFalse(CalendarioDateRules.isSelectable(dayAfter, today, publishedDay))

        // Two weeks out, and a far future month: both still refused.
        assertFalse(CalendarioDateRules.isSelectable(LocalDate.of(2026, 9, 1), today, publishedDay))
        assertFalse(CalendarioDateRules.isSelectable(LocalDate.of(2027, 3, 4), today, publishedDay))
    }

    @Test
    fun `without a published day every future date stays rejected`() {
        // The default is the whole current behaviour: no published quote means
        // no published day, so the bound is untouched.
        assertEquals(
            CalendarioRejection.AFTER_TODAY,
            CalendarioDateRules.rejection(publishedDay, today)
        )
        assertFalse(CalendarioDateRules.isSelectable(publishedDay, today))
    }

    @Test
    fun `a published day does not loosen the other rules`() {
        // Past days, weekends and the three month window keep their verdicts
        // even while a published day is supplied: only one future day changed.
        assertTrue(CalendarioDateRules.isSelectable(LocalDate.of(2026, 8, 17), today, publishedDay))
        assertEquals(
            CalendarioRejection.WEEKEND,
            CalendarioDateRules.rejection(LocalDate.of(2026, 8, 15), today, publishedDay)
        )
        assertEquals(
            CalendarioRejection.TOO_OLD,
            CalendarioDateRules.rejection(LocalDate.of(2026, 5, 15), today, publishedDay)
        )
        // And the inclusive three month bound still is a boundary, not a hint.
        assertTrue(
            CalendarioDateRules.isSelectable(LocalDate.of(2026, 5, 18), today, publishedDay)
        )
        assertFalse(
            CalendarioDateRules.isSelectable(LocalDate.of(2026, 5, 17), today, publishedDay)
        )
    }

    @Test
    fun `a published weekend day is accepted because a rate was published for it`() {
        // 2026-08-22 is a Saturday. The weekend rule exists because BCV does not
        // publish on weekends — so if a quote ever names a Saturday as its day,
        // the fact of the quote outranks the heuristic.
        val publishedSaturday = LocalDate.of(2026, 8, 22)
        assertEquals(
            CalendarioRejection.NONE,
            CalendarioDateRules.rejection(publishedSaturday, today, publishedSaturday)
        )
        // A different Saturday is still a weekend.
        assertEquals(
            CalendarioRejection.WEEKEND,
            CalendarioDateRules.rejection(LocalDate.of(2026, 8, 15), today, publishedSaturday)
        )
        // And a Saturday further in the future keeps the pinned order: the
        // future bound is still the message the reader already knows.
        assertEquals(
            CalendarioRejection.AFTER_TODAY,
            CalendarioDateRules.rejection(LocalDate.of(2026, 8, 29), today, publishedSaturday)
        )
    }

    @Test
    fun `the utc millis rule honours the published day too`() {
        assertTrue(
            CalendarioDateRules.isSelectableUtc(
                utcMillisOf(publishedDay),
                today,
                publishedDay
            )
        )
        assertFalse(
            CalendarioDateRules.isSelectableUtc(
                utcMillisOf(LocalDate.of(2026, 8, 20)),
                today,
                publishedDay
            )
        )
        // Unchanged for every other category of day.
        assertFalse(
            CalendarioDateRules.isSelectableUtc(
                utcMillisOf(LocalDate.of(2026, 8, 15)),
                today,
                publishedDay
            )
        )
        assertFalse(
            CalendarioDateRules.isSelectableUtc(
                utcMillisOf(LocalDate.of(2026, 5, 15)),
                today,
                publishedDay
            )
        )
    }

    @Test
    fun `a published day does not affect a day before it`() {
        // Regression guard for the exemption's blast radius: the rule must not
        // quietly turn "anything on or after this date" into "selectable".
        assertEquals(
            CalendarioRejection.NONE,
            CalendarioDateRules.rejection(today, today, publishedDay)
        )
        assertEquals(
            CalendarioRejection.NONE,
            CalendarioDateRules.rejection(LocalDate.of(2026, 8, 17), today, publishedDay)
        )
}

    @Test
    fun `the lookup label is the day in the format the reader and calculator share`() {
        assertEquals("19/08/2026", CalendarioDateRules.lookupLabel(publishedDay))
        assertEquals("18/08/2026", CalendarioDateRules.lookupLabel(today))
    }
}