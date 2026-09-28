package com.example.erp.data

import java.time.ZoneId

/**
 * "Avisame cuando el paralelo pase de 900."
 *
 * The rule is deliberately one-shot per crossing rather than "notify while the
 * condition holds": a threshold that stays breached must not turn into an
 * hourly siren, and the alert has to re-arm once the price comes back and
 * crosses again.
 */
object PriceAlert {

    enum class Fuente(val id: String) {
        USD("usd"),
        EUR("eur"),
        PARALELO("usdt");

        companion object {
            fun fromId(id: String): Fuente = entries.firstOrNull { it.id == id } ?: PARALELO
        }
    }

    enum class Direccion { ARRIBA, ABAJO }

    /**
     * Whether this sample should fire the alert, given whether the alert is
     * currently armed and whether the previous observed value was already on
     * the triggering side.
     */
    fun shouldFire(
        enabled: Boolean,
        previous: Double?,
        current: Double,
        threshold: Double,
        direction: Direccion
    ): Boolean {
        if (!enabled) return false
        // A stale or missing previous value cannot prove a crossing, so the
        // first observation only arms the alert.
        if (previous == null || previous <= 0.0) return false

        val wasBeyond = when (direction) {
            Direccion.ARRIBA -> previous >= threshold
            Direccion.ABAJO -> previous <= threshold
        }
        val isBeyond = when (direction) {
            Direccion.ARRIBA -> current >= threshold
            Direccion.ABAJO -> current <= threshold
        }
        // Fire only on the edge: not already beyond, now beyond.
        return !wasBeyond && isBeyond
    }

    /**
     * Whether the alert should be re-armed: the price came back to the other
     * side of the threshold, so a future crossing is news again.
     */
    fun shouldRearm(
        current: Double,
        threshold: Double,
        direction: Direccion
    ): Boolean = when (direction) {
        Direccion.ARRIBA -> current < threshold
        Direccion.ABAJO -> current > threshold
    }

    /**
     * Reads a stored sample for [fuente] in the most recent [windowMillis].
     * Pure and time-injected so the lookback is testable.
     */
    fun mostRecent(
        samples: List<RateSample>,
        fuente: Fuente,
        nowMillis: Long,
        windowMillis: Long
    ): RateSample? = samples
        .filter { it.fuente == fuente.id }
        .filter { nowMillis - it.timestampEpochMillis in 0..windowMillis }
        .maxByOrNull { it.timestampEpochMillis }

    fun todayInMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        java.time.LocalDate.now(zoneId)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
}
