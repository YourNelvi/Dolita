package com.example.erp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.erp.data.DolarQuote
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
    var divDigits by remember { mutableStateOf("") }

    // Convierte dígitos (estilo calculadora clásico) a BigDecimal: "300" -> 3.00
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

    // Convierte resultado formateado ("1,00" o "1.234,56") a dígitos calculadora ("100" o "123456")
    fun toDigits(formatted: String): String {
        val parts = formatted.split(',')
        val integerPart = parts[0].replace(".", "") // quitar separador de miles
        val decimalPart = if (parts.size > 1) parts[1] else "00"
        // Combinar parte entera + decimales (siempre 2 dígitos)
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
                value = vesDigits,
                label = "Bolívares",
                onValueChange = { raw ->
                    vesDigits = raw.filter { it.isDigit() }.take(10)
                    lastEdited = "ves"
                    vesToDiv()
                }
            )
            Spacer(Modifier.height(10.dp))
            AmountField(
                value = divDigits,
                label = shortName,
                onValueChange = { raw ->
                    divDigits = raw.filter { it.isDigit() }.take(10)
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
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next
        ),
        visualTransformation = NumberVisualTransformation,
        modifier = Modifier.fillMaxWidth()
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
