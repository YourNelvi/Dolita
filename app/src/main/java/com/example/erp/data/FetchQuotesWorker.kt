package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.erp.notification.NotificationHelper
import com.example.erp.widget.RateWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Official-rate worker (USD + EUR from BCV).
 *
 * Runs twice a day: a morning pass that announces the rate of the day, and an
 * evening pass that only looks for the next day's rate, which BCV publishes
 * later. It fetches BCV alone — the parallel market belongs to the hourly
 * worker — and it notifies only when something actually changed, because a job
 * on a cadence is not the same thing as news.
 */
class FetchQuotesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val apiRepository = ApiDolarRepository()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val isEveningCheck = inputData.getString(KEY_CHECK) == CHECK_EVENING
        return@withContext try {
            Log.d(TAG, "BCV fetch (${if (isEveningCheck) "evening" else "morning"})")
            val quotes = apiRepository.fetchBcvQuotes()
            if (quotes.isEmpty()) {
                Log.w(TAG, "BCV returned no quotes")
                return@withContext Result.retry()
            }

            val usdQuote = quotes.firstOrNull { it.fuente == "usd" }
            val eurQuote = quotes.firstOrNull { it.fuente == "eur" }

            quotes.forEach { QuotesCache.upsert(applicationContext, it) }
            RateWidgetProvider.updateAllWidgets(applicationContext)

            // Morning only: the rate of the day. The evening pass exists purely
            // to catch tomorrow's number, so re-announcing today's here would
            // be the duplicate this whole class exists to avoid.
            if (!isEveningCheck) {
                usdQuote?.let { usd ->
                    if (NotificationState.shouldNotifyDailyRate(
                            applicationContext,
                            usd.promedio,
                            usd.fechaActualizacion
                        )
                    ) {
                        NotificationHelper.showDailyRateNotification(
                            context = applicationContext,
                            usdRate = usd.promedio,
                            eurRate = eurQuote?.promedio
                        )
                    } else {
                        Log.d(TAG, "Official rate unchanged; skipping daily notification")
                    }
                }
            }

            // Both passes: the next day's rate, once, when it appears.
            usdQuote?.let { usd ->
                val quoteDate = runCatching { LocalDate.parse(usd.fechaActualizacion) }.getOrNull()
                if (quoteDate != null && quoteDate.isAfter(LocalDate.now())) {
                    if (NotificationState.shouldNotifyNextRate(
                            applicationContext,
                            usd.promedio,
                            usd.fechaActualizacion
                        )
                    ) {
                        NotificationHelper.showNextRateNotification(
                            context = applicationContext,
                            nextUsdRate = usd.promedio,
                            nextDate = usd.fechaActualizacion
                        )
                    } else {
                        Log.d(TAG, "Next rate already announced; skipping")
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "BCV fetch failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FetchQuotesWorker"
        const val KEY_CHECK = "check"
        const val CHECK_MORNING = "morning"
        const val CHECK_EVENING = "evening"
    }
}
