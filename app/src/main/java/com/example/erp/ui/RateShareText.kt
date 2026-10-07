package com.example.erp.ui

import com.example.erp.data.DolarQuote
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The text a rate is shared as — plain `EXTRA_TEXT`, no image and no
 * FileProvider, because the reader pastes it into a chat and a picture of a
 * number is a worse number.
 *
 * A pure object with no Android types so the whole payload can be asserted on a
 * plain JVM: what actually leaves the device is decided here, not inside a
 * click handler.
 *
 * Every value comes from the quote itself, plus the moment the share happens —
 * never a re-fetch and never a stored "last shared" string. The day comes from
 * the quote and the hour from now, so a reader can always tell which rate they
 * were sent and when it was sent.
 */
object RateShareText {

    private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * @param highPrecision same digit rule the screen renders with: four
     *   decimals when the user asked for them, two otherwise.
     * @param nowMillis injectable so the "when this was sent" line is testable.
     * @return the text to share, or **null** when there is no quote to describe —
     *   an empty string would put a blank card in front of the reader.
     */
    fun build(
        quote: DolarQuote?,
        highPrecision: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (quote == null) return null

        val fracDigits = if (highPrecision) 4 else 2
        val priceFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = fracDigits
            maximumFractionDigits = fracDigits
        }
        val sentAt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)

        // Three lines, no decoration: what it is, what it costs, and when.
        return buildString {
            appendLine(quote.nombre)
            append("Bs ${priceFormat.format(quote.promedio)}")
            appendLine()
            appendLine("Tasa del ${quoteDay(quote.fechaActualizacion)} · ${timeFormat.format(sentAt)}")
        }
    }

    /**
     * The quote's own day. `fechaActualizacion` arrives either as a plain day
     * (BCV) or as a full timestamp (P2P); both name the same thing the reader
     * needs, so both land on dd/MM/yyyy. An unrecognised value is passed through
     * untouched rather than dropped — a raw string is still better than a wrong
     * day.
     *
     * Shared with [CalculatorShareText] so both payloads name a rate's day the
     * same way; a second copy of this parsing is how two share messages end up
     * disagreeing about which day a rate belongs to.
     */
    internal fun quoteDay(fechaActualizacion: String): String = try {
        LocalDate.parse(fechaActualizacion).format(dayFormat)
    } catch (_: Exception) {
        try {
            // ISO_DATE_TIME takes both shapes the API uses — an offset
            // ("…T10:30:00-04:00") and a bare local stamp ("…T10:30:00", P2P) —
            // and resolves each to its own day. A parser that only accepts the
            // offset form silently drops the whole day for the P2P source.
            LocalDate.from(DateTimeFormatter.ISO_DATE_TIME.parse(fechaActualizacion))
                .format(dayFormat)
        } catch (_: Exception) {
            fechaActualizacion
        }
    }
}