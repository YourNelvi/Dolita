package com.example.erp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.cardContainerColor
import kotlinx.coroutines.delay

/**
 * The cold-start curtain: a quiet brand moment while the first rate is on its
 * way. It is not a progress report, because the fetch has no progress to
 * report; the travelling highlight says "working" without inventing a number.
 *
 * The composition is hand-built from the same tokens as the rest of the app,
 * so it follows every palette, dynamic colour and AMOLED variant instead of
 * hardcoding a splash colour that would disagree with the screen behind it.
 */
@Composable
fun OpeningScreen(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val accent = accentColor()
    val coin = cardContainerColor()
    val background = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // The system splash hands over to this screen, so it enters from slightly
    // small and transparent instead of popping in on the first Compose frame.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(40)
        entered = true
    }

    val markScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "openingMarkScale"
    )
    val markAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = Emphasized),
        label = "openingMarkAlpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 520, delayMillis = 120, easing = Emphasized),
        label = "openingTextAlpha"
    )
    val textOffset by animateDpAsState(
        targetValue = if (entered) 0.dp else 10.dp,
        animationSpec = tween(durationMillis = 520, delayMillis = 120, easing = Emphasized),
        label = "openingTextOffset"
    )
    val transition = rememberInfiniteTransition(label = "opening")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = Emphasized),
            repeatMode = RepeatMode.Reverse
        ),
        label = "openingGlow"
    )
    val breath by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = Emphasized),
            repeatMode = RepeatMode.Reverse
        ),
        label = "openingBreath"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        // A soft accent halo gives the mark depth on graphite and on white.
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = if (reduced) 0.10f else glowAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(132.dp)
                    .scale(markScale)
                    .alpha(markAlpha)
            ) {
                // The coin sits on the same surface as a card, so the brand
                // moment belongs to the app's material language.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(coin)
                        .border(1.dp, accent.copy(alpha = 0.28f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CurrencyExchange,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .size(42.dp)
                            .scale(if (reduced) 1f else breath)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Dolita",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onSurface,
                letterSpacing = 0.4.sp,
                modifier = Modifier
                    .alpha(textAlpha)
                    .offset(y = textOffset)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Actualizando la tasa",
                style = MaterialTheme.typography.bodyLarge,
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha)
                    .offset(y = textOffset)
            )
        }
    }
}
