package com.example.erp.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chart must draw the published rates, not a prettier version of them.
 *
 * The euro series was the proof: it genuinely oscillates a few bolivars, and the
 * old fixed-handle spline turned each turn of the series into a peak larger than
 * the real move. These tests assert the property directly.
 */
class MonotoneSplineTest {

    /** Evenly spaced slots, one per published rate, as the chart now lays them out. */
    private fun points(prices: List<Double>) =
        prices.mapIndexed { i, p -> Offset(i.toFloat(), p.toFloat()) }

    /** Walks each segment and checks the curve stays between its two endpoints. */
    private fun assertNoOvershoot(prices: List<Double>) {
        val pts = points(prices)
        val m = monotoneTangents(pts)
        for (i in 0 until prices.size - 1) {
            val lo = minOf(prices[i], prices[i + 1]).toFloat()
            val hi = maxOf(prices[i], prices[i + 1]).toFloat()
            var t = 0f
            while (t <= 1.0001f) {
                val y = hermiteY(
                    prices[i].toFloat(), prices[i + 1].toFloat(), m[i], m[i + 1], t
                )
                assertTrue(
                    "segment $i at t=$t drew $y outside [$lo, $hi]",
                    y >= lo - 0.001f && y <= hi + 0.001f
                )
                t += 0.02f
            }
        }
    }

    @Test
    fun `an oscillating series never draws outside the corridor between its points`() {
        // The real euro shape from the source: a few bolivars up and down.
        assertNoOvershoot(
            listOf(
                963.21, 968.07, 977.88, 977.18, 977.68, 974.42, 974.09, 978.17,
                976.55, 974.06, 972.65, 976.90, 974.71, 973.31, 976.84, 973.93,
                981.18, 977.22
            )
        )
    }

    @Test
    fun `a rising series never draws outside the corridor either`() {
        assertNoOvershoot(
            listOf(
                827.74, 832.49, 842.21, 846.51, 847.44, 848.55, 849.56, 852.42,
                853.50, 854.46, 855.66, 857.01, 857.89, 859.06, 860.18, 866.56,
                871.37, 872.39
            )
        )
    }

    @Test
    fun `a series that alternates every single step stays inside its corridor`() {
        // The worst case for overshoot: the direction flips on every segment.
        assertNoOvershoot(listOf(100.0, 104.0, 101.0, 105.0, 102.0, 106.0, 103.0))
    }

    @Test
    fun `a repeated value stays flat across the step`() {
        assertNoOvershoot(listOf(973.93, 973.93, 973.93, 981.18))
    }

    @Test
    fun `a flat step gets zero tangents so the curve does not dive through it`() {
        val m = monotoneTangents(points(listOf(10.0, 10.0, 12.0)))
        assertEquals(0f, m[1], 0.0001f)
        assertEquals(0f, m[0], 0.0001f)
    }

    @Test
    fun `a single point and an empty series are handled without crashing`() {
        assertEquals(0, monotoneTangents(emptyList()).size)
        assertEquals(1, monotoneTangents(points(listOf(5.0))).size)
        assertEquals(2, monotoneTangents(points(listOf(5.0, 6.0))).size)
    }

    @Test
    fun `tangents never explode on extreme alternating input`() {
        val m = monotoneTangents(points(listOf(0.0, 1000.0, 0.0, 1000.0, 0.0)))
        m.forEach { assertTrue("tangent $it is not finite", it.isFinite()) }
    }
}
