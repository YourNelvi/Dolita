package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.erp.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchQuotesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = CachedDolarRepository(ApiDolarRepository(), applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("FetchQuotesWorker", "Starting scheduled fetch")
            val quotes = repository.getQuotes()
            if (quotes.isNotEmpty()) {
                Log.d("FetchQuotesWorker", "Fetch successful: ${quotes.size} quotes")

                // Show daily rate notification
                val usdQuote = quotes.firstOrNull { it.fuente == "usd" }
                val eurQuote = quotes.firstOrNull { it.fuente == "eur" }
                usdQuote?.let {
                    NotificationHelper.showDailyRateNotification(
                        context = applicationContext,
                        usdRate = it.promedio,
                        eurRate = eurQuote?.promedio
                    )
                }

                // Check for "next rate" (future quote) and notify
                val nextUsdQuote = quotes.firstOrNull { it.fuente == "usd" && it.fechaAnterior != null }
                nextUsdQuote?.let { quote ->
                    // If the API date is in the future, it's a "next rate"
                    val today = java.time.LocalDate.now()
                    val quoteDate = try {
                        java.time.LocalDate.parse(quote.fechaActualizacion)
                    } catch (e: Exception) { null }

                    if (quoteDate != null && quoteDate.isAfter(today)) {
                        NotificationHelper.showNextRateNotification(
                            context = applicationContext,
                            nextUsdRate = quote.promedio,
                            nextDate = quote.fechaActualizacion
                        )
                    }
                }

                Result.success()
            } else {
                Log.w("FetchQuotesWorker", "Fetch returned empty quotes")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("FetchQuotesWorker", "Fetch failed: ${e.message}")
            // Reintentar con backoff exponencial
            Result.retry()
        }
    }
}