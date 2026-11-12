package com.example.erp.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.max

/**
 * Draws a [ShareCard] onto a bitmap sized for a chat.
 *
 * Canvas rather than a rendered Compose view on purpose: this runs off the
 * composition, must never depend on the current screen, and has to produce the
 * same picture whether the sheet is open or the app is in the background. A
 * Compose view would need a composition and a live window for no gain, because
 * this layout never scrolls, animates or reacts.
 *
 * The width is fixed and the height follows the content: a rate with a date is
 * shorter than one that also carries a payment block. The previous fixed
 * portrait frame left a band of empty space between the amount and the account,
 * which made the picture look larger than the message it announced. The card
 * still never exceeds [MAX_HEIGHT], so it stays a chat card instead of a poster.
 *
 * The palette is fixed and deliberately independent of the user's theme. A card
 * is read on someone else's phone, in their chat, next to other people's
 * messages: it has to look like the app's card every time, not like a
 * screenshot of whichever palette the sender happens to prefer.
 */
object ShareCardRenderer {

    const val WIDTH = 1080

    private const val MIN_HEIGHT = 760
    private const val MAX_HEIGHT = 1080

    private const val PLATE_MARGIN = 36f
    private const val PLATE_RADIUS = 40f
    private const val PAD = 76f
    private const val CONTENT_TOP = PAD + 160f

    // Type sizes and the gaps between them live together so the height measured
    // for the bitmap and the positions actually drawn cannot drift apart.
    private const val TITLE_SIZE = 40f
    private const val GAP_TITLE_HEADLINE = 28f
    private const val HEADLINE_MAX = 116f
    private const val HEADLINE_MIN = 44f

    private const val GAP_HEADLINE_SUPPORT = 22f
    private const val SUPPORT_SIZE = 42f
    private const val SUPPORT_STEP = 44f

    // The payment block is a panel of its own now: a surface behind the account
    // lines separates them from the figure without a rule floating in space.
    private const val GAP_SUPPORT_PAYMENT = 38f
    private const val PAYMENT_PANEL_PAD = 26f
    private const val PAYMENT_PANEL_RADIUS = 24f
    private const val PAYMENT_LABEL_SIZE = 22f
    private const val PAYMENT_LABEL_GAP = 18f
    private const val PAYMENT_SIZE = 32f
    private const val PAYMENT_STEP = 40f
    private const val MAX_PAYMENT_LINES = 5

    private const val GAP_BLOCK_FOOTNOTE = 40f
    private const val FOOTNOTE_RULE_GAP = 46f
    private const val FOOTNOTE_SIZE = 32f
    private const val FOOTNOTE_DESCENDER = 10f

    private val background = Color.rgb(0x0E, 0x11, 0x16)
    private val plate = Color.rgb(0x17, 0x1A, 0x20)
    private val panel = Color.rgb(0x1C, 0x20, 0x28)
    private val border = Color.rgb(0x2A, 0x2F, 0x38)
    private val panelBorder = Color.rgb(0x2F, 0x35, 0x40)
    private val accent = Color.rgb(0x00, 0xE6, 0x76)
    private val onSurface = Color.WHITE
    private val muted = Color.rgb(0xA8, 0xB0, 0xBD)
    private val paymentText = Color.rgb(0xC7, 0xCE, 0xD9)
    private val dim = Color.rgb(0x8A, 0x93, 0xA0)

    fun render(card: ShareCard): Bitmap {
        val contentWidth = WIDTH - PAD * 2

        // The figure, as large as it can be without touching the edges. This is
        // the one thing the reader is here for, so it decides its own size
        // before the bitmap height is known.
        val headline = card.body.firstOrNull()
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurface
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val headlineSize = headline
            ?.let { fitSize(headlinePaint, it, contentWidth, HEADLINE_MAX, HEADLINE_MIN) }
            ?: 0f
        headlinePaint.textSize = headlineSize

        val supporting = card.body.drop(1)
        val paymentLines = card.payment.take(MAX_PAYMENT_LINES)
        val blockHeight = blockHeight(headlineSize, supporting.size, paymentLines.size)

        // Height follows the content. The minimum keeps a rate-only card from
        // becoming a thin strip; the maximum keeps a full payment card from
        // turning into the tall poster this change removes.
        val neededHeight = CONTENT_TOP + blockHeight + GAP_BLOCK_FOOTNOTE +
            FOOTNOTE_RULE_GAP + FOOTNOTE_DESCENDER + PAD
        val height = neededHeight.toInt().coerceIn(MIN_HEIGHT, MAX_HEIGHT)

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background)

        drawPlate(canvas, height)
        drawWordmark(canvas)

        val footnoteBaseline = height - PAD - FOOTNOTE_DESCENDER
        val ruleTop = footnoteBaseline - FOOTNOTE_RULE_GAP
        val regionTop = CONTENT_TOP
        val regionBottom = ruleTop - GAP_BLOCK_FOOTNOTE
        // When the minimum height leaves extra room, the figure block is centred
        // in it; when the content already fills the card, the slack is zero.
        val slack = max(0f, (regionBottom - regionTop - blockHeight) / 2f)
        var cursor = regionTop + slack

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = TITLE_SIZE
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText(card.title, PAD, cursor + TITLE_SIZE, titlePaint)
        cursor += TITLE_SIZE + GAP_TITLE_HEADLINE

