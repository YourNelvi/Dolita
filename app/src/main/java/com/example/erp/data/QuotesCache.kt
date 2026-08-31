package com.example.erp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "quotes_cache"
private const val KEY_QUOTES_JSON = "cached_quotes_json"
private const val KEY_TIMESTAMP = "cached_quotes_timestamp"
private const val KEY_VERSION = "cache_version"
private const val CURRENT_VERSION = 1
private const val EXPIRY_HOURS = 12L

@Serializable
data class CachedQuotes(
    val quotes: List<DolarQuote>,
    val timestamp: Long
)

object QuotesCache {
    private val json = Json { ignoreUnknownKeys = true }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun save(quotes: List<DolarQuote>, context: Context) = withContext(Dispatchers.IO) {
        val cached = CachedQuotes(quotes, System.currentTimeMillis())
        val jsonString = json.encodeToString(cached)
        getPrefs(context).edit()
            .putString(KEY_QUOTES_JSON, jsonString)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_VERSION, CURRENT_VERSION)
            .apply()
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

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        getPrefs(context).edit()
            .remove(KEY_QUOTES_JSON)
            .remove(KEY_TIMESTAMP)
            .remove(KEY_VERSION)
            .apply()
    }
}