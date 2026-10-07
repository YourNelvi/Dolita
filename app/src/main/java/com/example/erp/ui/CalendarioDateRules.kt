package com.example.erp.ui

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Why a day cannot be looked up, in the order the reader should be told.
 *
 * The order is the contract: [AFTER_TODAY] wins over everything else because it
 * is the bound the reader already knows about, and it is the only one the
 * DatePicker can still hand back on confirm.
 */
enum class CalendarioRejection { NONE, AFTER_TODAY, WEEKEND, TOO_OLD }

/**
 * Which days the "Consultar por fecha" calendar offers.
 *
 * Kept as a pure object with no Android types so the DatePicker and the unit
 * test cannot drift apart: a rule copied into the composable is a rule nobody
 * verified.
 *
 * Two rules, both about what actually exists rather than what is merely empty:
 *
 * - **Weekends are not selectable at all.** BCV does not publish on Saturdays or
 *   Sundays, so a weekend is not a day without a rate — it is not a day. Offering
 *   it could only ever produce "Sin datos para el …", which reads as missing
 *   data when nothing is missing.
 * - **The window is the last [MONTHS_BACK] months.** Older history is not
 *   rendered as a day that happens to have no rate either; it is out of reach.
 */
object CalendarioDateRules {

    /** How far back the calendar goes, inclusive. */
    const val MONTHS_BACK: Long = 3

    /** Why [date] cannot be looked up on [today]'s calendar. */
    fun rejection(date: LocalDate, today: LocalDate): CalendarioRejection = when {
        date.isAfter(today) -> CalendarioRejection.AFTER_TODAY
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY ->
            CalendarioRejection.WEEKEND
        date.isBefore(today.minusMonths(MONTHS_BACK)) -> CalendarioRejection.TOO_OLD
        else -> CalendarioRejection.NONE
    }

    /** True when the day can be looked up at all. */
    fun isSelectable(date: LocalDate, today: LocalDate): Boolean =
        rejection(date, today) == CalendarioRejection.NONE

    /**
     * The DatePicker speaks UTC midnights, so the rule reads them back the same
     * way [DolarViewModel.lookupDateRate] does — otherwise the day the picker
     * offers and the day the lookup searches for are two different days.
     */
    fun rejectionUtc(utcTimeMillis: Long, today: LocalDate): CalendarioRejection =
        rejection(
            Instant.ofEpochMilli(utcTimeMillis).atOffset(ZoneOffset.UTC).toLocalDate(),
            today
        )

    fun isSelectableUtc(utcTimeMillis: Long, today: LocalDate): Boolean =
        rejectionUtc(utcTimeMillis, today) == CalendarioRejection.NONE
}