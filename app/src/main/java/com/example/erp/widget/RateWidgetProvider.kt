package com.example.erp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.erp.MainActivity
import com.example.erp.R
import com.example.erp.data.ApiDolarRepository
import com.example.erp.data.CachedDolarRepository
import com.example.erp.data.QuotesCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class RateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, RateWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_rate)

            // Click opens the app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            // Paint from what is already stored. The widget used to fetch on
            // every update — once per placed instance, so three widgets on a
            // home screen meant three requests every time the launcher decided
            // to refresh, to redraw a rate that publishes once a day. The
            // scheduled workers keep the cache current and push an update when
            // they do; the fetch below is only the cold-start fallback.
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val cached = QuotesCache.getCached(context.applicationContext)
                    val quotes = cached?.quotes?.takeIf { it.isNotEmpty() }
                        ?: runCatching {
                            CachedDolarRepository(
                                ApiDolarRepository(),
                                context.applicationContext
                            ).getQuotes()
                        }.getOrDefault(emptyList())

                    val fmt = NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
                        minimumFractionDigits = 2
                        maximumFractionDigits = 2
                    }

                    val usd = quotes.firstOrNull { it.fuente == "usd" }
                    val eur = quotes.firstOrNull { it.fuente == "eur" }
                    val usdt = quotes.firstOrNull { it.fuente == "usdt" }

                    views.setTextViewText(R.id.widget_usd,
                        usd?.let { "$${fmt.format(it.promedio)}" } ?: "--")
                    views.setTextViewText(R.id.widget_eur,
                        eur?.let { "€${fmt.format(it.promedio)}" } ?: "--")
                    views.setTextViewText(R.id.widget_usdt,
                        usdt?.let { "$${fmt.format(it.promedio)}" } ?: "--")

                    val now = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    views.setTextViewText(R.id.widget_updated, "Act. $now")

                    manager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_usd, "Error")
                    views.setTextViewText(R.id.widget_eur, "")
                    views.setTextViewText(R.id.widget_usdt, "")
                    views.setTextViewText(R.id.widget_updated, "Sin conexion")
                    manager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}