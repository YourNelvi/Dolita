package com.example.erp.data

import android.content.Context

/**
 * How often the app may ask GitHub for a new release on its own.
 *
 * This never gates the manual button: a check the user asks for always runs.
 * It only decides how much time has to pass before an app launch is allowed to
 * check behind their back.
 */
enum class UpdateCheckInterval(val storageKey: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    NEVER("never");

    /**
     * Milliseconds between automatic checks, or null when the app should never
     * check without being asked.
     */
    val intervalMs: Long?
        get() = when (this) {
            DAILY -> 24L * 60L * 60L * 1000L
            WEEKLY -> 7L * 24L * 60L * 60L * 1000L
            NEVER -> null
        }

    /** The value shown at the end of the settings row. */
    val label: String
        get() = when (this) {
            DAILY -> "Cada día"
            WEEKLY -> "Cada semana"
            NEVER -> "Solo manual"
        }

    /** What the choice means, in one line, under the row title. */
    val description: String
        get() = when (this) {
            DAILY -> "Al abrir la app, una vez por día"
            WEEKLY -> "Al abrir la app, una vez por semana"
            NEVER -> "Solo cuando tocás «Buscar actualización»"
        }

    companion object {
        /**
         * Turns a stored value back into an interval.
         *
         * A missing or unknown value means the default, never "never": an
         * unreadable preference must not silently turn update checks off.
         */
        fun fromStorage(value: String?): UpdateCheckInterval =
            entries.firstOrNull { it.storageKey == value } ?: DAILY
    }
}

/**
 * The update check's persisted state.
 *
 * The timestamp key predates this class and is kept verbatim: changing it would
 * make every existing install look like it never checked and fire a request on
 * the very next launch.
 */
class UpdatePreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var interval: UpdateCheckInterval
        get() = UpdateCheckInterval.fromStorage(preferences.getString(KEY_INTERVAL, null))
        set(value) {
            preferences.edit().putString(KEY_INTERVAL, value.storageKey).apply()
        }

    var lastSuccessfulCheckEpochMs: Long
        get() = preferences.getLong(KEY_LAST_CHECK, 0L)
        set(value) {
            preferences.edit().putLong(KEY_LAST_CHECK, value).apply()
        }

    private companion object {
        const val PREFS_NAME = "update_check_prefs"
        const val KEY_LAST_CHECK = "last_successful_check_epoch_ms"
        const val KEY_INTERVAL = "check_interval"
    }
}
