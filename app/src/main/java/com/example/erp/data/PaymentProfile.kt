package com.example.erp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

/** The two ways a client can be paid. */
@Serializable
enum class PaymentProfileType { PAGO_MOVIL, CUENTA_BANCO }

/**
 * A saved way to receive a payment.
 *
 * Every value here is local-only: profiles live in this device's
 * SharedPreferences, are never logged, and are never sent to any server. They
 * leave the app only when the user explicitly shares one with a client.
 *
 * The model is deliberately flat instead of a sealed hierarchy per type. The
 * payment methods share most of their fields (holder, bank, document) and a
 * flat record serializes to JSON without polymorphic type tags, so a profile
 * written by one app version still decodes in the next one.
 */
@Serializable
data class PaymentProfile(
    val id: String,
    val type: PaymentProfileType,
    val nombre: String = "",
    val telefono: String = "",
    val documentoTipo: String = "V",
    val documentoNumero: String = "",
    val banco: String = "",
    val numeroCuenta: String = ""
) {

    /** Display name of the payment method, capitalized for titles and rows. */
    val typeLabel: String
        get() = when (type) {
            PaymentProfileType.PAGO_MOVIL -> "Pago móvil"
            PaymentProfileType.CUENTA_BANCO -> "Cuenta de banco"
        }

    /**
     * One-line subtitle for the profile list: what identifies this account
     * within its type. Blank fields are skipped rather than left as dangling
     * separators.
     */
    val summaryLine: String
        get() = when (type) {
            PaymentProfileType.PAGO_MOVIL -> joinParts(telefono, banco)
            PaymentProfileType.CUENTA_BANCO -> joinParts(numeroCuenta, banco)
        }

    /**
     * The block a client receives when the user shares this profile.
     *
     * The method and bank come first, then who to pay and the document that
     * backs the account. Empty pieces are dropped so a half-filled profile never
     * prints an orphan separator.
     */
    fun shareLines(): List<String> {
        val lines = when (type) {
            PaymentProfileType.PAGO_MOVIL -> listOf(
                joinParts("Pago móvil", banco),
                joinParts(nombre, telefono),
                documentLine()
            )

            PaymentProfileType.CUENTA_BANCO -> listOf(
                joinParts("Cuenta de banco", banco),
                joinParts(nombre, numeroCuenta),
                documentLine()
            )
        }
        return lines.filter { it.isNotBlank() }
    }

    private fun documentLine(): String {
        val numero = documentoNumero.trim()
        if (numero.isEmpty()) return ""
        val tipo = documentoTipo.trim()
        return if (tipo.isEmpty()) numero else "$tipo-$numero"
    }

    private fun joinParts(vararg parts: String): String =
        parts.filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * JSON codec for the stored profile list.
 *
 * Decoding is total: a missing key, an empty string, or a blob written by a
 * broken build all become an empty list. The settings screen must open even if
 * the stored data does not.
 *
 * Entries are decoded one at a time. A profile saved by an older build with a
 * method this version no longer offers (Zelle) is skipped on its own instead of
 * failing the whole list, so the user keeps every other saved account.
 */
object PaymentProfileCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(profiles: List<PaymentProfile>): String = json.encodeToString(profiles)

    fun decode(raw: String?): List<PaymentProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
                runCatching { json.decodeFromJsonElement<PaymentProfile>(element) }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
