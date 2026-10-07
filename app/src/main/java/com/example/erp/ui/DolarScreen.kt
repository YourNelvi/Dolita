package com.example.erp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.erp.data.DolarQuote
import com.example.erp.data.Error as AppError
import com.example.erp.data.HISTORICO_PUNTOS
import com.example.erp.data.RateSample
import com.example.erp.data.ThemeMode
import com.example.erp.ui.components.CalculatorCard
import com.example.erp.ui.components.Emphasized
import com.example.erp.ui.components.Entrance
import com.example.erp.ui.components.EvolutionChart
import com.example.erp.ui.components.MotionDurations
import com.example.erp.ui.components.PulseDot
import com.example.erp.ui.components.ShimmerBox
import com.example.erp.ui.components.ThemeBottomSheetContent
import com.example.erp.ui.components.TickerNumber
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.DownRedLight
import com.example.erp.ui.theme.ERPTheme
import com.example.erp.ui.theme.FintechSignalRed
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.positiveColor
import com.example.erp.ui.theme.cardBadgeColor
import com.example.erp.ui.theme.cardBorder
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.cardContainerColor
import com.example.erp.ui.theme.isDarkTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import android.os.Build

private val priceFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private fun formatPrice(value: Double): String = "Bs ${priceFormatter.format(value)}"

private val updatedFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

private fun formatUpdated(value: String): String {
    try {
        return LocalDate.parse(value).format(dateFormatter)
    } catch (e: Exception) {
        // not a date-only value, try full timestamp below
    }
    return try {
        OffsetDateTime.parse(value).format(updatedFormatter)
    } catch (e: Exception) {
        value
    }
}

// Positive signal: always green (independent of the palette accent).
@Composable
private fun trendColor(): Color = positiveColor()

// Negative signal: always semantic red (never the accent green).
@Composable
private fun downColor(): Color = if (isDarkTheme()) FintechSignalRed else DownRedLight

/**
 * The calculator's amount fields as the card holds them, in the digit encoding
 * the fields use ("123456" is 1.234,56). One holder for both sides so the two
 * published amounts can never be read from two different points in time.
 */
private data class CalculatorAmounts(
    val bolivaresDigits: String = "",
    val divisaDigits: String = ""
)

