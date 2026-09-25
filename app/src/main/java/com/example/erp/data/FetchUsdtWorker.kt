package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.erp.notification.NotificationHelper
import com.example.erp.widget.RateWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hourly worker that samples the parallel market.
 *
 * Two things changed here. It fetches ONLY the parallel quote — the official
 * rate publishes once a day, so re-downloading it every hour was a request
 * spent on data that cannot have moved. And it refuses to run outside
 * 08:00–21:59 on the device clock, because nobody is trading at 4 AM and the
 * job would exist only to wake the device.
 */
class FetchUsdtWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val apiRepository = ApiDolarRepository()
    private val historyStore: RateHistoryStore = FileHistoryStore(
        dir = context.filesDir.resolve("rate_history"),
        zoneId = java.time.ZoneId.systemDefault()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val zone = java.time.ZoneId.systemDefault()
        if (!RateSchedulePolicy.shouldFetchParallel(System.currentTimeMillis(), zone)) {
            Log.d("FetchUsdtWorker", "Outside the ${RateSchedulePolicy.PARALLEL_WINDOW_START_HOUR}:00-" +
                "${RateSchedulePolicy.PARALLEL_WINDOW_END_HOUR}:00 window on the device clock; nothing to do")
            return@withContext Result.success()
        }

        return@withContext try {
            Log.d("FetchUsdtWorker", "Sampling Paralelo")
            val parQuote = apiRepository.fetchUsdtQuote()
            Log.d("FetchUsdtWorker", "Paralelo fetched: ${parQuote.promedio}")

            // Sample and persist
            historyStore.append(
                listOf(
                    RateSample(
                        fuente = "usdt",
                        nombre = "Paralelo",
                        precio = parQuote.promedio,
                        timestampEpochMillis = System.currentTimeMillis()
                    )
                )
            )

            // Keep the shared cache fresh so the app, the widget and the overlay
            // all read this sample instead of each hitting the network.
            QuotesCache.upsert(applicationContext, parQuote)

            // Update home screen widget
            RateWidgetProvider.updateAllWidgets(applicationContext)

            // Notify only when the number actually moved.
            if (NotificationState.shouldNotifyParallel(applicationContext, parQuote.promedio)) {
                NotificationHelper.showUsdtNotification(
                    context = applicationContext,
                    usdtRate = parQuote.promedio
                )
            } else {
                Log.d("FetchUsdtWorker", "Paralelo unchanged; skipping notification")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("FetchUsdtWorker", "Paralelo fetch failed: ${e.message}")
            Result.retry()
        }
    }
}