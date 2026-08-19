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

/**
 * Persists [RateSample]s into per-year JSON files (`rates-YYYY.json`).
 * Appends are serialized and written atomically (tmp file + rename).
 */
interface RateHistoryStore {
    suspend fun append(samples: List<RateSample>)
    /** Samples of the current calendar year sorted by timestamp; corrupt/absent file -> emptyList. */
    suspend fun readCurrentYear(): List<RateSample>
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
                        val merged = (readSamples(target) + yearSamples)
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
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<RateSample> {
        val newSamples = mutableListOf<RateSample>()

        fun alreadySampledToday(fuente: String): Boolean {
            val today = localDateOf(nowEpochMillis, zoneId)
            return existing.any { it.fuente == fuente && localDateOf(it.timestampEpochMillis, zoneId) == today } ||
                newSamples.any { it.fuente == fuente }
        }

        quotes.forEach { quote ->
            when (quote.fuente) {
                "usd", "eur" -> if (!alreadySampledToday(quote.fuente)) {
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
                }
                "usdt" -> if (!usdtSampledThisSession && !alreadySampledToday(quote.fuente)) {
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