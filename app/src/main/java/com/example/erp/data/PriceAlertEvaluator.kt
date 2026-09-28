package com.example.erp.data

import android.content.Context
import android.util.Log
import com.example.erp.notification.NotificationHelper

/**
 * Evaluates the user's price alert against a price a worker just observed.
 *
 * There is no scheduled work of its own: the workers already fetch on a
 * cadence, and re-reading a price that was just downloaded is free. The alert
 * only ever fires on a crossing, so a threshold that stays breached is one
 * notification and not an hourly siren.
 */
object PriceAlertEvaluator {

    private const val TAG = "PriceAlert"

    private val SOURCE_NAMES = mapOf(
        PriceAlert.Fuente.USD to "Dólar BCV",
        PriceAlert.Fuente.EUR to "Euro BCV",
        PriceAlert.Fuente.PARALELO to "Paralelo"
    )

    fun evaluate(context: Context, fuente: PriceAlert.Fuente, current: Double) {
        val store = PriceAlertStore(context)
        val config = store.load()
        if (!config.enabled) return
        if (config.fuente != fuente) return
        if (config.threshold <= 0.0) return

        val previous = store.lastObserved(fuente)
        val fired = PriceAlert.shouldFire(
            enabled = true,
            previous = previous,
            current = current,
            threshold = config.threshold,
            direction = config.direction
        )
        store.recordObserved(fuente, current)

        if (fired) {
            Log.d(TAG, "${fuente.id} crossed ${config.threshold} at $current")
            NotificationHelper.showPriceAlertNotification(
                context = context,
                sourceName = SOURCE_NAMES[fuente] ?: fuente.id,
                rate = current,
                direction = config.direction
            )
        }
    }
}
