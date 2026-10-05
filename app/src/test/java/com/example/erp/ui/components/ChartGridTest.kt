package com.example.erp.ui.components

import com.example.erp.data.HISTORICO_PUNTOS
import com.example.erp.ui.HISTORICO_MUESTRAS_HORARIAS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the chart names its points: the axis subset that fits a phone width, and
 * the grid rows that carry the dates the axis had to leave out.
 */
class ChartGridTest {

    private fun assertAscendingNoDuplicates(indices: List<Int>, count: Int) {
        assertTrue("indices must be strictly ascending: $indices", indices.zipWithNext().all { (a, b) -> a < b })
        assertEquals("indices must be unique: $indices", indices.size, indices.toSet().size)
        assertTrue("indices must stay inside the series: $indices", indices.all { it in 0 until count })
    }

    // --- X axis labels ----------------------------------------------------

    @Test
    fun `a full daily chart spreads five labels across the whole window`() {
        // The case that started this: fifteen points but only two dates on the axis.
        assertEquals(
            listOf(0, 4, 7, 11, HISTORICO_PUNTOS - 1),
            xAxisLabelIndices(HISTORICO_PUNTOS, X_AXIS_MAX_LABELS)
        )
    }

    @Test
    fun `the spread is even instead of clustered at the start`() {
        // A front-loaded selection satisfies "include both ends" while answering
        // a question nobody asked: the gaps would be tiny on the left and huge on
        // the right, so the axis would name the recent days and skip the rest.
        val indices = xAxisLabelIndices(HISTORICO_PUNTOS, X_AXIS_MAX_LABELS)
        val gaps = indices.zipWithNext { a, b -> b - a }
        assertTrue("gaps too uneven: $gaps", gaps.max() - gaps.min() <= 1)
        // No date goes unnamed for longer than one step between labels.
        assertTrue(indices.last() - indices.first() >= HISTORICO_PUNTOS - X_AXIS_MAX_LABELS)
    }

    @Test
    fun `both ends are always labelled`() {
        for (count in 1..60) {
            for (maxLabels in 2..8) {
                val indices = xAxisLabelIndices(count, maxLabels)
                if (indices.isEmpty()) continue
                assertEquals("first index for count=$count", 0, indices.first())
                assertEquals("last index for count=$count", count - 1, indices.last())
            }
        }
    }

    @Test
    fun `the label budget is never exceeded`() {
        for (count in 0..40) {
            for (maxLabels in 2..8) {
                val indices = xAxisLabelIndices(count, maxLabels)
                assertTrue(
                    "count=$count maxLabels=$maxLabels returned ${indices.size}",
                    indices.size <= maxLabels
                )
                assertAscendingNoDuplicates(indices, count.coerceAtLeast(1))
            }
        }
    }

    @Test
    fun `a series shorter than the budget labels every point`() {
        assertEquals(listOf(0, 1, 2, 3, 4), xAxisLabelIndices(5, 5))
        assertEquals(listOf(0, 1, 2, 3), xAxisLabelIndices(4, 5))
    }

    @Test
    fun `degenerate counts do not crash`() {
        assertEquals(emptyList<Int>(), xAxisLabelIndices(0, 5))
        assertEquals(emptyList<Int>(), xAxisLabelIndices(-3, 5))
        assertEquals(listOf(0), xAxisLabelIndices(1, 5))
        assertEquals(listOf(0, 1), xAxisLabelIndices(2, 5))
    }

    @Test
    fun `a budget that cannot hold both ends is rejected instead of clamped`() {
        // Clamping to one label would break the "never more than maxLabels"
        // promise the caller relies on and would date a window with one day.
        assertThrows(IllegalArgumentException::class.java) { xAxisLabelIndices(15, 1) }
        assertThrows(IllegalArgumentException::class.java) { xAxisLabelIndices(15, 0) }
        // A lone point still needs no second label, so it is not an error.
        assertEquals(listOf(0), xAxisLabelIndices(1, 1))
    }

    // --- grid rows --------------------------------------------------------

    @Test
    fun `fifteen updates become three columns of five rows`() {
        val rows = historyGridRows((1..HISTORICO_PUNTOS).toList())
        assertEquals(5, rows.size)
        assertTrue(rows.all { it.size == HISTORY_GRID_COLUMNS })
        // Chronological order is the reading order; chunking must not reorder.
        assertEquals((1..HISTORICO_PUNTOS).toList(), rows.flatten())
    }

    @Test
    fun `a short last row is left short rather than padded`() {
        // Padding would draw empty cells that look like rates that do not exist.
        assertEquals(listOf(listOf(1, 2, 3), listOf(4)), historyGridRows(listOf(1, 2, 3, 4)))
    }

    @Test
    fun `every entry is kept exactly once`() {
        val rows = historyGridRows((1..HISTORICO_MUESTRAS_HORARIAS).toList())
        assertEquals((1..HISTORICO_MUESTRAS_HORARIAS).toList(), rows.flatten())
        assertEquals(16, rows.size)
    }

    @Test
    fun `an empty series produces no rows`() {
        assertEquals(emptyList<List<Int>>(), historyGridRows(emptyList<Int>()))
    }

    @Test
    fun `a column count below one is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { historyGridRows(listOf(1), columns = 0) }
    }

    @Test
    fun `the column count is configurable without changing the order`() {
        assertEquals(
            listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6)),
            historyGridRows((1..6).toList(), columns = 2)
        )
    }
}
