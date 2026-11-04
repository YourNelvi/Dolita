package com.example.erp.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * When Dolita is allowed to spend a request, and when a stored rate is still
 * worth showing.
 *
 * The parallel market is watched hourly, but only during the hours people
 * actually trade: 08:00 to 21:59 on the device's own clock. Outside that
 * window a fetch would wake the device to learn nothing.
 *
 * The official rates are checked in the morning when BCV publishes the rate of
 * the day, and again in the evening when the next day's rate appears. BCV
 * flips the published date a little after 16:00 Caracas, so the evening pass
 * keeps retrying until 22:00 instead of betting the whole announcement on a
 * single run.
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

    /**
     * BCV publishes the next day's rate a little after this hour. It is the
     * earliest moment a cached rate dated "today" can already be obsolete.
     */
    const val BCV_PUBLICATION_HOUR = 16

    /**
     * Last chance to catch the next day's rate before the date rolls over.
     * Past this hour the evening pass stops retrying and waits for tomorrow.
     */
    const val BCV_NEXT_RATE_RETRY_HOUR = 22

    fun isParallelWindowOpen(hourOfDay: Int): Boolean =
        hourOfDay in PARALLEL_WINDOW_START_HOUR until PARALLEL_WINDOW_END_HOUR

    fun shouldFetchParallel(epochMillis: Long, zoneId: ZoneId): Boolean =
        isParallelWindowOpen(Instant.ofEpochMilli(epochMillis).atZone(zoneId).hour)

    /**
     * The two hours an announcement is expected. The evening retries between
     * 20:00 and 22:00 are deliberately excluded: that pass only looks for a
     * next rate that has not been announced yet, and the future-date check
     * already gates it.
     */
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
     * Every source the app offers as a tab.
     */
    val REQUIRED_SOURCES = setOf("usd", "eur", "usdt")

    /**
     * Whether a stored snapshot can fill the whole screen.
     *
     * Recency is not enough on its own: the official worker only writes usd and
     * eur, so a snapshot written minutes ago can be missing the parallel market.
     * Rendering it anyway made the USDT tab disappear until the hourly worker
     * happened to run.
     */
    fun isSnapshotComplete(sources: Set<String>): Boolean =
        REQUIRED_SOURCES.all { sources.contains(it) }

    /**
     * Whether every source in the snapshot was refreshed recently.
     *
     * Freshness is per source on purpose. The hourly USDT worker used to bump
     * a single global timestamp, which made a snapshot with a three-hour-old
     * BCV rate look fresh just because the parallel market had been sampled
     * minutes earlier. The app then skipped the fetch that would have shown the
     * newly published official rate.
     */
    fun isSnapshotFresh(
        sourceTimestamps: Map<String, Long>,
        sources: Set<String>,
        nowMillis: Long
    ): Boolean =
        sources.isNotEmpty() && sources.all { source ->
            val fetchedAt = sourceTimestamps[source] ?: return@all false
            isCacheFresh(nowMillis - fetchedAt)
        }

    /**
     * Whether a cached official rate dated [cachedDate] may already have been
     * superseded by the rate BCV publishes the previous afternoon.
     *
     * A rate dated today is only the newest until [BCV_PUBLICATION_HOUR]. After
     * that the API may already be serving tomorrow's date, so the cache has to
     * be revalidated even when it is only a few hours old. A rate dated in the
     * future is already the next one and stays valid for the rest of the day.
     */
    fun isOfficialRateStale(
        cachedDate: String?,
        nowMillis: Long,
        zoneId: ZoneId
    ): Boolean {
        if (cachedDate.isNullOrBlank()) return true
        val date = runCatching { LocalDate.parse(cachedDate) }.getOrNull() ?: return true
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val today = now.toLocalDate()
        return when {
            date.isBefore(today) -> true
            date.isAfter(today) -> false
            else -> now.hour >= BCV_PUBLICATION_HOUR
        }
    }

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
    fun millisUntilHour(now: Long, targetHour: Int, zoneId: ZoneId): Long =
        nextHourAt(now, targetHour, zoneId) - now

    /**
     * Absolute instant, in [System.currentTimeMillis] time, of the next
     * [targetHour]:0 on the device clock. Same rule as [millisUntilHour], but
     * expressed as the value WorkManager's schedule override expects.
     */
    fun nextHourAt(now: Long, targetHour: Int, zoneId: ZoneId): Long {
        val zoned = Instant.ofEpochMilli(now).atZone(zoneId)
        var target = zoned
            .withHour(targetHour)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(zoned)) target = target.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    /**
     * Next evening check slot, strictly after [now]: 19:00, 20:00, 21:00 or
     * 22:00 today, or 19:00 tomorrow once the window has closed.
     *
     * Used both when arming the work and after an app open inside the window,
     * so there is always at least one chance left before midnight even if the
     * 19:00 run was delayed by Doze.
     */
    fun nextEveningSlot(now: Long, zoneId: ZoneId): Long {
        val zoned = Instant.ofEpochMilli(now).atZone(zoneId)
        for (hour in BCV_EVENING_HOUR..BCV_NEXT_RATE_RETRY_HOUR) {
            val candidate = zoned
                .withHour(hour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            if (candidate.isAfter(zoned)) return candidate.toInstant().toEpochMilli()
        }
        val tomorrow = zoned
            .plusDays(1)
            .withHour(BCV_EVENING_HOUR)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        return tomorrow.toInstant().toEpochMilli()
    }

    /**
     * When the evening pass should run again. A published next rate ends the
     * evening work for the day; otherwise the retry keeps moving one hour at a
     * time until the window closes, so a late publication is still caught.
     */
    fun nextEveningCheck(now: Long, nextRateFound: Boolean, zoneId: ZoneId): Long =
        if (nextRateFound) nextHourAt(now, BCV_EVENING_HOUR, zoneId)
        else nextEveningSlot(now, zoneId)
}
