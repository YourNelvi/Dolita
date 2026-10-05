package com.example.erp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.erp.data.HISTORICO_PUNTOS
import com.example.erp.data.RateSample
import com.example.erp.ui.theme.DownRedLight
import com.example.erp.ui.theme.FintechSignalRed
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.positiveColor
import com.example.erp.ui.theme.isDarkTheme
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

// Tabular figures: fifteen prices only read as columns when every digit is the
// same width. Same reason CalculatorCard pins it on its amount field.
private const val TabularFigures = "tnum"

/**
 * One X axis label, anchored per position. A centered label sitting on the plot
 * padding hangs off the canvas and clips the very date it names, so the first
 * label is left-aligned and the last one right-aligned.
 */
private fun axisLabelPaint(
    labelColor: Color,
    textSizePx: Float,
    align: android.graphics.Paint.Align
) = android.graphics.Paint().apply {
    color = labelColor.toArgb()
    textSize = textSizePx
    textAlign = align
    isAntiAlias = true
}

/** Negative signal red, shared with the quote list so both read identically. */
@Composable
private fun negativeColor(): Color =
    if (isDarkTheme()) FintechSignalRed else DownRedLight

/**
 * Builds a smooth cubic (Bézier) spline through [points] using horizontal
 * control handles, so the series renders as a curve instead of straight
 * segments. One point draws nothing (a lone moveTo is not a visible stroke).
 */
private fun Path.addSmoothSpline(points: List<Offset>) {
    if (points.isEmpty()) return
    moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val start = points[i]
        val end = points[i + 1]
        val handle = (end.x - start.x) * 0.4f
        cubicTo(start.x + handle, start.y, end.x - handle, end.y, end.x, end.y)
    }
}

