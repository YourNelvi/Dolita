package com.example.erp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "quotes_cache"
private const val KEY_QUOTES_JSON = "cached_quotes_json"
private const val KEY_TIMESTAMP = "cached_quotes_timestamp"
private const val KEY_VERSION = "cache_version"

/**
 * Version 2 stores a timestamp per source. Version 1 kept one global write
 * time, which made a recent USDT sample vouch for a stale BCV rate; bumping the
 * version drops those snapshots instead of trusting the wrong ones.
 */
private const val CURRENT_VERSION = 2

private const val EXPIRY_HOURS = 12L

@Serializable
data class CachedQuotes(
    val quotes: List<DolarQuote>,
    /** Last write to the snapshot as a whole; drives the stale UI notice. */
    val timestamp: Long,
    /** Last successful fetch per [DolarQuote.fuente], e.g. "usd" to epoch millis. */
    val sourceTimestamps: Map<String, Long> = emptyMap()
)

object QuotesCache {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Serializes the read-merge-write in [upsert]. The BCV worker, the USDT
     * worker and an app-open fetch can overlap; without this, one of them could
     * read the snapshot before another wrote it and drop the other's update.
     * SharedPreferences is thread-safe per operation, not for a whole
     * read-modify-write.
     */
    private val cacheMutex = Mutex()

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun save(quotes: List<DolarQuote>, context: Context) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val now = System.currentTimeMillis()
            val cached = CachedQuotes(
                quotes = quotes,
                timestamp = now,
                sourceTimestamps = quotes.associate { it.fuente to now }
            )
            write(getPrefs(context), cached)
        }
    }

    fun getCachedFlow(context: Context): Flow<CachedQuotes?> {
        val prefs = getPrefs(context)
        val flow = MutableStateFlow<CachedQuotes?>(readCachedSync(prefs))
        return flow.asStateFlow()
    }

    private fun readCachedSync(prefs: SharedPreferences): CachedQuotes? {
        val version = prefs.getInt(KEY_VERSION, 0)
        if (version != CURRENT_VERSION) return null

        val jsonString = prefs.getString(KEY_QUOTES_JSON, "") ?: ""
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        if (jsonString.isEmpty() || timestamp == 0L) return null

        val now = System.currentTimeMillis()
        val ageHours = (now - timestamp) / (1000 * 60 * 60)
        if (ageHours > EXPIRY_HOURS) return null

        return try {
            json.decodeFromString<CachedQuotes>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCached(context: Context): CachedQuotes? = withContext(Dispatchers.IO) {
        readCachedSync(getPrefs(context))
    }

    /**
     * Replaces the entry for [quote]'s source and leaves every other source
     * untouched. `save` overwrites the whole snapshot, so a worker that
     * refreshed only one market would otherwise erase the others.
     *
     * An existing source is replaced IN PLACE. Rebuilding the list as
     * `filterNot { it } + quote` moved the refreshed source to the end, which
     * made the stored order depend on whichever worker wrote last — and the
     * tabs in the UI are rendered in stored order, so the source selector used
     * to rearrange itself between runs.
     *
     * Only the refreshed source's timestamp moves. Writing the whole snapshot
     * through [save] used to bump every source's age, which is how a minutely
     * USDT sample kept hiding a BCV rate that had been superseded hours
     * earlier.
     */
    suspend fun upsert(context: Context, quote: DolarQuote) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val now = System.currentTimeMillis()
            val existing = readCachedSync(getPrefs(context))
            val current = existing?.quotes.orEmpty()
            val index = current.indexOfFirst { it.fuente == quote.fuente }
            val merged = if (index >= 0) {
                current.toMutableList().apply { this[index] = quote }
            } else {
                current + quote
            }
            val cached = CachedQuotes(
                quotes = merged,
                timestamp = now,
                sourceTimestamps = existing?.sourceTimestamps.orEmpty() + (quote.fuente to now)
            )
            write(getPrefs(context), cached)
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            getPrefs(context).edit()
                .remove(KEY_QUOTES_JSON)
                .remove(KEY_TIMESTAMP)
                .remove(KEY_VERSION)
                .apply()
        }
    }

    private fun write(prefs: SharedPreferences, cached: CachedQuotes) {
        prefs.edit()
            .putString(KEY_QUOTES_JSON, json.encodeToString(cached))
            .putLong(KEY_TIMESTAMP, cached.timestamp)
            .putInt(KEY_VERSION, CURRENT_VERSION)
            .apply()
    }
}
