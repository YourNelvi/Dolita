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
 * How many dates the X axis may name at once. Fifteen `dd/MM` labels cannot fit
 * a phone width without overlapping, so the axis names a readable subset and the
 * tap on a point still reports the exact date.
 */
/**
 * Drops the axis labels that would sit on top of another one.
 *
 * A fixed label budget cannot work: "dd/MM" fits five times across a phone and
 * "dd/MM HH:mm" fits barely three, and picking evenly spaced indices is not
 * enough on its own — with eight hourly samples and five labels the indices come
 * out 0, 2, 4, 5, 7, and 4 and 5 are adjacent, so two full timestamps landed on
 * top of each other and read as one garbled string.
 *
 * Measuring the rendered text is what makes this hold for any format and any
 * screen. The final label always survives: it is the one that anchors the right
 * edge, so a middle label yields to it rather than the other way round.
 */
internal fun selectAxisLabels(
    candidates: List<Int>,
    lastIndex: Int,
    labelLeft: (Int) -> Float,
    labelWidth: (Int) -> Float,
    minGap: Float
): List<Int> {
    if (candidates.isEmpty()) return emptyList()
    val kept = mutableListOf<Int>()
    val finalLeft = if (lastIndex in candidates) labelLeft(lastIndex) else Float.POSITIVE_INFINITY
    var previousRight = Float.NEGATIVE_INFINITY

    candidates.forEach { index ->
        val isLast = index == lastIndex
        val left = labelLeft(index)
        val right = left + labelWidth(index)
        val overlapsPrevious = left < previousRight + minGap
        val crowdsTheFinal = !isLast && right + minGap > finalLeft
        if (isLast || (!overlapsPrevious && !crowdsTheFinal)) {
            kept += index
            previousRight = right
        }
    }
    return kept
}

/** Upper bound on named dates before the collision pass thins them further. */
internal const val X_AXIS_MAX_LABELS = 5

/** Breathing room between two axis dates, in canvas pixels. */
private const val LABEL_MIN_GAP_PX = 10f

/**
 * Which sample indices the X axis should label: the first and last always, the
 * rest spread as evenly as possible, never more than [maxLabels].
 *
 * Returning only what fits is what lets the axis name more than the two endpoints
 * without drawing a wall of overlapping dates.
 */
internal fun xAxisLabelIndices(count: Int, maxLabels: Int): List<Int> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(0)
    val wanted = maxLabels.coerceIn(2, count)
    if (wanted == 2) return listOf(0, count - 1)
    return (0 until wanted).map { i ->
        Math.round(i * (count - 1).toFloat() / (wanted - 1).toFloat()).toInt()
    }.distinct()
}

/**
 * Fritsch-Carlson tangent limiter: produces the slope at each sample that makes
 * a Hermite curve pass through every one of them WITHOUT overshooting between
 * two of them.
 *
 * This replaced fixed horizontal Bézier handles, which overshoot wherever the
 * direction changes. On the official dollar that went unnoticed because the rate
 * only ever rises; on the euro, which tracks the international cross and does
 * fall as well as rise, every turn of the series grew a peak that no published
 * rate ever reached. The data was honest and the drawing invented the drama.
 */
internal fun monotoneTangents(points: List<Offset>): FloatArray {
    val n = points.size
    if (n < 2) return FloatArray(n)
    val slopes = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        val dx = points[i + 1].x - points[i].x
        slopes[i] = if (dx == 0f) 0f else (points[i + 1].y - points[i].y) / dx
    }
    val m = FloatArray(n)
    m[0] = slopes[0]
    m[n - 1] = slopes[n - 2]
    for (i in 1 until n - 1) m[i] = (slopes[i - 1] + slopes[i]) / 2f
    // A peak or a trough has to leave horizontally. Averaging the two slopes
    // around an extremum can leave a tangent pointing back across it, and the
    // curve then swings past the turning point and draws an excursion the rate
    // never made. This is the case the euro series hits on every direction
    // change, which is why its line used to look mountainous.
    for (i in 1 until n - 1) {
        if (slopes[i - 1] * slopes[i] < 0f) m[i] = 0f
    }
    for (i in 0 until n - 1) {
        if (slopes[i] == 0f) {
            // A flat step keeps the curve flat across it instead of letting the
            // neighbouring slopes dive through it.
            m[i] = 0f
            m[i + 1] = 0f
            continue
        }
        val a = m[i] / slopes[i]
        val b = m[i + 1] / slopes[i]
        val magnitude = a * a + b * b
        if (magnitude > 9f) {
            val scale = 3f / kotlin.math.sqrt(magnitude)
            m[i] = scale * a * slopes[i]
            m[i + 1] = scale * b * slopes[i]
        }
    }
    return m
}

/**
 * Y of the monotone Hermite segment at [t], for unit x spacing — which is how
 * the chart spaces samples, one slot per published rate. Exposed so the
 * no-overshoot property can be asserted directly instead of eyeballed.
 */
internal fun hermiteY(y0: Float, y1: Float, m0: Float, m1: Float, t: Float): Float {
    val t2 = t * t
    val t3 = t2 * t
    return (2f * t3 - 3f * t2 + 1f) * y0 + (t3 - 2f * t2 + t) * m0 +
        (-2f * t3 + 3f * t2) * y1 + (t3 - t2) * m1
}

/**
 * Draws the series as a smooth curve that cannot leave the corridor between
 * consecutive samples. One point draws nothing (a lone moveTo is not a stroke).
 */
private fun Path.addSmoothSpline(points: List<Offset>) {
    if (points.isEmpty()) return
    moveTo(points.first().x, points.first().y)
    if (points.size < 2) return
    val m = monotoneTangents(points)
    for (i in 0 until points.size - 1) {
        val start = points[i]
        val end = points[i + 1]
        val h = (end.x - start.x) / 3f
        cubicTo(
            start.x + h, start.y + m[i] * h,
            end.x - h, end.y - m[i + 1] * h,
            end.x, end.y
        )
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

    Column(modifier = modifier.fillMaxWidth()) {
    Box(modifier = Modifier.fillMaxWidth()) {
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
            //
            // Evenly spaced indices are only a first pass. They can land on
            // neighbouring points — indices 4 and 5 of eight — which for the
            // hourly format means two full timestamps drawn over each other, so
            // the ones that would collide are dropped after measuring the text.
            val labelTexts = axisLabelIndices.associateWith { index ->
                dayFormat.format(Instant.ofEpochMilli(sorted[index].timestampEpochMillis))
            }
            fun widthOf(index: Int) = labelPaint.measureText(labelTexts.getValue(index))
            val drawnLabels = selectAxisLabels(
                candidates = axisLabelIndices,
                lastIndex = sorted.size - 1,
                labelLeft = { index ->
                    when (index) {
                        0 -> points[index].x
                        sorted.size - 1 -> points[index].x - widthOf(index)
                        else -> points[index].x - widthOf(index) / 2f
                    }
                },
                labelWidth = ::widthOf,
                minGap = LABEL_MIN_GAP_PX
            )

            drawnLabels.forEach { index ->
                val paint = when (index) {
                    0 -> firstLabelPaint
                    sorted.size - 1 -> lastLabelPaint
                    else -> labelPaint
                }
                drawContext.canvas.nativeCanvas.drawText(
                    labelTexts.getValue(index),
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
            val priceStr = "Bs ${priceFormat.format(sample.precio)}"
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
    }
    }
}
