package com.example.erp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EvolutionChart(
    values: List<Double>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).coerceAtLeast(1e-9)
        val stepX = size.width / (values.size - 1)
        val paddingY = 8.dp.toPx()

        fun yFor(value: Double): Float =
            size.height - paddingY - (((value - minV) / range).toFloat() * (size.height - paddingY * 2))

        val linePath = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = yFor(value)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
                startY = paddingY,
                endY = size.height
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val lastY = yFor(values.last())
        drawCircle(
            color = lineColor,
            radius = 5.dp.toPx(),
            center = Offset(size.width, lastY)
        )
    }
}

@Composable
fun ChartLabels(
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val min = values.min()
        val max = values.max()
        Text(
            text = "Mín ${min.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Máx ${max.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}