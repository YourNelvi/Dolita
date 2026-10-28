package com.example.erp.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Draws a [ShareCard] onto a bitmap sized for a chat.
 *
 * Canvas rather than a rendered Compose view on purpose: this runs off the
 * composition, must never depend on the current screen, and has to produce the
 * same picture whether the sheet is open or the app is in the background. A
 * Compose view would need a composition and a live window for no gain, because
 * this layout never scrolls, animates or reacts.
 *
 * The palette is fixed and deliberately independent of the user's theme. A card
 * is read on someone else's phone, in their chat, next to other people's
 * messages: it has to look like the app's card every time, not like a
 * screenshot of whichever palette the sender happens to prefer.
 */
object ShareCardRenderer {

    /** 4:5, the shape WhatsApp shows without cropping the sides. */
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val PAD = 96f

    // Type sizes and the gaps between them live here so the height measured for
    // centring and the positions actually drawn cannot drift apart.
    private const val TITLE_SIZE = 46f
    private const val SUPPORT_SIZE = 48f
    private const val HEADLINE_MAX = 132f
    private const val HEADLINE_MIN = 48f
    private const val GAP_TITLE_TO_HEADLINE = 40f
    private const val GAP_HEADLINE_TO_SUPPORT = 28f
    private const val GAP_BETWEEN_SUPPORT = 14f

    private val background = Color.rgb(0x14, 0x16, 0x1B)
    private val surface = Color.rgb(0x19, 0x1C, 0x22)
    private val border = Color.rgb(0x2A, 0x2F, 0x38)
    private val accent = Color.rgb(0x00, 0xE6, 0x76)
    private val onSurface = Color.WHITE
    private val muted = Color.rgb(0xA8, 0xB0, 0xBD)
    private val dim = Color.rgb(0x8A, 0x93, 0xA0)

    fun render(card: ShareCard): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background)

        val contentWidth = WIDTH - PAD * 2

        // Card plate, so the picture reads as one object rather than text
        // floating on a background that could be any shade in the chat.
        val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surface }
        canvas.drawRoundRect(
            RectF(PAD / 2, PAD / 2, WIDTH - PAD / 2, HEIGHT - PAD / 2),
            48f, 48f, plate
        )
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = border
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(
            RectF(PAD / 2, PAD / 2, WIDTH - PAD / 2, HEIGHT - PAD / 2),
            48f, 48f, stroke
        )

        // Wordmark and the accent bar under it: identity first, so a reader
        // knows where the number came from before reading the number.
        val wordmark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurface
            textSize = 58f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText("Dolita", PAD, PAD + 60f, wordmark)

        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRoundRect(
            RectF(PAD, PAD + 88f, PAD + 120f, PAD + 96f),
            4f, 4f, bar
        )

        // What the number is.
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = TITLE_SIZE
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        // The figure, as large as it can be without touching the edges. This is
        // the one thing the reader is here for.
        val headline = card.body.firstOrNull()
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurface
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val headlineSize = headline
            ?.let { fitSize(headlinePaint, it, contentWidth, HEADLINE_MAX, HEADLINE_MIN) }
            ?: 0f
        headlinePaint.textSize = headlineSize

        // Any further line, e.g. the rate that produced a conversion. Smaller
        // than the headline because it explains the figure, it is not the figure.
        val supporting = card.body.drop(1)
        val supportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = SUPPORT_SIZE
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        // The block is measured before it is drawn, because it is centred in the
        // space between the wordmark and the date. Anchoring it to the top
        // instead left well over half the card empty: a figure pinned under the
        // logo with a void beneath it reads as a layout that ran out of content,
        // not as one with room to breathe.
        var blockHeight = TITLE_SIZE
        if (headline != null) blockHeight += GAP_TITLE_TO_HEADLINE + headlineSize
        if (supporting.isNotEmpty()) {
            blockHeight += GAP_HEADLINE_TO_SUPPORT +
                supporting.size * (SUPPORT_SIZE + GAP_BETWEEN_SUPPORT)
        }

        val regionTop = PAD + 210f
        val regionBottom = HEIGHT - PAD - 130f
        val slack = ((regionBottom - regionTop - blockHeight) / 2f).coerceAtLeast(0f)
        var cursor = regionTop + slack

        canvas.drawText(card.title, PAD, cursor + TITLE_SIZE, titlePaint)
        cursor += TITLE_SIZE + GAP_TITLE_TO_HEADLINE
        if (headline != null) {
            canvas.drawText(headline, PAD, cursor + headlineSize, headlinePaint)
            cursor += headlineSize + GAP_HEADLINE_TO_SUPPORT
        }
        supporting.forEach { line ->
            canvas.drawText(line, PAD, cursor + SUPPORT_SIZE, supportPaint)
            cursor += SUPPORT_SIZE + GAP_BETWEEN_SUPPORT
        }

        // The date, pinned to the bottom and separated by a hairline: a rate
        // without its day is the mistake this card exists to prevent.
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = border }
        canvas.drawRect(PAD, HEIGHT - PAD - 96f, WIDTH - PAD, HEIGHT - PAD - 94f, rule)

        val footnote = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dim
            textSize = 38f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText(card.footnote, PAD, HEIGHT - PAD - 34f, footnote)

        return bitmap
    }

    /**
     * Largest size at or below [max] where [text] still fits [maxWidth].
     *
     * Shrinking beats wrapping here: a figure split across two lines stops
     * reading as one amount, and the card has a fixed height that a wrapped
     * line would push the date out of.
     */
    private fun fitSize(
        paint: Paint,
        text: String,
        maxWidth: Float,
        max: Float,
        min: Float
    ): Float {
        var size = max
        paint.textSize = size
        while (size > min && paint.measureText(text) > maxWidth) {
            size -= 2f
            paint.textSize = size
        }
        return size
    }
}
