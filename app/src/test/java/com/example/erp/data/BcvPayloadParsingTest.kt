package com.example.erp.data

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The BCV payload, parsed for real.
 *
 * The shape below was read off the live endpoint
 * (`https://rates.dolarvzla.com/bcv/current.json`) and is pinned verbatim. It is
 * the only thing standing between the app and a screen full of zeros, and it used
 * to be reachable only through a network call: parsing was a `private` method
 * behind `fetchBcv()`, so no JVM test could touch it.
 *
 * `parseBcvPayload` is the same code, not a copy of it. A test that duplicated
 * the parsing would keep passing after the production branch it mirrors broke,
 * which is the exact failure mode that lets a payload change ship unannounced.
 */
class BcvPayloadParsingTest {

    /**
     * Verbatim from the endpoint. `previous` and `changePercentage` are separate
     * objects, and `date` is a plain ISO day — not the timestamp the app assumes
     * elsewhere.
     */
    private val livePayload = """
        {
          "current": {
            "date": "2026-10-07",
            "usd": 36.5842,
            "eur": 4.0315
          },
          "previous": {
            "date": "2026-10-06",
            "usd": 36.5120,
            "eur": 4.0188
          },
          "changePercentage": {
            "usd": 0.198,
            "eur": 0.316
          }
        }
    """.trimIndent()

    @Test
    fun `one payload yields both official currencies`() {
        val quotes = parseBcvPayload(livePayload)

        assertEquals(
            "ONE request serves both markets; dropping one hides a tab",
            2,
            quotes.size
        )
        assertEquals(listOf("usd", "eur"), quotes.map { it.fuente })
        assertEquals(listOf("Dólar (BCV)", "Euro (BCV)"), quotes.map { it.nombre })
    }

    @Test
    fun `current, previous and change each reach the right field`() {
        val usd = parseBcvPayload(livePayload).first { it.fuente == "usd" }
        val eur = parseBcvPayload(livePayload).first { it.fuente == "eur" }

        assertEquals(36.5842, usd.promedio, 0.0)
        assertEquals(36.5120, usd.anterior!!, 0.0)
        assertEquals(0.198, usd.variacion!!, 0.0)
        assertEquals("2026-10-07", usd.fechaActualizacion)
        assertEquals("2026-10-06", usd.fechaAnterior)

        assertEquals(4.0315, eur.promedio, 0.0)
        assertEquals(4.0188, eur.anterior!!, 0.0)
        assertEquals(0.316, eur.variacion!!, 0.0)
        assertEquals("2026-10-07", eur.fechaActualizacion)
        assertEquals("2026-10-06", eur.fechaAnterior)
    }

    @Test
    fun `unknown top-level keys are ignored`() {
        // The endpoint is not versioned by this app. A field added upstream must
        // not turn every launch into a parse failure.
        val extended = """
            {"generatedAt":"2026-10-07T12:00:00Z","source":"bcv",
             "current":{"date":"2026-10-07","usd":36.5842,"eur":4.0315,"monetary":true},
             "previous":{"date":"2026-10-06","usd":36.5120,"eur":4.0188},
             "changePercentage":{"usd":0.198,"eur":0.316,"previous":"36.51"}}
        """.trimIndent()

        val usd = parseBcvPayload(extended).first { it.fuente == "usd" }

        assertEquals(36.5842, usd.promedio, 0.0)
        assertEquals(36.5120, usd.anterior!!, 0.0)
        assertEquals(0.198, usd.variacion!!, 0.0)
    }

    @Test
    fun `a payload with no previous block leaves the previous rate unknown`() {
        // The first publication of a day can legitimately arrive without one.
        // A silent 0.0 would be read as "the rate collapsed to zero", which the
        // change indicator then renders as a crash rather than as missing.
        val currentOnly = """
            {"current":{"date":"2026-10-07","usd":36.5842,"eur":4.0315}}
        """.trimIndent()

        val quotes = parseBcvPayload(currentOnly)

        assertEquals(2, quotes.size)
        quotes.forEach {
            assertNull("${it.fuente}: a missing previous rate is not zero", it.anterior)
            assertNull("${it.fuente}: there is no previous date to report", it.fechaAnterior)
            assertEquals("2026-10-07", it.fechaActualizacion)
        }
        assertEquals(36.5842, quotes.first { it.fuente == "usd" }.promedio, 0.0)
    }

    @Test
    fun `a payload with no changePercentage leaves the variation unknown`() {
        val noChange = """
            {"current":{"date":"2026-10-07","usd":36.5842,"eur":4.0315},
             "previous":{"date":"2026-10-06","usd":36.5120,"eur":4.0188}}
        """.trimIndent()

        val quotes = parseBcvPayload(noChange)

        quotes.forEach { assertNull("${it.fuente}: no change block means no change", it.variacion) }
        assertEquals(36.5120, quotes.first { it.fuente == "usd" }.anterior!!, 0.0)
    }

    @Test
    fun `a payload missing the current block fails loudly`() {
        // Silently returning zeros here would render a plausible-looking screen of
        // 0,00. A JSONException is the only honest answer, and it is the branch
        // DolarViewModel.mapExceptionToError exists to map to a parse error.
        val noCurrent = """{"previous":{"date":"2026-10-06","usd":36.5120,"eur":4.0188}}"""

        val thrown = runCatching { parseBcvPayload(noCurrent) }.exceptionOrNull()

        assertEquals(JSONException::class.java, thrown?.javaClass)
    }
}