@Composable
fun DolarScreen(
    viewModel: DolarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    DolarScreenContent(
        uiState = uiState,
        onSelectCasa = viewModel::select,
        // Explicit lambda, not `viewModel::load`: a method reference to a
        // function with a default argument binds force=false, so the refresh
        // button was silently asking the cache to render itself instead of
        // asking the network — and "Reintentar" inherited the same no-op.
        onRefresh = { viewModel.load(force = true) },
        onToggleFutureRate = viewModel::toggleFutureRate,
        onToggleSelectedDateRate = viewModel::toggleSelectedDateRate,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DolarScreenContent(
    uiState: DolarUiState,
    onSelectCasa: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleFutureRate: () -> Unit,
    onToggleSelectedDateRate: () -> Unit = {},
    viewModel: DolarViewModel
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val currentTheme by viewModel.theme.collectAsState(initial = AppTheme.AZUL_BANCARIO)
    val currentMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val currentDynamicColor by viewModel.dynamicColorEnabled.collectAsState(initial = false)
    val currentHighPrecision by viewModel.highPrecisionEnabled.collectAsState(initial = false)
    val isDynamicColorAvailable = Build.VERSION.SDK_INT >= 31 // Android 12+
    // The refresh glyph completes one turn per fetch and parks there — a spin
    // that keeps turning forever pretends to be working.
    val refreshTurn by animateFloatAsState(
        targetValue = if (uiState.loading) 1f else 0f,
        animationSpec = if (uiState.loading) {
            tween(durationMillis = 700, easing = LinearEasing)
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "refreshTurn"
    )

    Scaffold(
        topBar = {
            Entrance(index = 0) {
                CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CurrencyExchange,
                            contentDescription = null,
                            // Chrome stays neutral: the accent belongs to the numbers.
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Dolita",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(refreshTurn * 360f)
                        )
                    }
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
                )
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()

        when {
            uiState.loading && uiState.quotes.isEmpty() -> {
                LoadingSkeleton(modifier = contentModifier)
            }

            uiState.error != null && uiState.quotes.isEmpty() -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = onRefresh,
                    modifier = contentModifier
                )
            }

            else -> {
                DolarContent(
                    uiState = uiState,
                    onSelectCasa = onSelectCasa,
                    onToggleFutureRate = onToggleFutureRate,
                    onToggleSelectedDateRate = onToggleSelectedDateRate,
                    viewModel = viewModel,
                    highPrecision = currentHighPrecision,
                    modifier = contentModifier
                )
            }
        }

        // Custom Bottom Sheet Overlay — rises from the bottom edge and settles,
        // exits the same way instead of blinking out of existence.
        AnimatedVisibility(
            visible = showBottomSheet,
            enter = fadeIn(
                animationSpec = tween(MotionDurations.BASE, easing = Emphasized)
            ) + slideInVertically(
                animationSpec = tween(MotionDurations.SLOW, easing = Emphasized),
                initialOffsetY = { it / 3 }
            ),
            exit = fadeOut(animationSpec = tween(MotionDurations.FAST)) +
                slideOutVertically(
                    animationSpec = tween(MotionDurations.BASE, easing = Emphasized),
                    targetOffsetY = { it / 3 }
                )
        ) {
            // Back button closes the sheet instead of exiting the app
            BackHandler { showBottomSheet = false }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showBottomSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Clip must wrap the background, otherwise the sheet's
                        // rounded top corners would be drawn over a square fill.
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { /* consume clicks */ }
                ) {
                    ThemeBottomSheetContent(
                        currentTheme = currentTheme,
                        currentMode = currentMode,
                        currentDynamicColor = currentDynamicColor,
                        currentHighPrecision = currentHighPrecision,
                        onThemeChange = { theme ->
                            viewModel.setTheme(theme)
                            // NO cerrar — dejar al usuario seguir configurando
                        },
                        onModeChange = { mode ->
                            viewModel.setThemeMode(mode)
                            // NO cerrar — dejar al usuario seguir configurando
                        },
                        onDynamicColorChange = { enabled ->
                            viewModel.setDynamicColorEnabled(enabled)
                        },
                        onHighPrecisionChange = { enabled ->
                            viewModel.setHighPrecisionEnabled(enabled)
                        },
                        isDynamicColorAvailable = isDynamicColorAvailable,
                        onDismiss = { showBottomSheet = false }
                    )
                }
            }
        }
    }
}

/**
 * Loading state that mirrors the real layout: a rate board of the right shape,
 * not a spinner floating in a void. The silhouette stays stable so nothing
 * jumps when the data lands.
 */
@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp),
                shape = RoundedCornerShape(16.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Cold-start failure with nothing stored to show. The raw exception belongs to
 * the developer, not the reader: "Unable to resolve host …" is a stack trace
 * wearing a UI costume. This states what is missing and what unblocks it, and
 * nothing else.
 */
@Composable
private fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offline = error is AppError.NetworkError
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (offline) Icons.Rounded.CloudOff else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (offline) "Sin conexión" else "No pudimos cargar la tasa",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (offline) {
                "Conectate a internet y volvé a intentar para ver la tasa de hoy."
            } else {
                "El servidor no respondió. Probá de nuevo en un momento."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        HairlineAction(label = "Reintentar", onClick = onRetry)
    }
}

/**
 * Quiet action for empty and failure states. A filled M3 pill shouts in a
 * layout that is otherwise flat and hairline-bordered; this keeps the same
 * voice as every other affordance in the app.
 */
@Composable
private fun HairlineAction(
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(shape)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, accentColor().copy(alpha = 0.4f)),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = accentColor(),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor()
        )
    }
}

/**
 * The share affordance itself: a flat square with a hairline border and no
 * tonal fill, in the same voice as [HairlineAction]. A filled M3 pill next to
 * these numbers would outshout them.
 *
 * The tap does nothing when there is nothing to send. An empty share sheet is a
 * dead control, not a harmless one, so callers build the payload first and
 * return before launching anything.
 */
@Composable
private fun ShareIconButton(
    onShare: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        shape = shape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor()),
        modifier = modifier
            // Clip before the click so the touch area is the rounded square the
            // eye sees, not the untrimmed bounds behind it.
            .clip(shape)
            .clickable(onClick = onShare)
    ) {
        Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                // 16 + 16 on each side keeps the touch target at 48dp, the
                // minimum Material asks for, without a filled affordance around it.
                .padding(16.dp)
                .size(16.dp)
        )
    }
}

