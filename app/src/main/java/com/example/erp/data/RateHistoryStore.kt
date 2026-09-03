package com.example.erp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Persists [RateSample]s into per-year JSON files (`rates-YYYY.json`).
 * Appends are serialized and written atomically (tmp file + rename).
 */
interface RateHistoryStore {
    suspend fun append(samples: List<RateSample>)
    /** Samples of the current calendar year sorted by timestamp; corrupt/absent file -> emptyList. */
    suspend fun readCurrentYear(): List<RateSample>
    /** Ensures the current-year file exists and is seeded with historical data if empty. */
    suspend fun ensureSeeded()
    /** Fetches historical data from API and populates the store (one-time). */
    suspend fun fetchAndPopulateHistorical()
    /** Returns true if the store has any real data (not just empty). */
    suspend fun hasData(): Boolean
}

/**
 * File-backed [RateHistoryStore]. A [Mutex] serializes the whole
 * read-modify-write cycle so concurrent appends never interleave.
 */
class FileHistoryStore(
    private val dir: File,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : RateHistoryStore {

    private val mutex = Mutex()

    override suspend fun append(samples: List<RateSample>) {
        if (samples.isEmpty()) return
        mutex.withLock {
            withContext(Dispatchers.IO) {
                samples.groupBy { rateYearName(it.timestampEpochMillis, zoneId) }
                    .forEach { (fileName, yearSamples) ->
                        val target = File(dir, fileName)
                        val existing = readSamples(target)
                        val filtered = existing.filter { old ->
                            yearSamples.none { new ->
                                new.fuente == old.fuente &&
                                localDateOf(new.timestampEpochMillis, zoneId) == localDateOf(old.timestampEpochMillis, zoneId)
                            }
                        }
                        val merged = (filtered + yearSamples)
                            .sortedBy { it.timestampEpochMillis }
                        writeAtomically(target, RateHistoryCodec.encode(merged))
                    }
            }
        }
    }

    override suspend fun readCurrentYear(): List<RateSample> = mutex.withLock {
        withContext(Dispatchers.IO) {
            readSamples(File(dir, rateYearName(System.currentTimeMillis(), zoneId)))
                .sortedBy { it.timestampEpochMillis }
        }
    }

    override suspend fun ensureSeeded() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val currentYearFile = File(dir, rateYearName(System.currentTimeMillis(), zoneId))
            if (!currentYearFile.exists() || currentYearFile.length() == 0L) {
                // No fake seed — leave empty until real data arrives
            }
        }
    }

    override suspend fun hasData(): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            val currentYearFile = File(dir, rateYearName(System.currentTimeMillis(), zoneId))
            currentYearFile.exists() && currentYearFile.length() > 0L
        }
    }

    override suspend fun fetchAndPopulateHistorical() {
        // Fetch from ve.dolarapi.com and populate
        val historicalData = try {
            fetchHistoricalFromApi()
        } catch (e: Exception) {
            android.util.Log.w("RateHistoryStore", "Historical fetch failed: ${e.message}")
            return
        }
        if (historicalData.isEmpty()) return

        mutex.withLock {
            withContext(Dispatchers.IO) {
                historicalData.groupBy { rateYearName(it.timestampEpochMillis, zoneId) }
                    .forEach { (fileName, yearSamples) ->
                        val target = File(dir, fileName)
                        val existing = readSamples(target)
                        val filtered = existing.filter { old ->
                            yearSamples.none { new ->
                                new.fuente == old.fuente &&
                                localDateOf(new.timestampEpochMillis, zoneId) == localDateOf(old.timestampEpochMillis, zoneId)
                            }
                        }
                        val merged = (filtered + yearSamples)
                            .sortedBy { it.timestampEpochMillis }
                        writeAtomically(target, RateHistoryCodec.encode(merged))
                    }
            }
        }
    }

    /**
     * Fetches historical USD rates from ve.dolarapi.com API.
     * Returns list of RateSample with real BCV data.
     */
    private fun fetchHistoricalFromApi(): List<RateSample> {
        val url = "https://ve.dolarapi.com/v1/historicos/dolares/oficial"
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string().orEmpty()

        if (response.code != 200) {
            throw Exception("HTTP ${response.code}")
        }

        val samples = mutableListOf<RateSample>()
        val root = org.json.JSONArray(body)

        for (i in 0 until root.length()) {
            val item = root.optJSONObject(i) ?: continue
            val fecha = item.optString("fecha", "")
            val promedio = item.optDouble("promedio", 0.0)

            if (fecha.isBlank() || promedio <= 0.0) continue

            try {
                val date = java.time.LocalDate.parse(fecha, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val timestamp = date.atStartOfDay(zoneId).toInstant().toEpochMilli()

                samples.add(RateSample(
                    fuente = "usd",
                    nombre = "Dólar (BCV)",
                    precio = promedio,
                    timestampEpochMillis = timestamp,
                    anterior = null,
                    variacion = null
                ))
            } catch (e: Exception) {
                // Skip invalid dates
            }
        }
        return samples
    }

    private fun readSamples(file: File): List<RateSample> {
        if (!file.exists()) return emptyList()
        return try {
            RateHistoryCodec.decode(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeAtomically(target: File, content: String) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(content)
        try {
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

/**
 * Derives calendar dates from epoch millis in the given zone.
 * Used for per-year file naming and per-day sampling buckets.
 */
fun yearOf(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).year

fun dayOfYear(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).dayOfYear

fun localDateOf(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

fun rateYearName(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    "rates-${yearOf(epochMillis, zoneId)}.json"

/**
 * Decides which quotes deserve a new sample for this load.
 * BCV (usd, eur) dedupes to one sample per fuente per calendar day;
 * USDT dedupes to one sample per app-open window (session flag).
 */
object RateSamplingPolicy {

    fun shouldSample(
        existing: List<RateSample>,
        quotes: List<DolarQuote>,
        nowEpochMillis: Long,
        usdtSampledThisSession: Boolean,
        previousDateMillis: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<RateSample> {
        val newSamples = mutableListOf<RateSample>()
        val today = localDateOf(nowEpochMillis, zoneId)

        quotes.forEach { quote ->
            when (quote.fuente) {
                "usd", "eur" -> {
                    // Always sample today (overwrites seed data)
                    newSamples.add(
                        RateSample(
                            fuente = quote.fuente,
                            nombre = quote.nombre,
                            precio = quote.promedio,
                            timestampEpochMillis = nowEpochMillis,
                            anterior = quote.anterior,
                            variacion = quote.variacion
                        )
                    )
                    // Also sample the "previous" date from the API if different from today
                    if (previousDateMillis != null && quote.anterior != null) {
                        val prevDate = localDateOf(previousDateMillis, zoneId)
                        if (prevDate != today) {
                            val alreadyHasPrev = existing.any {
                                it.fuente == quote.fuente &&
                                localDateOf(it.timestampEpochMillis, zoneId) == prevDate
                            } || newSamples.any {
                                it.fuente == quote.fuente &&
                                localDateOf(it.timestampEpochMillis, zoneId) == prevDate
                            }
                            if (!alreadyHasPrev) {
                                newSamples.add(
                                    RateSample(
                                        fuente = quote.fuente,
                                        nombre = quote.nombre,
                                        precio = quote.anterior,
                                        timestampEpochMillis = previousDateMillis,
                                        anterior = null,
                                        variacion = null
                                    )
                                )
                            }
                        }
                    }
                }
                "usdt" -> if (!usdtSampledThisSession) {
                    newSamples.add(
                        RateSample(
                            fuente = quote.fuente,
                            nombre = quote.nombre,
                            precio = quote.promedio,
                            timestampEpochMillis = nowEpochMillis
                        )
                    )
                }
            }
        }
        return newSamples
    }
}

/**
 * Prices of the current-year samples ordered by timestamp, for the chart.
 */
fun chartValues(samples: List<RateSample>): List<Double> =
    samples.sortedBy { it.timestampEpochMillis }.map { it.precio }

/**
 * Serializes [RateSample] lists to/from the `rates-YYYY.json` schema.
 * Tolerant decode: malformed input yields an empty list instead of crashing.
 */
object RateHistoryCodec {

    fun encode(samples: List<RateSample>): String {
        val samplesArray = JSONArray()
        samples.forEach { sample ->
            val obj = JSONObject()
                .put("fuente", sample.fuente)
                .put("nombre", sample.nombre)
                .put("precio", sample.precio)
                .put("timestampEpochMillis", sample.timestampEpochMillis)
            sample.anterior?.let { obj.put("anterior", it) }
            sample.variacion?.let { obj.put("variacion", it) }
            samplesArray.put(obj)
        }
        return JSONObject()
            .put("anio", samples.firstOrNull()?.let { yearOf(it.timestampEpochMillis) } ?: JSONObject.NULL)
            .put("samples", samplesArray)
            .toString()
    }

    fun decode(json: String): List<RateSample> {
        if (json.isBlank()) return emptyList()
        return try {
            val root = JSONObject(json)
            val samplesArray = root.optJSONArray("samples") ?: return emptyList()
            buildList {
                for (i in 0 until samplesArray.length()) {
                    val obj = samplesArray.optJSONObject(i) ?: continue
                    add(
                        RateSample(
                            fuente = obj.optString("fuente"),
                            nombre = obj.optString("nombre"),
                            precio = obj.optDouble("precio", 0.0),
                            timestampEpochMillis = obj.optLong("timestampEpochMillis", 0L),
                            anterior = obj.optDouble("anterior", Double.NaN).takeUnless { it.isNaN() },
                            variacion = obj.optDouble("variacion", Double.NaN).takeUnless { it.isNaN() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}