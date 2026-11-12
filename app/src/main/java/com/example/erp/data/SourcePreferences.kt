package com.example.erp.data

import android.content.Context

/** The stored ids of every source the app can open on. */
private val FAVORITE_SOURCES = setOf("usd", "eur", "usdt")

private const val DEFAULT_FAVORITE_SOURCE = "usd"

/**
 * The source the app opens on.
 *
 * Only the initial selection reads this: tapping a source chip changes what is
 * on screen right now, not the currency the next launch starts from. Stored on
 * the device, in plain SharedPreferences, because it is a display preference and
 * nothing else.
 */
class SourcePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * The canonical source id the next launch starts with.
     *
     * Reads are normalized: a value written by an older build, hand-edited, or
     * simply absent falls back to the dollar instead of leaving the screen with
     * no tab selected. Writes store the normalized value for the same reason.
     */
    var favoriteSource: String
        get() = normalizeFavoriteSource(prefs.getString(KEY_FAVORITE_SOURCE, null))
        set(value) {
            prefs.edit().putString(KEY_FAVORITE_SOURCE, normalizeFavoriteSource(value)).apply()
        }

    private companion object {
        const val PREFS_NAME = "source_prefs"
        const val KEY_FAVORITE_SOURCE = "favorite_source"
    }
}

/**
 * Maps stored text onto one of the sources the app can open on.
 *
 * The dollar is the default because it is the app's primary rate and the one
 * every snapshot carries; returning an unknown id would hand the UI a tab that
 * does not exist. Matching is case-insensitive and ignores surrounding spaces so
 * a value that only differs cosmetically is not thrown away.
 */
internal fun normalizeFavoriteSource(raw: String?): String {
    val candidate = raw?.trim()?.lowercase()
    return candidate?.takeIf { it in FAVORITE_SOURCES } ?: DEFAULT_FAVORITE_SOURCE
}
