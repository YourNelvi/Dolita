package com.example.erp.ui

import com.example.erp.data.CurrencyConverter
import com.example.erp.data.DolarQuote
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The caption a calculator RESULT is shared as.
 *
 * The `EXTRA_TEXT` half of the share: `ShareImage` draws a card out of these
 * same lines and sends both together, so the result reads as the day's rate
 * while the amounts stay selectable. The payload stays plain text on purpose —
 * it is what the reader pastes into a calculator, and it is what still goes out
 * when the card cannot be drawn.
 *
 * The rate alone is already shareable from the hero. What this adds is the
 * answer the user actually produced: both amounts as they stand in the fields,
 * the rate that produced them, and the day that rate belongs to. A pure object
 * with no Android types so the whole payload can be asserted on a plain JVM:
 * what leaves the device is decided here, not inside a click handler.
 *
 * The day is not decoration here. When the calculator is left on the next-day
 * rate or on a rate picked from the calendar, the shared message is the only
 * place that still says WHICH rate was used and WHICH day it belongs to — the
 * message outlives the screen, so that distinction cannot collapse into
 * "today's rate".
 */
object CalculatorShareText {

    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * @param bolivaresDigits the Bolívares field's digit string, exactly as the
     *   card holds it ("123456" is 1.234,56). Read as digits, not as a formatted
     *   string, so the message shows the amount the user sees rather than a
     *   second formatting of the same number.
     * @param divisaDigits the divisa field's digit string.
     * @param highPrecision same digit rule the screen renders the rate with:
     *   four decimals when the user asked for them, two otherwise.
     * @param nowMillis injectable so the "when this was sent" line is testable.
     * @return the text to share, or **null** when there is nothing worth
     *   sending: no rate, or nothing typed at all. A message that carries a
     *   rate and two blank amounts teaches the reader nothing, so the caller
     *   never launches a share sheet for it.
     */
    fun build(
        quote: DolarQuote?,
        bolivaresDigits: String,
        divisaDigits: String,
        highPrecision: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (quote == null) return null

        // An empty field is "the user has not typed that yet", which is not the
        // same as zero: it is omitted rather than printed as 0,00.
        val bolivares = amount(bolivaresDigits)
        val divisa = amount(divisaDigits)
        if (bolivares == null && divisa == null) return null

        val fracDigits = if (highPrecision) 4 else 2
        val priceFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = fracDigits
            maximumFractionDigits = fracDigits
        }
        val sentAt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val currency = quote.fuente.uppercase()

        // Four lines, no decoration: what it is, what it came to, what rate did
        // it, and when that rate belongs to. A half-empty conversion line is
        // built from the sides that exist rather than padded with a blank.
        val conversion = buildList {
            bolivares?.let { add("Bs $it") }
            divisa?.let { add("$currency $it") }
        }.joinToString(" = ")

        return buildString {
            appendLine(quote.nombre)
            appendLine(conversion)
            appendLine("1 $currency = Bs ${priceFormat.format(quote.promedio)}")
            append("Tasa del ${RateShareText.quoteDay(quote.fechaActualizacion)} · ${timeFormat.format(sentAt)}")
        }
    }

    /**
     * The amount as the card shows it, reusing the card's own digits-to-"es-VE"
     * formatting so the shared number and the on-screen number are the same
     * string, not two spellings of one value.
     */
    private fun amount(digits: String): String? =
        CurrencyConverter.parseDigits(digits)?.let { CurrencyConverter.format(it) }
}
