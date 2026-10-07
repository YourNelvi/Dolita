package com.example.erp.ui

import com.example.erp.data.DolarQuote
import com.example.erp.data.RateSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the calculator rate choice as a pure function: no ViewModel, no
 * Android Application, no coroutines.
 */
class CalculatorRateSourceTest {

    private val today = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 773.31,
        anterior = 772.54,
        variacion = 0.099,
        fechaActualizacion = "2026-08-18"
    )

    private val future = DolarQuote(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        promedio = 780.10,
        anterior = 773.31,
        variacion = 0.878,
        fechaActualizacion = "2026-08-19"
    )

    private val sample = RateSample(
        fuente = "usd",
        nombre = "Dólar (BCV)",
        precio = 750.42,
        timestampEpochMillis = 1_787_155_200_000L,
        anterior = 749.10,
        variacion = 0.176
    )

    @Test
    fun `TODAY wins by default`() {
        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = future,
            futureSource = CalculatorRateSource.TODAY,
            selectedSample = sample,
            selectedLabel = "18/08/2026",
            dateSource = CalculatorRateSource.TODAY
        )

        assertEquals(today, resolved)
    }

    @Test
    fun `NEXT_RATE uses the future promedio`() {
        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = future,
            futureSource = CalculatorRateSource.NEXT_RATE,
            selectedSample = null,
            selectedLabel = null,
            dateSource = CalculatorRateSource.TODAY
        )

        assertNotNull(resolved)
        assertEquals(780.10, resolved!!.promedio, 0.0001)
        assertEquals("Dólar (BCV) (futura)", resolved.nombre)
        assertEquals("usd", resolved.fuente)
    }

    @Test
    fun `NEXT_RATE carries the future quote's own anterior and variacion`() {
        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = future,
            futureSource = CalculatorRateSource.NEXT_RATE,
            selectedSample = null,
            selectedLabel = null,
            dateSource = CalculatorRateSource.TODAY
        )

        assertNotNull(resolved)
        // fetchBcv() maps anterior and variacion from the SAME payload as
        // promedio, so the future quote's own pair describes the future rate.
        assertEquals(773.31, resolved!!.anterior!!, 0.0001)
        assertEquals(0.878, resolved.variacion!!, 0.0001)
        // And explicitly NOT today's pair, which belongs to a different day.
        assertNotEquals(772.54, resolved.anterior!!, 0.0001)
        assertNotEquals(0.099, resolved.variacion!!, 0.0001)
    }

    @Test
    fun `NEXT_RATE keeps the variation when today's synthetic quote has none`() {
        // The ViewModel builds today's quote with variacion = null on purpose,
        // so inheriting it silently dropped the variation altogether.
        val syntheticToday = today.copy(variacion = null, anterior = 771.90)

        val resolved = CalculatorRateResolver.resolve(
            today = syntheticToday,
            future = future,
            futureSource = CalculatorRateSource.NEXT_RATE,
            selectedSample = null,
            selectedLabel = null,
            dateSource = CalculatorRateSource.TODAY
        )

        assertNotNull(resolved)
        assertEquals(0.878, resolved!!.variacion!!, 0.0001)
        assertEquals(773.31, resolved.anterior!!, 0.0001)
    }

    @Test
    fun `NEXT_RATE never borrows today's anterior and variacion, even when the future has none`() {
        // The guard against the fix being half-reverted: a future quote with no
        // pair of its own must stay empty instead of quietly re-inheriting
        // today's, which is the bug this branch was corrected for.
        val bareFuture = future.copy(anterior = null, variacion = null)

        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = bareFuture,
            futureSource = CalculatorRateSource.NEXT_RATE,
            selectedSample = null,
            selectedLabel = null,
            dateSource = CalculatorRateSource.TODAY
        )

        assertNotNull(resolved)
        assertNull(resolved!!.anterior)
        assertNull(resolved.variacion)
    }

    @Test
    fun `SELECTED_DATE uses the sample precio and the label date`() {
        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = null,
            futureSource = CalculatorRateSource.TODAY,
            selectedSample = sample,
            selectedLabel = "17/08/2026",
            dateSource = CalculatorRateSource.SELECTED_DATE
        )

        assertNotNull(resolved)
        assertEquals(750.42, resolved!!.promedio, 0.0001)
        assertEquals("17/08/2026", resolved.fechaActualizacion)
        assertEquals("Dólar (BCV) (17/08/2026)", resolved.nombre)
        assertEquals("usd", resolved.fuente)
    }

    @Test
    fun `null future falls back to today instead of null`() {
        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = null,
            futureSource = CalculatorRateSource.NEXT_RATE,
            selectedSample = null,
            selectedLabel = null,
            dateSource = CalculatorRateSource.TODAY
        )

        assertEquals(today, resolved)
    }

    @Test
    fun `null sample falls back to today instead of null`() {
        val resolved = CalculatorRateResolver.resolve(
            today = today,
            future = future,
            futureSource = CalculatorRateSource.TODAY,
            selectedSample = null,
            selectedLabel = "17/08/2026",
            dateSource = CalculatorRateSource.SELECTED_DATE
        )

        assertEquals(today, resolved)
    }

    @Test
    fun `no today quote returns null`() {
        val resolved = CalculatorRateResolver.resolve(
            today = null,
            future = future,
            futureSource = CalculatorRateSource.NEXT_RATE,
            selectedSample = null,
            selectedLabel = null,
            dateSource = CalculatorRateSource.TODAY
        )

        // With no today quote there is no source to copy an absent name from,
        // so nothing renders rather than showing a fabricated rate.
        assertEquals(null, resolved)
    }

    @Test
    fun `SELECTED_DATE branch survives a null today quote when the sample exists`() {
        val resolved = CalculatorRateResolver.resolve(
            today = null,
            future = null,
            futureSource = CalculatorRateSource.TODAY,
            selectedSample = sample,
            selectedLabel = "17/08/2026",
            dateSource = CalculatorRateSource.SELECTED_DATE
        )

        assertNotNull(resolved)
        assertEquals(750.42, resolved!!.promedio, 0.0001)
        assertEquals("usd", resolved.fuente)
    }
}