package com.example.erp.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.OffsetDateTime

private const val TAG = "DolarAPI"

class ApiDolarRepository : DolarRepository {

    override suspend fun getQuotes(): List<DolarQuote> = withContext(Dispatchers.IO) {
        val bcv = fetchBcv()
        val usdt = try {
            fetchUsdt()
        } catch (e: Exception) {
            Log.w(TAG, "USDT failed: ${e.message}")
            null
        }
        buildList {
            addAll(bcv)
            usdt?.let(::add)
        }
    }

    /**
     * BCV only: ONE request returns both USD and EUR, so a caller that only
     * needs the official rate never pays for a second call.
     */
    suspend fun fetchBcvQuotes(): List<DolarQuote> = withContext(Dispatchers.IO) { fetchBcv() }

    /**
     * Parallel market only (Binance P2P USDT/VES). Kept separate so a caller
     * showing the official rate does not trigger this request at all.
     */
    suspend fun fetchUsdtQuote(): DolarQuote = withContext(Dispatchers.IO) { fetchUsdt() }

    override suspend fun refreshUsdt(): DolarQuote? = runCatching {
        fetchUsdtQuote()
    }.onFailure { error ->
        Log.w(TAG, "USDT refresh failed: ${error.message}")
    }.getOrNull()

    private fun fetchBcv(): List<DolarQuote> {
        Log.d(TAG, "Requesting $BCV_URL")
        return sharedHttpClient.newCall(buildGet(BCV_URL)).execute().use { response ->
            val code = response.code
            Log.d(TAG, "HTTP $code")
            val body = response.body?.string().orEmpty()
            Log.d(TAG, "Body: ${body.take(200)}")
            if (code != 200) throw IOException("HTTP $code")
            parseBcvPayload(body)
        }
    }

    /**
     * Binance exposes one side per request: `BUY` is the price to buy USDT and
     * `SELL` the price to sell it. Both run together so the spread belongs to
     * the same moment, and one failing side never takes the other down.
     */
    private suspend fun fetchUsdt(): DolarQuote = coroutineScope {
        val compra = async(Dispatchers.IO) { fetchUsdtSide("BUY") }
        val venta = async(Dispatchers.IO) { fetchUsdtSide("SELL") }
        buildUsdtQuote(compra.await(), venta.await())
            ?: throw IOException("Binance: no offers")
    }

    private fun fetchUsdtSide(tradeType: String): Double? {
        Log.d(TAG, "Requesting Binance P2P USDT/VES $tradeType")
        val request = buildGet(BINANCE_URL)
            .newBuilder()
            .post(
                BINANCE_BODY_TEMPLATE.replace(TRADE_TYPE_TOKEN, tradeType)
                    .toRequestBody("application/json".toMediaType())
            )
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .build()
        return runCatching {
            sharedHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                Log.d(TAG, "Binance $tradeType HTTP $code")
                val body = response.body?.string().orEmpty()
                if (code != 200) throw IOException("Binance HTTP $code")
                parseUsdtPrice(body) ?: throw IOException("Binance: no offers")
            }
        }.onFailure { error ->
            Log.w(TAG, "Binance $tradeType failed: ${error.message}")
        }.getOrNull()
    }

    private fun buildGet(url: String): Request =
        Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

    companion object {
        private const val BCV_URL = "https://rates.dolarvzla.com/bcv/current.json"
        private const val BINANCE_URL = "https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search"
        private const val TRADE_TYPE_TOKEN = "__TRADE_TYPE__"
        private const val BINANCE_BODY_TEMPLATE =
            """{"asset":"USDT","fiat":"VES","merchantCheck":false,"page":1,"rows":20,"payTypes":[],"publisherType":null,"tradeType":"__TRADE_TYPE__"}"""
    }
}

/**
 * Deterministic parser for one side of Binance P2P.
 *
 * The endpoint returns an array of advertisements. A plain mean is dragged by a
 * single bait/outlier ad (one 1.115,80 buyer can push the sell side above the
 * buy side), so the side uses the median instead: the typical offer, robust to
 * one extreme. Invalid JSON, a missing `data` array and an empty page all mean
 * "no usable offers" -> null, never 0.0.
 */
internal fun parseUsdtPrice(body: String): Double? {
    return runCatching {
        val data = JSONObject(body).optJSONArray("data") ?: return null
        val prices = buildList {
            for (i in 0 until data.length()) {
                val adv = data.optJSONObject(i)?.optJSONObject("adv") ?: continue
                val price = adv.optString("price").toDoubleOrNull() ?: continue
                if (price > 0.0) add(price)
            }
        }.sorted()
        if (prices.isEmpty()) return null
        val middle = prices.size / 2
        if (prices.size % 2 == 1) {
            prices[middle]
        } else {
            (prices[middle - 1] + prices[middle]) / 2.0
        }
    }.getOrNull()
}

/**
 * Assembles the parallel quote from the two sides.
 *
 * `promedio` stays the canonical number the hero and the history use: the mean
 * of whichever sides answered. A single side is still a usable quote, and a
 * night where only one request survived must not hide USDT entirely.
 */
internal fun buildUsdtQuote(
    compra: Double?,
    venta: Double?,
    updatedAt: OffsetDateTime = OffsetDateTime.now()
): DolarQuote? {
    val prices = listOfNotNull(compra, venta)
    if (prices.isEmpty()) return null
    return DolarQuote(
        fuente = "usdt",
        nombre = "USDT (P2P)",
        promedio = prices.average(),
        anterior = null,
        variacion = null,
        fechaActualizacion = updatedAt.toString(),
        compra = compra,
        venta = venta
    )
}

/**
 * Turns one BCV payload into both official quotes.
 *
 * internal, not private, and top-level rather than a member: this is the whole
 * risk of talking to the BCV endpoint — the shape is owned by the endpoint, not by
 * this app, and the app shows zeros when it is wrong — and as a `private` method
 * behind `fetchBcv()` it was only reachable through a live HTTP request, so no
 * JVM test could reach it. [BcvPayloadParsingTest] pins the real shape against
 * this exact function, not a copy of it.
 *
 * `current` is required and everything else is optional, because that is what the
 * endpoint actually sends: `previous` and `changePercentage` are absent before a
 * change has been published. A missing optional block yields null rather than
 * 0.0, since a zero would read as "the rate collapsed" instead of "unknown".
 */
internal fun parseBcvPayload(body: String): List<DolarQuote> {
    val root = JSONObject(body)
    val current = root.getJSONObject("current")
    val previous = root.optJSONObject("previous")
    val change = root.optJSONObject("changePercentage")
    val date = current.optString("date", "")
    val previousDate = previous?.optString("date")
    return listOf(
        DolarQuote(
            fuente = "usd",
            nombre = "Dólar (BCV)",
            promedio = current.optDouble("usd", 0.0),
            anterior = previous?.optDouble("usd"),
            variacion = change?.optDouble("usd"),
            fechaActualizacion = date,
            fechaAnterior = previousDate
        ),
        DolarQuote(
            fuente = "eur",
            nombre = "Euro (BCV)",
            promedio = current.optDouble("eur", 0.0),
            anterior = previous?.optDouble("eur"),
            variacion = change?.optDouble("eur"),
            fechaActualizacion = date,
            fechaAnterior = previousDate
        )
    )
}