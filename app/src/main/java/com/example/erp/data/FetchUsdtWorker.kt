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
 * Hourly worker that fetches Paralelo rate and samples it.
 * P2P rates change frequently, so we sample once per hour.
 */
class FetchUsdtWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = CachedDolarRepository(ApiDolarRepository(), applicationContext)
    private val historyStore: RateHistoryStore = FileHistoryStore(
        dir = context.filesDir.resolve("rate_history"),
        zoneId = java.time.ZoneId.systemDefault()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("FetchUsdtWorker", "Starting hourly Paralelo fetch")
            val quotes = repository.getQuotes()
            val parQuote = quotes.firstOrNull { it.fuente == "usdt" }
            if (parQuote != null) {
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
                // Update home screen widget
                RateWidgetProvider.updateAllWidgets(applicationContext)

                // Show Paralelo hourly notification
                NotificationHelper.showUsdtNotification(
                    context = applicationContext,
                    usdtRate = parQuote.promedio
                )

                Result.success()
            } else {
                Log.w("FetchUsdtWorker", "Paralelo quote not available")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("FetchUsdtWorker", "Paralelo fetch failed: ${e.message}")
            Result.retry()
        }
    }
}