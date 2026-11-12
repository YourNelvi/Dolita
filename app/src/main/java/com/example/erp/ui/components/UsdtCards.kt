package com.example.erp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.erp.data.DolarQuote
import com.example.erp.data.RateSchedulePolicy
import com.example.erp.ui.UsdtRateMode
import com.example.erp.ui.precioPara
import com.example.erp.ui.tieneTasa
import com.example.erp.ui.theme.cardBorder
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.cardContainerColor
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.ZoneId
import java.util.Locale

/**
 * The hourly USDT sample as a countdown the reader can see, with the manual
 * refresh button that spends only the one request it advertises.
 *
 * The next fetch is a function of the clock and the worker's real window, not
 * of the last sample, so it keeps ticking while a refresh is in flight. The bar
 * drains from the previous scheduled slot to the next one: full right after a
 * sample, empty right before the next. A manual refresh does not restart it
 * because the automatic sample still lands on the same hour. Without a
 * parseable sample the card states the cadence and leaves the bar empty instead
 * of inventing progress.
 */
@Composable
fun UsdtCountdownCard(
    lastFetch: String?,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastFetchMillis = remember(lastFetch) {
        RateSchedulePolicy.parseUsdtFetchMillis(lastFetch)
    }
    // One clock read per second: the countdown ticks, the progress bar moves
    // with it, and recomposition stays scoped to this card.
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1_000L)
            value = System.currentTimeMillis()
        }
    }
    val countdown = RateSchedulePolicy.usdtCountdown(
        nowMillis = nowMillis,
        lastFetchMillis = lastFetchMillis,
        zoneId = ZoneId.systemDefault()
    )
    val hasSample = lastFetchMillis != null
    val remainingFraction = if (hasSample) countdown.remainingFraction else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Próxima actualización",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (hasSample) {
                            RateSchedulePolicy.formatUsdtCountdown(countdown.remainingMillis)
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasSample) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                FilledTonalIconButton(
                    onClick = onRefresh,
                    enabled = !refreshing,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Actualizar USDT",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { remainingFraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Cada hora, de 8:00 a 22:00",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            error?.let { message ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * The USDT buy and sell sides, and the choice of which one the calculator (and
 * its shared result) converts with.
 *
 * The canonical average stays on the hero, the history and the cache; this card
 * only picks the number the calculator reads. A side the payload did not
 * publish is shown as unavailable and cannot be selected, because offering a
 * switch to a rate that does not exist would be a lie with a nice border.
 */
@Composable
fun UsdtRatesCard(
    quote: DolarQuote,
    selected: UsdtRateMode,
    onSelect: (UsdtRateMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val modes = listOf(UsdtRateMode.COMPRA, UsdtRateMode.VENTA, UsdtRateMode.PROMEDIO)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Tasas USDT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Mediana de ofertas P2P · elige la tasa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { mode ->
                    val available = quote.tieneTasa(mode)
                    UsdtRateTile(
                        label = labelFor(mode),
                        value = if (available) {
                            "Bs ${formatter.format(quote.precioPara(mode))}"
                        } else {
                            "—"
                        },
                        available = available,
                        selected = selected == mode,
                        onSelect = { onSelect(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * One selectable side. The border and the tint carry the selected state, and
 * the "En uso" line is always laid out — transparent when idle — so selecting a
 * tile never changes the row's height.
 */
@Composable
private fun UsdtRateTile(
    label: String,
    value: String,
    available: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val active = selected && available
    val borderColor = when {
        active -> MaterialTheme.colorScheme.primary
        else -> cardBorderColor()
    }
    val labelColor = if (available) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    val valueColor = when {
        !available -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = shape,
        color = if (active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            Color.Transparent
        },
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(shape)
            .clickable(enabled = available, onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "En uso",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                maxLines = 1
            )
        }
    }
}

private fun labelFor(mode: UsdtRateMode): String = when (mode) {
    UsdtRateMode.COMPRA -> "Compra"
    UsdtRateMode.VENTA -> "Venta"
    UsdtRateMode.PROMEDIO -> "Promedio"
}
