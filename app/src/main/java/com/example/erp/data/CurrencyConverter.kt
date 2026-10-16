package com.example.erp.data

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * The cambiario differential, quoted the way the market quotes it: the parallel
 * measured against the OFFICIAL rate.
 *
 * Both sides have to agree on the number. Measuring over whichever rate the open
 * tab happened to hold made one fact read two ways on one screen — +13,65% on
 * the official tab and −12,01% on the parallel tab, for a gap of Bs 119,08 per
 * dollar that both tabs already reported identically. Naming the reference side
 * is what makes the figure a fact instead of a function of where you are.
 */
data class DifferentialGap(
    val percent: Double,
    val perUnit: Double,
    val parallelAbove: Boolean
)

/** Null when either rate is unusable — an undrawn gap beats a fabricated one. */
fun differentialGap(parallelRate: Double, officialRate: Double): DifferentialGap? {
    if (parallelRate <= 0.0 || officialRate <= 0.0) return null
    val gap = parallelRate - officialRate
    return DifferentialGap(
        percent = (gap / officialRate) * 100.0,
        perUnit = abs(gap),
        parallelAbove = gap > 0
    )
}

/**
 * What the parallel tab owes its reader once an amount is entered: what that
 * amount is worth in OFFICIAL dollars, and what the differential cost in
 * bolivars. A percentage says the two rates are 11% apart; it does not say how
 * many dollars or how many bolivars, which is the question actually asked.
 */
data class DifferentialCambiario(
    val enteredAmount: BigDecimal,
    val amountInBs: BigDecimal,
    val officialDollars: BigDecimal,
    val costExtraBs: BigDecimal
) {
    /** True when the parallel rate charges MORE than the official one. */
    val overpaying: Boolean get() = costExtraBs.signum() > 0
}

/**
 * Conversion math behind the calculator card.
 *
 * Both amount fields hold a plain digit string instead of a formatted number
 * so the soft keyboard stays numeric ("1234" means 1234,00), and this object is
 * the single place that maps between that encoding and a VES/divisa amount.
 * Extracted from `CalculatorCard` so the money math is unit-testable without a
 * composition.
 */
object CurrencyConverter {

    /** The digit string is capped at ten: eight integer digits plus two decimals. */
    const val MAX_DIGITS = 10

    /**
     * Keeps a typed digit string usable.
     *
     * The field holds a digit string whose LAST TWO characters are the decimals,
     * so a leading zero is a trap rather than a digit: "0" already reads as
     * 0,00 because [parseDigits] left-pads to three characters, and every extra
     * zero typed after it is accepted, grows the field and changes nothing. Ten
     * presses of the zero key leave a dead string behind with no way to tell it
     * apart from real input.
     *
     * So leading zeros are dropped. They never change the amount — "0500000"
     * and "5000000" are the same 50.000,00 — and leaving them in shows the
     * reader a number they did not type. A run that is all zeros collapses to a
     * single zero so the key never occupies space, and an empty result stays
     * empty rather than inventing a digit.
     */
    fun normalizeDigits(digits: String): String {
        val capped = digits.filter { it.isDigit() }.take(MAX_DIGITS)
        if (capped.isEmpty()) return ""
        val stripped = capped.trimStart('0')
        return if (stripped.isEmpty()) "0" else stripped
    }

    /**
     * Reads the calculator's digit encoding: the last two digits are the
     * decimals, the rest is the integer part, and the string is left-padded to
     * at least three digits ("3" -> 0,03, "300" -> 3,00).
     *
     * Returns null only for an empty string. Anything that is not a decimal
     * number is rejected by [BigDecimal] with a `NumberFormatException`
     * instead of being sanitised, so callers must keep the field digit-only.
     */
    fun parseDigits(digits: String): BigDecimal? {
        if (digits.isEmpty()) return null
        val padded = digits.padStart(3, '0')
        val integerPart = padded.substring(0, padded.length - 2)
        val decimalPart = padded.substring(padded.length - 2)
        return BigDecimal("$integerPart.$decimalPart")
    }

    /**
     * Groups [value] with thousands dots for the "es-VE" copy the card shows,
     * e.g. `1234.567` -> "1.234,57". Always two decimals, rounded half up.
     */
    fun format(value: BigDecimal): String {
        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        val plain = rounded.toPlainString().replace('.', ',')
        val parts = plain.split(',')
        val withThousands = parts[0].reversed().chunked(3).joinToString(".").reversed()
        return if (parts.size > 1) "$withThousands,${parts[1]}" else "$withThousands,00"
    }

