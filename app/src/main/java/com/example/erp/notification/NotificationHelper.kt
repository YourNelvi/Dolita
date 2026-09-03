package com.example.erp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.erp.MainActivity
import com.example.erp.R
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID_DAILY = "dolar_daily_rate"
    private const val CHANNEL_ID_NEXT = "dolar_next_rate"
    private const val NOTIFICATION_ID_DAILY = 1001
    private const val NOTIFICATION_ID_NEXT = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val dailyChannel = NotificationChannel(
                CHANNEL_ID_DAILY,
                "Tasa diaria del dólar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificación diaria con la tasa del dólar BCV"
            }

            val nextChannel = NotificationChannel(
                CHANNEL_ID_NEXT,
                "Próxima tasa",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso cuando se publica la próxima tasa del dólar"
            }

            manager.createNotificationChannel(dailyChannel)
            manager.createNotificationChannel(nextChannel)
        }
    }

    fun showDailyRateNotification(context: Context, usdRate: Double, eurRate: Double?) {
        createChannels(context)

        val fmt = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val title = "Dólar BCV hoy"
        val body = buildString {
            append("USD: ${fmt.format(usdRate)} Bs")
            eurRate?.let { append(" | EUR: ${fmt.format(it)} Bs") }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_DAILY, notification)
    }

    fun showNextRateNotification(context: Context, nextUsdRate: Double, nextDate: String) {
        createChannels(context)

        val fmt = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val title = "Próxima tasa publicada"
        val body = "Dólar $nextDate: ${fmt.format(nextUsdRate)} Bs"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_NEXT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_NEXT, notification)
    }
}
