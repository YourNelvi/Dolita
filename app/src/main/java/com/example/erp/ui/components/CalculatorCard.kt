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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.erp.data.DolarQuote
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode

private fun formatCalc(value: Double): String {
    val bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    val plain = bd.toPlainString().replace('.', ',')
    val parts = plain.split(',')
    val withThousands = parts[0].reversed().chunked(3).joinToString(".").reversed()
    return if (parts.size > 1) "$withThousands,${parts[1]}" else "$withThousands,00"
}

@Composable
fun CalculatorCard(quote: DolarQuote?) {
    if (quote == null) return
    val rate = quote.promedio
    val shortName = quote.fuente.uppercase()
    var lastEdited by remember { mutableStateOf("ves") }
    // Estado: solo dígitos puros (ej: "1234" = 1234,00)
    var vesDigits by remember { mutableStateOf("") }
    var divDigits by remember { mutableStateOf("100") } // Empezar en 1,00
    var hasTyped by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Estado de animación para cada botón
    var copiedVes by remember { mutableStateOf(false) }
    var copiedDiv by remember { mutableStateOf(false) }

    fun parseDigits(digits: String): BigDecimal? {
        if (digits.isEmpty()) return null
        val padded = digits.padStart(3, '0')
        val integerPart = padded.substring(0, padded.length - 2)
        val decimalPart = padded.substring(padded.length - 2)
        return BigDecimal("$integerPart.$decimalPart")
    }

    fun format(value: BigDecimal): String {
        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        val plain = rounded.toPlainString().replace('.', ',')
        val parts = plain.split(',')
        val withThousands = parts[0].reversed().chunked(3).joinToString(".").reversed()
        return if (parts.size > 1) "$withThousands,${parts[1]}" else "$withThousands,00"
    }

    fun toDigits(formatted: String): String {
        val parts = formatted.split(',')
        val integerPart = parts[0].replace(".", "")
        val decimalPart = if (parts.size > 1) parts[1] else "00"
        return (integerPart + decimalPart.padEnd(2, '0').take(2)).removePrefix("0").takeIf { it.isNotEmpty() } ?: "0"
    }

    fun vesToDiv() {
        val v = parseDigits(vesDigits)
        if (v == null || rate <= 0.0) {
            divDigits = ""
            return
        }
        val rateBD = BigDecimal.valueOf(rate)
        val result = v.divide(rateBD, 10, RoundingMode.HALF_UP)
        divDigits = toDigits(format(result))
    }

    fun divToVes() {
        val v = parseDigits(divDigits)
        if (v == null || rate <= 0.0) {
            vesDigits = ""
            return
        }
        val rateBD = BigDecimal.valueOf(rate)
        val result = v.multiply(rateBD)
        vesDigits = toDigits(format(result))
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
    }

    // Calcular valor inicial cuando cambia la tasa
    LaunchedEffect(rate) {
        if (!hasTyped && rate > 0.0) {
            divDigits = "100"
            val rateBD = BigDecimal.valueOf(rate)
            vesDigits = toDigits(format(rateBD))
        }
    }

    // Limpiar calculadora al cambiar de fuente
    LaunchedEffect(quote.fuente) {
        vesDigits = ""
        divDigits = "100"
        lastEdited = "div"
        hasTyped = false
        if (rate > 0.0) {
            val rateBD = BigDecimal.valueOf(rate)
            vesDigits = toDigits(format(rateBD))
        }
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

            // Campo Bolívares con botón de copiar dentro
            AmountField(
                value = vesDigits,
                label = "Bolívares",
                onValueChange = { raw ->
                    if (!hasTyped) {
                        hasTyped = true
                        vesDigits = ""
                        divDigits = ""
                    } else {
                        vesDigits = raw.filter { it.isDigit() }.take(10)
                        lastEdited = "ves"
                        vesToDiv()
                    }
                },
                trailingIcon = {
                    CopyIconButton(
                        copied = copiedVes,
                        onClick = {
                            val displayValue = if (vesDigits.isNotEmpty()) {
                                val v = parseDigits(vesDigits)
                                if (v != null) format(v) else ""
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
                        vesDigits = ""
                        divDigits = ""
                    } else {
                        divDigits = raw.filter { it.isDigit() }.take(10)
                        lastEdited = "div"
                        divToVes()
                    }
                },
                trailingIcon = {
                    CopyIconButton(
                        copied = copiedDiv,
                        onClick = {
                            val displayValue = if (divDigits.isNotEmpty()) {
                                val v = parseDigits(divDigits)
                                if (v != null) format(v) else ""
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
    // Animación de color
    val iconColor by animateColorAsState(
        targetValue = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
        onClick = onClick,
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
