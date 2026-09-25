package com.example.erp.data

/**
 * When the floating overlay is allowed to touch the network.
 *
 * The overlay used to poll three endpoints every 60 seconds for as long as it
 * stayed open: 4,320 requests a day to redraw a rate that publishes once. The
 * rule here is that silence is information — a rate that has not moved does not
 * need to be re-checked on a fixed cadence, so the interval backs off while
 * nothing changes and snaps back the moment something does.
 *
 * Pure and clock-injected on purpose: the decisions are testable without a
 * device, an emulator, or a real network.
 */
object RateRefreshPolicy {

    /** Intervals in ascending order; the streak index walks this ladder. */
    val BACKOFF_LADDER_MILLIS = listOf(
        60_000L,   // 1 min  — right after something moved
        2 * 60_000L,   // 2 min
        5 * 60_000L,   // 5 min
        15 * 60_000L   // 15 min — parked; a rate that is this quiet is not moving
    )

    /**
     * A cached rate younger than this is shown without touching the network.
     * Sits between the first two rungs so opening the overlay right after the
     * app already refreshed costs zero requests.
     */
    const val CACHE_FRESH_WINDOW_MILLIS = 2 * 60_000L

    /**
     * How long to wait before the next refresh, given how many consecutive
     * refreshes have returned exactly the same numbers. A negative streak is
     * treated as "just changed".
     */
    fun nextDelayMillis(unchangedStreak: Int): Long {
        val index = unchangedStreak.coerceIn(0, BACKOFF_LADDER_MILLIS.lastIndex)
        return BACKOFF_LADDER_MILLIS[index]
    }

    /** True when a cached snapshot is young enough to paint without a fetch. */
    fun isCacheUsable(ageMillis: Long): Boolean =
        ageMillis in 0 until CACHE_FRESH_WINDOW_MILLIS
}
