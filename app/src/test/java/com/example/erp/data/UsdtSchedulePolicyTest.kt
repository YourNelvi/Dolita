package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The USDT countdown is a function of the clock and the worker's real window,
 * so every boundary of that window is pinned here.
 */
class UsdtSchedulePolicyTest {

    private val caracas = ZoneId.of("America/Caracas")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0, second: Int = 0): Long =
        ZonedDateTime.of(year, month, day, hour, minute, second, 0, caracas).toInstant().toEpochMilli()

    // =========================================================================
    // Next effective fetch
    // =========================================================================

    @Test
    fun `before eight the next fetch is today at eight`() {
        assertEquals(at(2026, 11, 12, 8, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 7, 30), caracas))
    }

    @Test
    fun `seven fifty nine is still before the window`() {
        assertEquals(at(2026, 11, 12, 8, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 7, 59), caracas))
    }

    @Test
    fun `exactly at eight the next fetch is nine`() {
        assertEquals(at(2026, 11, 12, 9, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 8, 0), caracas))
    }

    @Test
    fun `inside the window the next fetch is the next full hour`() {
        assertEquals(at(2026, 11, 12, 9, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 8, 30), caracas))
        assertEquals(at(2026, 11, 12, 21, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 20, 30), caracas))
    }

    @Test
    fun `nine pm is the last slot of the day`() {
        assertEquals(at(2026, 11, 12, 21, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 20, 59), caracas))
    }

    @Test
    fun `at nine pm the window is closed until tomorrow at eight`() {
        assertEquals(at(2026, 11, 13, 8, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 21, 0), caracas))
    }

    @Test
    fun `after the window the next fetch is tomorrow at eight`() {
        assertEquals(at(2026, 11, 13, 8, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 21, 59), caracas))
        assertEquals(at(2026, 11, 13, 8, 0), RateSchedulePolicy.nextUsdtFetchMillis(at(2026, 11, 12, 23, 45), caracas))
    }

    @Test
    fun `the next fetch follows the device time zone, not a fixed offset`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        // 09:00 in Caracas is 22:00 in Tokyo: inside the window in one zone and
        // already closed in the other at the very same instant.
        val instant = at(2026, 11, 12, 9, 0)
        assertEquals(at(2026, 11, 12, 10, 0), RateSchedulePolicy.nextUsdtFetchMillis(instant, caracas))
        assertEquals(
            ZonedDateTime.of(2026, 11, 13, 8, 0, 0, 0, tokyo).toInstant().toEpochMilli(),
            RateSchedulePolicy.nextUsdtFetchMillis(instant, tokyo)
        )
    }

    @Test
    fun `the previous slot is the start of the current hour`() {
        assertEquals(
            at(2026, 11, 12, 10, 0),
            RateSchedulePolicy.previousUsdtFetchMillis(at(2026, 11, 12, 10, 30), caracas)
        )
        assertEquals(
            at(2026, 11, 12, 21, 0),
            RateSchedulePolicy.previousUsdtFetchMillis(at(2026, 11, 12, 21, 59), caracas)
        )
    }

    @Test
    fun `the previous slot before eight is yesterday at nine pm`() {
        assertEquals(
            at(2026, 11, 11, 21, 0),
            RateSchedulePolicy.previousUsdtFetchMillis(at(2026, 11, 12, 7, 59), caracas)
        )
    }

    // =========================================================================
    // Parsing the stored sample timestamp
    // =========================================================================

    @Test
    fun `an ISO offset timestamp is parsed as an instant`() {
        assertEquals(
            at(2026, 11, 12, 10, 15, 30),
            RateSchedulePolicy.parseUsdtFetchMillis("2026-11-12T10:15:30-04:00", caracas)
        )
    }

    @Test
    fun `an instant with a Z suffix is parsed`() {
        assertEquals(
            at(2026, 11, 12, 10, 15, 30),
            RateSchedulePolicy.parseUsdtFetchMillis("2026-11-12T14:15:30Z", caracas)
        )
    }

    @Test
    fun `a local date time is interpreted in the device zone`() {
        assertEquals(
            at(2026, 11, 12, 10, 15, 30),
            RateSchedulePolicy.parseUsdtFetchMillis("2026-11-12T10:15:30", caracas)
        )
    }

    @Test
    fun `blank or garbage timestamps are unparseable`() {
        assertNull(RateSchedulePolicy.parseUsdtFetchMillis(null, caracas))
        assertNull(RateSchedulePolicy.parseUsdtFetchMillis("", caracas))
        assertNull(RateSchedulePolicy.parseUsdtFetchMillis("   ", caracas))
        assertNull(RateSchedulePolicy.parseUsdtFetchMillis("not-a-date", caracas))
    }

    // =========================================================================
    // Countdown and progress
    // =========================================================================

    @Test
    fun `a countdown without a previous sample has no remaining bar`() {
        val countdown = RateSchedulePolicy.usdtCountdown(at(2026, 11, 12, 10, 30), null, caracas)
        assertEquals(at(2026, 11, 12, 11, 0), countdown.nextFetchMillis)
        assertEquals(30 * 60_000L, countdown.remainingMillis)
        assertEquals(0f, countdown.remainingFraction, 0.0001f)
    }

    @Test
    fun `the bar drains backwards through the hourly slot`() {
        // Inside the daily window the span is exactly one hour. At :15 three
        // quarters are left, at :30 half, at :45 one quarter: it shrinks toward
        // the next sample instead of filling toward it.
        fun fractionAt(hour: Int, minute: Int): Float = RateSchedulePolicy.usdtCountdown(
            nowMillis = at(2026, 11, 12, hour, minute),
            lastFetchMillis = at(2026, 11, 12, hour, 0),
            zoneId = caracas
        ).remainingFraction

        assertEquals(1f, fractionAt(10, 0), 0.0001f)
        assertEquals(0.75f, fractionAt(10, 15), 0.0001f)
        assertEquals(0.5f, fractionAt(10, 30), 0.0001f)
        assertEquals(0.25f, fractionAt(10, 45), 0.0001f)
    }

    @Test
    fun `a manual refresh does not restart the hourly bar`() {
        // A refresh at :30 updates the sample but the next automatic fetch is
        // still 11:00, so the bar keeps the half hour that is actually left.
        val countdown = RateSchedulePolicy.usdtCountdown(
            nowMillis = at(2026, 11, 12, 10, 30),
            lastFetchMillis = at(2026, 11, 12, 10, 30),
            zoneId = caracas
        )
        assertEquals(0.5f, countdown.remainingFraction, 0.0001f)
        assertTrue(countdown.remainingFraction < 1f)
    }

    @Test
    fun `the overnight gap drains over eleven hours`() {
        // 21:00 is the last slot; the next real sample is 08:00. The bar spans
        // the whole gap instead of pretending another hourly slot exists.
        val lateNight = RateSchedulePolicy.usdtCountdown(
            nowMillis = at(2026, 11, 12, 22, 30),
            lastFetchMillis = at(2026, 11, 12, 21, 0),
            zoneId = caracas
        )
        assertEquals(9.5f / 11f, lateNight.remainingFraction, 0.0001f)

        val beforeDawn = RateSchedulePolicy.usdtCountdown(
            nowMillis = at(2026, 11, 13, 7, 30),
            lastFetchMillis = at(2026, 11, 12, 21, 0),
            zoneId = caracas
        )
        assertEquals(0.5f / 11f, beforeDawn.remainingFraction, 0.0001f)
    }

    @Test
    fun `remaining time is never negative`() {
        // nextUsdtFetchMillis always lands ahead of now, but the invariant is
        // part of the contract the card renders.
        val countdown = RateSchedulePolicy.usdtCountdown(at(2026, 11, 12, 20, 59), at(2026, 11, 12, 20, 0), caracas)
        assertEquals(60_000L, countdown.remainingMillis)
    }

    // =========================================================================
    // Formatting
    // =========================================================================

    @Test
    fun `zero remaining formats as zero`() {
        assertEquals("00:00", RateSchedulePolicy.formatUsdtCountdown(0))
    }

    @Test
    fun `under an hour formats as minutes and seconds`() {
        assertEquals("44:17", RateSchedulePolicy.formatUsdtCountdown(44 * 60_000L + 17_000L))
        assertEquals("59:59", RateSchedulePolicy.formatUsdtCountdown(59 * 60_000L + 59_000L))
    }

    @Test
    fun `one hour or more formats with hours, minutes and seconds`() {
        assertEquals("1:00:00", RateSchedulePolicy.formatUsdtCountdown(60 * 60_000L))
        assertEquals("8:00:00", RateSchedulePolicy.formatUsdtCountdown(8 * 60 * 60_000L))
        assertEquals("1:02:03", RateSchedulePolicy.formatUsdtCountdown(60 * 60_000L + 2 * 60_000L + 3_000L))
    }

    @Test
    fun `a negative remaining never renders a nonsense clock`() {
        assertEquals("00:00", RateSchedulePolicy.formatUsdtCountdown(-5_000L))
    }
}
