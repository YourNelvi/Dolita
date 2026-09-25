package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
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
            .addTag(EVENING_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MORNING_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            morning
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EVENING_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
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
            .addTag(USDT_WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                USDT_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
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