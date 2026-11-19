package com.example.erp.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.erp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Looks for a published release while the app is closed.
 *
 * The check itself is the cheap one: the releases page answers with a redirect
 * and does not spend the API quota that every Dolita reader behind the same
 * carrier shares. The API is not consulted at all here — the worker only
 * remembers WHICH version is published.
 *
 * The reader is not notified. The notice belongs to the home screen: when the
 * app opens, it offers the update it already knows about. A notification would
 * interrupt a reader who did not ask for anything, and the app is the place
 * where the update can actually be explained and installed.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val preferences = UpdatePreferences(applicationContext)

        // A reader who chose "only manual" does not want the app talking to
        // GitHub behind their back, and that choice governs this worker too.
        if (preferences.interval == UpdateCheckInterval.NEVER) {
            Log.d(TAG, "Manual-only reader; skipping the background check")
            return@withContext Result.success()
        }

        // The same window the launch check uses, and the same timestamp: a
        // weekly reader is checked once a week, not once every two hours, and
        // a check the app already made a moment ago is not repeated.
        val now = System.currentTimeMillis()
        val due = shouldCheckForUpdate(
            lastCheckEpochMs = preferences.lastSuccessfulCheckEpochMs,
            nowEpochMs = now,
            force = false,
            interval = preferences.interval
        )
        if (!due) {
            Log.d(TAG, "Not due yet; leaving the window to the app")
            return@withContext Result.success()
        }

        return@withContext try {
            val published = UpdateChecker(applicationContext).latestPublishedVersion()

            // A successful answer is a successful check, even when there is
            // nothing new: the timestamp is what keeps the app from asking
            // again the moment it opens.
            preferences.lastSuccessfulCheckEpochMs = now

            if (published.isBlank()) {
                Log.d(TAG, "No release published to look at")
                return@withContext Result.success()
            }

            if (!isNewerVersion(published, BuildConfig.VERSION_NAME)) {
                Log.d(TAG, "Nothing newer than the installed build ($published)")
                return@withContext Result.success()
            }

            // Remember it: the home screen offers it as soon as the app opens.
            preferences.knownNewerVersion = published
            Log.d(TAG, "Release $published is published; the home will offer it")
            Result.success()
        } catch (e: Exception) {
            // A network hiccup must not cancel the schedule: WorkManager
            // retries with its own backoff.
            Log.w(TAG, "Update check failed: ${e.message}")
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "UpdateCheckWorker"
    }
}
