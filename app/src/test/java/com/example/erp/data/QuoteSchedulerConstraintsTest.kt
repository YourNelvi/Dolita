package com.example.erp.data

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the gates every scheduled fetch runs behind.
 *
 * These assertions exist because the constraints are invisible at runtime: a
 * worker with no network gate still "works", it just wakes the device and fails
 * privately. Nothing in the UI changes if the constraint is dropped, so the only
 * thing that can catch the regression is a test on the request itself.
 */
class QuoteSchedulerConstraintsTest {

    @Test
    fun `every scheduled fetch requires a network connection`() {
        // Without this the worker wakes on a dead network, opens a socket, fails
        // and leaves WorkManager to schedule a backoff retry.
        assertEquals(
            NetworkType.CONNECTED,
            QuoteScheduler.fetchConstraints.requiredNetworkType
        )
        assertEquals(
            NetworkType.CONNECTED,
            QuoteScheduler.bcvConstraints.requiredNetworkType
        )
    }

    @Test
    fun `official rate checks are not deferred by the battery gate`() {
        // WorkManager waits for a gated request instead of cancelling it. For
        // the next day's rate that is the same as losing the notice: once the
        // date rolls over, the evening pass has nothing new to announce.
        assertFalse(QuoteScheduler.bcvConstraints.requiresBatteryNotLow())
    }

    @Test
    fun `the hourly sample still waits for the battery to leave the low window`() {
        // Nothing time-sensitive depends on the exact sample, so deferring it
        // costs one request instead of an announcement.
        assertTrue(QuoteScheduler.fetchConstraints.requiresBatteryNotLow())
    }

    @Test
    fun `charging is not required`() {
        // Requiring a charger would mean the daily rate only ever updates while
        // the phone is plugged in, which defeats a rate that changes daily.
        assertFalse(QuoteScheduler.fetchConstraints.requiresCharging())
        assertFalse(QuoteScheduler.bcvConstraints.requiresCharging())
    }

    @Test
    fun `device idle is not required`() {
        // Idle-only would push every fetch to whatever hour the phone happens to
        // be untouched, so the morning announcement could land at 23:00.
        assertFalse(QuoteScheduler.fetchConstraints.requiresDeviceIdle())
        assertFalse(QuoteScheduler.bcvConstraints.requiresDeviceIdle())
    }
}