        if (headline != null) {
            canvas.drawText(headline, PAD, cursor + headlineSize, headlinePaint)
            cursor += headlineSize
        }

        if (supporting.isNotEmpty()) {
            cursor += GAP_HEADLINE_SUPPORT
            val supportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = muted
                textSize = SUPPORT_SIZE
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }
            supporting.forEach { line ->
                canvas.drawText(line, PAD, cursor + SUPPORT_SIZE, supportPaint)
                cursor += SUPPORT_STEP
            }
        }

        if (paymentLines.isNotEmpty()) {
            drawPaymentPanel(canvas, paymentLines, cursor + GAP_SUPPORT_PAYMENT)
        }

        // The date, separated by a hairline: a rate without its day is the
        // mistake this card exists to prevent.
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = border }
        canvas.drawRect(PAD, ruleTop, WIDTH - PAD, ruleTop + 2f, rule)

        val footnotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dim
            textSize = FOOTNOTE_SIZE
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText(card.footnote, PAD, footnoteBaseline, footnotePaint)

        return bitmap
    }

    /**
     * Height of the text block above the footnote. It has to match the cursor
     * walk in [render] exactly: the title, the figure, the supporting lines and
     * the optional payment panel.
     */
    private fun blockHeight(headlineSize: Float, supportCount: Int, paymentCount: Int): Float {
        var height = TITLE_SIZE + GAP_TITLE_HEADLINE + headlineSize
        if (supportCount > 0) {
            height += GAP_HEADLINE_SUPPORT + supportCount * SUPPORT_STEP
        }
        if (paymentCount > 0) {
            height += GAP_SUPPORT_PAYMENT + paymentPanelHeight(paymentCount)
        }
        return height
    }

    /** Panel height for [lineCount] account lines, padding included. */
    private fun paymentPanelHeight(lineCount: Int): Float =
        PAYMENT_PANEL_PAD * 2 + PAYMENT_LABEL_SIZE + PAYMENT_LABEL_GAP + PAYMENT_SIZE +
            (lineCount - 1) * PAYMENT_STEP

    private fun drawPlate(canvas: Canvas, height: Int) {
        val rect = RectF(PLATE_MARGIN, PLATE_MARGIN, WIDTH - PLATE_MARGIN, height - PLATE_MARGIN)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = plate }
        canvas.drawRoundRect(rect, PLATE_RADIUS, PLATE_RADIUS, fill)

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = border
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, PLATE_RADIUS, PLATE_RADIUS, stroke)
    }

    /**
     * Identity first, so a reader knows where the number came from before
     * reading the number.
     */
    private fun drawWordmark(canvas: Canvas) {
        val wordmark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurface
            textSize = 54f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText("Dolita", PAD, PAD + 50f, wordmark)

        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRoundRect(
            RectF(PAD, PAD + 70f, PAD + 110f, PAD + 78f),
            4f, 4f, bar
        )
    }

    /**
     * Draws the payment block inside its own rounded surface: a muted label and
     * the account lines, all smaller than the figure above them.
     *
     * A saved profile can carry a long bank alias, so each line is ellipsized to
     * the panel width rather than wrapped into the date's reserved space.
     */
    private fun drawPaymentPanel(canvas: Canvas, lines: List<String>, top: Float) {
        val panelHeight = paymentPanelHeight(lines.size)
        val rect = RectF(PAD, top, WIDTH - PAD, top + panelHeight)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = panel }
        canvas.drawRoundRect(rect, PAYMENT_PANEL_RADIUS, PAYMENT_PANEL_RADIUS, fill)

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = panelBorder
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, PAYMENT_PANEL_RADIUS, PAYMENT_PANEL_RADIUS, stroke)

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dim
            textSize = PAYMENT_LABEL_SIZE
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.08f
        }
        val labelBaseline = top + PAYMENT_PANEL_PAD + PAYMENT_LABEL_SIZE
        canvas.drawText("DATOS DE PAGO", PAD + PAYMENT_PANEL_PAD, labelBaseline, label)

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paymentText
            textSize = PAYMENT_SIZE
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val maxTextWidth = WIDTH - PAD * 2 - PAYMENT_PANEL_PAD * 2
        var baseline = labelBaseline + PAYMENT_LABEL_GAP + PAYMENT_SIZE
        lines.forEach { line ->
            val fitted = TextUtils.ellipsize(
                line,
                bodyPaint,
                maxTextWidth,
                TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(fitted, PAD + PAYMENT_PANEL_PAD, baseline, bodyPaint)
            baseline += PAYMENT_STEP
        }
    }

    /**
     * Largest size at or below [max] where [text] still fits [maxWidth].
     *
     * Shrinking beats wrapping here: a figure split across two lines stops
     * reading as one amount, and a wrapped line would push the date out of the
     * height measured before drawing.
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
