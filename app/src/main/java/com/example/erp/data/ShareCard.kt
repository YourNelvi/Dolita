package com.example.erp.data

/**
 * What a shared card shows, derived from the exact text the app sends as the
 * caption.
 *
 * Deriving instead of formatting a second time is the whole point. The picture
 * and the caption travel in the same intent, and two independent formatters is
 * how a reader ends up looking at a card that disagrees with the message under
 * it. One payload, two presentations.
 */
data class ShareCard(
    val title: String,
    val body: List<String>,
    val footnote: String
) {
    companion object {
        /**
         * Splits a share payload into the card's three bands: the first line
         * names the rate, the last one dates it, and everything between is the
         * figure being shared.
         *
         * @return null when the payload cannot carry a card. Two lines is a
         *   title with a date and no number between them; drawing that would
         *   put an empty card in front of the reader, so the caller falls back
         *   to sharing the text alone.
         */
        fun fromText(text: String): ShareCard? {
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 3) return null
            return ShareCard(
                title = lines.first(),
                body = lines.subList(1, lines.size - 1),
                footnote = lines.last()
            )
        }
    }
}
