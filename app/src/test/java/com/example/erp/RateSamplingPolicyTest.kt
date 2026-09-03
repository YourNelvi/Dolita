package com.example.erp

import com.example.erp.data.DolarQuote
import com.example.erp.data.RateSamplingPolicy
import com.example.erp.data.RateSample
import com.example.erp.data.chartValues
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class RateSamplingPolicyTest {

    private val caracas: ZoneId = ZoneId.of("America/Caracas")

    // Same calendar day (2026-08-19) in America/Caracas: 12:00 and 18:30 local
    private val morningEpochMillis = 1_787_155_200_000L
    private val eveningEpochMillis = 1_787_175_000_000L
    // Next calendar day (2026-08-20) in America/Caracas: 09:00 local
    private val nextDayEpochMillis = 1_787_216_400_000L

    private val usdQuote = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 773.31,
        anterior = 772.54,
        variacion = 0.099,
        fechaActualizacion = "2026-08-19"
    )
    private val eurQuote = DolarQuote(
        fuente = "eur",
        nombre = "Euro (BCV)",
        promedio = 889.5,
        anterior = null,
        variacion = null,
        fechaActualizacion = "2026-08-19"
    )
    private val usdtQuote = DolarQuote(
        fuente = "usdt",
        nombre = "USDT (P2P)",
        promedio = 105.5,
        anterior = null,
        variacion = null,
        fechaActualizacion = "2026-08-19T12:00:00"
    )

    @Test
    fun `bcv same day second load replaces with new data`() {
        val first = RateSamplingPolicy.shouldSample(
            existing = emptyList(),
            quotes = listOf(usdQuote, eurQuote),
            nowEpochMillis = morningEpochMillis,
            usdtSampledThisSession = false,
            zoneId = caracas
        )
        assertEquals(listOf("usd", "eur"), first.map { it.fuente })
        assertEquals(773.31, first.first { it.fuente == "usd" }.precio, 0.0)

        // Now with a different price — should always produce new samples (replace seed data)
        val updatedUsd = usdQuote.copy(promedio = 780.00)
        val second = RateSamplingPolicy.shouldSample(
            existing = first,
            quotes = listOf(updatedUsd, eurQuote),
            nowEpochMillis = eveningEpochMillis,
            usdtSampledThisSession = false,
            zoneId = caracas
        )
        assertEquals(listOf("usd", "eur"), second.map { it.fuente })
        assertEquals(780.00, second.first { it.fuente == "usd" }.precio, 0.0)
    }

    @Test
    fun `bcv next calendar day samples again`() {
        val existing = listOf(
            RateSample(
                fuente = "usd",
                nombre = "Dólar (BCV)",
                precio = 773.31,
                timestampEpochMillis = morningEpochMillis
            )
        )

        val result = RateSamplingPolicy.shouldSample(
            existing = existing,
            quotes = listOf(usdQuote),
            nowEpochMillis = nextDayEpochMillis,
            usdtSampledThisSession = false,
            zoneId = caracas
        )

        assertEquals(listOf("usd"), result.map { it.fuente })
        assertEquals(773.31, result.single().precio, 0.0)
    }

    @Test
    fun `usdt sampled this session is skipped`() {
        val result = RateSamplingPolicy.shouldSample(
            existing = emptyList(),
            quotes = listOf(usdtQuote),
            nowEpochMillis = morningEpochMillis,
            usdtSampledThisSession = true,
            zoneId = caracas
        )

        assertEquals(emptyList<RateSample>(), result)
    }

    @Test
    fun `usdt not sampled this session is added once`() {
        val result = RateSamplingPolicy.shouldSample(
            existing = emptyList(),
            quotes = listOf(usdtQuote),
            nowEpochMillis = morningEpochMillis,
            usdtSampledThisSession = false,
            zoneId = caracas
        )

        assertEquals(listOf("usdt"), result.map { it.fuente })
        assertEquals(105.5, result.single().precio, 0.0)
    }

    @Test
    fun `absent quote fuente produces no phantom sample`() {
        // USDT fetch failed: only BCV quotes present in this load
        val result = RateSamplingPolicy.shouldSample(
            existing = emptyList(),
            quotes = listOf(usdQuote),
            nowEpochMillis = morningEpochMillis,
            usdtSampledThisSession = false,
            zoneId = caracas
        )

        assertEquals(listOf("usd"), result.map { it.fuente })
    }

    @Test
    fun `empty quotes produce no samples`() {
        val result = RateSamplingPolicy.shouldSample(
            existing = emptyList(),
            quotes = emptyList(),
            nowEpochMillis = morningEpochMillis,
            usdtSampledThisSession = false,
            zoneId = caracas
        )

        assertEquals(emptyList<RateSample>(), result)
    }

    @Test
    fun `chartValues sorts by timestamp and maps to prices`() {
        val samples = listOf(
            RateSample("usd", "Dólar (BCV)", 773.31, timestampEpochMillis = eveningEpochMillis),
            RateSample("usd", "Dólar (BCV)", 772.54, timestampEpochMillis = morningEpochMillis),
            RateSample("eur", "Euro (BCV)", 889.5, timestampEpochMillis = nextDayEpochMillis)
        )

        val values = chartValues(samples)

        assertEquals(listOf(772.54, 773.31, 889.5), values)
    }

    @Test
    fun `chartValues on empty samples returns empty`() {
        assertEquals(emptyList<Double>(), chartValues(emptyList()))
    }
}