    /**
     * Inverse of [format] for the digit encoding: drops the thousands dots and
     * merges the two decimals back in. The fraction is truncated, not rounded,
     * and a single leading zero is dropped ("0,50" -> "50", "0,00" -> "00").
     */
    fun toDigits(formatted: String): String {
        val parts = formatted.split(',')
        val integerPart = parts[0].replace(".", "")
        val decimalPart = if (parts.size > 1) parts[1] else "00"
        return (integerPart + decimalPart.padEnd(2, '0').take(2)).removePrefix("0").takeIf { it.isNotEmpty() } ?: "0"
    }

    /**
     * [vesDigits] expressed in the divisa, ready to be shown by the second
     * field. Rounds to two decimals, so an amount under half a cent of the
     * divisa collapses to zero. Empty when the field is empty or the rate is
     * unusable.
     */
    fun vesToDiv(vesDigits: String, rate: Double): String {
        val v = parseDigits(vesDigits)
        if (v == null || rate <= 0.0) {
            return ""
        }
        val rateBD = BigDecimal.valueOf(rate)
        val result = v.divide(rateBD, 10, RoundingMode.HALF_UP)
        return toDigits(format(result))
    }

    /**
     * [divDigits] expressed in VES, ready to be shown by the first field.
     * Empty when the field is empty or the rate is unusable.
     */
    fun divToVes(divDigits: String, rate: Double): String {
        val v = parseDigits(divDigits)
        if (v == null || rate <= 0.0) {
            return ""
        }
        val rateBD = BigDecimal.valueOf(rate)
        val result = v.multiply(rateBD)
        return toDigits(format(result))
    }

/**
     * The cambiario differential for [entered], where [rate] is the parallel
     * rate in Bs per USDT and [usdRate] the official BCV rate in Bs per dollar.
     *
     * Both figures derive from the amount in BOLIVARS, never from the field the
     * reader typed in, so an equivalent amount reports the same numbers either
     * way: 100 USDT and the 95.406 Bs those USDT are worth agree. Deriving them
     * from the field instead is how the same purchase reports two different
     * numbers depending on which box had focus.     *
     * Null when the amount is not positive or a rate is unusable - an
     * unrendered differential beats a fabricated one.
     */
    fun differentialCambiario(
        entered: BigDecimal,
        rate: Double,
        usdRate: Double,
        editingBolivares: Boolean
    ): DifferentialCambiario? {
        if (entered <= BigDecimal.ZERO || rate <= 0.0 || usdRate <= 0.0) return null
        val rateBD = BigDecimal.valueOf(rate)
        val usdBD = BigDecimal.valueOf(usdRate)
        val amountInBs = if (editingBolivares) {
            entered
        } else {
            entered.multiply(rateBD)
        }
        val parallelDollars = amountInBs.divide(rateBD, 6, RoundingMode.HALF_UP)
        val officialDollars = amountInBs.divide(usdBD, 2, RoundingMode.HALF_UP)
        // What those same parallel dollars would have cost at the official rate.
        val costExtraBs = amountInBs.subtract(parallelDollars.multiply(usdBD))
        return DifferentialCambiario(entered, amountInBs, officialDollars, costExtraBs)
    }

