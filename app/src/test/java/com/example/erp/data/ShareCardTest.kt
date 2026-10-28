package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The card is drawn from the caption it travels with, so the split is the only
 * thing that can make the picture disagree with the message under it.
 */
class ShareCardTest {

    private val rateText = """
        Dólar (BCV)
        Bs 887,30
        Tasa del 27/10/2026 · 10:24
    """.trimIndent()

    @Test
    fun `a rate payload splits into title figure and day`() {
        val card = ShareCard.fromText(rateText)!!

        assertEquals("Dólar (BCV)", card.title)
        assertEquals(listOf("Bs 887,30"), card.body)
        assertEquals("Tasa del 27/10/2026 · 10:24", card.footnote)
    }

    @Test
    fun `a calculator payload keeps both amounts in order`() {
        val card = ShareCard.fromText(
            """
            Dólar (BCV)
            Bs 887,30 = USD 1,00
            1 USD = Bs 887,30
            Tasa del 27/10/2026 · 10:24
            """.trimIndent()
        )!!

        assertEquals("Dólar (BCV)", card.title)
        // The conversion the user typed comes first because it is the answer;
        // the rate that produced it explains the answer and follows it.
        assertEquals(listOf("Bs 887,30 = USD 1,00", "1 USD = Bs 887,30"), card.body)
        assertEquals("Tasa del 27/10/2026 · 10:24", card.footnote)
    }

    @Test
    fun `the trailing newline the builders append is not a blank band`() {
        val card = ShareCard.fromText(
            "Dólar (BCV)\nBs 887,30\nTasa del 27/10/2026 · 10:24\n"
        )!!

        assertEquals(listOf("Bs 887,30"), card.body)
        assertEquals("Tasa del 27/10/2026 · 10:24", card.footnote)
    }

    @Test
    fun `windows line endings split the same way`() {
        val card = ShareCard.fromText(
            "Dólar (BCV)\r\nBs 887,30\r\nTasa del 27/10/2026 · 10:24\r\n"
        )!!

        assertEquals("Dólar (BCV)", card.title)
        assertEquals(listOf("Bs 887,30"), card.body)
    }

    @Test
    fun `a payload with no figure between title and day is not a card`() {
        // Two lines is a name and a date with nothing to share. Drawing it would
        // hand the reader an empty plate, so the caller falls back to text.
        assertNull(ShareCard.fromText("Dólar (BCV)\nTasa del 27/10/2026 · 10:24"))
    }

    @Test
    fun `an empty payload is not a card`() {
        assertNull(ShareCard.fromText(""))
        assertNull(ShareCard.fromText("   \n\n  "))
        assertNull(ShareCard.fromText("Dólar (BCV)"))
    }
}
