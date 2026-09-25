package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The overlay's network budget. Every assertion here corresponds to a real
 * request that used to be made every 60 seconds.
 */
class RateRefreshPolicyTest {

    @Test
    fun `first unchanged check waits one minute`() {
        assertEquals(60_000L, RateRefreshPolicy.nextDelayMillis(0))
    }

    @Test
    fun `backoff climbs while nothing moves`() {
        assertEquals(2 * 60_000L, RateRefreshPolicy.nextDelayMillis(1))
        assertEquals(5 * 60_000L, RateRefreshPolicy.nextDelayMillis(2))
        assertEquals(15 * 60_000L, RateRefreshPolicy.nextDelayMillis(3))
    }

    @Test
    fun `backoff parks at the top of the ladder`() {
        val top = RateRefreshPolicy.BACKOFF_LADDER_MILLIS.last()
        assertEquals(top, RateRefreshPolicy.nextDelayMillis(4))
        assertEquals(top, RateRefreshPolicy.nextDelayMillis(50))
        assertEquals(top, RateRefreshPolicy.nextDelayMillis(Int.MAX_VALUE))
    }

    @Test
    fun `a changed rate resets the streak to the fast rung`() {
        // streak 0 is what the caller passes right after a value change
        assertEquals(60_000L, RateRefreshPolicy.nextDelayMillis(0))
    }

    @Test
    fun `negative streak is treated as changed, not as an index error`() {
        assertEquals(60_000L, RateRefreshPolicy.nextDelayMillis(-1))
    }

    @Test
    fun `fresh cache is used without a network call`() {
        assertTrue(RateRefreshPolicy.isCacheUsable(0))
        assertTrue(RateRefreshPolicy.isCacheUsable(30_000))
        assertTrue(RateRefreshPolicy.isCacheUsable(RateRefreshPolicy.CACHE_FRESH_WINDOW_MILLIS - 1))
    }

    @Test
    fun `stale cache forces a fetch`() {
        assertFalse(RateRefreshPolicy.isCacheUsable(RateRefreshPolicy.CACHE_FRESH_WINDOW_MILLIS))
        assertFalse(RateRefreshPolicy.isCacheUsable(12 * 60 * 60_000L))
    }

    @Test
    fun `a clock skew into the future does not pass as fresh forever`() {
        // negative age = device clock moved backwards; treat as unusable
        assertFalse(RateRefreshPolicy.isCacheUsable(-5_000))
    }
}
