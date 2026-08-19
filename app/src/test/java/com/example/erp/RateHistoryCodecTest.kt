package com.example.erp

import com.example.erp.data.RateHistoryCodec
import com.example.erp.data.RateSample
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateHistoryCodecTest {

    private val fullSample = RateSample(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        precio = 773.31,
        timestampEpochMillis = 1_787_133_600_000L,
        anterior = 772.54,
        variacion = 0.099
    )

    private val minimalSample = RateSample(
        fuente = "usdt",
        nombre = "USDT (P2P)",
        precio = 889.5,
        timestampEpochMillis = 1_787_137_200_000L
    )

    @Test
    fun `round trip preserves all samples and optional fields`() {
        val original = listOf(fullSample, minimalSample)

        val decoded = RateHistoryCodec.decode(RateHistoryCodec.encode(original))

        assertEquals(original, decoded)
    }

    @Test
    fun `encode omits optional anterior and variacion when null`() {
        val json = RateHistoryCodec.encode(listOf(minimalSample))
        val root = JSONObject(json)

        assertTrue("anterior must be omitted", !root.getJSONArray("samples").getJSONObject(0).has("anterior"))
        assertTrue("variacion must be omitted", !root.getJSONArray("samples").getJSONObject(0).has("variacion"))
        assertEquals(1_787_137_200_000L, root.getJSONArray("samples").getJSONObject(0).optLong("timestampEpochMillis"))
    }

    @Test
    fun `decode returns empty list for malformed json`() {
        assertEquals(emptyList<RateSample>(), RateHistoryCodec.decode("not-json{"))
        assertEquals(emptyList<RateSample>(), RateHistoryCodec.decode(""))
    }
}