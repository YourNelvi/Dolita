package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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