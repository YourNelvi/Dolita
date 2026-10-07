package com.example.erp.ui

import com.example.erp.data.DolarQuote
import com.example.erp.data.RateSample

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