package com.example.erp.data

import java.io.File

/**
 * The exact shape of a legacy history file, and nothing looser.
 *
 * Anchored at both ends and fixed at four digits on purpose. `rates-<year>.json`
 * is what [rateYearName] has always produced, and matching only that leaves a file
 * this app did not write strictly untouched — a loose `startsWith("rates")` would
 * take anything else stored beside it, and a stray temp file from a failed write
 * is not the same thing as a year of history.
 *
 * Lowercase only, because the writer only ever emitted lowercase, so an uppercase
 * variant is a file the app cannot account for.
 */
private val LEGACY_RATE_FILE = Regex("""^rates-\d{4}\.json$""")

/**
 * Deletes the legacy per-year history files from [dir] and returns the names it
 * actually removed.
 *
 * ## Why deleting this is safe
 *
 * These files are dead weight, not history. Every reader and writer moved to
 * `filesDir/rate_history/` in one move, so nothing opens them any more: the app,
 * the widget and the USDT worker each used a different path, which is exactly why
 * this directory ended up full of series no reader ever saw.
 *
 * Their contents are also fully reproducible. [FileHistoryStore.ensureSeeded] and
 * [FileHistoryStore.fetchAndPopulateHistorical] refill from the API, and the
 * backfill gate refetches precisely because the store can look empty after this
 * move. **Do not "protect" these files.** Keeping them costs every install ~174 KB
 * and buys nothing: no code path reads them, and a reader that started reading
 * them would be reintroducing the split that made the widget render an empty
 * sparkline on every phone.
 *
 * ## Why it is written this defensively
 *
 * [dir] is the app's private root, not a folder reserved for history: the
 * canonical history directory is a *subdirectory* of it, and the app stores other
 * things at that level too. So this deliberately:
 *
 * - matches only [LEGACY_RATE_FILE], never a prefix;
 * - refuses outright when [dir] IS the canonical directory, by name;
 * - does not recurse, so `rate_history/` and anything else below is unreachable;
 * - reports only deletions that happened, so a caller can log the truth.
 *
 * Each entry is checked with `isFile` first, so a *directory* that happens to be
 * named `rates-2026.json` is skipped rather than emptied — deleting a file this
 * app cannot account for is a far worse outcome than leaving bytes behind.
 *
 * Never throws: a missing directory lists as `null`, and a file it cannot unlink
 * is simply not reported. A stale file is not worth a crash on launch.
 *
 * Idempotent, so startup can call it unconditionally without tracking state.
 */
fun deleteLegacyHistoryFiles(dir: File): List<String> {
    // The one call that must never be wrong. Handed the canonical directory — by a
    // caller that passed the path one level too low — this function would delete
    // the live history, which is the exact opposite of its job. Recognised by name,
    // checked before anything is listed, and independent of how the caller spelled
    // the path.
    if (dir.name == RATE_HISTORY_DIR_NAME) return emptyList()
    return (dir.listFiles() ?: emptyArray())
        .filter { LEGACY_RATE_FILE.matches(it.name) && it.isFile && it.delete() }
        .map { it.name }
        .sorted()
}