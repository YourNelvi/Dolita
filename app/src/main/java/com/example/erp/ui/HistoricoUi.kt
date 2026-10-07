package com.example.erp.ui

import com.example.erp.data.HISTORICO_PUNTOS
import com.example.erp.data.RateSample
import com.example.erp.data.chartValues
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
 * The chart header, stated in the unit the reader actually cares about.
 *
 * BCV does not publish on weekends, so a Saturday and a Sunday are not days
 * without a rate — they are not days at all for this chart. Counting calendar
 * time therefore inflated a full chart of fifteen published days into "22 días",
 * which reads as missing data when nothing is missing. The meaningful unit is the
 * published day: one sample is one business day, so the header counts samples
 * and names them as such.
 *
 * The axis still spans more calendar dates than that count, because the weekends
 * in between are real elapsed time; that is not a contradiction once the header
 * says "días hábiles". Contradicting the axis means labelling fifteen points as
 * fifteen calendar days, which is what the hardcoded string used to do.
 *
 * Mirrors the chart's own `takeLast(maxPoints)` so the header and the drawn line
 * can never describe different ranges.
 */
fun historicoTitle(
    samples: List<RateSample>
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

    val shown = drawn.size
    val loading = if (shown < HISTORICO_PUNTOS) " · histórico en carga" else ""
    return if (shown == 1) {
        "Último día hábil$loading $HINT"
    } else {
        "Últimos $shown días hábiles$loading $HINT"
    }
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
            precio = "Bs ${priceFormat.format(sample.precio)}",
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