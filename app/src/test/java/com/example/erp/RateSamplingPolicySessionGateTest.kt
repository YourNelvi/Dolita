package com.example.erp

import com.example.erp.data.DolarQuote
import com.example.erp.data.RateSamplingPolicy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

/**
 * Edge cases for the per-session USDT sampling gate that
 * [RateSamplingPolicyTest] does not cover.
 *
 * The base suite never passes `usdtSampledThisSession = true` together with a
 * BCV quote, so it cannot tell a USDT-scoped gate apart from a blunt
 * "return nothing at all" short-circuit. These tests pin the gate to USDT.
 */
class RateSamplingPolicySessionGateTest {

    private val caracas: ZoneId = ZoneId.of("America/Caracas")

    // Same calendar day (2026-08-19) in America/Caracas: 12:00 local
    private val morningEpochMillis = 1_787_155_200_000L

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

    /**
     * The gate is USDT-scoped: closing the USDT session gate must not stop
     * BCV quotes from being sampled on the very same load.
     */
    @Test
    fun `closed usdt session gate does not suppress bcv quotes`() {
        val result = RateSamplingPolicy.shouldSample(
            existing = emptyList(),
            quotes = listOf(usdQuote, eurQuote, usdtQuote),
            nowEpochMillis = morningEpochMillis,
            usdtSampledThisSession = true,
            zoneId = caracas
        )

        assertEquals(listOf("usd", "eur"), result.map { it.fuente })
    }

    /**
     * Same load, gate open: the gate removes only the USDT sample, leaving
     * the other two sources untouched.
     */
    @Test
    fun `open usdt session gate still admits usdt alongside bcv`() {
        val result = RateSamplingPolicy.shouldSample(
            existing = emptyList(),
            quotes = listOf(usdQuote, eurQuote, usdtQuote),
            nowEpochMillis = morningEpochMillis,
            usdtSampledThisSession = false,
            zoneId = caracas
        )

        assertEquals(listOf("usd", "eur", "usdt"), result.map { it.fuente })
    }
}
