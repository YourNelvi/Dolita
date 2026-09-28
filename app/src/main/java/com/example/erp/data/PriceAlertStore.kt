package com.example.erp.data

import android.content.Context

/**
 * Persisted state of the user's price alert.
 *
 * The alert survives a reboot, and so does the last observed price: a crossing is
 * a comparison between two observations, so dropping the previous value would
 * make the alert fire (or stay silent) for reasons the user cannot explain.
 */
class PriceAlertStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Config(
        val enabled: Boolean = false,
        val fuente: PriceAlert.Fuente = PriceAlert.Fuente.PARALELO,
        val threshold: Double = 0.0,
        val direction: PriceAlert.Direccion = PriceAlert.Direccion.ARRIBA
    )

    fun load(): Config = Config(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        fuente = PriceAlert.Fuente.fromId(prefs.getString(KEY_FUENTE, null) ?: "usdt"),
        threshold = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_THRESHOLD, 0L)),
        direction = if (prefs.getString(KEY_DIRECTION, null) == "down") {
            PriceAlert.Direccion.ABAJO
        } else {
            PriceAlert.Direccion.ARRIBA
        }
    )

    fun save(config: Config) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_FUENTE, config.fuente.id)
            .putLong(KEY_THRESHOLD, java.lang.Double.doubleToRawLongBits(config.threshold))
            .putString(
                KEY_DIRECTION,
                if (config.direction == PriceAlert.Direccion.ABAJO) "down" else "up"
            )
            .apply()
    }

    /** Last price seen for [fuente], or null if this alert has never been evaluated. */
    fun lastObserved(fuente: PriceAlert.Fuente): Double? {
        if (!prefs.contains(lastKey(fuente))) return null
        return java.lang.Double.longBitsToDouble(prefs.getLong(lastKey(fuente), 0L))
    }

    fun recordObserved(fuente: PriceAlert.Fuente, value: Double) {
        prefs.edit()
            .putLong(lastKey(fuente), java.lang.Double.doubleToRawLongBits(value))
            .apply()
    }

    private fun lastKey(fuente: PriceAlert.Fuente) = "$KEY_LAST_PREFIX${fuente.id}"

    private companion object {
        const val PREFS = "price_alert"
        const val KEY_ENABLED = "enabled"
        const val KEY_FUENTE = "fuente"
        const val KEY_THRESHOLD = "threshold"
        const val KEY_DIRECTION = "direction"
        const val KEY_LAST_PREFIX = "last_"
    }
}
