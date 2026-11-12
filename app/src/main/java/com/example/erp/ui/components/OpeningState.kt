package com.example.erp.ui.components

/**
 * Decides whether the opening animation still owns the screen.
 *
 * The animation exists to cover the first load, not to delay a warm start:
 * - a screen that already has data never shows it;
 * - a cold start holds it for [minElapsed] even when the data lands at once,
 *   so the brand does not flash for a single frame;
 * - once [maxElapsed] is reached the skeleton takes over, so a slow network
 *   still gets an honest loading state instead of an endless splash.
 */
internal fun openingVisible(
    coldStart: Boolean,
    loading: Boolean,
    hasQuotes: Boolean,
    minElapsed: Boolean,
    maxElapsed: Boolean
): Boolean {
    if (!coldStart || maxElapsed) return false
    val waitingForFirstPaint = loading && !hasQuotes
    return waitingForFirstPaint || !minElapsed
}
