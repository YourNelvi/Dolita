package com.example.erp.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * What the on-disk schema actually preserves.
 *
 * `RateHistoryCodecTest` already covers a round trip, but with tidy two-decimal
 * values that any encoder would survive. Rates are not tidy: the BCV publishes
 * four decimals, the parallel market carries a mean of twenty P2P offers, and a
 * chart compares samples for exact equality. A codec that quietly rounds is
 * indistinguishable from correct data until someone diffs two days of history.
 *
 * So this pins the values that break naive encoders — full binary-float precision,
 * and the difference between "no previous rate" and "the previous rate was zero".
 */
class HistoryEncodingTest {

    private fun sample(
        fuente: String = "usd",
        nombre: String = "Dólar (BCV)",
        precio: Double,
        timestampEpochMillis: Long = 1_787_133_600_000L,
        anterior: Double? = null,
        variacion: Double? = null
    ) = RateSample(fuente, nombre, precio, timestampEpochMillis, anterior, variacion)

    private fun roundTrip(samples: List<RateSample>): List<RateSample> =
        RateHistoryCodec.decode(RateHistoryCodec.encode(samples))

    @Test
    fun `full double precision survives the round trip exactly`() {
        // Every one of these is a value whose shortest decimal form is NOT its
        // source literal, so an encoder that formats rather than stores loses
        // them. assertEquals on Double is bit-exact, which is the point.
        val original = listOf(
            sample(precio = 4.028135789012345, anterior = 36.512098765432109, variacion = 0.12345678901234567),
            sample(fuente = "eur", nombre = "Euro (BCV)", precio = 0.30000000000000004, anterior = 0.1 + 0.2),
            sample(fuente = "usdt", nombre = "USDT (P2P)", precio = 889.12345678901234)
        )

        assertEquals(original, roundTrip(original))
    }

    @Test
    fun `a trailing zero in the written form does not change the value`() {
        // The encoder is allowed to normalise 36.5120 to 36.512. What it is not
        // allowed to do is change the number, so this asserts the value, not the
        // string.
        val original = listOf(sample(precio = 36.5120, anterior = 772.0))

        val decoded = roundTrip(original)

        assertEquals(36.512, decoded.single().precio, 0.0)
        assertEquals(772.0, decoded.single().anterior!!, 0.0)
        assertEquals(original, decoded)
    }

    @Test
    fun `an absent optional field is null and not zero`() {
        // `optDouble(key, 0.0)` is the obvious implementation and it is wrong:
        // USDT has no previous rate ever, and a zero would render as a 100%
        // collapse the moment the change indicator divided by it.
        val decoded = roundTrip(listOf(sample(fuente = "usdt", nombre = "USDT (P2P)", precio = 889.5)))

        val only = decoded.single()
        assertNull("usdt has no previous rate at all", only.anterior)
        assertNull("usdt has no variation", only.variacion)
    }

    @Test
    fun `null and zero optional fields stay distinguishable`() {
        // Both shapes appear in the same list on purpose. Asserting them
        // separately would pass even if the encoder mapped null to 0.0.
        val original = listOf(
            sample(fuente = "usd", precio = 36.58, anterior = null, variacion = null),
            sample(fuente = "eur", nombre = "Euro (BCV)", precio = 4.03, anterior = 0.0, variacion = 0.0)
        )

        val decoded = roundTrip(original)

        assertNull(decoded.first { it.fuente == "usd" }.anterior)
        assertEquals(0.0, decoded.first { it.fuente == "eur" }.anterior!!, 0.0)
        assertEquals(0.0, decoded.first { it.fuente == "eur" }.variacion!!, 0.0)
        assertEquals(original, decoded)
    }

    @Test
    fun `a zero optional field is still written to the json`() {
        // The other half of the distinction: a 0.0 that only exists in memory
        // would decode back as null once persisted.
        val json = RateHistoryCodec.encode(listOf(sample(precio = 4.03, anterior = 0.0, variacion = 0.0)))
        val stored = JSONObject(json).getJSONArray("samples").getJSONObject(0)

        assertTrue("anterior = 0.0 must be present, not omitted", stored.has("anterior"))
        assertTrue("variacion = 0.0 must be present, not omitted", stored.has("variacion"))
        assertEquals(0.0, stored.getDouble("anterior"), 0.0)
    }

    @Test
    fun `a far-future timestamp is not truncated`() {
        // millis since epoch for 2200-01-01. A codec that stored seconds, or went
        // through a float, would land the chart in the wrong century.
        val millis = 7_258_118_400_000L
        val original = listOf(sample(precio = 36.58, timestampEpochMillis = millis))

        val decoded = roundTrip(original)

        assertEquals(millis, decoded.single().timestampEpochMillis)
        // UTC on purpose: the default zone shifts midnight into the previous year
        // west of Greenwich, and this test is about precision, not about the zone.
        assertEquals(
            "the year the file name is derived from must survive too",
            2200,
            yearOf(decoded.single().timestampEpochMillis, ZoneId.of("UTC"))
        )
    }

    @Test
    fun `a whole store keeps every sample in order`() {
        // A dropped or reordered sample would silently deform the series instead
        // of failing loudly, so the batch is asserted by full equality.
        val original = (1..64).map { i ->
            sample(
                fuente = if (i % 2 == 0) "eur" else "usd",
                precio = 700.0 + i / 8.0,
                timestampEpochMillis = 1_787_133_600_000L + i * 3_600_000L,
                anterior = if (i % 3 == 0) null else 700.0 + i,
                variacion = if (i % 5 == 0) null else i / 100.0
            )
        }

        assertEquals(original, roundTrip(original))
    }

    @Test
    fun `an empty store encodes to an empty array rather than a null`() {
        val json = RateHistoryCodec.encode(emptyList())
        val root = JSONObject(json)

        assertNotNull("samples must exist even with nothing in it", root.optJSONArray("samples"))
        assertEquals(0, root.getJSONArray("samples").length())
        assertEquals(emptyList<RateSample>(), RateHistoryCodec.decode(json))
    }

    @Test
    fun `decoding a file that lost its samples array yields nothing rather than throwing`() {
        // The tolerant decode is what keeps a half-written file from taking the
        // app down with it.
        assertEquals(emptyList<RateSample>(), RateHistoryCodec.decode("""{"anio":2026}"""))
        assertEquals(emptyList<RateSample>(), RateHistoryCodec.decode("[]"))
        assertEquals(emptyList<RateSample>(), RateHistoryCodec.decode("   "))
    }
}