    /**
     * What the differential actually costs, said in every currency at once.
     *
     * The case that asked for it: someone is standing on the parallel tab with
     * an amount in bolivares and wants to know what that purchase overpaid —
     * not as a percentage, but as money. A percentage says the two rates are 15%
     * apart; it does not say how many dollars, how many euros or how many
     * bolivars, and those are the numbers a reader actually needs.
     *
     * One figure, four readings. The amount in Bs is the cost; the rest are that
     * same cost divided by each currency's own rate, which is what makes them
     * comparable at a glance instead of forcing the reader to divide by hand.
     *
     * Every rate is Bs-per-unit and [usdtRate] is what creates the gap in the
     * first place, so on the dollar side the USD and USDT readings coincide.
     * That is arithmetic, not a duplicate row: they are the same money under two
     * names, and the reader is better served by one line than by two that
     * always agree.
     *
     * Null when the amount is not positive or any rate is unusable.
     */
    fun differentialEnMonedas(
        amountInBs: BigDecimal,
        usdtRate: Double,
        usdRate: Double,
        eurRate: Double?
    ): DifferentialEnMonedas? {
        if (amountInBs <= BigDecimal.ZERO) return null
        if (usdtRate <= 0.0 || usdRate <= 0.0) return null
        val usdt = BigDecimal.valueOf(usdtRate)
        val usd = BigDecimal.valueOf(usdRate)

        // What those parallel dollars would have cost at the official rate; the
        // difference is what the purchase overpaid in bolivares.
        //
        // Rounded to cents BEFORE the zero test, because the intermediate
        // division carries six decimals and leaves residue: with identical
        // rates, 50000/875,65 comes back as 57,096212 and multiplied by 875,65
        // again gives 49.999,99987 rather than 50.000. Un-rounded, a purchase
        // that overpaid nothing reported a fraction of a bolivar as a cost.
        val parallelDollars = amountInBs.divide(usdt, 6, RoundingMode.HALF_UP)
        val extraBs = amountInBs
            .subtract(parallelDollars.multiply(usd))
            .setScale(2, RoundingMode.HALF_UP)
        if (extraBs.signum() == 0) return null

        val extraUsdt = extraBs.divide(usdt, 2, RoundingMode.HALF_UP)
        val extraUsd = extraBs.divide(usd, 2, RoundingMode.HALF_UP)
        val extraEur = eurRate
            ?.takeIf { it > 0.0 }
            ?.let { extraBs.divide(BigDecimal.valueOf(it), 2, RoundingMode.HALF_UP) }

        return DifferentialEnMonedas(
            extraBs = extraBs,
            extraUsdt = extraUsdt,
            extraUsd = extraUsd,
            extraEur = extraEur
        )
    }
}

/** The overpayment of one purchase, expressed in every currency that prices it. */
data class DifferentialEnMonedas(
    val extraBs: BigDecimal,
    val extraUsdt: BigDecimal,
    val extraUsd: BigDecimal,
    val extraEur: BigDecimal?
) {
    /** True when the parallel rate charges MORE than the official one. */
    val overpaying: Boolean get() = extraBs.signum() > 0
}

/**
 * What the parallel market charges OVER the official one, per currency, in
 * bolivares.
 *
 * Two DIFFERENT numbers, not one number translated. The dollar's gap and the
 * euro's gap are independent facts: the euro parallel is derived through the
 * dollar, so the euro carries its own spread. Expressing the dollar's gap in
 * euros, which an earlier version did, answered a question nobody asked and
 * buried the figure that was actually wanted.
 *
 * Both are money the reader spends, so both are quoted in bolivares — the unit
 * the rate difference is decided in. Converting them into dollars or euros would
 * divide away the very spread being reported.
 */
data class DiferenciasPorMoneda(
    val dolarBs: BigDecimal?,
    val euroBs: BigDecimal?
)

/**
 * The overpayment per official currency, in bolivares.
 *
 * The dollar gap is the parallel USDT rate against the official dollar rate —
 * P2P trades dollars, so that is the spread that exists. The euro gap is the
 * euro's own: BCV quotes the official euro, the parallel euro is reached
 * through the dollar, and the difference between the two is a second, larger
 * number.
 *
 * A missing rate yields null for that currency rather than a zero, because a
 * gap of "Bs 0,00" reads as "no premium exists" and is the opposite of "this
 * rate is unknown".
 */
fun diferenciasPorMoneda(
    usdRate: Double,
    usdtRate: Double,
    eurRate: Double?
): DiferenciasPorMoneda {
    val dolar = if (usdRate > 0.0 && usdtRate > 0.0) {
        BigDecimal.valueOf(usdtRate - usdRate).setScale(2, RoundingMode.HALF_UP)
    } else {
        null
    }
    val euro = if (eurRate != null && eurRate > 0.0 && usdRate > 0.0 && usdtRate > 0.0) {
        val paralelo = (eurRate / usdRate) * usdtRate
        BigDecimal.valueOf(paralelo - eurRate).setScale(2, RoundingMode.HALF_UP)
    } else {
        null
    }
    return DiferenciasPorMoneda(dolarBs = dolar, euroBs = euro)
}
