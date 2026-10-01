package com.example.erp.ui

import com.example.erp.data.HISTORICO_DIAS
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
 * The chart header, stated from the samples that actually exist.
 *
 * A hardcoded window is a promise the data cannot keep. Right after a fresh
 * install the parallel series holds one sample and grows only from live
 * sampling, while the BCV series backfills from the API — yet both headers used
 * to advertise a full window. That is how a chart teaches its reader to distrust
 * it.
 *
 * The BCV span is measured in calendar days from oldest to newest, not as a
 * count of stored samples: BCV does not publish on weekends, so counting samples
 * would report fewer days than the axis actually draws.
 */
fun historicoTitle(
    samples: List<RateSample>,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (samples.isEmpty()) return "Histórico $HINT"

    // USDT is sampled hourly and the BCV sources daily; labelling an hourly
    // series in days would understate how much of it there is.
    if (samples.any { it.fuente == "usdt" }) {
        val total = samples.size
        return if (total >= HISTORICO_MUESTRAS_HORARIAS) {
            "Últimas $HISTORICO_MUESTRAS_HORARIAS muestras horarias $HINT"
        } else {
            "Muestreo horario: $total de $HISTORICO_MUESTRAS_HORARIAS $HINT"
        }
    }

    val days = samples.map { localDateOf(it.timestampEpochMillis, zoneId) }
    val span = ChronoUnit.DAYS.between(days.min(), days.max()) + 1
    val unit = if (span == 1L) "día" else "días"
    return if (span >= HISTORICO_DIAS) {
        "Últimos $HISTORICO_DIAS $unit $HINT"
    } else {
        "Últimos $span $unit · histórico en carga $HINT"
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