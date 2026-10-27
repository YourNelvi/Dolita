package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object QuoteScheduler {
    private const val MORNING_WORK_NAME = "daily_quotes_fetch"
    private const val EVENING_WORK_NAME = "evening_next_rate_fetch"
    private const val USDT_WORK_NAME = "hourly_usdt_fetch"
    private const val FLEX_WINDOW_HOURS = 1L

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /**
     * Gates every scheduled fetch. Both are deliberately conservative:
     *
     * `CONNECTED` because a worker that runs without a network wakes the device,
     * opens a socket, fails, and leaves WorkManager to schedule a backoff retry.
     * The retry is what costs - not the attempt.
     *
     * `requiresBatteryNotLow` because WorkManager does not cancel gated work, it
     * waits for the condition. A deferred fetch still arrives once the device
     * leaves the low-battery window, so nothing is lost by not spending a
     * request at 8%.
     *
     * `requiresCharging` is absent on purpose: the app would then only ever
     * update while plugged in, which is the opposite of a rate that changes
     * daily.
     */
    internal val fetchConstraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun scheduleDailyFetch(context: Context) {
        // Morning: announce the rate of the day.
        val morning = PeriodicWorkRequest.Builder(
            FetchQuotesWorker::class.java,
            24L, TimeUnit.HOURS,
            FLEX_WINDOW_HOURS, TimeUnit.HOURS
        )
            .setInitialDelay(
                RateSchedulePolicy.millisUntilHour(
                    System.currentTimeMillis(),
                    RateSchedulePolicy.BCV_MORNING_HOUR,
                    zone
                ),
                TimeUnit.MILLISECONDS
            )
            .setInputData(workDataOf(FetchQuotesWorker.KEY_CHECK to FetchQuotesWorker.CHECK_MORNING))
            .setConstraints(fetchConstraints)
            .addTag(MORNING_WORK_NAME)
            .build()

        // Evening: BCV publishes tomorrow's rate later in the day, so the only
        // thing this pass can announce is the next rate.
        val evening = PeriodicWorkRequest.Builder(
            FetchQuotesWorker::class.java,
            24L, TimeUnit.HOURS,
            FLEX_WINDOW_HOURS, TimeUnit.HOURS
        )
            .setInitialDelay(
                RateSchedulePolicy.millisUntilHour(
                    System.currentTimeMillis(),
                    RateSchedulePolicy.BCV_EVENING_HOUR,
                    zone
                ),
                TimeUnit.MILLISECONDS
            )
            .setInputData(workDataOf(FetchQuotesWorker.KEY_CHECK to FetchQuotesWorker.CHECK_EVENING))
            .setConstraints(fetchConstraints)
            .addTag(EVENING_WORK_NAME)
            .build()

        // UPDATE, not KEEP. This is what makes the constraints above reach a
        // device that already has the app installed: KEEP leaves an existing
        // work request completely untouched, so a build that only changes the
        // request would silently keep running the old one forever. UPDATE
        // rewrites the existing request in place instead of resetting the
        // schedule, so the next run time survives the app update.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MORNING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            morning
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EVENING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            evening
        )
        Log.d(
            "QuoteScheduler",
            "Scheduled BCV checks at ${RateSchedulePolicy.BCV_MORNING_HOUR}:00 and " +
                "${RateSchedulePolicy.BCV_EVENING_HOUR}:00 device time"
        )
    }

    fun scheduleHourlyUsdtFetch(context: Context) {
        // The cadence itself lands on the hour; the worker additionally refuses
        // to spend a request outside the 08:00-22:00 device window.
        val workRequest = PeriodicWorkRequest.Builder(
            FetchUsdtWorker::class.java,
            1L, TimeUnit.HOURS,
            15L, TimeUnit.MINUTES
        )
            .setInitialDelay(
                RateSchedulePolicy.millisToNextHour(System.currentTimeMillis(), zone),
                TimeUnit.MILLISECONDS
            )
            .setConstraints(fetchConstraints)
            .addTag(USDT_WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                USDT_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        Log.d(
            "QuoteScheduler",
            "Scheduled hourly Paralelo sampling, on the hour, " +
                "${RateSchedulePolicy.PARALLEL_WINDOW_START_HOUR}:00-" +
                "${RateSchedulePolicy.PARALLEL_WINDOW_END_HOUR}:00 device time"
        )
    }

    fun cancelScheduledFetch(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(MORNING_WORK_NAME)
        workManager.cancelUniqueWork(EVENING_WORK_NAME)
        workManager.cancelUniqueWork(USDT_WORK_NAME)
        Log.d("QuoteScheduler", "Cancelled all scheduled fetches")
    }
}