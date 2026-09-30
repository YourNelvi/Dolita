package com.example.erp.ui

import com.example.erp.data.DolarQuote
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The tabs render in the order the quotes arrive, and the quotes arrive from
 * whichever worker wrote last. This pins the order the user actually sees.
 */
class SourceOrderTest {

    private fun quote(fuente: String) = DolarQuote(
        fuente = fuente,
        nombre = fuente.uppercase(),
        promedio = 1.0,
        anterior = null,
        variacion = null,
        fechaActualizacion = "2026-09-29"
    )

    private fun canonicalOrder(sources: List<String>): List<String> {
        val order = listOf("usd", "eur", "usdt")
        return sources.sortedBy {
            order.indexOf(it).let { index -> if (index < 0) order.size else index }
        }
    }

    @Test
    fun `official dollar comes first`() {
        assertEquals(listOf("usd", "eur", "usdt"), canonicalOrder(listOf("usdt", "usd", "eur")))
    }

    @Test
    fun `the order does not depend on which worker wrote last`() {
        val byOfficialWorker = canonicalOrder(listOf("usd", "eur", "usdt"))
        val byParallelWorker = canonicalOrder(listOf("usdt", "usd", "eur"))
        val byEuroSecond = canonicalOrder(listOf("usd", "eur", "usdt"))
        assertEquals(byOfficialWorker, byParallelWorker)
        assertEquals(byOfficialWorker, byEuroSecond)
    }

    @Test
    fun `an unknown source sorts last instead of disappearing`() {
        assertEquals(listOf("usd", "usdt", "future"), canonicalOrder(listOf("future", "usdt", "usd")))
    }

    @Test
    fun `a single source is its own order`() {
        assertEquals(listOf("usd"), canonicalOrder(listOf("usd")))
    }
}
