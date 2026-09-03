package com.example.erp

import com.example.erp.data.FileHistoryStore
import com.example.erp.data.RateHistoryCodec
import com.example.erp.data.RateHistoryStore
import com.example.erp.data.RateSample
import com.example.erp.data.rateYearName
import com.example.erp.data.yearOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class FileHistoryStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val zone: ZoneId = ZoneId.of("America/Caracas")

    private val nowEpochMillis: Long = System.currentTimeMillis()
    private val thisYear: Int = yearOf(nowEpochMillis, zone)
    private val yearFileName: String = rateYearName(nowEpochMillis, zone)

    private fun epochMillisAt(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun sample(
        fuente: String,
        precio: Double,
        timestamp: Long = epochMillisAt(LocalDate.of(thisYear, 2, 10))
    ): RateSample =
        RateSample(
            fuente = fuente,
            nombre = "Fuente $fuente",
            precio = precio,
            timestampEpochMillis = timestamp
        )

    @Test
    fun `append creates the current year file with the sample`() = runBlocking {
        val store: RateHistoryStore = FileHistoryStore(tmp.root, zone)
        store.append(listOf(sample("usd", 773.31)))

        val file = File(tmp.root, yearFileName)
        assertTrue("rates-YYYY.json must exist after append", file.exists())
        val decoded = RateHistoryCodec.decode(file.readText())
        assertEquals(1, decoded.size)
        assertEquals("usd", decoded.single().fuente)
        assertEquals(773.31, decoded.single().precio, 0.0)
    }

    @Test
    fun `append merges with stored samples and readCurrentYear sorts by timestamp`() = runBlocking {
        val store = FileHistoryStore(tmp.root, zone)
        val earlier = sample("usd", 772.54, epochMillisAt(LocalDate.of(thisYear, 2, 10)))
        val later = sample("usd", 773.31, epochMillisAt(LocalDate.of(thisYear, 2, 11)))
        store.append(listOf(later))
        store.append(listOf(earlier))

        assertEquals(listOf(772.54, 773.31), store.readCurrentYear().map { it.precio })
    }

    @Test
    fun `readCurrentYear returns empty when file is absent`() = runBlocking {
        val store = FileHistoryStore(tmp.root, zone)

        assertEquals(emptyList<RateSample>(), store.readCurrentYear())
    }

    @Test
    fun `corrupt file reads as empty and next append rewrites valid json`() = runBlocking {
        File(tmp.root, yearFileName).writeText("{not-valid-json")

        val store = FileHistoryStore(tmp.root, zone)
        assertEquals(emptyList<RateSample>(), store.readCurrentYear())

        store.append(listOf(sample("usdt", 105.5)))

        val root = JSONObject(File(tmp.root, yearFileName).readText())
        assertEquals(thisYear, root.getInt("anio"))
        val history = store.readCurrentYear()
        assertEquals(1, history.size)
        assertEquals("usdt", history.single().fuente)
    }

    @Test
    fun `concurrent appends serialize and keep every sample with valid json`() = runBlocking {
        val store = FileHistoryStore(tmp.root, zone)
        val count = 20
        // Use different dates so they don't get replaced by same-day logic
        (1..count).map { i ->
            val date = LocalDate.of(thisYear, 3, 1).plusDays(i.toLong())
            async { store.append(listOf(sample("usd", 700.0 + i, epochMillisAt(date)))) }
        }.awaitAll()

        val history = store.readCurrentYear()
        assertEquals(count, history.size)
        assertEquals((1..count).map { 700.0 + it }.sorted(), history.map { it.precio }.sorted())
        val root = JSONObject(File(tmp.root, yearFileName).readText())
        assertEquals(count, root.getJSONArray("samples").length())
    }

    @Test
    fun `append groups samples into their own year file and leaves other years untouched`() = runBlocking {
        val store = FileHistoryStore(tmp.root, zone)
        val lastYearTimestamp = epochMillisAt(LocalDate.of(thisYear - 1, 12, 15))
        store.append(listOf(sample("usd", 700.0, nowEpochMillis)))
        store.append(listOf(sample("eur", 800.0, lastYearTimestamp)))

        val lastYearFile = File(tmp.root, rateYearName(lastYearTimestamp, zone))
        assertTrue("previous year file must exist", lastYearFile.exists())
        assertEquals(1, RateHistoryCodec.decode(lastYearFile.readText()).size)

        assertEquals(listOf("usd"), store.readCurrentYear().map { it.fuente })
    }
}