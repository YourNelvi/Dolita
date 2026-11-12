package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

/**
 * Binance P2P has one payload per side. These fixtures pin the exact shape the
 * app depends on and the fallback rules around a partial failure, without a
 * live request.
 */
class UsdtPayloadParsingTest {

    private fun payload(vararg prices: String): String {
        val ads = prices.joinToString(",") { price ->
            """{"adv":{"price":"$price"}}"""
        }
        return """{"data":[$ads]}"""
    }

    @Test
    fun `returns the median of the BUY page`() {
        val parsed = parseUsdtPrice(payload("1076.00", "1076.65", "1077.00"))
        assertEquals(1076.65, parsed!!, 0.0001)
    }

    @Test
    fun `returns the median of the SELL page`() {
        val parsed = parseUsdtPrice(payload("1073.00", "1073.66", "1074.00"))
        assertEquals(1073.66, parsed!!, 0.0001)
    }

    @Test
    fun `an outlier ad cannot drag the side`() {
        // The live sell page can carry a 1.115,80 buyer next to ~1.072-1.074
        // buyers. The average would land near 1.083; the median stays typical.
        val parsed = parseUsdtPrice(
            payload("1072.12", "1072.70", "1074.00", "1115.80")
        )
        assertEquals(1073.35, parsed!!, 0.0001)
    }

    @Test
    fun `skips ads with no price or a non-positive price`() {
        val body = """
            {"data":[
              {"adv":{"price":"100.00"}},
              {"adv":{}},
              {"adv":{"price":"not-a-number"}},
              {"adv":{"price":"0"}},
              {}
            ]}
        """.trimIndent()

        assertEquals(100.0, parseUsdtPrice(body)!!, 0.0001)
    }

    @Test
    fun `an empty page is null, not zero`() {
        assertNull(parseUsdtPrice("""{"data":[]}"""))
    }

    @Test
    fun `invalid json is null`() {
        assertNull(parseUsdtPrice("not json"))
    }

    @Test
    fun `the canonical promedio is the mean of both sides`() {
        val updatedAt = OffsetDateTime.parse("2026-11-12T15:30:00-04:00")
        val quote = buildUsdtQuote(1076.65, 1073.66, updatedAt)!!

        assertEquals("usdt", quote.fuente)
        assertEquals("USDT (P2P)", quote.nombre)
        assertEquals(1076.65, quote.compra!!, 0.0001)
        assertEquals(1073.66, quote.venta!!, 0.0001)
        assertEquals(1075.155, quote.promedio, 0.0001)
        assertEquals(updatedAt.toString(), quote.fechaActualizacion)
    }

    @Test
    fun `a surviving side still publishes a quote`() {
        val quote = buildUsdtQuote(compra = 1076.65, venta = null)!!

        assertEquals(1076.65, quote.promedio, 0.0001)
        assertEquals(1076.65, quote.compra!!, 0.0001)
        assertNull(quote.venta)
    }

    @Test
    fun `no side at all publishes nothing`() {
        assertNull(buildUsdtQuote(null, null))
    }
}
