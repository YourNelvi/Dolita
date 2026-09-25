package com.example.erp.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.erp.ui.theme.isDarkTheme
import kotlinx.coroutines.delay

/**
 * Motion system for Dolita.
 *
 * The budget is deliberately small: one orchestrated launch sequence plus
 * micro-motions that answer a user action (switching a tab, converting, copying,
 * refreshing). Nothing animates just because it can — decoration that moves
 * without a reason is noise, and on a rate board noise costs legibility.
 */
object MotionDurations {
    const val FAST = 150
    const val BASE = 250
    const val SLOW = 400
    const val LAUNCH = 500
    const val TICKER = 420
    const val STAGGER = 60
}

/** Decelerating curve used everywhere: fast out of the gate, soft landing. */
val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * True when the user turned animations off at the system level
 * (Settings > Accessibility > Remove animations). Infinite animations are the
 * only ones worth suppressing here; short transitions stay, because a reader
 * perceives them as a state change rather than as decoration.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * The signature moment: a value that rolls like a rate board instead of
 * blinking. A rise scrolls down into place and a drop scrolls up, so the
 * direction of the change is legible before you read the digits.
 */
@Composable
fun TickerNumber(
    value: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val target = targetState.toRateValue() ?: 0.0
            val initial = initialState.toRateValue() ?: 0.0
            val rising = target >= initial
            (fadeIn(animationSpec = tween(MotionDurations.FAST, easing = Emphasized)) +
                slideInVertically(
                    animationSpec = tween(MotionDurations.TICKER, easing = Emphasized)
                ) { height -> if (rising) -(height / 3) else (height / 3) })
                .togetherWith(fadeOut(animationSpec = tween(MotionDurations.FAST)))
        },
        label = "ticker",
        modifier = modifier
    ) { current ->
        Text(
            text = current,
            style = style,
            color = color,
            textAlign = textAlign
        )
    }
}

/** Live indicator: a rate that looks fresh beats a timestamp that claims it is. */
@Composable
fun PulseDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 6.dp
) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = Emphasized),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = Emphasized),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (reduced) 1f else scale)
            .clip(CircleShape)
            .background(color.copy(alpha = if (reduced) 1f else alpha))
    )
}

/**
 * Loading placeholder with a highlight sweeping across it. The sweep is the one
 * animation that gets suppressed under reduced motion: a moving highlight with
 * no end state is decoration by definition.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape
) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing)),
        label = "shimmerProgress"
    )

    val rest = if (isDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
    val peak = if (isDarkTheme()) Color.White.copy(alpha = 0.11f) else Color.Black.copy(alpha = 0.08f)
    val brush = if (reduced) {
        SolidColor(rest)
    } else {
        Brush.linearGradient(
            colors = listOf(rest, peak, rest),
            start = Offset(progress * 900f - 250f, 0f),
            end = Offset(progress * 900f + 250f, 500f)
        )
    }

    Box(modifier = modifier.clip(shape).background(brush))
}

/**
 * One-shot entrance used by the launch sequence. `index` staggers the sections
 * so the screen assembles top-down instead of appearing all at once; once
 * revealed it stays revealed for the rest of the process.
 */
@Composable
fun Entrance(
    index: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * MotionDurations.STAGGER.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(MotionDurations.LAUNCH, easing = Emphasized)) +
            slideInVertically(
                animationSpec = tween(MotionDurations.LAUNCH, easing = Emphasized),
                initialOffsetY = { it / 8 }
            )
    ) {
        content()
    }
}

/**
 * Parses a formatted rate for direction detection. Venezuelan formatting is
 * "36,50" and sometimes "$36,50", neither of which `toDoubleOrNull` accepts, so
 * the decimal separator is normalized and the currency glyph dropped first.
 */
private fun String.toRateValue(): Double? =
    replace(",", ".")
        .filter { it.isDigit() || it == '.' || it == '-' }
        .takeIf { it.isNotEmpty() }
        ?.toDoubleOrNull()

/** Reveal helper for content that answers an action (search result, sheet body). */
@Composable
fun FadeSlideIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(MotionDurations.BASE, easing = Emphasized)),
        exit = fadeOut(animationSpec = tween(MotionDurations.FAST)),
        modifier = modifier
    ) {
        content()
    }
}
