package com.example.erp.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.abs

/**
 * Draws the widget sparkline.
 *
 * A home-screen widget is a `RemoteViews`, and `RemoteViews` cannot draw: no
 * Canvas, no custom View. The only way to put a curve on a widget is to
 * pre-render it to a `Bitmap` and assign it with `setViewBitmap`. That works
 * here because the widget is push-driven — the worker that writes the cache is
 * the same one that repaints it — so the bitmap is produced exactly when the
 * data changes and never has to be kept fresh by a polling loop.
 *
 * The shape is drawn, not decorated: one stroke, no fill, no axes, no dots.
 * A sparkline's job is to say which way the line went at a glance.
 */
object WidgetSparkline {

    private const val MIN_SAMPLES = 3

    fun render(
        values: List<Double>,
        widthPx: Int,
        heightPx: Int,
        strokeColor: Int = Color.parseColor("#4CAF50")
    ): Bitmap? {
        if (values.size < MIN_SAMPLES) return null
        if (widthPx <= 0 || heightPx <= 0) return null

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            this.strokeWidth = maxOf(2f, heightPx / 14f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val min = values.min()
        val max = values.max()
        val range = if (abs(max - min) < 0.0001) 1.0 else (max - min)
        val inset = stroke.strokeWidth
        val usableHeight = (heightPx - inset * 2).coerceAtLeast(1f)
        val stepX = widthPx.toFloat() / (values.size - 1)

        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            // Inverted: canvas y grows downward, price grows upward.
            val y = inset + usableHeight * (1.0 - ((value - min) / range)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, stroke)
        return bitmap
    }
}
