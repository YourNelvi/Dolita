package com.example.erp.ui

/**
 * The version line at the foot of the settings sheet.
 *
 * A plain function instead of an inlined string for two reasons: the format
 * lives in one place the whole app can agree on, and a test can pin it. The
 * version a reader reports in a bug is only useful if it is the version the app
 * actually installed.
 *
 * A blank name yields null rather than an empty row. "Versión " with nothing
 * after it tells the reader nothing and looks like a layout bug.
 */
internal fun versionLabel(versionName: String): String? {
    val trimmed = versionName.trim()
    return if (trimmed.isEmpty()) null else "Versión $trimmed"
}