/**
 * Hands plain text to the system chooser. One place decides HOW this app shares,
 * so every payload leaves the device as `ACTION_SEND` text — never a file, never
 * an image, never a rendered screenshot of a number.
 */
private fun sharePlainText(context: android.content.Context, text: String, chooserTitle: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(send, chooserTitle))
}

/**
 * Share a rate as plain text. Text and not a picture: the reader pastes this
 * into a chat, where a rendered image of a number is a worse number than the
 * number itself.
 */
@Composable
private fun ShareRateAction(
    quote: DolarQuote?,
    highPrecision: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    ShareIconButton(
        onShare = {
            val text = RateShareText.build(
                quote = quote,
                highPrecision = highPrecision
            ) ?: return@ShareIconButton
            sharePlainText(context, text, "Compartir tasa")
        },
        contentDescription = "Compartir ${quote?.nombre ?: "la tasa"}",
        modifier = modifier
    )
}

/**
 * Share what the calculator produced, not just the rate it converted with.
 *
 * Both amounts travel, because a rate on its own is already shareable from the
 * hero and this control exists for the RESULT the user typed. The day comes
 * from the rate that did the conversion, so a next-day or calendar-selected rate
 * says which day it belongs to instead of letting the message read as today's.
 */
@Composable
private fun ShareCalculatorResultAction(
    quote: DolarQuote?,
    amounts: CalculatorAmounts,
    highPrecision: Boolean,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    ShareIconButton(
        onShare = {
            val text = CalculatorShareText.build(
                quote = quote,
                bolivaresDigits = amounts.bolivaresDigits,
                divisaDigits = amounts.divisaDigits,
                highPrecision = highPrecision
            ) ?: return@ShareIconButton
            sharePlainText(context, text, "Compartir cálculo")
        },
        contentDescription = "Compartir el cálculo",
        modifier = modifier
    )
}

/**
 * Honesty strip for a snapshot the app could not refresh. Known-good numbers
 * with a clear "these are old" note beat an empty screen — the reader always
 * knows which of the two they are looking at.
 */
