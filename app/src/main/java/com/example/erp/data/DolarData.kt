package com.example.erp.data

data class DolarQuote(
    val fuente: String,
    val nombre: String,
    val promedio: Double,
    val anterior: Double? = null,
    val variacion: Double? = null,
    val fechaActualizacion: String
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
}