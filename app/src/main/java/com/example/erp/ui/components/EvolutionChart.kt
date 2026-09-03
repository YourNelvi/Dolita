package com.example.erp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.erp.data.RateSample
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

@Composable
fun EvolutionChart(
    samples: List<RateSample>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
) {
    if (samples.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin datos historicos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Tomar ultimos 15 dias
    val sorted = remember(samples) {
        samples.sortedBy { it.timestampEpochMillis }.takeLast(15)
    }
    val priceFormat = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val dayFormat = remember(zoneId) {
        DateTimeFormatter.ofPattern("dd/MM").withZone(zoneId)
    }
    val fullDateFormat = remember(zoneId) {
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(zoneId)
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outline

    val density = LocalDensity.current
    val textSizePx = with(density) { 12.sp.toPx() }
    val smallTextSizePx = with(density) { 10.sp.toPx() }

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .pointerInput(sorted.size) {
                    detectTapGestures { offset ->
                        if (sorted.isEmpty()) return@detectTapGestures
                        val canvasWidth = size.width.toFloat()
                        val padding = 24f
                        val chartWidth = canvasWidth - padding * 2
                        val firstTimestamp = sorted.first().timestampEpochMillis
                        val lastTimestamp = sorted.last().timestampEpochMillis
                        val totalDuration = if (lastTimestamp > firstTimestamp) lastTimestamp - firstTimestamp else 1L
                        // Encontrar el punto mas cercano al toque usando fechas reales
                        var closestIdx = 0
                        var closestDist = Float.MAX_VALUE
                        sorted.forEachIndexed { idx, sample ->
                            val xRatio = (sample.timestampEpochMillis - firstTimestamp).toFloat() / totalDuration.toFloat()
                            val x = padding + xRatio * chartWidth
                            val dist = abs(offset.x - x)
                            if (dist < closestDist) {
                                closestDist = dist
                                closestIdx = idx
                            }
                        }
                        selectedIndex = if (selectedIndex == closestIdx) -1 else closestIdx
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 24f
            val chartWidth = canvasWidth - padding * 2
            val chartHeight = canvasHeight - padding * 2

            if (sorted.isEmpty()) return@Canvas

            val precios = sorted.map { it.precio }
            val minPrice = precios.min()
            val maxPrice = precios.max()
            val range = if (maxPrice - minPrice < 0.01) 1.0 else maxPrice - minPrice

            // Use actual date range for X axis (handles gaps from weekends)
            val firstTimestamp = sorted.first().timestampEpochMillis
            val lastTimestamp = sorted.last().timestampEpochMillis
            val totalDuration = if (lastTimestamp > firstTimestamp) lastTimestamp - firstTimestamp else 1L

            // Dibujar linea de guia horizontal (grid sutil)
            for (i in 0..4) {
                val y = padding + chartHeight - (chartHeight * i / 4f)
                drawLine(
                    color = outlineColor.copy(alpha = 0.2f),
                    start = Offset(padding, y),
                    end = Offset(canvasWidth - padding, y),
                    strokeWidth = 1f
                )
            }

            // Calcular puntos usando fechas reales
            val points = sorted.map { sample ->
                val xRatio = (sample.timestampEpochMillis - firstTimestamp).toFloat() / totalDuration.toFloat()
                val x = padding + xRatio * chartWidth
                val normalized = (sample.precio - minPrice) / range
                val y = padding + chartHeight - (chartHeight * normalized.toFloat())
                Offset(x, y)
            }

            // Dibujar area bajo la curva
            val areaPath = Path().apply {
                moveTo(points.first().x, padding + chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, padding + chartHeight)
                close()
            }
            drawPath(
                path = areaPath,
                color = primaryColor.copy(alpha = 0.08f)
            )

            // Dibujar linea
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(width = 2.5f)
            )

            // Dibujar puntos
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                val radius = if (isSelected) 6f else 3.5f
                val sample = sorted[idx]
                val dotColor = when {
                    isSelected -> primaryColor
                    sample.variacion != null && sample.variacion >= 0 -> tertiaryColor
                    sample.variacion != null && sample.variacion < 0 -> errorColor
                    else -> primaryColor
                }
                drawCircle(color = dotColor, radius = radius, center = pt)
                if (isSelected) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = 12f,
                        center = pt
                    )
                }
            }

            // Etiquetas de eje X (primeros, ultimo, y seleccionado)
            val labelPaint = android.graphics.Paint().apply {
                color = onSurface.hashCode()
                textSize = smallTextSizePx
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            // Primera fecha
            drawContext.canvas.nativeCanvas.drawText(
                dayFormat.format(Instant.ofEpochMilli(sorted.first().timestampEpochMillis)),
                points.first().x,
                canvasHeight - 2f,
                labelPaint
            )
            // Ultima fecha (si hay mas de 1 punto)
            if (sorted.size > 1) {
                drawContext.canvas.nativeCanvas.drawText(
                    dayFormat.format(Instant.ofEpochMilli(sorted.last().timestampEpochMillis)),
                    points.last().x,
                    canvasHeight - 2f,
                    labelPaint
                )
            }
        }

        // Tooltip flotante cuando se selecciona un punto
        if (selectedIndex in sorted.indices) {
            val sample = sorted[selectedIndex]
            val dateStr = fullDateFormat.format(Instant.ofEpochMilli(sample.timestampEpochMillis))
            val priceStr = "$${priceFormat.format(sample.precio)}"
            val varStr = sample.variacion?.let { v ->
                val sign = if (v >= 0) "+" else ""
                "${sign}${priceFormat.format(v)}%"
            }
            val varColor = when {
                sample.variacion == null -> onSurface
                sample.variacion >= 0 -> tertiaryColor
                else -> errorColor
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = priceStr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = primaryColor
                    )
                    varStr?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = varColor
                        )
                    }
                }
            }
        }
    }
}