@Composable
private fun StaleBanner(
    timestampMillis: Long?,
    modifier: Modifier = Modifier
) {
    val stamp = remember(timestampMillis) {
        timestampMillis?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM · HH:mm"))
        }
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardContainerColor())
            .border(cardBorder(), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sin conexión",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stamp?.let { "Mostrando la tasa del $it" }
                    ?: "Mostrando la última tasa guardada",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DolarContent(
    uiState: DolarUiState,
    onSelectCasa: (String) -> Unit,
    onToggleFutureRate: () -> Unit,
    onToggleSelectedDateRate: () -> Unit,
    viewModel: DolarViewModel,
    highPrecision: Boolean,
    modifier: Modifier = Modifier
) {
    val selected = uiState.quotes.firstOrNull { it.fuente == uiState.selectedFuente }
        ?: uiState.quotes.firstOrNull()

    // The calculator's two amount fields, lifted out of the card so the share
    // control can report exactly what is on screen. Empty until the card
    // publishes them, which also makes an early share a no-op rather than a
    // message about nothing.
    var calculatorAmounts by remember { mutableStateOf(CalculatorAmounts()) }

    // The launch sequence lives here, on the container: one reveal for the whole
    // board, 60ms behind the top bar. Animating individual items would replay
    // every time they re-enter the viewport.
    Entrance(index = 1) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sits above the hero rate on purpose: the reader learns these numbers
        // are old before they read them, not after.
        if (uiState.isStale) {
            item { StaleBanner(timestampMillis = uiState.staleSinceEpochMillis) }
        }

        item {
            // Switching casa crossfades the board instead of hard-swapping it.
            // No Entrance() here on purpose: per-item entrance animations
            // re-run every time an item scrolls back into view, which reads as
            // the list sticking under your thumb.
            AnimatedContent(
                targetState = selected,
                // contentKey on the SOURCE, not on the quote object: a rate
                // refresh hands us a brand new DolarQuote every time, and
                // re-running the crossfade on each of those made the whole page
                // shift under the reader. Only a real source switch animates.
                contentKey = { it?.fuente },
                transitionSpec = {
                    (fadeIn(
                        animationSpec = tween(MotionDurations.BASE, easing = Emphasized)
                    ) + slideInVertically(
                        animationSpec = tween(MotionDurations.BASE, easing = Emphasized)
                    ) { it / 10 })
                        .togetherWith(fadeOut(animationSpec = tween(MotionDurations.FAST)))
                },
                label = "heroSwap"
            ) { quote ->
                FeaturedCard(
                    quote = quote,
                    highPrecision = highPrecision,
                    futureQuote = uiState.futureQuote,
                    officialUsdRate = uiState.quotes.firstOrNull { it.fuente == "usd" }?.promedio
                )
            }
        }

        item {
            CasaChips(
                quotes = uiState.quotes,
                selectedCasa = uiState.selectedFuente,
                onSelect = onSelectCasa
            )
        }

        // The two alternative rates live together right above the section they
        // change: separated, they read as independent read-only cards, and the
        // calculator's actual input is invisible.
        item {
            CalculatorRateBars(
                uiState = uiState,
                highPrecision = highPrecision,
                onToggleFutureRate = onToggleFutureRate,
                onToggleSelectedDateRate = onToggleSelectedDateRate
            )
        }

        item { SectionHeader("Calculadora") }
        item {
            val calcQuote = CalculatorRateResolver.resolve(
                today = selected,
                future = uiState.futureQuote,
                futureSource = uiState.calcRateSource,
                selectedSample = uiState.selectedDateRate,
                selectedLabel = uiState.selectedDateLabel,
                dateSource = uiState.calcRateSource
            )
            calcQuote?.let { quote ->
                Column {
                    CalculatorCard(
                        quote = quote,
                        quotes = uiState.quotes,
                        onAmountsChanged = { bolivares, divisa ->
                            calculatorAmounts = CalculatorAmounts(bolivares, divisa)
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    // Inside the calculator section, not in the top bar: it
                    // shares this card's answer, and it sits with the numbers it
                    // is about.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ShareCalculatorResultAction(
                            quote = quote,
                            amounts = calculatorAmounts,
                            highPrecision = highPrecision
                        )
                    }
                }
            }
        }

        item { SectionHeader("Histórico") }
        when (val historico = historicoState(uiState.historial)) {
            is HistoricoState.SinDatos -> item { SinDatosCard() }
            is HistoricoState.ConDatos -> {
                item { HistoricoChartCard(samples = historico.samples) }
            }
        }

        // Calendario: consultar tasa por fecha
        item { CalendarLookupSection(uiState = uiState, viewModel = viewModel) }

        item { SectionHeader("Cotizaciones") }
        items(uiState.quotes, key = { it.fuente }) { quote ->
            QuoteRow(
                quote = quote,
                selected = quote.fuente == uiState.selectedFuente,
                highPrecision = highPrecision,
                onClick = { onSelectCasa(quote.fuente) }
            )
        }

        item {
                Text(
                    text = "Precios: rates.dolarvzla.com (BCV)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
        }
    }
    }
}

