package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The JSON blob is the only copy of a payment profile. These cases pin the two
 * properties that keep it safe: a full profile survives a round trip unchanged,
 * and nothing a user can do to the file — empty, corrupt, old schema — turns
 * into an exception on the settings screen.
 */
class PaymentProfileCodecTest {

    private fun pagoMovil() = PaymentProfile(
        id = "pm-1",
        type = PaymentProfileType.PAGO_MOVIL,
        nombre = "Ana Pérez",
        telefono = "04141234567",
        documentoTipo = "V",
        documentoNumero = "12345678",
        banco = "Banesco"
    )

    private fun cuentaBanco() = PaymentProfile(
        id = "cb-1",
        type = PaymentProfileType.CUENTA_BANCO,
        nombre = "Luis Gómez",
        telefono = "",
        documentoTipo = "E",
        documentoNumero = "87654321",
        banco = "Mercantil",
        numeroCuenta = "01050000000000000000"
    )

    @Test
    fun `round trip preserves every field of every type`() {
        val profiles = listOf(pagoMovil(), cuentaBanco())

        assertEquals(profiles, PaymentProfileCodec.decode(PaymentProfileCodec.encode(profiles)))
    }

    @Test
    fun `default fields survive a round trip`() {
        val bare = PaymentProfile(id = "pm-2", type = PaymentProfileType.PAGO_MOVIL)

        assertEquals(listOf(bare), PaymentProfileCodec.decode(PaymentProfileCodec.encode(listOf(bare))))
    }

    @Test
    fun `null blank and whitespace input decode to an empty list`() {
        assertEquals(emptyList<PaymentProfile>(), PaymentProfileCodec.decode(null))
        assertEquals(emptyList<PaymentProfile>(), PaymentProfileCodec.decode(""))
        assertEquals(emptyList<PaymentProfile>(), PaymentProfileCodec.decode("   "))
    }

    @Test
    fun `corrupt json decodes to an empty list`() {
        assertEquals(emptyList<PaymentProfile>(), PaymentProfileCodec.decode("{not json"))
        assertEquals(emptyList<PaymentProfile>(), PaymentProfileCodec.decode("[1,2,3]"))
        assertEquals(emptyList<PaymentProfile>(), PaymentProfileCodec.decode("null"))
        assertEquals(
            emptyList<PaymentProfile>(),
            PaymentProfileCodec.decode("[{\"id\":\"x\",\"type\":\"DESCONOCIDO\"}]")
        )
    }

    @Test
    fun `unknown fields are ignored`() {
        val raw = """
            [{"id":"pm-9","type":"PAGO_MOVIL","nombre":"Ana","telefono":"0414",
            "documentoTipo":"V","documentoNumero":"1","banco":"Banesco","campoFuturo":"x"}]
        """.trimIndent()

        val decoded = PaymentProfileCodec.decode(raw)

        assertEquals(1, decoded.size)
        assertEquals("Ana", decoded.single().nombre)
        assertEquals("Banesco", decoded.single().banco)
        assertEquals("", decoded.single().numeroCuenta)
    }

    @Test
    fun `missing optional fields fall back to their defaults`() {
        val decoded = PaymentProfileCodec.decode("""[{"id":"x","type":"PAGO_MOVIL"}]""")

        assertEquals(
            listOf(PaymentProfile(id = "x", type = PaymentProfileType.PAGO_MOVIL)),
            decoded
        )
    }

    @Test
    fun `a removed Zelle profile is skipped without losing the valid ones`() {
        val raw = """
            [{"id":"pm-1","type":"PAGO_MOVIL","nombre":"Ana","telefono":"04141234567",
            "documentoTipo":"V","documentoNumero":"12345678","banco":"Banesco"},
            {"id":"z-1","type":"ZELLE","nombre":"María","zelle":"maria@example.com"}]
        """.trimIndent()

        val decoded = PaymentProfileCodec.decode(raw)

        assertEquals(listOf("pm-1"), decoded.map { it.id })
    }
}
