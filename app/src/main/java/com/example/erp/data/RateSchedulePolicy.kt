package com.example.erp.data

import java.time.Instant
import java.time.ZoneId

/**
 * When Dolita is allowed to spend a request.
 *
 * The parallel market is watched hourly, but only during the hours people
 * actually trade: 08:00 to 21:59 on the device's own clock. Outside that
 * window a fetch would wake the device to learn nothing.
 *
 * The official rates are checked twice a day: in the morning when BCV
 * publishes, and again in the evening when the next day's rate appears.
 *
 * Pure and clock-injected so the boundaries are testable without a device.
 */
object RateSchedulePolicy {

    /** Paralelo is sampled hourly between these hours, [start, end). */
    const val PARALLEL_WINDOW_START_HOUR = 8
    const val PARALLEL_WINDOW_END_HOUR = 22

    /** Official-rate checks, in device-local hours. */
    const val BCV_MORNING_HOUR = 8
    const val BCV_EVENING_HOUR = 19

    fun isParallelWindowOpen(hourOfDay: Int): Boolean =
        hourOfDay in PARALLEL_WINDOW_START_HOUR until PARALLEL_WINDOW_END_HOUR

    fun shouldFetchParallel(epochMillis: Long, zoneId: ZoneId): Boolean =
        isParallelWindowOpen(Instant.ofEpochMilli(epochMillis).atZone(zoneId).hour)

    fun shouldFetchBcv(epochMillis: Long, zoneId: ZoneId): Boolean {
        val hour = Instant.ofEpochMilli(epochMillis).atZone(zoneId).hour
        return hour == BCV_MORNING_HOUR || hour == BCV_EVENING_HOUR
    }

    /**
     * A stored rate younger than this can be shown as-is. Two hours sits above
     * the hourly parallelo cadence, so opening the app inside a window almost
     * never needs the network.
     */
    const val CACHE_MAX_AGE_MILLIS = 2 * 60 * 60_000L

    fun isCacheFresh(ageMillis: Long): Boolean =
        ageMillis in 0 until CACHE_MAX_AGE_MILLIS

    /**
     * Milliseconds from [now] to the next full hour, so the periodic worker
     * lands on the hour instead of drifting with its own start time.
     */
    fun millisToNextHour(now: Long, zoneId: ZoneId): Long {
        val next = Instant.ofEpochMilli(now).atZone(zoneId).plusHours(1).withMinute(0).withSecond(0).withNano(0)
        return next.toInstant().toEpochMilli() - now
    }

    /**
     * Milliseconds from [now] to [targetHour]:0 on the device clock, rolling to
     * tomorrow when the hour has already passed today.
     */
    fun millisUntilHour(now: Long, targetHour: Int, zoneId: ZoneId): Long {
        val zoned = Instant.ofEpochMilli(now).atZone(zoneId)
        var target = zoned
            .withHour(targetHour)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(zoned)) target = target.plusDays(1)
        return target.toInstant().toEpochMilli() - now
    }
}
