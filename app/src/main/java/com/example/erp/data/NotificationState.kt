package com.example.erp.data

import android.content.Context

/**
 * Remembers what the user has already been told.
 *
 * A scheduled worker runs on a cadence, not on a change. Without this, the
 * daily job re-announces the same rate every time it fires, and the hourly
 * job re-announces an unchanged parallel market every hour. Both are noise, and
 * noise is what makes people silence the notifications that matter.
 *
 * Stored as plain strings keyed per topic; comparing the value is enough, so
 * "855,66|2026-09-25" changes exactly when the number or the rate date does.
 */
object NotificationState {

    private const val PREFS = "notification_state"

    private const val KEY_DAILY_RATE = "daily_rate"
    private const val KEY_NEXT_RATE = "next_rate"
    private const val KEY_PARALLEL = "parallel_rate"

    /**
     * Records [value] for [topic] and returns true when it differs from what was
     * recorded before — the caller should notify only then.
     */
    fun markIfChanged(context: Context, topic: String, value: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(topic, null)
        prefs.edit().putString(topic, value).apply()
        return previous != value
    }

    fun wasNotified(context: Context, topic: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(topic, null) != null

    fun shouldNotifyDailyRate(context: Context, rate: Double, date: String): Boolean =
        markIfChanged(context, KEY_DAILY_RATE, "$rate|$date")

    fun shouldNotifyNextRate(context: Context, rate: Double, date: String): Boolean =
        markIfChanged(context, KEY_NEXT_RATE, "$rate|$date")

    fun shouldNotifyParallel(context: Context, rate: Double): Boolean =
        markIfChanged(context, KEY_PARALLEL, "$rate")
}
