package com.example.erp.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CachedDolarRepo"

class CachedDolarRepository(
    private val apiRepository: ApiDolarRepository,
    private val context: Context
) : DolarRepository {

    override suspend fun getQuotes(): List<DolarQuote> = withContext(Dispatchers.IO) {
        // Intentar obtener de la API primero
        return@withContext try {
            val quotes = apiRepository.getQuotes()
            if (quotes.isNotEmpty()) {
                // Guardar en cache si la API responde bien
                QuotesCache.save(quotes, context)
                Log.d(TAG, "API success, cached ${quotes.size} quotes")
            }
            quotes
        } catch (e: Exception) {
            Log.w(TAG, "API failed, trying cache: ${e.message}")
            // Si falla la API, intentar cache
            val cached = QuotesCache.getCached(context)
            if (cached != null) {
                Log.d(TAG, "Returning ${cached.quotes.size} cached quotes (age: ${(System.currentTimeMillis() - cached.timestamp) / 1000 / 60} min)")
                cached.quotes
            } else {
                Log.e(TAG, "No cached quotes available")
                throw e // Re-lanzar la excepción original si no hay cache
            }
        }
    }
}