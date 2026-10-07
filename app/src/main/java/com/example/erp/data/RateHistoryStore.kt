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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Persists [RateSample]s into per-year JSON files (`rates-YYYY.json`).
 * Appends are serialized and written atomically (tmp file + rename).
 */
interface RateHistoryStore {
    /** Appends samples and returns the merged list for the current year. */
    suspend fun append(samples: List<RateSample>): List<RateSample>
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

    companion object {
        /**
         * One location for every reader and writer.
         *
         * The app, the widget and the USDT worker each used their own path, so
         * `filesDir/rates-2026.json` and `filesDir/rate_history/rates-2026.json`
         * were different files. The widget sparkline read the second one, which
         * only the USDT worker ever wrote, then filtered USDT out looking for
         * USD -- so it rendered nothing, on every phone, always.
         *
         * Existing installs have their series in the old flat path. Moving here
         * makes that look empty, which is what makes [needsHistoricalBackfill]
         * fetch it again: a user who upgrades lands on a refilled history rather
         * than a permanently blank chart.
         */
        fun defaultDir(context: android.content.Context): File =
            context.applicationContext.filesDir.resolve("rate_history")
    }

    private val mutex = Mutex()

    override suspend fun append(samples: List<RateSample>): List<RateSample> {
        if (samples.isEmpty()) return readCurrentYear()
        return mutex.withLock {
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
                readSamples(File(dir, rateYearName(System.currentTimeMillis(), zoneId)))
                    .sortedBy { it.timestampEpochMillis }
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
                return@withContext
            }
            // Repair history written before a sample meant a change. Sampling
            // unconditionally stored the last published rate under Saturday and
            // Sunday, and those points are still on disk for whoever already had
            // the app. The BCV publishes on neither day: zero of 904 records at
            // the source fall on a weekend.
            purgeBcvWeekendSamples(currentYearFile)
        }
    }

    /** Drops BCV samples dated Saturday or Sunday, keeping every other source. */
    private fun purgeBcvWeekendSamples(file: File) {
        val stored = readSamples(file)
        val kept = stored.filterNot { sample ->
            val weekend = localDateOf(sample.timestampEpochMillis, zoneId).dayOfWeek.let {
                it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY
            }
            weekend && (sample.fuente == "usd" || sample.fuente == "eur")
        }
        if (kept.size != stored.size) {
            android.util.Log.d(
                "RateHistoryStore",
                "Dropped ${stored.size - kept.size} BCV weekend samples with no published rate"
            )
            writeAtomically(file, RateHistoryCodec.encode(kept))
        }
    }

    override suspend fun hasData(): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            val currentYearFile = File(dir, rateYearName(System.currentTimeMillis(), zoneId))
            currentYearFile.exists() && currentYearFile.length() > 0L
        }
    }

    override suspend fun fetchAndPopulateHistorical() {
        // History only changes when BCV publishes, which is once a day. This
        // used to run on every app open and re-download two full history series
        // just to merge samples that were already on disk. If the store already
        // reaches yesterday, any newer day simply has not been published yet.
        //
        // Recency alone turned out to be the wrong gate. A store holding only
        // today's sample satisfies "reaches yesterday" forever, so after a data
        // wipe the series never regained depth and the chart under-filled its
        // window for good. See [needsHistoricalBackfill].
        val today = java.time.LocalDate.now(zoneId)
        val storedDays = readCurrentYear().map { localDateOf(it.timestampEpochMillis, zoneId) }
        if (!needsHistoricalBackfill(storedDays, today)) {
            val newestStoredDay = storedDays.filter { !it.isAfter(today) }.max()
            android.util.Log.d("RateHistoryStore", "History already current through $newestStoredDay; skipping fetch")
            return
        }

        // Each source fetches on its own. One try around both meant a USD outage
        // skipped the EUR call entirely, and the EUR fetcher's own emptyList()
        // fallback -- already written, already correct -- could never run. The
        // tolerance existed and the call order made it unreachable.
        val historicalData = withContext(Dispatchers.IO) {
            val usd = try {
                fetchHistoricalFromApi()
            } catch (e: Exception) {
                android.util.Log.w("RateHistoryStore", "USD historical failed: ${e.message}")
                emptyList()
            }
            val eur = try {
                fetchHistoricalEuroFromApi()
            } catch (e: Exception) {
                android.util.Log.w("RateHistoryStore", "EUR historical failed: ${e.message}")
                emptyList()
            }
            usd + eur
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

        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        android.util.Log.d("RateHistoryStore", "Fetching historical from: $url")
        val response = sharedHttpClient.newCall(request).execute()
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

    /**
     * Fetches historical EUR rates from ve.dolarapi.com API.
     * Returns list of RateSample with real BCV Euro data.
     */
    private fun fetchHistoricalEuroFromApi(): List<RateSample> {
        val url = "https://ve.dolarapi.com/v1/historicos/euros/oficial"

        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        android.util.Log.d("RateHistoryStore", "Fetching EUR historical from: $url")
        val response = sharedHttpClient.newCall(request).execute()
        val body = response.body?.string().orEmpty()

        if (response.code != 200) {
            android.util.Log.w("RateHistoryStore", "EUR historical fetch failed: HTTP ${response.code}")
            return emptyList()
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
                    fuente = "eur",
                    nombre = "Euro (BCV)",
                    precio = promedio,
                    timestampEpochMillis = timestamp,
                    anterior = null,
                    variacion = null
                ))
            } catch (e: Exception) {
                // Skip invalid dates
            }
        }
        android.util.Log.d("RateHistoryStore", "EUR historical: ${samples.size} samples fetched")
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