@Composable
private fun FeaturedCard(
    quote: DolarQuote?,
    highPrecision: Boolean = false,
    futureQuote: DolarQuote? = null,
    officialUsdRate: Double? = null
) {
    if (quote == null) return
    val shape = RoundedCornerShape(20.dp)
    val fracDigits = if (highPrecision) 4 else 2
    val featPriceFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = fracDigits
        maximumFractionDigits = fracDigits
    }
    fun featFormatPrice(value: Double): String = "Bs ${featPriceFormatter.format(value)}"

    // The gap between the parallel market and the official rate is a MAGNITUDE,
    // not a direction: a wider gap is not a "gain" and must not wear the green
    // that means "subió". The sign carries the information, the accent carries
    // the emphasis, and it only appears when both rates are actually known.
    val spreadPercent = if (quote.fuente == "usdt" && officialUsdRate != null && officialUsdRate > 0.0) {
        (quote.promedio - officialUsdRate) / officialUsdRate * 100.0
    } else {
        null
    }
    // Fintech hero card: flat surface + hairline border (no M3 elevation, no
    // full-background color fill). The accent green is spent on the big number.
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = quote.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            // The signature moment: the rate rolls into place like a board
            // instead of blinking between two values.
            TickerNumber(
                value = featFormatPrice(quote.promedio),
                style = MaterialTheme.typography.displayLarge,
                color = accentColor(),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = cardBadgeColor(),
                modifier = Modifier.clip(RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quote.variacion?.let { variacion ->
                        val signalColor = if (variacion >= 0) trendColor() else downColor()
                        TrendIcon(variacion, signalColor)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${if (variacion >= 0) "+" else ""}${"%.2f".format(variacion)}%",
                            color = signalColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    // Liveness, not direction: the dot wears the theme accent.
                    // Direction (the +/- values) keeps semantic green/red.
                    PulseDot(color = accentColor())
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Act. ${formatUpdated(quote.fechaActualizacion)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Only the parallel market has a gap worth stating, and only while
            // both rates are known.
            spreadPercent?.let { spread ->
                val gapPerDollar = spreadPercent?.let { percent ->
                    officialUsdRate?.let { it * percent / 100.0 }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (spread >= 0) "+" else ""}${"%.2f".format(spread)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor()
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "sobre el oficial",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                gapPerDollar?.let { gap ->
                    Spacer(Modifier.height(2.dp))
                    // Bolívares, never dollars: the gap is what one extra dollar
                    // costs on the parallel, so the "$" of featFormatPrice read as
                    // "105,43 dollars" — off by the price of a dollar, and the
                    // calculator states the very same figure as "Bs 105,43".
                    Text(
                        text = "Son Bs ${featPriceFormatter.format(abs(gap))} por dólar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeaturedStat(
                    label = "Fuente",
                    value = quote.fuente.uppercase()
                )
                FeaturedStat(label = "Promedio", value = featFormatPrice(quote.promedio))
                quote.anterior?.let {
                    FeaturedStat(label = "Anterior", value = featFormatPrice(it))
                }
                quote.anterior?.let { anterior ->
                    val cambio = quote.promedio - anterior
                    val signo = if (cambio >= 0) "+" else ""
                    val color = if (cambio >= 0) trendColor() else downColor()
                    FeaturedStatColored(
                        label = "Cambio",
                        value = "${signo}${featPriceFormatter.format(cambio)} Bs",
                        color = color
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // The share affordance sits directly under the number it shares:
            // on the hero card the rate is the subject, so the control belongs
            // with the number, not out in a corner competing with the chrome.
            ShareRateAction(quote = quote, highPrecision = highPrecision)
        }
    }
}

@Composable
private fun FeaturedStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FeaturedStatColored(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * The calculator's rate choice, shown as a stack of toggles right above the
 * calculator. Only rates that actually exist are rendered: a next-day quote
 * that the API never published and a calendar lookup that found nothing are
 * not options, so offering them would promise a switch that cannot happen.
 */
@Composable
private fun CalculatorRateBars(
    uiState: DolarUiState,
    highPrecision: Boolean = false,
    onToggleFutureRate: () -> Unit = {},
    onToggleSelectedDateRate: () -> Unit = {}
) {
    val future = uiState.futureQuote
    val sample = uiState.selectedDateRate
    if (future == null && sample == null) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (future != null) {
            ProximaTasaCard(
                future = future,
                highPrecision = highPrecision,
                isActive = uiState.calcRateSource == CalculatorRateSource.NEXT_RATE,
                onClick = onToggleFutureRate
            )
        }
        if (sample != null) {
            TasaPorFechaCard(
                sample = sample,
                label = uiState.selectedDateLabel,
                highPrecision = highPrecision,
                isActive = uiState.calcRateSource == CalculatorRateSource.SELECTED_DATE,
                onClick = onToggleSelectedDateRate
            )
        }
    }
}

@Composable
private fun ProximaTasaCard(
    future: DolarQuote,
    highPrecision: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    val fracDigits = if (highPrecision) 4 else 2
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = fracDigits
        maximumFractionDigits = fracDigits
    }
    // Thin 1dp hairline in every state; the accent border only marks the
    // active toggle, so the card reads as a control and not a colored banner.
    val borderColor = if (isActive) accentColor() else cardBorderColor()
    val titleColor = if (isActive) accentColor() else MaterialTheme.colorScheme.onSurface
    val valueColor = if (isActive) accentColor() else MaterialTheme.colorScheme.onSurface

    // What tomorrow's rate costs against the rate it replaces. The arithmetic
    // and the missing-value rules live in rateChangeOf(); this composable only
    // decides how the answer reads.
    val change = rateChangeOf(future)
    // Neutral for zero AND for absent, the same way EvolutionChart reads a
    // missing or flat variation: an unknown move must not wear a green.
    val changeColor = when (change?.direction) {
        RateChangeDirection.UP -> trendColor()
        RateChangeDirection.DOWN -> downColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // Each half is printed only if the quote published it, so a rate that
    // arrives without a previous value shows no delta at all rather than a
    // confident "+0,00 Bs" the API never claimed.
    val changeText = change?.let { rateChange ->
        buildList {
            rateChange.bolivaresDelta?.let { add(formatBolivaresDelta(it)) }
            rateChange.percent?.let { add(formatRatePercent(it)) }
        }.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isActive) "Usando tasa futura" else "Proxima tasa",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = formatUpdated(future.fechaActualizacion),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Same voice as the line above: no pill, no fill, just the
                // number in its signal colour.
                changeText?.let { text ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = changeColor
                    )
                }
            }
            Text(
                text = "Bs ${fmt.format(future.promedio)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

/**
 * Same visual language as the next-day bar, because it fills the same role: a
 * selectable alternative rate for the calculator. The calendar section keeps
 * showing the lookup result; this bar is the control that applies it.
 */
@Composable
private fun TasaPorFechaCard(
    sample: RateSample,
    label: String?,
    highPrecision: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    val fracDigits = if (highPrecision) 4 else 2
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = fracDigits
        maximumFractionDigits = fracDigits
    }
    val borderColor = if (isActive) accentColor() else cardBorderColor()
    val titleColor = if (isActive) accentColor() else MaterialTheme.colorScheme.onSurface
    val valueColor = if (isActive) accentColor() else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isActive) "Usando tasa del ${label ?: "ese dia"}" else "Tasa de ${label ?: "esa fecha"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = sample.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Bs ${fmt.format(sample.precio)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

/**
 * Source selector as a fintech capsule segmented control: shared track with a
 * hairline border, a contrasting active item and a small accent indicator dot
 * bar. Replaces the Material FilterChip row (no ripple, no tonal fills).
 */
@Composable
private fun CasaChips(
    quotes: List<DolarQuote>,
    selectedCasa: String,
    onSelect: (String) -> Unit
) {
    val trackShape = RoundedCornerShape(50)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, cardBorderColor(), trackShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        quotes.forEach { quote ->
            val selected = quote.fuente == selectedCasa
            val activeBackground =
                if (isDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.White
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(trackShape)
                    .background(if (selected) activeBackground else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                        shape = trackShape
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onSelect(quote.fuente) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = quote.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                // Subtle active-state indicator under the selected segment: it
                // slides out from nothing instead of popping into existence.
                val indicatorWidth by animateDpAsState(
                    targetValue = if (selected) 16.dp else 0.dp,
                    animationSpec = tween(MotionDurations.BASE, easing = Emphasized),
                    label = "casaIndicator"
                )
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(2.dp)
                        .clip(trackShape)
                        .background(accentColor())
                )
            }
        }
    }
}

@Composable
private fun HistoricoChartCard(samples: List<com.example.erp.data.RateSample>) {
    // The header states the coverage the samples actually have. It used to be a
    // hardcoded "Últimos 15 días", which lied on a fresh install and permanently
    // for the parallel series — nothing fetches a historical P2P rate, so that
    // chart can only ever hold what was sampled live.
    val title = remember(samples) { historicoTitle(samples) }
    val isHourly = remember(samples) { samples.any { it.fuente == "usdt" } }
    val maxPoints = remember(samples) {
        if (isHourly) HISTORICO_MUESTRAS_HORARIAS else HISTORICO_PUNTOS
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            EvolutionChart(
                samples = samples,
                maxPoints = maxPoints,
                showHours = isHourly,
                // No fixed height: the chart also renders the dated grid of every
                // drawn point, so the card has to grow with the series.
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SinDatosCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sin datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "El histórico se genera con cada carga exitosa de cotizaciones.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarLookupSection(
    uiState: DolarUiState,
    viewModel: DolarViewModel
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // Limite: midnight UTC de hoy. The rule for which days exist at all lives in
    // CalendarioDateRules, so the picker and its unit test cannot drift apart.
    val todayUtc = remember { java.time.LocalDate.now() }
    val todayUtcMillis = remember(todayUtc) {
        todayUtc.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    // El dia publicado: el unico dia futuro que el calendario ofrece, y el unico
    // que lleva el punto verde. Viene de la cotizacion, no de un reloj: la tasa
    // "de manana" del BCV es para el proximo dia que publica, que no siempre es
    // manana (los fines de semana no publica).
    val publishedDay = remember(uiState.futureQuote) { publishedDayOf(uiState.futureQuote) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayUtcMillis,
        selectableDates = object : SelectableDates {
            // No weekends, and nothing older than the last 3 months. A weekend is
            // not a day without a rate, it is not a day at all, so offering it
            // could only ever answer "Sin datos para el …".
            //
            // The one exception is the published day: its rate exists, so the
            // "nothing after today" bound does not apply to it.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                CalendarioDateRules.isSelectableUtc(utcTimeMillis, todayUtc, publishedDay)

            // Around New Year the published day falls in the next year, so the
            // year gate cannot simply stop at the current one.
            override fun isSelectableYear(year: Int): Boolean =
                year <= todayUtc.year || year == publishedDay?.year
        }
    )
    var calendarRejection by remember { mutableStateOf(CalendarioRejection.NONE) }
    val priceFmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    } }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Consultar por fecha",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Selecciona un dia para ver la tasa de ese momento",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Button(onClick = { calendarRejection = CalendarioRejection.NONE; showDatePicker = true }) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Elegir fecha")
            }

            // Aviso: la fecha elegida no existe en el rango que el calendario ofrece
            val calendarErrorText = when (calendarRejection) {
                CalendarioRejection.NONE -> null
                CalendarioRejection.AFTER_TODAY ->
                    "Solo se pueden consultar fechas anteriores o iguales a hoy"
                CalendarioRejection.WEEKEND ->
                    "El BCV no publica tasas los fines de semana"
                // Light on purpose: the reader asked for a rate, not a rejection
                // notice, and three months is a window, not a failure.
                CalendarioRejection.TOO_OLD ->
                    "Por ahora guardamos los últimos 3 meses, probá con una fecha más reciente"
            }
            calendarErrorText?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Resultado de la busqueda
            if (uiState.dateLookupDone) {
                Spacer(Modifier.height(12.dp))
                val sample = uiState.selectedDateRate
                if (sample != null) {
                    // The row grows into place so the tap that produced it has a
                    // visible consequence.
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(MotionDurations.BASE, easing = Emphasized)
                        ) + expandVertically(
                            animationSpec = tween(MotionDurations.SLOW, easing = Emphasized)
                        )
                    ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        // Nested inset control: capsule fill + hairline border.
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedDateLabel ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sample.nombre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                TickerNumber(
                                    value = "Bs ${priceFmt.format(sample.precio)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = accentColor(),
                                    textAlign = TextAlign.End
                                )
                                sample.variacion?.let { variacion ->
                                    val signo = if (variacion >= 0) "+" else ""
                                    val color = if (variacion >= 0) trendColor() else downColor()
                                    Text(
                                        text = "${signo}${"%.2f".format(variacion)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                    }
                } else {
                    // Sin datos para esa fecha
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sin datos para el ${uiState.selectedDateLabel ?: "ese dia"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                viewModel.clearDateRate()
            },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Same rule the picker renders with, so the confirm button
                        // cannot accept a day the calendar just refused.
                        when (
                            val rejection = CalendarioDateRules.rejectionUtc(
                                millis,
                                todayUtc,
                                publishedDay
                            )
                        ) {
                            CalendarioRejection.NONE -> {
                                calendarRejection = CalendarioRejection.NONE
                                viewModel.lookupDateRate(millis)
                                showDatePicker = false
                            }
                            else -> calendarRejection = rejection
                        }
                    }
                }) {
                    Text("Ver tasa")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDatePicker = false
                    calendarRejection = CalendarioRejection.NONE
                    viewModel.clearDateRate()
                }) {
                    Text("Cancelar")
                }
            }
        ) {
            // The marker cannot live inside a Material3 day cell (there is no
            // dayContent slot), so it is drawn over the picker. matchParentSize
            // keeps it exactly as wide and tall as the picker it sits on, which
            // is what the overlay's grid arithmetic is measured against.
            Box {
                DatePicker(state = datePickerState)
                CalendarioPublishedDayMarker(
                    displayedMonthMillis = datePickerState.displayedMonthMillis,
                    // To verify the overlay geometry against a day that is
                    // guaranteed visible, substitute a fixed visible date here
                    // (today works) — the real marked day only exists once BCV
                    // publishes it, which cannot be forced.
                    markedDate = publishedDay,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

@Composable
private fun HistoricoTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Fecha",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Fuente",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.4f)
        )
        Text(
            text = "Precio",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Variación",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HistoricoRowItem(row: HistoricoRow) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.fecha,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = row.fuente,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.4f)
            )
            Text(
                text = row.precio,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = row.variacion,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun QuoteRow(
    quote: DolarQuote,
    selected: Boolean,
    highPrecision: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        // Selected rows are marked with an accent hairline instead of a tonal fill.
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) accentColor().copy(alpha = 0.55f) else cardBorderColor()
        ),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quote.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Fuente: ${quote.fuente.uppercase()} · Act. ${formatUpdated(quote.fechaActualizacion)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(quote.promedio),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                quote.variacion?.let { variacion ->
                    TrendIndicator(variacion = variacion)
                }
            }
            Spacer(Modifier.width(10.dp))
            // Nested inside the row's own click: the child consumes the tap, so
            // sharing never reads as a source switch.
            ShareRateAction(quote = quote, highPrecision = highPrecision)
        }
    }
}

