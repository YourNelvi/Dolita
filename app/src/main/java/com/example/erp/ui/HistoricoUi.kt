package com.example.erp.ui

import com.example.erp.data.HISTORICO_PUNTOS
import com.example.erp.data.RateSample
import com.example.erp.data.chartValues
import com.example.erp.data.localDateOf
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Hourly samples the parallel chart shows before it counts as full. The parallel
 * series is only ever sampled live — nothing in the app fetches a historical P2P
 * series — so after a fresh install it is legitimately short and stays that way
 * until it has accumulated.
 */
const val HISTORICO_MUESTRAS_HORARIAS = 48

private const val HINT = "(toca un punto para ver precio)"

/**
 * The chart header, stating what the axis actually spans.
 *
 * `maxPoints` counts SAMPLES, but the axis spans CALENDAR time, and BCV does not
 * publish on weekends — so a full chart reads "Últimos 22 días", not 15. The
 * header has to name the span the reader is looking at; a label that echoed the
 * point count would contradict the dates printed directly beneath it.
 *
 * This function mirrors the chart's own `takeLast(maxPoints)` so the header and
 * the drawn line can never describe different ranges.
 */
fun historicoTitle(
    samples: List<RateSample>,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (samples.isEmpty()) return "Histórico $HINT"

    val isHourly = samples.any { it.fuente == "usdt" }
    val maxPoints = if (isHourly) HISTORICO_MUESTRAS_HORARIAS else HISTORICO_PUNTOS
    val drawn = samples.sortedBy { it.timestampEpochMillis }.takeLast(maxPoints)

    if (isHourly) {
        return if (drawn.size >= HISTORICO_MUESTRAS_HORARIAS) {
            "Últimas $HISTORICO_MUESTRAS_HORARIAS muestras horarias $HINT"
        } else {
            "Muestreo horario: ${drawn.size} de $HISTORICO_MUESTRAS_HORARIAS $HINT"
        }
    }

    val days = drawn.map { localDateOf(it.timestampEpochMillis, zoneId) }
    val span = ChronoUnit.DAYS.between(days.min(), days.max()) + 1
    val unit = if (span == 1L) "día" else "días"
    val loading = if (drawn.size < HISTORICO_PUNTOS) " · histórico en carga" else ""
    return "Últimos $span $unit$loading $HINT"
}

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
    data class ConDatos(
        val chart: List<Double>,
        val samples: List<RateSample>,
        val rows: List<HistoricoRow>
    ) : HistoricoState
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
    else HistoricoState.ConDatos(
        chart = chartValues(samples),
        samples = samples.sortedBy { it.timestampEpochMillis },
        rows = historicoRows(samples, zoneId, locale)
    )