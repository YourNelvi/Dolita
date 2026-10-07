package com.example.erp.ui

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
 *
 * On top of those, a **published day** opens exactly one future day. "Nothing
 * after today" is a rule about what COULD exist; once the API has published the
 * next day's rate, that day is a day that exists, and refusing to look it up
 * would make the calendar promise less than the app already knows. It is
 * supplied by the caller rather than read from a clock here: whether a rate was
 * published is data, and a pure function cannot know it.
 */
object CalendarioDateRules {

    /** How far back the calendar goes, inclusive. */
    const val MONTHS_BACK: Long = 3

    /**
     * The label the lookup shows for [date], and the same string the calculator
     * and the share payload use for that day's rate.
     *
     * One formatter for all three: a second copy is how the result row and the
     * shared message end up disagreeing about which day a rate belongs to.
     */
    private val LABEL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** The reader-facing day for a looked-up date, e.g. `19/08/2026`. */
    fun lookupLabel(date: LocalDate): String = date.format(LABEL_FORMAT)

    /**
     * Why [date] cannot be looked up on [today]'s calendar.
     *
     * @param publishedNextDay the one future date the API has already published
     *   a rate for, or null when it has published none. Every other rule is
     *   unchanged while it is supplied.
     */
    fun rejection(
        date: LocalDate,
        today: LocalDate,
        publishedNextDay: LocalDate? = null
    ): CalendarioRejection = when {
        // A published rate is a fact about that day, and it outranks every
        // heuristic here — including the weekend rule, which only exists
        // because BCV normally skips weekends.
        date == publishedNextDay -> CalendarioRejection.NONE
        date.isAfter(today) -> CalendarioRejection.AFTER_TODAY
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY ->
            CalendarioRejection.WEEKEND
        date.isBefore(today.minusMonths(MONTHS_BACK)) -> CalendarioRejection.TOO_OLD
        else -> CalendarioRejection.NONE
    }

    /** True when the day can be looked up at all. */
    fun isSelectable(date: LocalDate, today: LocalDate, publishedNextDay: LocalDate? = null): Boolean =
        rejection(date, today, publishedNextDay) == CalendarioRejection.NONE

    /**
     * The DatePicker speaks UTC midnights, so the rule reads them back the same
     * way [DolarViewModel.lookupDateRate] does — otherwise the day the picker
     * offers and the day the lookup searches for are two different days.
     */
    fun rejectionUtc(
        utcTimeMillis: Long,
        today: LocalDate,
        publishedNextDay: LocalDate? = null
    ): CalendarioRejection =
        rejection(
            Instant.ofEpochMilli(utcTimeMillis).atOffset(ZoneOffset.UTC).toLocalDate(),
            today,
            publishedNextDay
        )

    fun isSelectableUtc(
        utcTimeMillis: Long,
        today: LocalDate,
        publishedNextDay: LocalDate? = null
    ): Boolean =
        rejectionUtc(utcTimeMillis, today, publishedNextDay) == CalendarioRejection.NONE
}