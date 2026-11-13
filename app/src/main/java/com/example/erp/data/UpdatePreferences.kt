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
    /**
     * The storage key stays "daily" even though the window is no longer a
     * day: changing it would reset every existing install to the default and
     * silently undo the choice the user already made.
     */
    FREQUENT("daily"),
    WEEKLY("weekly"),
    NEVER("never");

    /**
     * Milliseconds between automatic checks, or null when the app should never
     * check without being asked.
     *
     * The frequent window is two hours rather than a day because a check that
     * finds nothing used to burn the whole day: a release published after the
     * user's last look stayed invisible until the next morning. Two hours is
     * the floor that keeps the traffic sane; the request itself is a
     * conditional one that GitHub does not charge for when nothing changed.
     */
    val intervalMs: Long?
        get() = when (this) {
            FREQUENT -> 2L * 60L * 60L * 1000L
            WEEKLY -> 7L * 24L * 60L * 60L * 1000L
            NEVER -> null
        }

    /** The value shown at the end of the settings row. */
    val label: String
        get() = when (this) {
            FREQUENT -> "Cada 2 horas"
            WEEKLY -> "Cada semana"
            NEVER -> "Solo manual"
        }

    /** What the choice means, in one line, under the row title. */
    val description: String
        get() = when (this) {
            FREQUENT -> "Al abrir la app, con un mínimo de 2 horas entre búsquedas. " +
                "Solo pregunta si hay algo nuevo."
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
            entries.firstOrNull { it.storageKey == value } ?: FREQUENT
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

    /**
     * The release the reader dismissed with "Más tarde".
     *
     * Without this, a check that runs on every launch would greet the reader
     * with the same dialog every time they open the app until they update.
     * A newer version clears it: the notice is about that version.
     */
    var dismissedVersion: String?
        get() = preferences.getString(KEY_DISMISSED_VERSION, null)
        set(value) {
            preferences.edit().putString(KEY_DISMISSED_VERSION, value).apply()
        }

    private companion object {
        const val PREFS_NAME = "update_check_prefs"
        const val KEY_LAST_CHECK = "last_successful_check_epoch_ms"
        const val KEY_INTERVAL = "check_interval"
        const val KEY_DISMISSED_VERSION = "dismissed_version"
    }
}
