package com.example.erp.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CachedDolarRepo"

/**
 * Network-first with a write-through cache.
 *
 * There is deliberately NO silent cache fallback here. Serving stored numbers
 * when the fetch failed, indistinguishably from a live result, is what made a
 * "Sin conexión" notice impossible to render: by the time the UI saw the
 * quotes, the only fact that mattered — these are old — had already been
 * discarded. Deciding whether a snapshot is fit to show, and marking it stale
 * when it is not, belongs to the ViewModel that owns that state. Callers that
 * genuinely want best-effort values read [QuotesCache] themselves; that is what
 * the widget does.
 */
class CachedDolarRepository(
    private val apiRepository: ApiDolarRepository,
    private val context: Context
) : DolarRepository {

    override suspend fun getQuotes(): List<DolarQuote> = withContext(Dispatchers.IO) {
        val quotes = apiRepository.getQuotes()
        if (quotes.isNotEmpty()) {
            QuotesCache.save(quotes, context)
            Log.d(TAG, "API success, cached ${quotes.size} quotes")
        }
        quotes
    }

    /**
     * Refreshes only USDT and writes it through the cache. Null means the
     * request failed; the caller keeps the quote already on screen instead of
     * blanking a market because one button press could not reach Binance.
     */
    override suspend fun refreshUsdt(): DolarQuote? = withContext(Dispatchers.IO) {
        runCatching { apiRepository.fetchUsdtQuote() }
            .onSuccess { quote ->
                QuotesCache.upsert(context, quote)
                Log.d(TAG, "USDT refreshed at ${quote.fechaActualizacion}")
            }
            .onFailure { error ->
                Log.w(TAG, "USDT refresh failed: ${error.message}")
            }
            .getOrNull()
    }
}