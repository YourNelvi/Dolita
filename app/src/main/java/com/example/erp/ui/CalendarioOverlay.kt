package com.example.erp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.erp.ui.theme.positiveColor
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Every layout number the marker needs, in ONE place — and each one is COUPLED
 * TO MATERIAL3'S INTERNAL CALENDAR GRID.
 *
 * Material3 1.4.0 gives no way to draw inside a day cell: `DatePicker` has no
 * `dayContent` slot, there is no `DatePickerContent` to copy, and no
 * `CalendarDayModel`. So the marker is an overlay positioned by hand against a
 * grid Material3 owns, and the values below were read off
 * `material3-android` 1.4.0 sources (`DatePicker.kt`, `DatePickerModalTokens.kt`,
 * `TypeScaleTokens.kt`). **Re-check every one of them on a Material3 upgrade**:
 * the failure is silent — the circle simply lands on the wrong cell — so this
 * object is the single place to look.
 *
 * Two things beyond an upgrade also move the grid and are NOT absorbed here,
 * because Material3 exposes no hook to absorb them: a raised system font scale
 * (the header's minimum height only wins while its content is shorter than it)
 * and a lowered `LocalMinimumInteractiveComponentSize` (which is what sizes the
 * day cell). Both are the same silent failure: the dot lands on another cell.
 */
private object MaterialCalendarGridGeometry {

    /** Days per grid row. Fixed by the week, and the reason columns are derived. */
    const val DAYS_IN_WEEK: Int = 7

    /**
     * The date picker header: title line plus the headline/divider block, held
     * to `DatePickerModalTokens.HeaderContainerHeight`. Its measured content is
     * shorter than this at Material3's default type scale, so the minimum wins
     * and the header is a fixed height rather than a font-dependent one.
     */
    val HEADER_HEIGHT: Dp = 120.dp

    /** The month/year navigation row (`MonthYearHeight`) under the header. */
    val MONTH_NAVIGATION_HEIGHT: Dp = 56.dp

    /** The weekday letter row (`RecommendedSizeForAccessibility`). */
    val WEEKDAY_HEADER_HEIGHT: Dp = 48.dp

    /**
     * The grid's inset inside the picker (`DatePickerHorizontalPadding`).
     *
     * The column width is DERIVED from the measured picker width rather than
     * hard-coded, because the picker is exactly as wide as the dialog gives it.
     */
    val GRID_HORIZONTAL_PADDING: Dp = 12.dp

    /**
     * The day cell's height, which is also the row pitch.
     *
     * Material3 sizes every day as `LocalMinimumInteractiveComponentSize` (48.dp
     * by default) and gives the month column `RecommendedSizeForAccessibility *
     * MONTH_ROWS`, with the rows distributed evenly — so the pitch is this same
     * number. A screen that lowered the minimum interactive size would break
     * this, which is why it lives beside the values it is derived from.
     */
    val DAY_CELL_SIZE: Dp = 48.dp

    /**
     * Rows Material3 always reserves for a month (`MaxCalendarRows`).
     *
     * Not used by the arithmetic itself: it is here because it is the other half
     * of the [DAY_CELL_SIZE] derivation (the month column is
     * `RecommendedSizeForAccessibility * MONTH_ROWS`, so evenly distributed rows
     * leave no remainder and the pitch is exactly that one number).
     */
    const val MONTH_ROWS: Int = 6

    /** Offset from the picker's top edge down to the top of the first day row. */
    val GRID_TOP: Dp = HEADER_HEIGHT + MONTH_NAVIGATION_HEIGHT + WEEKDAY_HEADER_HEIGHT

    /**
     * How far the circle sits above the bottom edge of its cell, so it lands
     * under the day number the way Material3's own "today" indicator does — a
     * dot the reader can see, never a fill that hides the number it marks.
     */
    val MARKER_BOTTOM_INSET: Dp = 6.dp

    /** The circle's diameter: a dot, not a second day cell. */
    val MARKER_DIAMETER: Dp = 10.dp
}

/**
 * Which grid cell a day occupies, as plain integers — the whole of the
 * marker's placement logic, with no Compose and no Dp in sight.
 *
 * It is a separate function because this arithmetic was WRONG on its first
 * render and 206 unit tests could not have seen it: the wrong answer is a valid
 * coordinate that lands on the neighbouring day. October 2026 starts on a
 * Thursday, so the 1st sits in the fourth column of a Sunday-first grid and the
 * 7th in the third-from-last; measuring the marked Wednesday instead of the
 * month's first day put the circle under the 6th. Deriving the offset from the
 * first of the month is the fix, and pinning it here is what stops it from
 * being "simplified" back out of a test.
 */
internal fun cellIndexOf(
    markedDate: LocalDate,
    displayedMonth: LocalDate,
    firstDayOfWeek: DayOfWeek
): Int {
    val daysInWeek = 7
    val firstOfMonth = displayedMonth.withDayOfMonth(1)
    val daysBeforeFirstOfMonth =
        (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + daysInWeek) % daysInWeek
    return daysBeforeFirstOfMonth + markedDate.dayOfMonth - 1
}

/**
 * A green dot on the day whose rate the API has already published — the one
 * future day the calendar accepts — so the reader can see that a day exists
 * before tapping it.
 *
 * **Drawn as an overlay, never as a day cell.** Material3 1.4.0 offers no hook
 * inside a day cell, so this sits on top of the picker inside a `Box` and is
 * positioned from [MaterialCalendarGridGeometry].
 *
 * Two things this deliberately does NOT do:
 *
 * - **It does not consume touches.** A `Box` with no pointer input contributes
 *   no pointer handlers, so the picker's own day cell still receives the tap
 *   underneath. Adding `clickable` here would make the marked day unselectable
 *   while looking selectable.
 * - **It does not draw for a month it is not in.** Material3 keeps a whole year
 *   of months in a lazy row, so this recomposes on every scroll; the guard is
 *   what stops the same offset from being painted over whichever cell happens
 *   to sit at that position in a different month.
 *
 * @param displayedMonthMillis the picker's own `displayedMonthMillis`: a UTC
 *   timestamp for the first day of the month on screen.
 * @param markedDate the day to mark, or null to draw nothing at all.
 *
 * `markedDate` is a parameter rather than something read from the ViewModel on
 * purpose: the real marked day only exists once BCV publishes tomorrow's rate,
 * which cannot be forced, so a caller that needs to verify the geometry passes
 * a day that is guaranteed to be visible. It defaults to null because the
 * published day is DATA — it comes from the quote's `fechaActualizacion`, which
 * is not always literally tomorrow — and the call site
 * ([publishedDayOf]) supplies it, so production needs no extra wiring.
 *
 * @param firstDayOfWeek the locale's first day of the week, which decides which
 *   column the month's first day lands in. Material3 derives it the same way
 *   (`WeekFields.of(locale).firstDayOfWeek.value`), so the marker and the grid
 *   cannot disagree about where the month starts; it stays a parameter because
 *   Material3 reads the *configuration's* locale and a caller that ever sets a
 *   per-app locale would need to hand over the matching value.
 */
@Composable
fun CalendarioPublishedDayMarker(
    displayedMonthMillis: Long,
    markedDate: LocalDate? = null,
    modifier: Modifier = Modifier,
    firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
) {
    // No published day, nothing to promise: an absent marker draws no circle
    // rather than an empty one somewhere in the grid.
    if (markedDate == null) return

    val displayedMonth = remember(displayedMonthMillis) {
        Instant.ofEpochMilli(displayedMonthMillis).atOffset(ZoneOffset.UTC).toLocalDate()
    }
    // Only while the marked day is on screen. See the KDoc: without this the
    // same offset would be painted over a different month after every scroll.
    if (displayedMonth.year != markedDate.year || displayedMonth.month != markedDate.month) return

    // The grid indexes cells the same way Material3 does. See [cellIndexOf] for
    // why this is a tested function and not a line of arithmetic: the first
    // version of it rendered the circle on the wrong day and nothing noticed.
    val cellIndex = cellIndexOf(markedDate, displayedMonth, firstDayOfWeek)
    val row = cellIndex / MaterialCalendarGridGeometry.DAYS_IN_WEEK
    val column = cellIndex % MaterialCalendarGridGeometry.DAYS_IN_WEEK

    val markerColor = positiveColor()

    BoxWithConstraints(modifier = modifier) {
        // Derived, never hard-coded: the picker is as wide as the dialog made it.
        val gridWidth = maxWidth - MaterialCalendarGridGeometry.GRID_HORIZONTAL_PADDING * 2
        val columnWidth = gridWidth / MaterialCalendarGridGeometry.DAYS_IN_WEEK

        val centerX =
            MaterialCalendarGridGeometry.GRID_HORIZONTAL_PADDING + columnWidth * (column + 0.5f)
        val cellTop = MaterialCalendarGridGeometry.GRID_TOP + MaterialCalendarGridGeometry.DAY_CELL_SIZE * row
        // The CENTRE of the dot, and of nothing else. offset() takes the
        // TOP-LEFT corner, so the radius is taken off once, below, for both axes.
        val centerY = cellTop + MaterialCalendarGridGeometry.DAY_CELL_SIZE -
            MaterialCalendarGridGeometry.MARKER_BOTTOM_INSET
        val radius = MaterialCalendarGridGeometry.MARKER_DIAMETER / 2

        Box(
            Modifier
                .offset(x = centerX - radius, y = centerY - radius)
                .size(MaterialCalendarGridGeometry.MARKER_DIAMETER)
                .clip(CircleShape)
                .background(markerColor)
        )
    }
}