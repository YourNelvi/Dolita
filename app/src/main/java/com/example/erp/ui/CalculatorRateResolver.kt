package com.example.erp.ui

import com.example.erp.data.DolarQuote
import com.example.erp.data.RateSample
import java.text.NumberFormat
import java.util.Locale

/**
 * Which rate the calculator converts with.
 *
 * TODAY is the default and the only fallback: the screen can be left pointing
 * at a next-day or historical rate that is no longer available (a source switch,
 * a cleared lookup), and in that case the honest answer is today's rate, never a
 * blank calculator.
 */
enum class CalculatorRateSource { TODAY, NEXT_RATE, SELECTED_DATE }

/**
 * Resolves the quote the calculator converts with, as a pure function so the
 * fallback rules are testable without an Android Application.
 *
 * Every branch degrades to [today]: a selection whose underlying rate has gone
 * missing must not leave the calculator without a rate at all.
 */
object CalculatorRateResolver {
    fun resolve(
        today: DolarQuote?,
        future: DolarQuote?,
        futureSource: CalculatorRateSource,
        selectedSample: RateSample?,
        selectedLabel: String?,
        dateSource: CalculatorRateSource
    ): DolarQuote? {
        // The synthetic next-day quote borrows today's name and source, so it
        // needs today's quote to exist; without it there is nothing to build on.
        if (futureSource == CalculatorRateSource.NEXT_RATE && future != null && today != null) {
            return today.copy(
                nombre = "${today.nombre} (futura)",
                promedio = future.promedio,
                // The future quote's OWN anterior and variacion are the coherent
                // pair here: fetchBcv() maps both from the same payload as
                // promedio, so they describe the same day-to-day move. Inheriting
                // today's instead labelled the future rate with YESTERDAY's rate
                // as its "anterior" — and because the ViewModel builds today's
                // synthetic quote with variacion = null on purpose, it dropped the
                // variation entirely, so the future rate showed no change at all.
                anterior = future.anterior,
                variacion = future.variacion,
                fechaActualizacion = future.fechaActualizacion
            )
        }
        if (dateSource == CalculatorRateSource.SELECTED_DATE && selectedSample != null) {
            return DolarQuote(
                // Keep the currency the screen is showing: a USD sample must not
                // be renamed to whatever the previous tab was.
                fuente = today?.fuente ?: selectedSample.fuente,
                // The date goes in the name because the calculator renders that
                // string next to the amount — that is what tells the reader
                // which day they are actually converting at.
                nombre = "${selectedSample.nombre} (${selectedLabel ?: ""})",
                promedio = selectedSample.precio,
                anterior = today?.anterior,
                variacion = selectedSample.variacion,
                // The label is already formatted dd/MM/yyyy, which is exactly
                // what the calculator displays as the rate's date.
                fechaActualizacion = selectedLabel ?: today?.fechaActualizacion.orEmpty()
            )
        }
        return today
    }
}

// ============================================================================
// Rate change
// ============================================================================

/**
 * Which way a rate moved. [FLAT] covers both "it did not move" and "there is
 * nothing to move", because the reader-facing consequence is the same: the
 * neutral on-surface colour, not a green or a red.
 */
enum class RateChangeDirection { UP, DOWN, FLAT }

/**
 * How much a rate moved since its own previous value.
 *
 * Both halves are nullable and that is the whole point: a quote published
 * without a previous value has no delta to report, and inventing one — a
 * fabricated `+0,00 Bs` or a percentage derived from a missing base — is worse
 * than showing nothing. Each half is answered only from the field it needs, so
 * a quote that carries a percentage but no previous rate still reports the
 * percentage, and one that carries a previous rate but no published variation
 * still reports the money.
 */
data class RateChange(
    /** `promedio - anterior`, or null when the quote has no `anterior`. */
    val bolivaresDelta: Double?,
    /** The quote's own `variacion`, in percent, or null when it has none. */
    val percent: Double?,
    val direction: RateChangeDirection
)

/**
 * The change a quote reports, as a pure function so the arithmetic and the
 * direction are testable without a composition.
 *
 * Null only for a null quote: a quote that publishes nothing is a real answer
 * with both halves null and [RateChangeDirection.FLAT], and the caller omits
 * the line entirely rather than printing zeros for a rate it knows nothing about.
 *
 * The direction follows the percentage when there is one and falls back to the
 * money when there is not, so the colour never contradicts the number printed
 * next to it.
 */
fun rateChangeOf(quote: DolarQuote?): RateChange? {
    if (quote == null) return null
    val delta = quote.anterior?.let { quote.promedio - it }
    val percent = quote.variacion
    val direction = when {
        percent != null && percent > 0.0 -> RateChangeDirection.UP
        percent != null && percent < 0.0 -> RateChangeDirection.DOWN
        percent == null && delta != null && delta > 0.0 -> RateChangeDirection.UP
        percent == null && delta != null && delta < 0.0 -> RateChangeDirection.DOWN
        else -> RateChangeDirection.FLAT
    }
    return RateChange(bolivaresDelta = delta, percent = percent, direction = direction)
}

/** Two decimals is the convention for a delta: the third decimal is noise. */
private const val CHANGE_FRACTION_DIGITS = 2

private fun changeFormat(locale: Locale): NumberFormat =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = CHANGE_FRACTION_DIGITS
        maximumFractionDigits = CHANGE_FRACTION_DIGITS
    }

/**
 * The money half of a change, signed and unit-labelled: `+1,47 Bs`, `-0,88 Bs`.
 *
 * The sign is always explicit, including for a rise, so two numbers on the same
 * card can be compared by looking at the symbol rather than by remembering which
 * side of the baseline they sit on.
 */
fun formatBolivaresDelta(value: Double, locale: Locale = Locale.getDefault()): String {
    val sign = if (value >= 0.0) "+" else ""
    return "$sign${changeFormat(locale).format(value)} Bs"
}

/**
 * The percentage half of a change: `+0,17 %`. Kept as its own formatter so the
 * space before the unit stays the one the rest of the app uses.
 */
fun formatRatePercent(value: Double, locale: Locale = Locale.getDefault()): String {
    val sign = if (value >= 0.0) "+" else ""
    return "$sign${changeFormat(locale).format(value)} %"
}