@Composable
private fun TrendIndicator(variacion: Double) {
    val isUp = variacion >= 0
    val color = if (isUp) trendColor() else downColor()
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrendIcon(variacion, color)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${if (isUp) "+" else ""}${"%.2f".format(variacion)}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TrendIcon(variacion: Double, tint: Color) {
    val icon: ImageVector = when {
        variacion > 0 -> Icons.AutoMirrored.Rounded.TrendingUp
        variacion < 0 -> Icons.AutoMirrored.Rounded.TrendingDown
        else -> Icons.AutoMirrored.Rounded.TrendingFlat
    }
    Icon(
        imageVector = icon,
        contentDescription = if (variacion > 0) "Sube" else if (variacion < 0) "Baja" else "Estable",
        tint = tint,
        modifier = Modifier.size(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun DolarScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fakeViewModel = FakePreviewViewModel(context)
    ERPTheme {
        DolarScreenContent(
            uiState = DolarUiState(
                quotes = previewQuotes(),
                selectedFuente = "usd",
                historial = listOf(
                    RateSample(
                        fuente = "usd",
                        nombre = "Dólar (BCV)",
                        precio = 772.54,
                        timestampEpochMillis = 1_787_068_800_000L,
                        anterior = 771.9,
                        variacion = 0.083
                    ),
                    RateSample(
                        fuente = "usd",
                        nombre = "Dólar (BCV)",
                        precio = 773.31,
                        timestampEpochMillis = 1_787_155_200_000L,
                        anterior = 772.54,
                        variacion = 0.099
                    )
                ),
                loading = false
            ),
            onSelectCasa = {},
            onRefresh = {},
            onToggleFutureRate = {},
            viewModel = fakeViewModel
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DolarScreenSinDatosPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fakeViewModel = FakePreviewViewModel(context)
    ERPTheme {
        DolarScreenContent(
            uiState = DolarUiState(
                quotes = previewQuotes(),
                selectedFuente = "usdt",
                historial = emptyList(),
                loading = false
            ),
            onSelectCasa = {},
            onRefresh = {},
            onToggleFutureRate = {},
            viewModel = fakeViewModel
        )
    }
}

private class FakePreviewViewModel(
    private val context: android.content.Context
) : DolarViewModel(
    application = context.applicationContext as android.app.Application,
    repository = com.example.erp.data.ApiDolarRepository(),
    historyStore = com.example.erp.data.FileHistoryStore(com.example.erp.data.FileHistoryStore.defaultDir(context)),
    themeRepository = com.example.erp.data.ThemeRepositoryImpl(com.example.erp.data.ThemePreferencesImpl(context))
)

private fun previewQuotes(): List<DolarQuote> = listOf(
    DolarQuote("usd", "Dólar (BCV)", 773.31, 772.54, 0.099, "2026-08-18"),
    DolarQuote("eur", "Euro (BCV)", 896.03, 894.49, 0.172, "2026-08-18"),
    DolarQuote("usdt", "USDT (P2P)", 889.5, null, null, "2026-08-18T10:30:00")
)