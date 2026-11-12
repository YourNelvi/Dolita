package com.example.erp.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "have I already told the user?" gate.
 *
 * This is the last thing standing between a scheduled worker and a phone that
 * buzzes every hour. A worker runs on a cadence, not on a change: without this
 * gate the daily job re-announces the same rate every time it fires and the
 * hourly job re-announces an unchanged parallel market every hour. Noise is what
 * makes people silence the notifications that matter.
 *
 * It had no coverage at all, and the reason it can be covered cheaply is that the
 * whole gate is one comparison over a stored string. [FakeSharedPreferences] is
 * the in-memory stand-in — the gate reads one key and writes one, so a fake of the
 * platform interface is enough and no device or network is involved.
 */
class NotificationStateTest {

    private val prefs = FakeSharedPreferences()

    /**
     * `ContextWrapper` is concrete, so only the one method the gate calls has to
     * be replaced. That is what keeps this a JVM test: nothing here needs an
     * Android runtime, only a `getSharedPreferences` that answers from memory.
     */
    private val context: Context = object : ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    @Test
    fun `the first sighting of a rate notifies`() {
        assertTrue(
            "nothing has been recorded yet, so this must notify",
            NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07")
        )
    }

    @Test
    fun `the same rate does not notify twice`() {
        // THE CONTRACT. The daily worker firing again for an unchanged rate is
        // the exact noise this gate exists to remove.
        assertTrue(NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07"))
        assertFalse(
            "a second run for the same rate and date must stay silent",
            NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07")
        )
        assertFalse(
            "and a third run too",
            NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07")
        )
    }

    @Test
    fun `a changed rate notifies`() {
        NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07")

        assertTrue(
            "a new rate is exactly what the alert is for",
            NotificationState.shouldNotifyDailyRate(context, 871.37, "2026-10-08")
        )
    }

    @Test
    fun `a new date alone is enough to notify`() {
        // The stored value carries the date, so BCV re-publishing an unchanged
        // number under a new date still tells the user something.
        NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07")

        assertTrue(
            "the same number on a new date must not be suppressed",
            NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-08")
        )
    }

    @Test
    fun `each topic is gated independently`() {
        // Sharing one key across topics means whichever worker ran last silences
        // the others for the day. USDT and the next rate would simply never
        // appear, and the user would see an app that had stopped alerting.
        NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07")
        NotificationState.shouldNotifyNextRate(context, 871.37, "2026-10-08")

        assertFalse(
            "the next-rate topic must not inherit the daily topic's silence",
            NotificationState.shouldNotifyNextRate(context, 871.37, "2026-10-08")
        )
        assertFalse(
            "the daily topic must not inherit the next-rate topic's silence",
            NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07")
        )
        assertTrue(
            "the parallel market keeps its own topic, so the same value still fires there",
            NotificationState.shouldNotifyParallel(context, 866.56)
        )
    }

    @Test
    fun `the parallel gate keys on the rate alone`() {
        // No date is passed here, so an unchanged P2P mean must be silent even
        // though the day rolled over.
        assertTrue(NotificationState.shouldNotifyParallel(context, 889.5))
        assertFalse(NotificationState.shouldNotifyParallel(context, 889.5))
        assertTrue(NotificationState.shouldNotifyParallel(context, 892.75))
    }

    @Test
    fun `wasNotified only reports topics that were actually recorded`() {
        assertFalse(NotificationState.wasNotified(context, "daily_rate"))
        assertTrue(NotificationState.shouldNotifyDailyRate(context, 866.56, "2026-10-07"))
        assertTrue(NotificationState.wasNotified(context, "daily_rate"))
    }
}