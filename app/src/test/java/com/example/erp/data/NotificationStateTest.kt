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

/**
 * The minimum [SharedPreferences] the gate touches, backed by a map.
 *
 * Implementations of the platform interface rather than a mocking framework: the
 * gate reads one string key and writes one string key, and a test dependency
 * that does exactly that is not worth a new library.
 */
private class FakeSharedPreferences : SharedPreferences {

    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun putStringSet(
            key: String?,
            value: MutableSet<String>?
        ): SharedPreferences.Editor = apply { pending[key.orEmpty()] = value }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            removals += key.orEmpty()
        }

        override fun clear(): SharedPreferences.Editor = apply { clearAll = true }

        override fun commit(): Boolean {
            flush()
            return true
        }

        override fun apply() = flush()

        private fun flush() {
            if (clearAll) values.clear()
            removals.forEach { values.remove(it) }
            values.putAll(pending)
        }
    }
}