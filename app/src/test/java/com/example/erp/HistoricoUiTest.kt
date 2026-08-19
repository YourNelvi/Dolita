package com.example.erp

import com.example.erp.data.RateSample
import com.example.erp.ui.HistoricoRow
import com.example.erp.ui.HistoricoState
import com.example.erp.ui.historicoRows
import com.example.erp.ui.historicoState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class HistoricoUiTest {

    private val caracas: ZoneId = ZoneId.of("America/Caracas")
    private val usLocale: Locale = Locale.US

    // 2026-08-18T12:00:00-04:00 in America/Caracas
    private val sampleMorning = 1_787_068_800_000L
    // 2026-08-18T13:00:00-04:00 in America/Caracas
    private val sampleLater = 1_787_072_400_000L
    // 2026-01-01T01:00:00Z = 2025-12-31T21:00 in America/Caracas
    private val newYearsEveLocal = 1_767_229_200_000L

    private fun sample(
        precio: Double,
        timestampEpochMillis: Long,
        variacion: Double? = null
    ): RateSample = RateSample(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        precio = precio,
        timestampEpochMillis = timestampEpochMillis,
        variacion = variacion
    )

    @Test
    fun `empty history shows the sin datos state`() {
        assertEquals(HistoricoState.SinDatos, historicoState(emptyList(), caracas, usLocale))
    }

    @Test
    fun `samples map to rows with date, fuente, price and variation`() {
        val samples = listOf(
            sample(precio = 772.54, timestampEpochMillis = sampleMorning, variacion = 0.10),
            sample(precio = 773.31, timestampEpochMillis = sampleLater, variacion = 0.099)
        )
        val state = historicoState(samples, caracas, usLocale)
        val rows = (state as HistoricoState.ConDatos).rows
        assertEquals(2, rows.size)
        assertEquals(HistoricoRow("18/08", "Dólar (BCV)", "$772.54", "+0.10%"), rows[0])
        assertEquals(HistoricoRow("18/08", "Dólar (BCV)", "$773.31", "+0.10%"), rows[1])
    }

    @Test
    fun `chart values are prices sorted by timestamp`() {
        // intentionally unsorted input: the section must render oldest first
        val samples = listOf(
            sample(precio = 773.31, timestampEpochMillis = sampleLater),
            sample(precio = 772.54, timestampEpochMillis = sampleMorning)
        )
        val state = historicoState(samples, caracas, usLocale)
        val chart = (state as HistoricoState.ConDatos).chart
        assertEquals(listOf(772.54, 773.31), chart)
    }

    @Test
    fun `missing variation renders as dash`() {
        val usdt = RateSample(
            fuente = "usdt",
            nombre = "USDT (P2P)",
            precio = 889.5,
            timestampEpochMillis = sampleMorning,
            variacion = null
        )
        val rows = historicoRows(listOf(usdt), caracas, usLocale)
        assertEquals("—", rows.single().variacion)
    }

    @Test
    fun `negative variation keeps its sign`() {
        val rows = historicoRows(
            listOf(sample(precio = 771.0, timestampEpochMillis = sampleMorning, variacion = -0.05)),
            caracas,
            usLocale
        )
        assertEquals("-0.05%", rows.single().variacion)
    }

    @Test
    fun `date derives from the sample timestamp in the given zone`() {
        // 2026-01-01T01:00Z is still 2025-12-31 in America/Caracas
        val rows = historicoRows(
            listOf(sample(precio = 770.0, timestampEpochMillis = newYearsEveLocal)),
            caracas,
            usLocale
        )
        assertEquals("31/12", rows.single().fecha)
    }
}