package com.example.erp.ui

import com.example.erp.data.DolarQuote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The USDT side selector as pure functions: no ViewModel, no Android, no
 * composition. The derived quote is what the calculator converts with; the
 * canonical average is what the hero, history and cache keep.
 */
class UsdtRateModeTest {

    private fun usdt(
        promedio: Double = 1_075.16,
        compra: Double? = 1_076.65,
        venta: Double? = 1_073.66
    ) = DolarQuote(
        fuente = "usdt",
        nombre = "USDT (Binance P2P)",
        promedio = promedio,
        compra = compra,
        venta = venta,
        fechaActualizacion = "2026-11-12T10:15:30-04:00"
    )

    private fun bcv() = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 772.54,
        fechaActualizacion = "2026-11-12"
    )

    @Test
    fun `COMPRA prices with the buy side`() {
        assertEquals(1_076.65, usdt().precioPara(UsdtRateMode.COMPRA), 0.0001)
    }

    @Test
    fun `VENTA prices with the sell side`() {
        assertEquals(1_073.66, usdt().precioPara(UsdtRateMode.VENTA), 0.0001)
    }

    @Test
    fun `PROMEDIO prices with the canonical average`() {
        assertEquals(1_075.16, usdt().precioPara(UsdtRateMode.PROMEDIO), 0.0001)
    }

    @Test
    fun `a missing side falls back to the average`() {
        val noBuy = usdt(compra = null)
        assertEquals(1_075.16, noBuy.precioPara(UsdtRateMode.COMPRA), 0.0001)
        assertEquals(1_073.66, noBuy.precioPara(UsdtRateMode.VENTA), 0.0001)
    }

    @Test
    fun `tieneTasa reports which sides the quote actually publishes`() {
        val both = usdt()
        assertTrue(both.tieneTasa(UsdtRateMode.COMPRA))
        assertTrue(both.tieneTasa(UsdtRateMode.VENTA))
        assertTrue(both.tieneTasa(UsdtRateMode.PROMEDIO))

        val noBuy = usdt(compra = null)
        assertFalse(noBuy.tieneTasa(UsdtRateMode.COMPRA))
        assertTrue(noBuy.tieneTasa(UsdtRateMode.VENTA))

        val noSell = usdt(venta = null)
        assertTrue(noSell.tieneTasa(UsdtRateMode.COMPRA))
        assertFalse(noSell.tieneTasa(UsdtRateMode.VENTA))

        // The average always exists; it is the fallback every branch uses.
        assertTrue(noBuy.tieneTasa(UsdtRateMode.PROMEDIO))
        assertTrue(noSell.tieneTasa(UsdtRateMode.PROMEDIO))
    }

    @Test
    fun `a USDT quote is re-priced for the calculator without losing its sides`() {
        val canonical = usdt()
        val derived = canonical.conTasaUsdt(UsdtRateMode.COMPRA)

        assertEquals(1_076.65, derived.promedio, 0.0001)
        assertEquals(canonical.nombre, derived.nombre)
        assertEquals(canonical.fuente, derived.fuente)
        assertEquals(canonical.fechaActualizacion, derived.fechaActualizacion)
        assertEquals(canonical.compra, derived.compra)
        assertEquals(canonical.venta, derived.venta)
        // The canonical quote is not mutated.
        assertEquals(1_075.16, canonical.promedio, 0.0001)
    }

    @Test
    fun `a non-USDT quote is returned untouched`() {
        val dollar = bcv()
        assertSame(dollar, dollar.conTasaUsdt(UsdtRateMode.COMPRA))
    }

    @Test
    fun `re-pricing with a missing side keeps the average`() {
        val noBuy = usdt(compra = null)
        assertEquals(1_075.16, noBuy.conTasaUsdt(UsdtRateMode.COMPRA).promedio, 0.0001)
    }
}
