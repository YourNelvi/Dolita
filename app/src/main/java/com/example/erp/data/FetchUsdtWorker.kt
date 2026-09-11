package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hourly worker that fetches USDT (P2P Binance) rate and samples it.
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
            Log.d("FetchUsdtWorker", "Starting hourly USDT fetch")
            val quotes = repository.getQuotes()
            val usdtQuote = quotes.firstOrNull { it.fuente == "usdt" }
            if (usdtQuote != null) {
                Log.d("FetchUsdtWorker", "USDT fetched: ${usdtQuote.promedio}")
                // Sample and persist
                historyStore.append(
                    listOf(
                        RateSample(
                            fuente = "usdt",
                            nombre = "USDT (P2P)",
                            precio = usdtQuote.promedio,
                            timestampEpochMillis = System.currentTimeMillis()
                        )
                    )
                )
                Result.success()
            } else {
                Log.w("FetchUsdtWorker", "USDT quote not available")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("FetchUsdtWorker", "USDT fetch failed: ${e.message}")
            Result.retry()
        }
    }
}
