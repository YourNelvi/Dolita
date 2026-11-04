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

    @Test
    fun `a snapshot with every source can render the whole screen`() {
        assertTrue(RateSchedulePolicy.isSnapshotComplete(setOf("usd", "eur", "usdt")))
    }

    @Test
    fun `a snapshot without the parallel market is not renderable`() {
        // This is the case the official worker alone produces: usd + eur, no usdt.
        assertFalse(RateSchedulePolicy.isSnapshotComplete(setOf("usd", "eur")))
    }

    @Test
    fun `an empty snapshot is never complete`() {
        assertFalse(RateSchedulePolicy.isSnapshotComplete(emptySet()))
    }

    @Test
    fun `extra sources do not break completeness`() {
        assertTrue(RateSchedulePolicy.isSnapshotComplete(setOf("usd", "eur", "usdt", "future")))
    }

    @Test
    fun `schedule override lands on the target hour today when it is still ahead`() {
        assertEquals(
            at(2026, 9, 25, 8, 0),
            RateSchedulePolicy.nextHourAt(at(2026, 9, 25, 6, 0), 8, caracas)
        )
    }

    @Test
    fun `schedule override rolls to tomorrow when the target hour has passed`() {
        assertEquals(
            at(2026, 9, 26, 8, 0),
            RateSchedulePolicy.nextHourAt(at(2026, 9, 25, 20, 0), 8, caracas)
        )
    }

    @Test
    fun `schedule override never returns the current instant`() {
        // Running exactly on the hour must move to the next day, not re-arm now.
        assertEquals(
            at(2026, 9, 26, 8, 0),
            RateSchedulePolicy.nextHourAt(at(2026, 9, 25, 8, 0), 8, caracas)
        )
    }

    @Test
    fun `evening slot before the window is seven pm`() {
        assertEquals(
            at(2026, 9, 25, 19, 0),
            RateSchedulePolicy.nextEveningSlot(at(2026, 9, 25, 15, 0), caracas)
        )
    }

    @Test
    fun `evening slot inside the window is the next hour`() {
        assertEquals(
            at(2026, 9, 25, 20, 0),
            RateSchedulePolicy.nextEveningSlot(at(2026, 9, 25, 19, 0), caracas)
        )
        assertEquals(
            at(2026, 9, 25, 22, 0),
            RateSchedulePolicy.nextEveningSlot(at(2026, 9, 25, 21, 30), caracas)
        )
    }

    @Test
    fun `evening slot after the window is tomorrow seven pm`() {
        assertEquals(
            at(2026, 9, 26, 19, 0),
            RateSchedulePolicy.nextEveningSlot(at(2026, 9, 25, 22, 0), caracas)
        )
        assertEquals(
            at(2026, 9, 26, 19, 0),
            RateSchedulePolicy.nextEveningSlot(at(2026, 9, 25, 23, 30), caracas)
        )
    }

    @Test
    fun `a delayed run after midnight still targets the same evening`() {
        // It must not skip the coming evening by always adding a day.
        assertEquals(
            at(2026, 9, 25, 19, 0),
            RateSchedulePolicy.nextHourAt(at(2026, 9, 25, 0, 30), 19, caracas)
        )
    }

    @Test
    fun `finding the next rate ends the evening retries`() {
        assertEquals(
            at(2026, 9, 26, 19, 0),
            RateSchedulePolicy.nextEveningCheck(at(2026, 9, 25, 19, 0), true, caracas)
        )
    }

    @Test
    fun `a missing next rate retries hourly inside the window`() {
        assertEquals(
            at(2026, 9, 25, 20, 0),
            RateSchedulePolicy.nextEveningCheck(at(2026, 9, 25, 19, 0), false, caracas)
        )
        assertEquals(
            at(2026, 9, 25, 22, 0),
            RateSchedulePolicy.nextEveningCheck(at(2026, 9, 25, 21, 30), false, caracas)
        )
    }

    @Test
    fun `the retry window closes before midnight`() {
        assertEquals(
            at(2026, 9, 26, 19, 0),
            RateSchedulePolicy.nextEveningCheck(at(2026, 9, 25, 22, 0), false, caracas)
        )
    }

    @Test
    fun `today's official rate is stale once the publication hour arrives`() {
        assertTrue(
            RateSchedulePolicy.isOfficialRateStale("2026-11-03", at(2026, 11, 3, 16, 0), caracas)
        )
        assertFalse(
            RateSchedulePolicy.isOfficialRateStale("2026-11-03", at(2026, 11, 3, 15, 59), caracas)
        )
    }

    @Test
    fun `tomorrow's official rate is fresh even after the publication hour`() {
        assertFalse(
            RateSchedulePolicy.isOfficialRateStale("2026-11-04", at(2026, 11, 3, 17, 0), caracas)
        )
    }

    @Test
    fun `an official rate older than today is always stale`() {
        assertTrue(
            RateSchedulePolicy.isOfficialRateStale("2026-11-02", at(2026, 11, 3, 9, 0), caracas)
        )
    }

    @Test
    fun `an unreadable official date is stale`() {
        assertTrue(
            RateSchedulePolicy.isOfficialRateStale(null, at(2026, 11, 3, 9, 0), caracas)
        )
        assertTrue(
            RateSchedulePolicy.isOfficialRateStale("not-a-date", at(2026, 11, 3, 9, 0), caracas)
        )
    }

    @Test
    fun `a snapshot is fresh only when every source was refreshed recently`() {
        val now = at(2026, 11, 3, 10, 0)
        val oneHourAgo = now - 60 * 60_000L
        val threeHoursAgo = now - 3 * 60 * 60_000L
        val sources = setOf("usd", "eur", "usdt")

        assertTrue(
            RateSchedulePolicy.isSnapshotFresh(
                mapOf("usd" to oneHourAgo, "eur" to oneHourAgo, "usdt" to oneHourAgo),
                sources,
                now
            )
        )
        // The hourly USDT sample being recent must not vouch for a stale BCV rate.
        assertFalse(
            RateSchedulePolicy.isSnapshotFresh(
                mapOf("usd" to threeHoursAgo, "eur" to oneHourAgo, "usdt" to oneHourAgo),
                sources,
                now
            )
        )
    }

    @Test
    fun `a source with no timestamp cannot be trusted as fresh`() {
        val now = at(2026, 11, 3, 10, 0)
        assertFalse(
            RateSchedulePolicy.isSnapshotFresh(
                mapOf("eur" to now, "usdt" to now),
                setOf("usd", "eur", "usdt"),
                now
            )
        )
    }

    @Test
    fun `an empty snapshot is never fresh`() {
        val now = at(2026, 11, 3, 10, 0)
        assertFalse(RateSchedulePolicy.isSnapshotFresh(emptyMap(), emptySet(), now))
    }
}
