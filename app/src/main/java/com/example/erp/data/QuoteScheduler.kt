package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object QuoteScheduler {
    private const val DAILY_WORK_NAME = "daily_quotes_fetch"
    private const val USDT_WORK_NAME = "hourly_usdt_fetch"
    private const val FETCH_HOUR = 8 // 8 AM
    private const val FLEX_WINDOW_HOURS = 1L // Ventana de 1 hora (8-9 AM)

    fun scheduleDailyFetch(context: Context) {
        val workRequest = PeriodicWorkRequest.Builder(
            FetchQuotesWorker::class.java,
            24L, TimeUnit.HOURS, // Repetir cada 24 horas
            FLEX_WINDOW_HOURS, TimeUnit.HOURS // Ventana flexible
        )
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .addTag(DAILY_WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                DAILY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        Log.d("QuoteScheduler", "Scheduled daily fetch for ~$FETCH_HOUR:00 AM")
    }

    fun scheduleHourlyUsdtFetch(context: Context) {
        val workRequest = PeriodicWorkRequest.Builder(
            FetchUsdtWorker::class.java,
            1L, TimeUnit.HOURS, // Every hour
            15L, TimeUnit.MINUTES // Flex window of 15 min
        )
            .addTag(USDT_WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                USDT_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        Log.d("QuoteScheduler", "Scheduled hourly USDT fetch")
    }

    fun cancelScheduledFetch(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(USDT_WORK_NAME)
        Log.d("QuoteScheduler", "Cancelled all scheduled fetches")
    }

    private fun calculateInitialDelay(): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, FETCH_HOUR)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // Si ya pasó la hora hoy, programar para mañana
        if (now >= calendar.timeInMillis) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        return calendar.timeInMillis - now
    }
}