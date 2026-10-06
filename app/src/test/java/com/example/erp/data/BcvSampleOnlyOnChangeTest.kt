package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * The BCV series must contain changes and nothing else.
 *
 * BCV does not publish on Saturday or Sunday, but it keeps serving the last
 * published rate, so sampling on those days stored Friday's number a second time
 * under Saturday's date. The chart was then asked to draw a point where nothing
 * happened. These tests pin the rule: a sample is a change.
 *
 * Fixed at 2026-10-04 (a Sunday), the date this was diagnosed on.
 */
class BcvSampleOnlyOnChangeTest {

    private val zone = ZoneId.of("America/Caracas")

    private fun day(date: LocalDate, precio: Double) = RateSample(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        precio = precio,
        timestampEpochMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
    )

    private fun quote(precio: Double, now: LocalDate) = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = precio,
        anterior = null,
        variacion = null,
        fechaActualizacion = now.toString()
    )

    private fun sample(
        existing: List<RateSample>,
        precio: Double,
        now: LocalDate
    ): List<RateSample> = RateSamplingPolicy.shouldSample(
        existing = existing,
        quotes = listOf(quote(precio, now)),
        nowEpochMillis = now.atStartOfDay(zone).plusHours(9).toInstant().toEpochMilli(),
        usdtSampledThisSession = false,
        zoneId = zone
    )

    // 2026-10-02 Friday, 10-03 Saturday, 10-04 Sunday, 10-05 Monday
    private val friday = LocalDate.of(2026, 10, 2)
    private val saturday = LocalDate.of(2026, 10, 3)
    private val sunday = LocalDate.of(2026, 10, 4)
    private val monday = LocalDate.of(2026, 10, 5)

    @Test
    fun `a weekend re-serving Friday's rate adds nothing`() {
        // THE REGRESSION. BCV answers 866,56 on Saturday and Sunday because that
        // is still Friday's published rate.
        val stored = listOf(day(friday, 866.56))

        assertTrue(
            "Saturday stored a duplicate of Friday's rate",
            sample(stored, 866.56, saturday).isEmpty()
        )
        assertTrue(
            "Sunday stored a duplicate of Friday's rate",
            sample(stored, 866.56, sunday).isEmpty()
        )
    }

    @Test
    fun `the new rate on Monday is stored`() {
        val stored = listOf(day(friday, 866.56))
        val added = sample(stored, 871.37, monday)

        assertEquals(1, added.size)
        assertEquals(871.37, added.first().precio, 0.0001)
    }

    @Test
    fun `an empty store always takes the first rate`() {
        val added = sample(emptyList(), 866.56, friday)
        assertEquals(1, added.size)
    }

    @Test
    fun `an unchanged rate on a weekday is not a change either`() {
        // Not only weekends: any day BCV republishes the same number carries no
        // new information, so it belongs in neither the series nor the chart.
        val stored = listOf(day(friday, 866.56))
        assertTrue(sample(stored, 866.56, monday).isEmpty())
    }

    @Test
    fun `a future-dated sample does not count as the last stored price`() {
        // The next rate is stored as a sample. Comparing against it would let a
        // pending rate suppress the real one and freeze the series a day early.
        val stored = listOf(
            day(friday, 866.56),
            day(saturday, 900.00)
        )
        val added = sample(stored, 871.37, saturday)

        assertEquals(1, added.size)
        assertEquals(871.37, added.first().precio, 0.0001)
    }

    @Test
    fun `two opens on the same day do not both write`() {
        val first = sample(emptyList(), 866.56, friday)
        val second = sample(first, 866.56, friday)

        assertEquals(1, first.size)
        assertTrue("a second open on the same day rewrote the sample", second.isEmpty())
    }
}
