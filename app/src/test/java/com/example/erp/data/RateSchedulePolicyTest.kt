package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The request budget is a function of the clock, so the clock boundaries are
 * pinned here. Every hour boundary in the parallelo window is a case.
 */
class RateSchedulePolicyTest {

    private val caracas = ZoneId.of("America/Caracas")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, caracas).toInstant().toEpochMilli()

    @Test
    fun `parallelo window opens at eight`() {
        assertTrue(RateSchedulePolicy.shouldFetchParallel(at(2026, 9, 25, 8, 0), caracas))
    }

    @Test
    fun `parallelo window is closed before eight`() {
        assertFalse(RateSchedulePolicy.shouldFetchParallel(at(2026, 9, 25, 7, 59), caracas))
    }

    @Test
    fun `parallelo window covers the whole day until ten pm`() {
        assertTrue(RateSchedulePolicy.shouldFetchParallel(at(2026, 9, 25, 21, 0), caracas))
    }

    @Test
    fun `parallelo window closes at ten pm`() {
        assertFalse(RateSchedulePolicy.shouldFetchParallel(at(2026, 9, 25, 22, 0), caracas))
    }

    @Test
    fun `parallelo window is closed in the middle of the night`() {
        assertFalse(RateSchedulePolicy.shouldFetchParallel(at(2026, 9, 25, 3, 0), caracas))
    }

    @Test
    fun `official rate is checked in the morning and in the evening only`() {
        assertTrue(RateSchedulePolicy.shouldFetchBcv(at(2026, 9, 25, 8, 0), caracas))
        assertTrue(RateSchedulePolicy.shouldFetchBcv(at(2026, 9, 25, 19, 0), caracas))
        assertFalse(RateSchedulePolicy.shouldFetchBcv(at(2026, 9, 25, 13, 0), caracas))
        assertFalse(RateSchedulePolicy.shouldFetchBcv(at(2026, 9, 25, 2, 0), caracas))
    }

    @Test
    fun `window boundaries follow the device timezone, not a fixed offset`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        // 09:00 in Caracas (UTC-4) is 22:00 in Tokyo (UTC+9). Same instant, and
        // it sits on opposite sides of the 22:00 close — which is the point:
        // the window must follow whatever zone the device reports.
        val instant = ZonedDateTime.of(2026, 9, 25, 9, 0, 0, 0, caracas).toInstant().toEpochMilli()
        assertTrue(RateSchedulePolicy.shouldFetchParallel(instant, caracas))
        assertFalse(RateSchedulePolicy.shouldFetchParallel(instant, tokyo))
    }

    @Test
    fun `next hour delay lands exactly on the hour`() {
        val now = at(2026, 9, 25, 14, 37)
        val delay = RateSchedulePolicy.millisToNextHour(now, caracas)
        assertEquals(23 * 60_000L, delay)
    }

    @Test
    fun `already on the hour waits a full hour instead of zero`() {
        val now = at(2026, 9, 25, 14, 0)
        assertEquals(60 * 60_000L, RateSchedulePolicy.millisToNextHour(now, caracas))
    }

    @Test
    fun `a passed target hour rolls to tomorrow`() {
        val now = at(2026, 9, 25, 20, 0)
        val delay = RateSchedulePolicy.millisUntilHour(now, RateSchedulePolicy.BCV_MORNING_HOUR, caracas)
        assertEquals(12 * 60 * 60_000L, delay)
    }

    @Test
    fun `a target hour still ahead today is used as is`() {
        val now = at(2026, 9, 25, 6, 0)
        assertEquals(2 * 60 * 60_000L, RateSchedulePolicy.millisUntilHour(now, 8, caracas))
    }

    @Test
    fun `fresh cache is used, stale cache is not`() {
        assertTrue(RateSchedulePolicy.isCacheFresh(0))
        assertTrue(RateSchedulePolicy.isCacheFresh(RateSchedulePolicy.CACHE_MAX_AGE_MILLIS - 1))
        assertFalse(RateSchedulePolicy.isCacheFresh(RateSchedulePolicy.CACHE_MAX_AGE_MILLIS))
    }

    @Test
    fun `a backwards clock jump does not read as permanently fresh`() {
        assertFalse(RateSchedulePolicy.isCacheFresh(-10_000))
    }
}
