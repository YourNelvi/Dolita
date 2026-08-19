package com.example.erp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.erp.data.DolarQuote
import com.example.erp.data.RateSample
import com.example.erp.ui.components.ChartLabels
import com.example.erp.ui.components.EvolutionChart
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.DownRedDark
import com.example.erp.ui.theme.DownRedLight
import com.example.erp.ui.theme.ERPTheme
import com.example.erp.ui.theme.UpGreenDark
import com.example.erp.ui.theme.UpGreenLight
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val priceFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private val calcFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 4
    minimumFractionDigits = 0
}

private fun formatPrice(value: Double): String = "$${priceFormatter.format(value)}"

private fun formatCalc(value: Double): String = calcFormatter.format(value)

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

@Composable
private fun trendColor(): Color =
    if (isSystemInDarkTheme()) UpGreenDark else UpGreenLight

@Composable
private fun downColor(): Color =
    if (isSystemInDarkTheme()) DownRedDark else DownRedLight

@Composable
fun DolarScreen(
    viewModel: DolarViewModel = viewModel(),
    onThemeChange: ((AppTheme) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    DolarScreenContent(
        uiState = uiState,
        onSelectCasa = viewModel::select,
        onRefresh = viewModel::load,
        onThemeChange = onThemeChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DolarScreenContent(
    uiState: DolarUiState,
    onSelectCasa: (String) -> Unit,
    onRefresh: () -> Unit,
    onThemeChange: ((AppTheme) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CurrencyExchange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Dolar",
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Tema",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AppTheme.values().forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(theme.displayName) },
                                onClick = {
                                    onThemeChange?.invoke(theme)
                                    expanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            uiState.loading && uiState.quotes.isEmpty() -> {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.quotes.isEmpty() -> {
                ErrorState(
                    message = uiState.error.orEmpty(),
                    onRetry = onRefresh,
                    modifier = contentModifier
                )
            }

            else -> {
                DolarContent(
                    uiState = uiState,
                    onSelectCasa = onSelectCasa,
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No se pudo cargar la cotización",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun DolarContent(
    uiState: DolarUiState,
    onSelectCasa: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = uiState.quotes.firstOrNull { it.fuente == uiState.selectedFuente }
        ?: uiState.quotes.firstOrNull()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FeaturedCard(quote = selected)
        }

        item {
            CasaChips(
                quotes = uiState.quotes,
                selectedCasa = uiState.selectedFuente,
                onSelect = onSelectCasa
            )
        }

        item { SectionHeader("Calculadora") }
        item {
            CalculatorCard(quote = selected)
        }

        item { SectionHeader("Histórico") }
        when (val historico = historicoState(uiState.historial)) {
            is HistoricoState.SinDatos -> item { SinDatosCard() }
            is HistoricoState.ConDatos -> {
                item { HistoricoChartCard(chart = historico.chart) }
                item { HistoricoTableHeader() }
                items(historico.rows) { row -> HistoricoRowItem(row) }
            }
        }

        item { SectionHeader("Cotizaciones") }
        items(uiState.quotes, key = { it.fuente }) { quote ->
            QuoteRow(
                quote = quote,
                selected = quote.fuente == uiState.selectedFuente,
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

@Composable
private fun FeaturedCard(quote: DolarQuote?) {
    if (quote == null) return
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(28.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.9f),
                        primary.copy(alpha = 0.55f)
                    )
                ),
                shape = shape
            )
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
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatPrice(quote.promedio),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.clip(RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quote.variacion?.let { variacion ->
                        TrendIcon(variacion, Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${if (variacion >= 0) "+" else ""}${"%.2f".format(variacion)}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = "Act. ${formatUpdated(quote.fechaActualizacion)}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeaturedStat(
                    label = "Fuente",
                    value = quote.fuente.uppercase()
                )
                FeaturedStat(label = "Promedio", value = formatPrice(quote.promedio))
                quote.anterior?.let {
                    FeaturedStat(label = "Anterior", value = formatPrice(it))
                }
            }
        }
    }
}

@Composable
private fun FeaturedStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CasaChips(
    quotes: List<DolarQuote>,
    selectedCasa: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(quotes, key = { it.fuente }) { quote ->
            FilterChip(
                selected = quote.fuente == selectedCasa,
                onClick = { onSelect(quote.fuente) },
                label = {
                    Text(
                        text = quote.nombre,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun CalculatorCard(quote: DolarQuote?) {
    if (quote == null) return
    val rate = quote.promedio
    val shortName = quote.fuente.uppercase()
    var lastEdited by remember { mutableStateOf("ves") }
    var vesInput by remember { mutableStateOf("") }
    var divInput by remember { mutableStateOf("") }

    fun sanitize(input: String): String =
        input.replace(" ", "").filter { it.isDigit() || it == ',' || it == '.' }.take(14)

    fun parse(input: String): BigDecimal? {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized == ".") return null
        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value
    }

    fun format(value: BigDecimal): String {
        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        return rounded.toPlainString().replace('.', ',')
    }

    fun vesToDiv() {
        val v = parse(vesInput)
        if (v == null || rate <= 0.0) {
            divInput = ""
            return
        }
        val rateBD = BigDecimal.valueOf(rate)
        divInput = format(v.divide(rateBD, 10, RoundingMode.HALF_UP))
    }

    fun divToVes() {
        val v = parse(divInput)
        if (v == null || rate <= 0.0) {
            vesInput = ""
            return
        }
        val rateBD = BigDecimal.valueOf(rate)
        vesInput = format(v.multiply(rateBD))
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Conversión a Bs. según $shortName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (rate > 0.0) "1 $shortName = ${formatCalc(rate)} Bs" else "Tasa no disponible",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            AmountField(
                value = vesInput,
                label = "Bolívares",
                onValueChange = {
                    vesInput = sanitize(it)
                    lastEdited = "ves"
                    vesToDiv()
                }
            )
            Spacer(Modifier.height(10.dp))
            AmountField(
                value = divInput,
                label = shortName,
                onValueChange = {
                    divInput = sanitize(it)
                    lastEdited = "div"
                    divToVes()
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (lastEdited == "ves") {
                    "Convertido a $shortName"
                } else {
                    "Convertido a Bs."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AmountField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HistoricoChartCard(chart: List<Double>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Evolución del año",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            EvolutionChart(
                values = chart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(Modifier.height(10.dp))
            ChartLabels(values = chart)
        }
    }
}

@Composable
private fun SinDatosCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
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
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
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
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DolarScreenSinDatosPreview() {
    ERPTheme {
        DolarScreenContent(
            uiState = DolarUiState(
                quotes = previewQuotes(),
                selectedFuente = "usdt",
                historial = emptyList(),
                loading = false
            ),
            onSelectCasa = {},
            onRefresh = {}
        )
    }
}

private fun previewQuotes(): List<DolarQuote> = listOf(
    DolarQuote("usd", "Dólar (BCV)", 773.31, 772.54, 0.099, "2026-08-18"),
    DolarQuote("eur", "Euro (BCV)", 896.03, 894.49, 0.172, "2026-08-18"),
    DolarQuote("usdt", "USDT (P2P)", 889.5, null, null, "2026-08-18T10:30:00")
)