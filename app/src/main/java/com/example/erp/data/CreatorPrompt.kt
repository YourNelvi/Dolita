package com.example.erp.data

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * When the "Apoya al creador de Dolita" invitation may be shown.
 *
 * Two rules, and only two:
 *  1. the app must have been used for more than one day, so a brand-new
 *     install is never interrupted by an invitation;
 *  2. it shows on the FIRST opening of the day, and never again that day.
 *
 * The rule is a pure function on purpose: "more than one day" and "once a
 * day" are the two properties that can silently break — a reinstall that
 * forgets the first-launch day, a counter that survives the day boundary —
 * and both are worth pinning with tests instead of trusting a
 * SharedPreferences dance.
 *
 * @param firstLaunchDay epoch day of the first launch, negative when the app
 *   has never been opened.
 * @param lastShownDay epoch day the invitation was last shown, negative if never.
 * @param today epoch day of the current opening.
 */
internal fun shouldShowCreatorPrompt(
    firstLaunchDay: Long,
    lastShownDay: Long,
    today: Long
): Boolean {
    // Not the first day of use: a brand-new user has not seen the app long
    // enough for an invitation to make sense.
    if (firstLaunchDay < 0 || today <= firstLaunchDay) return false
    // Already shown today. Once a day means once a day.
    if (today == lastShownDay) return false
    return true
}

/**
 * Persists the two dates the rule needs, plus the counter of today's
 * openings.
 *
 * The first-launch day is written on the first opening, so it survives
 * reinstalls only for as long as the preferences do; that is acceptable,
 * because a user who reinstalls is a new user as far as this invitation is
 * concerned.
 */
class CreatorPromptStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val appContext = context.applicationContext

    /**
     * The day the reader started using the app.
     *
     * The install date of the package is the honest answer for everyone who
     * was here before this feature existed: counting from the first opening
     * after the update would make the invitation start a day late for every
     * existing user, and a user who has been here for months is exactly the
     * one who should be invited. The stored value only matters for installs
     * where the package date is not available.
     */
    private fun firstUseDay(): Long {
        val stored = prefs.getLong(KEY_FIRST_LAUNCH, -1L)
        val installed = installDay()
        return when {
            stored < 0L -> installed
            installed < 0L -> stored
            else -> minOf(stored, installed)
        }
    }

    @Suppress("DEPRECATION")
    private fun installDay(): Long {
        val installedAt = runCatching {
            appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .firstInstallTime
        }.getOrNull() ?: return -1L
        return Instant.ofEpochMilli(installedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()
    }

    /**
     * Records an opening of the app and returns how many it is today.
     *
     * The counter belongs to the calendar day, so opening the app five times
     * today and once tomorrow reads as 5 and then 1. The number itself is not
     * what decides the invitation any more; what matters is that it changes
     * on every opening, which is what makes the screen re-check the rule.
     */
    fun noteOpen(): Int {
        val today = LocalDate.now().toEpochDay()
        if (prefs.getLong(KEY_FIRST_LAUNCH, -1L) < 0L) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, today).apply()
        }
        val storedDay = prefs.getLong(KEY_OPEN_DAY, -1L)
        val count = if (storedDay == today) prefs.getInt(KEY_OPEN_COUNT, 0) else 0
        val openNumber = count + 1
        prefs.edit()
            .putLong(KEY_OPEN_DAY, today)
            .putInt(KEY_OPEN_COUNT, openNumber)
            .apply()
        return openNumber
    }

    /**
     * Whether this opening is the one that carries the invitation.
     *
     * Does NOT record that the invitation was shown: the caller marks it only
     * when the invitation actually reaches the screen, so an opening that is
     * interrupted before that does not burn the day.
     */
    fun shouldShowNow(): Boolean = shouldShowCreatorPrompt(
        firstLaunchDay = firstUseDay(),
        lastShownDay = prefs.getLong(KEY_LAST_SHOWN, -1L),
        today = LocalDate.now().toEpochDay()
    )

    /** Records that the invitation was shown today. */
    fun markShownToday() {
        prefs.edit().putLong(KEY_LAST_SHOWN, LocalDate.now().toEpochDay()).apply()
    }

    companion object {
        private const val PREFS_NAME = "creator_prompt_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch_day"
        private const val KEY_LAST_SHOWN = "last_shown_day"
        private const val KEY_OPEN_DAY = "open_day"
        private const val KEY_OPEN_COUNT = "open_count"
    }
}
