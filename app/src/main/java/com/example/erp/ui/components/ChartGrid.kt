package com.example.erp.ui.components

import kotlin.math.roundToInt

/**
 * How many dates the X axis names.
 *
 * Fifteen `dd/MM` labels cannot fit a phone width and would overlap into an
 * unreadable smear. Five fit with room to spare, and the grid under the chart is
 * where the remaining dates are read — the axis only has to frame the window.
 */
const val X_AXIS_MAX_LABELS = 5

/**
 * How many dated cells sit side by side in the grid under the chart.
 *
 * Three columns turn fifteen updates into five rows, so the full series costs a
 * compact block instead of doubling the card's height with a 15-row list.
 */
const val HISTORY_GRID_COLUMNS = 3

/**
 * Indices of the drawn points that get an X axis label.
 *
 * Both ends are always labelled — they are the range of the window — and the
 * labels in between are spread evenly rather than packed at the start, because
 * a front-loaded axis answers "when did this start?" while the reader is asking
 * "how did it move across the whole window?".
 *
 * @throws IllegalArgumentException when [maxLabels] is below 1, or below 2 while
 *   there is more than one point. Naming a window needs both ends; a budget that
 *   cannot hold two labels is the caller asking for something impossible, and it
 *   fails loudly instead of letting one date stand in for the whole range.
 */
fun xAxisLabelIndices(count: Int, maxLabels: Int): List<Int> {
    require(maxLabels >= 1) { "maxLabels must be at least 1, was $maxLabels" }
    if (count <= 0) return emptyList()
    if (count <= 1) return listOf(0)
    require(maxLabels >= 2) { "maxLabels must be at least 2 to frame $count points, was $maxLabels" }
    if (count <= maxLabels) return List(count) { it }

    // Stepping i * (count-1) / (maxLabels-1) puts index 0 at i=0 and index
    // count-1 exactly at i=maxLabels-1. The step is strictly greater than 1
    // (count > maxLabels), so two rounded values can never collide into a
    // duplicate label or step backwards.
    return List(maxLabels) { i ->
        (i.toDouble() * (count - 1) / (maxLabels - 1)).roundToInt()
    }
}

/**
 * Splits [items] into rows of at most [columns] entries, left to right.
 *
 * The last row is short whenever the count is not a multiple of [columns]:
 * padding it would put blank cells in the grid, which is cheaper than the
 * alternative — a cell that looks like a rate and is not one.
 *
 * @throws IllegalArgumentException when [columns] is below 1, which would divide
 *   the series into rows of no width at all.
 */
fun <T> historyGridRows(items: List<T>, columns: Int = HISTORY_GRID_COLUMNS): List<List<T>> {
    require(columns >= 1) { "columns must be at least 1, was $columns" }
    if (items.isEmpty()) return emptyList()
    return items.chunked(columns)
}
