package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * A threshold that stays breached must be one notification, not an hourly one.
 * Every case below exists because that is the failure this replaces.
 */
class PriceAlertTest {

    private fun at(millis: Long) = RateSample("usdt", "Paralelo", 0.0, millis)

    @Test
    fun `crossing upward fires`() {
        assertTrue(
            PriceAlert.shouldFire(true, 899.0, 905.0, 900.0, PriceAlert.Direccion.ARRIBA)
        )
    }

    @Test
    fun `staying above the threshold does not fire again`() {
        assertFalse(
            PriceAlert.shouldFire(true, 905.0, 910.0, 900.0, PriceAlert.Direccion.ARRIBA)
        )
    }

    @Test
    fun `coming back and crossing again fires`() {
        assertTrue(
            PriceAlert.shouldFire(true, 880.0, 901.0, 900.0, PriceAlert.Direccion.ARRIBA)
        )
    }

    @Test
    fun `staying below never fires`() {
        assertFalse(
            PriceAlert.shouldFire(true, 850.0, 860.0, 900.0, PriceAlert.Direccion.ARRIBA)
        )
    }

    @Test
    fun `a downward alert fires on the way down`() {
        assertTrue(
            PriceAlert.shouldFire(true, 905.0, 895.0, 900.0, PriceAlert.Direccion.ABAJO)
        )
        assertFalse(
            PriceAlert.shouldFire(true, 895.0, 890.0, 900.0, PriceAlert.Direccion.ABAJO)
        )
    }

    @Test
    fun `a disabled alert never fires`() {
        assertFalse(
            PriceAlert.shouldFire(false, 899.0, 999.0, 900.0, PriceAlert.Direccion.ARRIBA)
        )
    }

    @Test
    fun `the first observation only arms, it does not alert`() {
        assertFalse(
            PriceAlert.shouldFire(true, null, 950.0, 900.0, PriceAlert.Direccion.ARRIBA)
        )
        assertFalse(
            PriceAlert.shouldFire(true, 0.0, 950.0, 900.0, PriceAlert.Direccion.ARRIBA)
        )
    }

    @Test
    fun `rearm needs the price back on the other side`() {
        assertTrue(PriceAlert.shouldRearm(880.0, 900.0, PriceAlert.Direccion.ARRIBA))
        assertFalse(PriceAlert.shouldRearm(905.0, 900.0, PriceAlert.Direccion.ARRIBA))
        assertTrue(PriceAlert.shouldRearm(920.0, 900.0, PriceAlert.Direccion.ABAJO))
    }

    @Test
    fun `most recent sample is picked from the right source and window`() {
        val now = 1_000_000L
        val samples = listOf(
            at(now - 10_000),
            at(now - 2 * 60 * 60_000),
            RateSample("usd", "Dólar (BCV)", 855.0, now - 1_000)
        )
        assertEquals(
            now - 10_000L,
            PriceAlert.mostRecent(samples, PriceAlert.Fuente.PARALELO, now, 60 * 60_000L)?.timestampEpochMillis
        )
    }

    @Test
    fun `a sample outside the lookback is ignored`() {
        val now = 1_000_000L
        assertNull(
            PriceAlert.mostRecent(listOf(at(now - 5 * 60 * 60_000L)), PriceAlert.Fuente.PARALELO, now, 60 * 60_000L)
        )
    }

    @Test
    fun `a clock that jumped backwards does not resurrect old samples`() {
        val now = 1_000_000L
        assertNull(
            PriceAlert.mostRecent(listOf(at(now + 60_000)), PriceAlert.Fuente.PARALELO, now, 60 * 60_000L)
        )
    }

    @Test
    fun `an unknown source id falls back to the parallel market`() {
        assertEquals(PriceAlert.Fuente.PARALELO, PriceAlert.Fuente.fromId("nope"))
        assertEquals(PriceAlert.Fuente.USD, PriceAlert.Fuente.fromId("usd"))
    }

    @Test
    fun `start of day is midnight on the device clock`() {
        val caracas = ZoneId.of("America/Caracas")
        val midnight = PriceAlert.todayInMillis(caracas)
        val zoned = java.time.Instant.ofEpochMilli(midnight).atZone(caracas)
        assertEquals(0, zoned.hour)
        assertEquals(0, zoned.minute)
    }
}
