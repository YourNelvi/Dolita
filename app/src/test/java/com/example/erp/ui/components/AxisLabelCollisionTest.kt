package com.example.erp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Axis dates must never be drawn on top of one another.
 *
 * Found on the hourly series: with eight samples and a five-label budget the
 * evenly spaced indices came out 0, 2, 4, 5, 7 — 4 and 5 adjacent — and two full
 * "dd/MM HH:mm" timestamps rendered over each other as one unreadable string. A
 * fixed budget cannot prevent that, because the daily format fits five labels
 * where the hourly format fits barely three.
 */
class AxisLabelCollisionTest {

    /** Evenly spaced slot positions, as the chart lays points out. */
    private fun slotX(index: Int, count: Int, width: Float = 900f) =
        if (count <= 1) 0f else 40f + index * (width - 80f) / (count - 1)

    private fun centreLeft(index: Int, count: Int, textWidth: Float) =
        slotX(index, count) - textWidth / 2f

    private fun pick(
        count: Int,
        textWidth: Float,
        candidates: List<Int> = xAxisLabelIndices(count, X_AXIS_MAX_LABELS),
        minGap: Float = 10f
    ) = selectAxisLabels(
        candidates = candidates,
        lastIndex = count - 1,
        labelLeft = { centreLeft(it, count, textWidth) },
        labelWidth = { textWidth },
        minGap = minGap
    )

    private fun assertNoOverlap(kept: List<Int>, count: Int, textWidth: Float, minGap: Float = 10f) {
        val rights = kept.map { centreLeft(it, count, textWidth) + textWidth }
        rights.zipWithNext { a, b -> assertTrue("labels overlap: $a then $b", b - a >= minGap) }
    }

    @Test
    fun `the reported hourly case no longer collides`() {
        // Eight hourly samples, budget five -> indices 0, 2, 4, 5, 7. The two
        // middle ones are adjacent points, which is exactly what overlapped.
        val kept = pick(count = 8, textWidth = 165f)

        assertTrue("the colliding pair 4 and 5 both survived", !(4 in kept && 5 in kept))
        assertNoOverlap(kept, count = 8, textWidth = 165f)
    }

    @Test
    fun `the endpoints always survive so the range is still readable`() {
        val kept = pick(count = 8, textWidth = 165f)
        assertEquals(0, kept.first())
        assertEquals(7, kept.last())
    }

    @Test
    fun `a narrow format keeps every label`() {
        // "dd/MM" fits: nothing is dropped when there was room for all of them.
        val kept = pick(count = 15, textWidth = 60f)
        assertEquals(5, kept.size)
        assertNoOverlap(kept, count = 15, textWidth = 60f)
    }

    @Test
    fun `labels never exceed the candidate budget`() {
        for (count in 1..48) {
            for (w in listOf(40f, 90f, 165f, 300f)) {
                val kept = pick(count = count, textWidth = w)
                assertTrue("too many labels for count=$count w=$w", kept.size <= X_AXIS_MAX_LABELS)
                assertTrue("not sorted for count=$count", kept == kept.sorted())
                assertTrue("duplicates for count=$count w=$w", kept.distinct() == kept)
            }
        }
    }

    @Test
    fun `a lone label and an empty series are handled`() {
        assertEquals(emptyList<Int>(), selectAxisLabels(emptyList(), 0, { 0f }, { 0f }, 10f))
        assertEquals(listOf(0), pick(count = 1, textWidth = 60f))
    }

    @Test
    fun `a middle label yields to the final one rather than pushing it off screen`() {
        // The last date anchors the right edge, so it keeps its room.
        val candidates = listOf(0, 3, 4, 7)
        val kept = pick(count = 8, textWidth = 165f, candidates = candidates)

        assertTrue(7 in kept)
        assertEquals(7, kept.last())
    }
}
