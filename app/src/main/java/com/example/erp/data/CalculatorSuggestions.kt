package com.example.erp.data

import java.math.BigDecimal

/**
 * Quick amounts offered under the calculator's dollar field.
 *
 * The values are the sequence the product asked for and are expressed in the
 * foreign currency (dollars or USDT), never in bolivars: tapping one sets the
 * dollar amount and lets the calculator multiply by the current rate.
 *
 * Extracted from the composable so the sequence and its digit encoding are
 * unit-testable without a composition.
 */
object CalculatorSuggestions {

    val amounts: List<Int> = listOf(2, 5, 10, 20, 50)

    /**
     * [amount] in the calculator's field encoding: the last two characters are
     * the decimals, so 2 becomes "200" (2,00), exactly as if the reader had
     * typed it.
     */
    fun digits(amount: Int): String =
        CurrencyConverter.toDigits(
            CurrencyConverter.format(BigDecimal.valueOf(amount.toLong()))
        )
}
