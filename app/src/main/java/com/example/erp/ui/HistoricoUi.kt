package com.example.erp.ui

import com.example.erp.data.RateSample
import com.example.erp.data.chartValues
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Display row for the historical table: Fecha | Fuente | Precio | Variación. */
data class HistoricoRow(
    val fecha: String,
    val fuente: String,
    val precio: String,
    val variacion: String
)

/** What the Histórico section must render for the current-year samples. */
sealed interface HistoricoState {
    data object SinDatos : HistoricoState
    data class ConDatos(val chart: List<Double>, val rows: List<HistoricoRow>) : HistoricoState
}

/** "—" when there is no variation; otherwise a signed percent, e.g. "+0,10%" / "-0,05%". */
fun formatVariacion(variacion: Double?, locale: Locale = Locale.getDefault()): String {
    if (variacion == null) return "—"
    val sign = if (variacion >= 0) "+" else ""
    return "$sign${String.format(locale, "%.2f", variacion)}%"
}

/** Maps samples to table rows sorted by timestamp, formatted in the given zone/locale. */
fun historicoRows(
    samples: List<RateSample>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): List<HistoricoRow> {
    val priceFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val dayFormat = DateTimeFormatter.ofPattern("dd/MM").withZone(zoneId)
    return samples.sortedBy { it.timestampEpochMillis }.map { sample ->
        HistoricoRow(
            fecha = dayFormat.format(Instant.ofEpochMilli(sample.timestampEpochMillis)),
            fuente = sample.nombre,
            precio = "$${priceFormat.format(sample.precio)}",
            variacion = formatVariacion(sample.variacion, locale)
        )
    }
}

/**
 * Derives the Histórico section state: no samples -> "Sin datos" (never a
 * generated series); otherwise the evolution chart values plus table rows.
 */
fun historicoState(
    samples: List<RateSample>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): HistoricoState =
    if (samples.isEmpty()) HistoricoState.SinDatos
    else HistoricoState.ConDatos(chart = chartValues(samples), rows = historicoRows(samples, zoneId, locale))