@Composable
fun EvolutionChart(
    samples: List<RateSample>,
    // Defaults to the palette accent, NOT `primary`. A flat series falls back to
    // this value for its stroke, and `primary` is a fill color that carries
    // white text and is allowed to be very dark (ROJO_DEGRADADO, GRIS_NEUTRO),
    // which disappears against the dark card surface.
    lineColor: Color = accentColor(),
    modifier: Modifier = Modifier,
    // The daily window is defined by the store that feeds the chart, not by a
    // literal here: the same constant decides the backfill depth, so a retype
    // here would let the chart claim a window the data was never asked for.
    maxPoints: Int = HISTORICO_PUNTOS,
    showHours: Boolean = false,
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

    val sorted = remember(samples) {
        samples.sortedBy { it.timestampEpochMillis }.takeLast(maxPoints)
    }
    val priceFormat = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val dayFormat = remember(zoneId, showHours) {
        DateTimeFormatter.ofPattern(if (showHours) "dd/MM HH:mm" else "dd/MM").withZone(zoneId)
    }
    val fullDateFormat = remember(zoneId, showHours) {
        DateTimeFormatter.ofPattern(if (showHours) "dd/MM HH:mm" else "dd/MM/yyyy").withZone(zoneId)
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    // The series draws itself in from the left whenever the data changes, so a
    // refresh reads as the chart re-measuring the rate rather than as a repaint.
    val reveal = remember { Animatable(1f) }
    val reducedMotion = rememberReducedMotion()
    LaunchedEffect(sorted.size, sorted.lastOrNull()?.timestampEpochMillis, sorted.lastOrNull()?.precio) {
        if (reducedMotion) {
            reveal.snapTo(1f)
        } else {
            reveal.snapTo(0f)
            reveal.animateTo(1f, animationSpec = tween(700, easing = Emphasized))
        }
    }
    val revealFraction = reveal.value

    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline

    // The series wears the selected theme's accent. Direction is still stated,
    // but in text where it is read as a number: the tooltip variation keeps the
    // semantic green/red, so a themed line never costs the "subió o bajó" cue.
    val accent = accentColor()
    val positive = positiveColor()
    val negative = negativeColor()
    val firstPrice = sorted.first().precio
    val lastPrice = sorted.last().precio
    val strokeColor = accent

    val density = LocalDensity.current
    val textSizePx = with(density) { 12.sp.toPx() }
    val smallTextSizePx = with(density) { 10.sp.toPx() }

    val labelPaint = remember(onSurface) {
        axisLabelPaint(onSurface, smallTextSizePx, android.graphics.Paint.Align.CENTER)
    }
    val firstLabelPaint = remember(onSurface) {
        axisLabelPaint(onSurface, smallTextSizePx, android.graphics.Paint.Align.LEFT)
    }
    val lastLabelPaint = remember(onSurface) {
        axisLabelPaint(onSurface, smallTextSizePx, android.graphics.Paint.Align.RIGHT)
    }
    // Computed here, not inside the Canvas: which points are named is a property
    // of the series, and the grid below reuses this same list to stay in step.
    val axisLabelIndices = remember(sorted.size) {
        xAxisLabelIndices(sorted.size, X_AXIS_MAX_LABELS)
    }

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
                        // Same index-based placement the drawing code uses. The
                        // tap used to resolve against calendar time, so after the
                        // axis moved to per-publish spacing a touch would have
                        // selected the wrong point — the highlight is the only
                        // feedback for a wrong hit.
                        var closestIdx = 0
                        var closestDist = Float.MAX_VALUE
                        sorted.forEachIndexed { idx, _ ->
                            val xRatio = if (sorted.size > 1) {
                                idx.toFloat() / (sorted.size - 1).toFloat()
                            } else {
                                0f
                            }
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

            // One slot per PUBLISHED rate, evenly spaced by index.
            //
            // The X axis used to be calendar time, which made every weekend a
            // flat two-day stretch with no dots on it: the line looked frozen
            // because the BCV simply does not publish on Saturday and Sunday.
            // That reads as "nothing happened" when the truth is "the market was
            // closed". The reader asked for the history of changes, so each
            // point is one change and the spacing says nothing about the gap
            // between two of them.
            val points = sorted.mapIndexed { index, sample ->
                val xRatio = if (sorted.size > 1) {
                    index.toFloat() / (sorted.size - 1).toFloat()
                } else {
                    0f
                }
                val x = padding + xRatio * chartWidth
                val normalized = (sample.precio - minPrice) / range
                val y = padding + chartHeight - (chartHeight * normalized.toFloat())
                Offset(x, y)
            }

            // Area fill under the spline: accent tint at the curve fading to
            // fully transparent at the baseline (vertical gradient).
            val baseY = padding + chartHeight
            val areaPath = Path().apply {
                addSmoothSpline(points)
                lineTo(points.last().x, baseY)
                lineTo(points.first().x, baseY)
                close()
            }
            // Guard against a zero-height gradient when the series is flat at
            // the very bottom of the plot area.
            val gradientTop = min(points.minOf { it.y }, baseY - 1f)
            // Reveal the whole plot from the left edge in step with the line.
            val revealRight = padding + (canvasWidth - padding * 2) * revealFraction
            clipRect(left = 0f, top = 0f, right = revealRight, bottom = canvasHeight) {
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        startY = gradientTop,
                        endY = baseY,
                        colors = listOf(
                            strokeColor.copy(alpha = 0.30f),
                            strokeColor.copy(alpha = 0f)
                        )
                    )
                )

                // Line: smooth cubic spline. Compose paints are built with
                // Paint.ANTI_ALIAS_FLAG, so drawPath is antialiased by default.
                val linePath = Path().apply { addSmoothSpline(points) }
                drawPath(
                    path = linePath,
                    color = strokeColor,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

            // Data points follow the themed stroke. Direction is read from the
            // tooltip's variation, not from dot colors: a themed line with
            // green and red dots on it would look broken.
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                val radius = if (isSelected) 6f else 3.5f
                drawCircle(color = strokeColor, radius = radius, center = pt)
                if (isSelected) {
                    drawCircle(
                        color = strokeColor.copy(alpha = 0.25f),
                        radius = 12f,
                        center = pt
                    )
                }
            }
            } // end clipRect reveal

            // Etiquetas de eje X: los puntos que caben, con los dos extremos
            // siempre nombrados y anclados hacia adentro para no recortarse.
            axisLabelIndices.forEach { index ->
                val paint = when (index) {
                    0 -> firstLabelPaint
                    sorted.size - 1 -> lastLabelPaint
                    else -> labelPaint
                }
                drawContext.canvas.nativeCanvas.drawText(
                    dayFormat.format(Instant.ofEpochMilli(sorted[index].timestampEpochMillis)),
                    points[index].x,
                    canvasHeight - 2f,
                    paint
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
                sample.variacion >= 0 -> positive
                else -> negative
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
                        // Primary numeric value -> accent.
                        color = accent
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

        Spacer(Modifier.height(12.dp))
        HistoryGrid(samples = sorted, dayFormat = dayFormat, priceFormat = priceFormat)
    }
}

/**
 * The whole drawn series as a compact grid: date over price, three across.
 *
 * The axis can only name a handful of dates, so this block is what actually
 * delivers "every update with its date". It renders the very list the canvas
 * drew — same samples, same order, no second selection — so the line and the
 * numbers underneath it cannot describe different windows.
 */
@Composable
private fun HistoryGrid(
    samples: List<RateSample>,
    dayFormat: DateTimeFormatter,
    priceFormat: NumberFormat
) {
    if (samples.isEmpty()) return
    val accent = accentColor()
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val hairline = cardBorderColor()
    val cellShape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        historyGridRows(samples).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { sample ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(cellShape)
                            .border(1.dp, hairline, cellShape)
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // The hourly series carries the clock too, so the same
                        // formatter names a time instead of a bare day.
                        Text(
                            text = dayFormat.format(Instant.ofEpochMilli(sample.timestampEpochMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor,
                            maxLines = 1
                        )
                        Text(
                            text = "$${priceFormat.format(sample.precio)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TabularFigures
                            ),
                            // Accent is spent on the numbers only, dates stay muted.
                            color = accent,
                            maxLines = 1
                        )
                    }
                }
                // Empty slots keep a short last row under the columns above it.
                repeat(HISTORY_GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
