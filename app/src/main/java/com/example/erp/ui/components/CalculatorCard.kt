package com.example.erp.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.erp.data.CurrencyConverter
import com.example.erp.data.DolarQuote
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.cardBorder
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.cardContainerColor
import com.example.erp.ui.theme.positiveColor
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

// Amount inputs are numeric: tabular figures keep digits aligned while typing.
private val AmountTextStyle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = "tnum"
)

private fun formatCalc(value: Double): String {
    val bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    val plain = bd.toPlainString().replace('.', ',')
    val parts = plain.split(',')
    val withThousands = parts[0].reversed().chunked(3).joinToString(".").reversed()
    return if (parts.size > 1) "$withThousands,${parts[1]}" else "$withThousands,00"
}

/**
 * The other reference the calculator prices against, always in bolivars per
 * unit of the source. For the euro this is the implied value through the
 * official cross and the parallel dollar, which the label names so the number
 * is not mistaken for a euro parallel market that does not exist.
 */
private data class AlternativeMarket(
    val label: String,
    val rate: Double
)

private fun currencySymbol(unit: String): String = when (unit) {
    "EUR" -> "€"
    else -> "$"
}

@Composable
fun CalculatorCard(
    quote: DolarQuote?,
    quotes: List<DolarQuote> = emptyList()
) {
    if (quote == null) return
    val rate = quote.promedio
    val shortName = quote.fuente.uppercase()

    /**
     * The same amount priced against the other reference, so the comparison
     * reads the same whichever tab you are on: on the official rate you learn
     * what the parallel costs you, on the parallel you learn what the official
     * saves you.
     *
     * The euro has no parallel market of its own, so its alternative is the
     * euro valued through the official EUR/USD cross and the parallel dollar
     * rate — but the result is shown in BOLIVARES, because that is the unit the
     * user is counting in. The dollars are the method, not the answer.
     */
    val usdRate = quotes.firstOrNull { it.fuente == "usd" }?.promedio
    val eurRate = quotes.firstOrNull { it.fuente == "eur" }?.promedio
    val usdtRate = quotes.firstOrNull { it.fuente == "usdt" }?.promedio

    val alternative: AlternativeMarket? = remember(quote.fuente, rate, usdRate, eurRate, usdtRate) {
        when (quote.fuente) {
            "usd" -> usdtRate?.let { AlternativeMarket("Al paralelo", it) }
            "usdt" -> usdRate?.let { AlternativeMarket("Al oficial", it) }
            "eur" -> if (usdRate != null && usdRate > 0.0 && usdtRate != null) {
                AlternativeMarket("Al paralelo (vía dólar)", (eurRate!! / usdRate) * usdtRate)
            } else {
                null
            }
            else -> null
        }
    }
    val alternativeRate = alternative?.rate?.takeIf { it > 0.0 && rate > 0.0 }
    var lastEdited by remember { mutableStateOf("ves") }
    // Estado: solo dígitos puros (ej: "1234" = 1234,00)
    var vesDigits by remember { mutableStateOf("") }
    var divDigits by remember { mutableStateOf("100") } // Empezar en 1,00
    var hasTyped by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Estado de animación para cada botón
    var copiedVes by remember { mutableStateOf(false) }
    var copiedDiv by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
    }

    // Initialize once per source. Keyed on the source ONLY: a background rate
    // refresh used to reset both fields, which wiped what the user was typing
    // and made the focused field jump under the LazyColumn's bring-into-view.
    LaunchedEffect(quote.fuente) {
        hasTyped = false
        lastEdited = "div"
        divDigits = "100"
        vesDigits = if (rate > 0.0) CurrencyConverter.toDigits(CurrencyConverter.format(BigDecimal.valueOf(rate))) else ""
    }

    // A new rate re-prices the calculator but never touches the field being
    // typed in: the user's digits are the source of truth, the rate is a
    // multiplier.
    LaunchedEffect(rate) {
        if (rate <= 0.0) {
            vesDigits = ""
            return@LaunchedEffect
        }
        if (hasTyped) {
            if (lastEdited == "ves") {
                divDigits = CurrencyConverter.vesToDiv(vesDigits, rate)
            } else {
                vesDigits = CurrencyConverter.divToVes(divDigits, rate)
            }
        } else {
            divDigits = "100"
            vesDigits = CurrencyConverter.toDigits(CurrencyConverter.format(BigDecimal.valueOf(rate)))
        }
    }

    // Fintech card: flat surface + hairline border, no M3 elevation.
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
                text = "Conversión a Bs. según $shortName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            // Only the number rolls: "1 BCV =" and "Bs" stay put so the eye
            // tracks the value, not the sentence.
            if (rate > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "1 $shortName =",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TickerNumber(
                        value = formatCalc(rate),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor(),
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                    Text(
                        text = "Bs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Tasa no disponible",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))

            // Campo Bolívares con botón de copiar dentro
            AmountField(
                value = vesDigits,
                label = "Bolívares",
                onValueChange = { raw ->
                    if (!hasTyped) {
                        hasTyped = true
                        // First keystroke replaces the prefilled default: keep only
                        // the freshly typed digits (cursor is pinned to the end, so
                        // the new input arrives appended to the current value).
                        val fresh = raw.removePrefix(vesDigits).filter { it.isDigit() }.take(10)
                        vesDigits = fresh
                        lastEdited = "ves"
                        divDigits = CurrencyConverter.vesToDiv(vesDigits, rate)
                    } else {
                        vesDigits = raw.filter { it.isDigit() }.take(10)
                        lastEdited = "ves"
                        divDigits = CurrencyConverter.vesToDiv(vesDigits, rate)
                    }
                },
                trailingIcon = {
                    CopyIconButton(
                        copied = copiedVes,
                        onClick = {
                            val displayValue = if (vesDigits.isNotEmpty()) {
                                val v = CurrencyConverter.parseDigits(vesDigits)
                                if (v != null) CurrencyConverter.format(v) else ""
                            } else {
                                "0,00"
                            }
                            if (displayValue.isNotEmpty()) {
                                copyToClipboard(displayValue, "Bolívares")
                                copiedVes = true
                            }
                        },
                        onReset = { copiedVes = false },
                        contentDescription = "Copiar bolívares"
                    )
                }
            )
            Spacer(Modifier.height(10.dp))

            // Campo divisa con botón de copiar dentro
            AmountField(
                value = divDigits,
                label = shortName,
                onValueChange = { raw ->
                    if (!hasTyped) {
                        hasTyped = true
                        // First keystroke replaces the prefilled default (see VES field).
                        val fresh = raw.removePrefix(divDigits).filter { it.isDigit() }.take(10)
                        divDigits = fresh
                        lastEdited = "div"
                        vesDigits = CurrencyConverter.divToVes(divDigits, rate)
                    } else {
                        divDigits = raw.filter { it.isDigit() }.take(10)
                        lastEdited = "div"
                        vesDigits = CurrencyConverter.divToVes(divDigits, rate)
                    }
                },
                trailingIcon = {
                    CopyIconButton(
                        copied = copiedDiv,
                        onClick = {
                            val displayValue = if (divDigits.isNotEmpty()) {
                                val v = CurrencyConverter.parseDigits(divDigits)
                                if (v != null) CurrencyConverter.format(v) else ""
                            } else {
                                "1,00"
                            }
                            if (displayValue.isNotEmpty()) {
                                copyToClipboard(displayValue, shortName)
                                copiedDiv = true
                            }
                        },
                        onReset = { copiedDiv = false },
                        contentDescription = "Copiar $shortName"
                    )
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

            // The same money, priced at the other market.
            if (alternativeRate != null && alternative != null) {
                val editingBolivares = lastEdited == "ves"
                val entered = if (editingBolivares) {
                    CurrencyConverter.parseDigits(vesDigits)
                } else {
                    CurrencyConverter.parseDigits(divDigits)
                }
                if (entered != null && entered > BigDecimal.ZERO) {
                    // Direction matters: bolivar input divides down into the
                    // currency, currency input multiplies up into bolivars.
                    // Getting this backwards is how 1,00 USD became "Bs 0,00".
                    fun convert(amount: BigDecimal, rateValue: Double): BigDecimal =
                        if (editingBolivares) {
                            amount.divide(BigDecimal.valueOf(rateValue), 6, RoundingMode.HALF_UP)
                        } else {
                            amount.multiply(BigDecimal.valueOf(rateValue))
                        }

                    val atSelected = convert(entered, rate)
                    val atOther = convert(entered, alternativeRate)
                    val delta = atOther.subtract(atSelected)
                    val difference = delta.abs()
                    val unit = if (editingBolivares) shortName else "Bs"
                    // Editing bolivars: the output is an asset, more is better.
                    // Editing the currency: the output is a cost, less is better.
                    val better = if (editingBolivares) delta.signum() > 0 else delta.signum() < 0

                    // The question is not what each rate costs — the card already
                    // says that above — it is HOW MUCH APART they are. Stating
                    // the gap as a percentage AND as money answers both halves,
                    // and cannot be misread as a second conversion of the same
                    // amount, which is what "EUR 1,00 -> Bs 1.082,38" looked like
                    // two lines under "1 EUR = 974,71 Bs".
                    val gapPercent = ((alternativeRate - rate) / rate) * 100.0
                    val gapPerUnit = abs(alternativeRate - rate)
                    val gapUp = alternativeRate > rate

                    Spacer(Modifier.height(14.dp))
                    androidx.compose.material3.HorizontalDivider(
                        color = cardBorderColor()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = alternative.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (gapUp) "+" else ""}${"%.2f".format(gapPercent)}%" +
                                "  ·  Bs ${CurrencyConverter.format(BigDecimal.valueOf(gapPerUnit))}" +
                                if (gapUp) " más" else " menos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = accentColor()
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        // Which side is "better" depends on what is being
                        // compared. Dollars are an asset you want MORE of;
                        // bolivars are what you pay, so you want FEWER. Painting
                        // an official rate that saves you money red — because the
                        // number went down — is how a true saving reads as a loss.
                        text = when {
                            editingBolivares && better ->
                                "Te quedan $unit más: ${CurrencyConverter.format(difference)}"
                            editingBolivares ->
                                "Te quedan $unit menos: ${CurrencyConverter.format(difference)}"
                            better -> "Te ahorrás ${CurrencyConverter.format(difference)} Bs"
                            else -> "Te cuesta Bs más: ${CurrencyConverter.format(difference)}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (better) {
                            positiveColor()
                        } else {
                            com.example.erp.ui.theme.DownRedLight
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyIconButton(
    copied: Boolean,
    onClick: () -> Unit,
    onReset: () -> Unit,
    contentDescription: String
) {
    // Copy confirmation: a state change, not a direction, so it wears the
    // theme accent instead of the semantic up/down green.
    val haptics = LocalHapticFeedback.current
    val iconColor by animateColorAsState(
        targetValue = if (copied) accentColor() else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "copyIconColor"
    )

    // Auto-reset después de 1.5 segundos
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            onReset()
        }
    }

    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.padding(0.dp)
    ) {
        AnimatedContent(
            targetState = copied,
            transitionSpec = {
                scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) togetherWith
                scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            },
            label = "copyIcon"
        ) { isCopied ->
            Icon(
                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = contentDescription,
                // Minimal copy affordance: 18dp glyph inside the 48dp touch target.
                modifier = Modifier.size(18.dp),
                tint = iconColor
            )
        }
    }
}

@Composable
private fun AmountField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = trailingIcon,
        textStyle = AmountTextStyle,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            // Clean inset box sitting on the fintech surface.
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            // Quiet hairline at rest, accent focus ring while editing.
            focusedBorderColor = accentColor(),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = accentColor(),
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = accentColor()
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next
        ),
        visualTransformation = NumberVisualTransformation,
        modifier = modifier.fillMaxWidth()
    )
}

private object NumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        // Calculadora clásica: dígitos entran derecha→izquierda
        // "3" -> "0,03", "30" -> "0,30", "300" -> "3,00", "3000" -> "30,00"
        val formatted = if (digits.isEmpty()) {
            "0,00"
        } else {
            val padded = digits.padStart(3, '0')
            val integerPart = padded.substring(0, padded.length - 2)
            val decimalPart = padded.substring(padded.length - 2)
            val withThousands = integerPart.reversed().chunked(3).joinToString(".").reversed()
            "$withThousands,$decimalPart"
        }
        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = formatted.length
                override fun transformedToOriginal(offset: Int): Int = digits.length
            }
        )
    }
}
