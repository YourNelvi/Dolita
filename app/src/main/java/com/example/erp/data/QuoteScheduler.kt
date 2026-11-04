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

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /**
     * Gates the hourly parallel-market sample.
     *
     * `CONNECTED` because a worker that runs without a network wakes the
     * device, opens a socket, fails, and leaves WorkManager to schedule a
     * backoff retry. The retry is what costs - not the attempt.
     *
     * `requiresBatteryNotLow` because WorkManager does not cancel gated work,
     * it waits for the condition. A deferred sample still arrives once the
     * device leaves the low-battery window, and no announcement depends on the
     * exact minute it lands.
     */
    internal val fetchConstraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /**
     * Gates the twice-daily official-rate checks.
     *
     * No battery gate on purpose, unlike [fetchConstraints]. The next day's
     * rate is only useful before midnight: WorkManager treats a low battery as
     * "wait", not "skip", so the one run that would have announced it can be
     * deferred past the date rollover and the notice is simply lost. The check
     * is two small requests a day, which is not worth that trade.
     */
    internal val bcvConstraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleDailyFetch(context: Context) {
        val now = System.currentTimeMillis()
        // Morning: update the request in place without moving its target. An
        // app open between 08:00 and 16:00 can happen while today's run is
        // still pending (Doze, reboot), and re-aiming it at tomorrow would
        // cancel the announcement. The worker pins the exact hour after every
        // run, so the schedule does not need to be rebuilt here.
        enqueueMorningCheck(context, nextRunMillis = null)
        scheduleEveningCheck(context, RateSchedulePolicy.nextEveningSlot(now, zone))
        Log.d(
            "QuoteScheduler",
            "Scheduled BCV checks at ${RateSchedulePolicy.BCV_MORNING_HOUR}:00 and " +
                "${RateSchedulePolicy.BCV_EVENING_HOUR}:00-" +
                "${RateSchedulePolicy.BCV_NEXT_RATE_RETRY_HOUR}:00 device time"
        )
    }

    /**
     * Arms the morning announcement for the next [RateSchedulePolicy.BCV_MORNING_HOUR].
     *
     * Called by the worker after a run, with [nextRunMillis] pinned so the next
     * run lands on the exact hour. The next run used to be
     * `last run + 24h`, which made every Doze delay permanent; an absolute
     * override returns to 08:00 instead of accumulating the delay. The flex
     * window is gone for the same reason: WorkManager subtracted it from the
     * first run, so a 19:00 request fired at 18:00.
     */
    internal fun pinMorningCheck(context: Context) {
        val now = System.currentTimeMillis()
        enqueueMorningCheck(
            context,
            nextRunMillis = RateSchedulePolicy.nextHourAt(now, RateSchedulePolicy.BCV_MORNING_HOUR, zone)
        )
    }

    private fun enqueueMorningCheck(context: Context, nextRunMillis: Long?) {
        val builder = PeriodicWorkRequest.Builder(
            FetchQuotesWorker::class.java,
            24L, TimeUnit.HOURS
        )
            .setInputData(workDataOf(FetchQuotesWorker.KEY_CHECK to FetchQuotesWorker.CHECK_MORNING))
            .setConstraints(bcvConstraints)
            .addTag(MORNING_WORK_NAME)

        if (nextRunMillis != null) {
            builder.setNextScheduleTimeOverride(nextRunMillis)
        }

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MORNING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            builder.build()
        )
    }

    /**
     * Arms the next attempt at catching the next day's rate.
     *
     * The worker calls [rescheduleNextRateCheck] after every run so the retry
     * window survives even if the app is never opened; [scheduleDailyFetch]
     * uses it on launch to make sure opening the app inside the window never
     * cancels the chance that is still ahead.
     */
    internal fun scheduleEveningCheck(context: Context, nextRunMillis: Long) {
        val request = PeriodicWorkRequest.Builder(
            FetchQuotesWorker::class.java,
            24L, TimeUnit.HOURS
        )
            .setNextScheduleTimeOverride(nextRunMillis)
            .setInputData(workDataOf(FetchQuotesWorker.KEY_CHECK to FetchQuotesWorker.CHECK_EVENING))
            .setConstraints(bcvConstraints)
            .addTag(EVENING_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EVENING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    internal fun rescheduleNextRateCheck(context: Context, nextRateFound: Boolean) {
        val next = RateSchedulePolicy.nextEveningCheck(
            System.currentTimeMillis(),
            nextRateFound,
            zone
        )
        scheduleEveningCheck(context, next)
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
