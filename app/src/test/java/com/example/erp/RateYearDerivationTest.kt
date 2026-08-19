package com.example.erp

import com.example.erp.data.dayOfYear
import com.example.erp.data.rateYearName
import com.example.erp.data.yearOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class RateYearDerivationTest {

    private val caracas: ZoneId = ZoneId.of("America/Caracas")

    // 2026-08-19T12:00:00-04:00 in America/Caracas
    private val midYearEpochMillis = 1_787_155_200_000L
    // 2026-12-31T23:30:00-04:00 in America/Caracas
    private val newYearsEveLate = 1_798_774_200_000L
    // 2027-01-01T00:30:00-04:00 in America/Caracas
    private val newYearsDayEarly = 1_798_777_800_000L

    @Test
    fun `epoch millis derive to year and day in fixed zone`() {
        assertEquals(2026, yearOf(midYearEpochMillis, caracas))
        assertEquals(231, dayOfYear(midYearEpochMillis, caracas))
    }

    @Test
    fun `new years eve late rolls over to next year`() {
        assertEquals(2026, yearOf(newYearsEveLate, caracas))
        assertEquals(365, dayOfYear(newYearsEveLate, caracas))
        assertEquals(2027, yearOf(newYearsDayEarly, caracas))
        assertEquals(1, dayOfYear(newYearsDayEarly, caracas))
    }

    @Test
    fun `rate file name derives from the sample year`() {
        assertEquals("rates-2026.json", rateYearName(newYearsEveLate, caracas))
        assertEquals("rates-2027.json", rateYearName(newYearsDayEarly, caracas))
    }
}