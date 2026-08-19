package com.example.erp.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

private const val TAG = "DolarAPI"

private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

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

    private fun fetchBcv(): List<DolarQuote> {
        Log.d(TAG, "Requesting $BCV_URL")
        return client.newCall(buildGet(BCV_URL)).execute().use { response ->
            val code = response.code
            Log.d(TAG, "HTTP $code")
            val body = response.body?.string().orEmpty()
            Log.d(TAG, "Body: ${body.take(200)}")
            if (code != 200) throw IOException("HTTP $code")
            parseBcv(body)
        }
    }

    private fun fetchUsdt(): DolarQuote {
        Log.d(TAG, "Requesting Binance P2P USDT/VES")
        val request = buildGet(BINANCE_URL)
            .newBuilder()
            .post(BINANCE_BODY.toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .build()
        return client.newCall(request).execute().use { response ->
            val code = response.code
            Log.d(TAG, "Binance HTTP $code")
            val body = response.body?.string().orEmpty()
            if (code != 200) throw IOException("Binance HTTP $code")
            parseUsdt(body)
        }
    }

    private fun buildGet(url: String): Request =
        Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

    private fun parseBcv(body: String): List<DolarQuote> {
        val root = JSONObject(body)
        val current = root.getJSONObject("current")
        val previous = root.optJSONObject("previous")
        val change = root.optJSONObject("changePercentage")
        val date = current.optString("date", "")
        return listOf(
            DolarQuote(
                fuente = "usd",
                nombre = "Dólar (BCV)",
                promedio = current.optDouble("usd", 0.0),
                anterior = previous?.optDouble("usd"),
                variacion = change?.optDouble("usd"),
                fechaActualizacion = date
            ),
            DolarQuote(
                fuente = "eur",
                nombre = "Euro (BCV)",
                promedio = current.optDouble("eur", 0.0),
                anterior = previous?.optDouble("eur"),
                variacion = change?.optDouble("eur"),
                fechaActualizacion = date
            )
        )
    }

    private fun parseUsdt(body: String): DolarQuote {
        val root = JSONObject(body)
        val data = root.getJSONArray("data")
        var sum = 0.0
        var count = 0
        for (i in 0 until data.length()) {
            val adv = data.getJSONObject(i).optJSONObject("adv") ?: continue
            val price = adv.optString("price").toDoubleOrNull() ?: continue
            sum += price
            count++
        }
        if (count == 0) throw IOException("Binance: no offers")
        return DolarQuote(
            fuente = "usdt",
            nombre = "USDT (P2P)",
            promedio = sum / count,
            anterior = null,
            variacion = null,
            fechaActualizacion = OffsetDateTime.now().toString()
        )
    }

    companion object {
        private const val BCV_URL = "https://rates.dolarvzla.com/bcv/current.json"
        private const val BINANCE_URL = "https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search"
        private const val BINANCE_BODY =
            """{"asset":"USDT","fiat":"VES","merchantCheck":false,"page":1,"rows":10,"payTypes":[],"publisherType":null,"tradeType":"BUY"}"""
    }
}