package com.example.erp.ui

import com.example.erp.data.CurrencyConverter
import java.math.BigDecimal
import java.util.Locale

/**
 * The differential gap line, as a pure function so the wording is assertable
 * without a composition.
 *
 * The line reports one gap twice: as a percentage and as money. Those two halves
 * are denominated differently and the reader needs to be told which is which.
 * The percentage is a RATIO — the same one on every tab — while the Bs figure is
 * per unit of the open tab's currency, so the euro tab's gap is necessarily the
 * bigger number under the same percentage. The euro parallel is derived from the
 * dollar parallel (`(eurRate / usdRate) * usdtRate`), which makes that identical
 * percentage an algebraic fact rather than a coincidence, and it is exactly why an
 * unnamed Bs figure reads as a wrong calculation: Bs 126,77 next to Bs 141,89
 * under one percentage, with nothing saying the units differ.
 *
 * So the unit is named, and it is named from [fuente] — the open tab's currency —
 * rather than from the side the rate happens to sit on. On the parallel tab the
 * selected rate IS the parallel, so the official side of the gap is the official
 * DOLLAR rate: both sides are per dollar there, which is why that tab says "por
 * dólar" and never "por dólar paralelo". The euro keeps its own unit.
 *
 * @param locale formats the percentage only; the money half keeps the app-wide
 *   "es-VE" grouping the rest of the card uses, so switching locale does not
 *   split one line into two number formats.
 */
fun differentialGapLabel(
    percent: Double,
    perUnitBs: Double,
    parallelAbove: Boolean,
    fuente: String,
    locale: Locale = Locale.getDefault()
): String {
    val sign = if (parallelAbove) "+" else ""
    val direction = if (parallelAbove) "más" else "menos"
    val amount = CurrencyConverter.format(BigDecimal.valueOf(perUnitBs))
    return "$sign${String.format(locale, "%.2f", percent)}%  ·  Bs $amount $direction ${gapUnitName(fuente)}"
}

/**
 * What the Bs figure is quoted per, in the words the sentence needs: the amount,
 * then the direction ("más por dólar"), then the unit the amount belongs to.
 *
 * Reading it the other way round — "Bs 126,77 por dólar más" — is a different
 * claim: it describes a per-dollar rate that is somehow "more" without naming
 * what it is more than. With the direction against the number and the unit last,
 * each word can only attach to the one thing it can modify.
 */
private fun gapUnitName(fuente: String): String = when (fuente) {
    "eur" -> "por euro"
    // Anything that is not the euro is quoted in dollars, so an unrecognised
    // source gets the dollar rather than an unnamed or invented unit.
    else -> "por dólar"
}