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
    }

    @Test
    fun `every scheduled fetch waits for the battery to leave the low window`() {
        // A gated request is deferred, never cancelled, so the rate still arrives
        // once the device recovers.
        assertTrue(QuoteScheduler.fetchConstraints.requiresBatteryNotLow())
    }

    @Test
    fun `charging is not required`() {
        // Requiring a charger would mean the daily rate only ever updates while
        // the phone is plugged in, which defeats a rate that changes daily.
        assertFalse(QuoteScheduler.fetchConstraints.requiresCharging())
    }

    @Test
    fun `device idle is not required`() {
        // Idle-only would push every fetch to whatever hour the phone happens to
        // be untouched, so the morning announcement could land at 23:00.
        assertFalse(QuoteScheduler.fetchConstraints.requiresDeviceIdle())
    }
}
