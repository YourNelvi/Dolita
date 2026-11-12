package com.example.erp.ui

import com.example.erp.data.DolarQuote
import com.example.erp.data.PaymentProfile
import com.example.erp.data.PaymentProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/**
 * The share payload now has an optional payment block between the figure and
 * the date. The no-payment payload is still the whole message, so it is pinned
 * here byte for byte; the payment block only ever inserts itself above the
 * footnote.
 */
class ShareContentTest {

    private val usLocale: Locale = Locale.US
    private val caracas: ZoneId = ZoneId.of("America/Caracas")

    /** 2026-08-18T14:32:00-04:00. */
    private val nowMillis = 1_787_077_920_000L

    private val quote = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 773.3112,
        anterior = 772.54,
        variacion = 0.099,
        fechaActualizacion = "2026-08-18"
    )

    private val pagoMovil = PaymentProfile(
        id = "pm-1",
        type = PaymentProfileType.PAGO_MOVIL,
        nombre = "Ana Pérez",
        telefono = "04141234567",
        documentoTipo = "V",
        documentoNumero = "12345678",
        banco = "Banesco"
    )

    private fun rate(payment: PaymentProfile? = null): ShareContent? =
        RateShareText.content(
            quote = quote,
            highPrecision = false,
            zoneId = caracas,
            locale = usLocale,
            nowMillis = nowMillis,
            payment = payment
        )

    private fun calculation(payment: PaymentProfile? = null): ShareContent? =
        CalculatorShareText.content(
            quote = quote,
            bolivaresDigits = "123456",
            divisaDigits = "158",
            highPrecision = false,
            zoneId = caracas,
            locale = usLocale,
            nowMillis = nowMillis,
            payment = payment
        )

    @Test
    fun `a rate without payment keeps the current payload`() {
        val expected = """
            Dólar (BCV)
            Bs 773.31
            Tasa del 18/08/2026 · 14:32
        """.trimIndent()

        assertEquals(expected, rate()!!.toText())
        assertEquals(
            expected,
            RateShareText.build(
                quote = quote,
                highPrecision = false,
                zoneId = caracas,
                locale = usLocale,
                nowMillis = nowMillis
            )
        )
    }

    @Test
    fun `a calculation without payment keeps the current payload`() {
        val expected = """
            Dólar (BCV)
            Bs 1.234,56 = USD 1,58
            1 USD = Bs 773.31
            Tasa del 18/08/2026 · 14:32
        """.trimIndent()

        assertEquals(expected, calculation()!!.toText())
        assertEquals(
            expected,
            CalculatorShareText.build(
                quote = quote,
                bolivaresDigits = "123456",
                divisaDigits = "158",
                highPrecision = false,
                zoneId = caracas,
                locale = usLocale,
                nowMillis = nowMillis
            )
        )
    }

    @Test
    fun `a payment profile adds its block and keeps the date last`() {
        val text = rate(pagoMovil)!!.toText()

        val expected = """
            Dólar (BCV)
            Bs 773.31

            Datos de pago:
            Pago móvil · Banesco
            Ana Pérez · 04141234567
            V-12345678
            Tasa del 18/08/2026 · 14:32
        """.trimIndent()

        assertEquals(expected, text)
        assertEquals("Tasa del 18/08/2026 · 14:32", text.lines().last())
    }

    @Test
    fun `a calculation carries the payment block too`() {
        val text = calculation(pagoMovil)!!.toText()

        assertTrue("expected the payment header in: $text", text.contains("Datos de pago:"))
        assertTrue("expected the holder in: $text", text.contains("Pago móvil · Banesco"))
        assertTrue("expected the account in: $text", text.contains("Ana Pérez · 04141234567"))
        assertEquals("Tasa del 18/08/2026 · 14:32", text.lines().last())
    }

    @Test
    fun `the card carries the payment lines between body and footnote`() {
        val card = calculation(pagoMovil)!!.toCard()!!

        assertEquals("Dólar (BCV)", card.title)
        assertEquals(listOf("Bs 1.234,56 = USD 1,58", "1 USD = Bs 773.31"), card.body)
        assertEquals("Tasa del 18/08/2026 · 14:32", card.footnote)
        assertEquals(pagoMovil.shareLines(), card.payment)
    }

    @Test
    fun `a content without a figure is not a card`() {
        val empty = ShareContent(
            title = "Dólar (BCV)",
            body = emptyList(),
            footnote = "Tasa del 18/08/2026 · 14:32"
        )

        assertNull(empty.toCard())
    }

    @Test
    fun `an unavailable quote yields no content`() {
        assertNull(
            RateShareText.content(
                quote = null,
                highPrecision = false,
                zoneId = caracas,
                locale = usLocale,
                nowMillis = nowMillis
            )
        )
        assertNull(
            CalculatorShareText.content(
                quote = null,
                bolivaresDigits = "123456",
                divisaDigits = "158",
                highPrecision = false,
                zoneId = caracas,
                locale = usLocale,
                nowMillis = nowMillis
            )
        )
    }
}
