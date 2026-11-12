package com.example.erp.data

import kotlinx.serialization.Serializable

@Serializable
data class DolarQuote(
    val fuente: String,
    val nombre: String,
    val promedio: Double,
    val anterior: Double? = null,
    val variacion: Double? = null,
    val fechaActualizacion: String,
    val fechaAnterior: String? = null,
    /**
     * Parallel market only: the average of the offers you BUY USDT from.
     * Null for BCV and for an older cached USDT quote.
     */
    val compra: Double? = null,
    /**
     * Parallel market only: the average of the offers you SELL USDT to.
     * Null for BCV and for an older cached USDT quote.
     */
    val venta: Double? = null
)

data class RateSample(
    val fuente: String,
    val nombre: String,
    val precio: Double,
    val timestampEpochMillis: Long,
    val anterior: Double? = null,
    val variacion: Double? = null
)

interface DolarRepository {
    suspend fun getQuotes(): List<DolarQuote>

    /**
     * Refreshes ONLY the parallel-market quote.
     *
     * The hourly card has its own button; making that button re-fetch BCV
     * would spend a request on a rate that cannot have moved and could fail on
     * the official endpoint while the USDT refresh was fine. The default keeps
     * test fakes and alternative repositories source-compatible.
     */
    suspend fun refreshUsdt(): DolarQuote? = null
}