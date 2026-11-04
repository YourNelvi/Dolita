package com.example.erp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.erp.MainActivity
import com.example.erp.R
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    /**
     * Extra that tells [MainActivity] which rate to open the app on.
     *
     * Public because the activity reads it; the values are the same source ids
     * the quotes carry ("usd", "eur", "usdt"), so there is no second vocabulary
     * to keep in sync.
     */
    const val EXTRA_RATE_SOURCE = "com.example.erp.extra.RATE_SOURCE"

    // A channel's sound is frozen by the system once the channel exists, so a
    // plain code change never reaches an installed app. The `_v2` suffix makes
    // Android treat these as new channels and take the custom sound; the old
    // ids are deleted below so the settings screen keeps one entry per topic.
    private const val CHANNEL_ID_DAILY = "dolar_daily_rate_v2"
    private const val CHANNEL_ID_NEXT = "dolar_next_rate_v2"
    private const val CHANNEL_ID_USDT = "dolar_usdt_hourly_v2"

    private const val NOTIFICATION_ID_DAILY = 1001
    private const val NOTIFICATION_ID_NEXT = 1002
    private const val NOTIFICATION_ID_USDT = 1003
    private const val CHANNEL_ID_ALERT = "dolar_price_alert_v2"
    private const val NOTIFICATION_ID_ALERT = 1004
    private const val NOTIFICATION_ID_EURO = 1005

    private val LEGACY_CHANNEL_IDS = listOf(
        "dolar_daily_rate",
        "dolar_next_rate",
        "dolar_usdt_hourly",
        "dolar_price_alert",
    )

    /**
     * The bundled alert tone, shipped in `res/raw` so it is always available
     * offline and survives the notification service reading it out of process.
     */
    private fun soundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.dolita_notification}")

    private fun soundAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    /**
     * Opens the app on the rate the notification is talking about.
     *
     * The request code is per source on purpose: a PendingIntent's identity
     * ignores extras, so one shared code would make every notification open the
     * same tab — whichever extras were written last.
     */
    private fun openRate(context: Context, source: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_RATE_SOURCE, source)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val sound = soundUri(context)
            val audioAttributes = soundAttributes()

            val dailyChannel = NotificationChannel(
                CHANNEL_ID_DAILY,
                "Tasa diaria del dólar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificación diaria con la tasa del dólar BCV"
                setSound(sound, audioAttributes)
            }

            val nextChannel = NotificationChannel(
                CHANNEL_ID_NEXT,
                "Próxima tasa",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso cuando se publica la próxima tasa del dólar"
                setSound(sound, audioAttributes)
            }

            // IMPORTANCE_LOW makes no sound at all, so the hourly USDT channel
            // has to be at least DEFAULT for the custom tone to play.
            val usdtChannel = NotificationChannel(
                CHANNEL_ID_USDT,
                "USDT cada hora",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificación horaria con el precio del USDT"
                setSound(sound, audioAttributes)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                "Alerta de precio",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso cuando una tasa cruza el valor que elegiste"
                setSound(sound, audioAttributes)
            }

            // Delete the pre-sound channels first: their sound is immutable and
            // leaving them behind would show duplicate entries in the settings.
            LEGACY_CHANNEL_IDS.forEach(manager::deleteNotificationChannel)

            manager.createNotificationChannel(dailyChannel)
            manager.createNotificationChannel(nextChannel)
            manager.createNotificationChannel(usdtChannel)
            manager.createNotificationChannel(alertChannel)
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

        val pendingIntent = openRate(context, "usd", 0)

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

    /**
     * The euro moves on its own: the daily notice is gated on the dollar, so a
     * euro that changed while the dollar did not would go unannounced. This one
     * has its own gate and its own notification id.
     */
    fun showEuroRateNotification(context: Context, eurRate: Double, date: String) {
        createChannels(context)

        val fmt = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val title = "Euro BCV hoy"
        val body = "EUR: ${fmt.format(eurRate)} Bs ($date)"

        val pendingIntent = openRate(context, "eur", 1)

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
        manager.notify(NOTIFICATION_ID_EURO, notification)
    }

    fun showNextRateNotification(context: Context, nextUsdRate: Double, nextDate: String) {
        createChannels(context)

        val fmt = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val title = "Próxima tasa publicada"
        val body = "Dólar $nextDate: ${fmt.format(nextUsdRate)} Bs"

        val pendingIntent = openRate(context, "usd", 3)

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

    fun showUsdtNotification(context: Context, usdtRate: Double) {
        createChannels(context)

        val fmt = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val title = "USDT (P2P)"
        val body = "Promedio: $${fmt.format(usdtRate)} Bs"

        val pendingIntent = openRate(context, "usdt", 2)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_USDT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_USDT, notification)
    }

    /**
     * The user chose this threshold, so the wording states the fact and the
     * direction rather than selling anything: "pasó 900" or "bajó de 900".
     */
    fun showPriceAlertNotification(
        context: Context,
        sourceName: String,
        rateSource: String,
        rate: Double,
        direction: com.example.erp.data.PriceAlert.Direccion
    ) {
        createChannels(context)

        val fmt = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val body = if (direction == com.example.erp.data.PriceAlert.Direccion.ARRIBA) {
            "$sourceName pasó $${fmt.format(rate)} Bs"
        } else {
            "$sourceName bajó de $${fmt.format(rate)} Bs"
        }

        val pendingIntent = openRate(context, rateSource, 4)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alerta de precio")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_ALERT, notification)
    }
}
