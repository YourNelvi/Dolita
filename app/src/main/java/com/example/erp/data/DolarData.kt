package com.example.erp.data

import kotlin.math.sin

data class DolarQuote(
    val fuente: String,
    val nombre: String,
    val promedio: Double,
    val anterior: Double? = null,
    val variacion: Double? = null,
    val fechaActualizacion: String
)

data class PricePoint(
    val precio: Double,
    val hora: String
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
    suspend fun getHistorial(quote: DolarQuote): List<PricePoint>
}

object DolarSimulation {

    fun historial(base: Double): List<PricePoint> {
        val drift = listOf(
            -0.8, 0.2, 1.1, -0.4, 0.6, 1.4,
            -0.2, 0.9, -0.5, 1.0, 0.3, 0.7
        )
        return drift.mapIndexed { index, percent ->
            val wave = sin(index * 1.05) * base * 0.003
            PricePoint(
                precio = base * (1 + percent / 100.0) + wave,
                hora = "${7 + index}:${if (index % 2 == 0) "00" else "30"}"
            )
        }
    }
}