/**
 * How many SAMPLES the history chart draws — points, NOT days.
 *
 * The distinction is not pedantic: BCV does not publish on weekends, so fifteen
 * samples span roughly three weeks of calendar time. Anything that labels this
 * number as days contradicts the axis it sits above. The window the reader sees
 * is a consequence of the samples, and the chart header has to name that
 * consequence instead.
 *
 * Shared by the backfill gate and the chart so the two cannot drift apart: a
 * store needs enough samples to fill the chart, and the chart reports what those
 * samples actually cover.
 */
const val HISTORICO_PUNTOS = 15

/**
 * Whether stored history still needs a backfill.
 *
 * Recency is not the question. "Do I have today's sample" is satisfied by a
 * single sample forever, so a store that lost its history — a data wipe, a
 * failed download — would keep passing the old gate while its chart showed a
 * fraction of the points it claims. The real question is whether the series can
 * fill the chart AND is not stale.
 *
 * Counted in samples, not calendar days, to match what the chart draws: fifteen
 * calendar days of BCV data is about eleven points, which is not the window the
 * UI advertises.
 *
 * Future-dated days are excluded: the next rate is stored as a sample, so a
 * pending rate would otherwise count as depth that does not exist yet.
 */
fun needsHistoricalBackfill(
    storedDays: List<LocalDate>,
    today: LocalDate,
    requiredSamples: Int = HISTORICO_PUNTOS
): Boolean {
    val real = storedDays.filter { !it.isAfter(today) }.distinct()
    if (real.isEmpty()) return true
    val reachesYesterday = !real.max().isBefore(today.minusDays(1))
    val fillsTheChart = real.size >= requiredSamples
    return !(reachesYesterday && fillsTheChart)
}

fun localDateOf(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

fun rateYearName(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    "rates-${yearOf(epochMillis, zoneId)}.json"

/**
 * Decides which quotes deserve a new sample for this load.
 * BCV (usd, eur) dedupes to one sample per fuente per calendar day;
 * USDT dedupes to one sample per hour (P2P rates change frequently)
 * and, on top of that, to one sample per app-open session.
 */
object RateSamplingPolicy {

    /**
     * @param usdtSampledThisSession true once a USDT sample has already been
     *   taken in the current app session. When true, USDT quotes are skipped
     *   entirely, even on an empty [existing] list or in a fresh hour.
     *   BCV quotes are never affected by this flag.
     */
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
        val nowHour = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDateTime().hour

        quotes.forEach { quote ->
            when (quote.fuente) {
                "usd", "eur" -> {
                    // A sample IS a change. BCV serves the last published rate on
                    // days it does not update -- Saturday and Sunday included --
                    // so sampling unconditionally stored the Friday rate a second
                    // time under Saturday's date, and the series grew a point
                    // where nothing happened. The chart then had to draw those
                    // flat runs as if they were history.
                    //
                    // Comparing the price against the newest stored one keeps the
                    // series to real moves whatever the reason a day is missing:
                    // a weekend, a holiday, or simply an unchanged rate.
                    val lastStored = existing
                        .filter { it.fuente == quote.fuente }
                        .filter { !localDateOf(it.timestampEpochMillis, zoneId).isAfter(today) }
                        .maxByOrNull { it.timestampEpochMillis }
                    val changed = lastStored == null || lastStored.precio != quote.promedio
                    if (changed) {
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
                "usdt" -> {
                    // Session gate: once this session has produced a USDT
                    // sample, later loads in the same session add nothing,
                    // regardless of hour or persisted history.
                    if (usdtSampledThisSession) return@forEach
                    // Sample USDT once per hour (P2P rates change frequently)
                    val alreadySampledThisHour = existing.any {
                        it.fuente == "usdt" &&
                        Instant.ofEpochMilli(it.timestampEpochMillis).atZone(zoneId).toLocalDateTime().hour == nowHour &&
                        localDateOf(it.timestampEpochMillis, zoneId) == today
                    } || newSamples.any {
                        it.fuente == "usdt" &&
                        Instant.ofEpochMilli(it.timestampEpochMillis).atZone(zoneId).toLocalDateTime().hour == nowHour
                    }
                    if (!alreadySampledThisHour) {
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