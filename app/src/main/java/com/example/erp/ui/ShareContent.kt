package com.example.erp.ui

import com.example.erp.data.ShareCard

/**
 * What a share sends, in the order the reader receives it.
 *
 * The caption and the picture are derived from this one value: [toText] is the
 * message and [toCard] is the same bands drawn on the PNG, so the two cannot
 * disagree about the figure, the payment block or the day.
 *
 * [payment] is a band of its own because it is not part of the figure. A client
 * reads the amount first and the account to pay it into second, and the card
 * renders those two at different sizes instead of letting account numbers
 * compete with the rate.
 */
data class ShareContent(
    val title: String,
    val body: List<String>,
    val payment: List<String> = emptyList(),
    val footnote: String
) {

    /**
     * The caption exactly as it leaves the device.
     *
     * The footnote is the last line on purpose: a rate without its day is the
     * mistake the shared card exists to prevent. The payment block, when there
     * is one, sits above it behind its own header and a blank line of air.
     */
    fun toText(): String = buildString {
        append(title)
        body.forEach { line -> append('\n').append(line) }
        if (payment.isNotEmpty()) {
            append("\n\nDatos de pago:")
            payment.forEach { line -> append('\n').append(line) }
        }
        append('\n').append(footnote)
    }

    /**
     * The same payload as a card, or **null** when there is no figure to draw.
     *
     * The rule mirrors [ShareCard.fromText]: a card without a body is an empty
     * plate in the conversation, so the caller falls back to sharing the text.
     */
    fun toCard(): ShareCard? =
        if (body.isEmpty()) null else ShareCard(title, body, footnote, payment)
}
