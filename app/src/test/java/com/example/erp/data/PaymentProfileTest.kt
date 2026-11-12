package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What a payment profile says out loud.
 *
 * The share block is the only place these values ever leave the device, so the
 * shape of each line matters: the type and bank first, then who to pay and how.
 */
class PaymentProfileTest {

    private val pagoMovil = PaymentProfile(
        id = "pm-1",
        type = PaymentProfileType.PAGO_MOVIL,
        nombre = "Ana Pérez",
        telefono = "04141234567",
        documentoTipo = "V",
        documentoNumero = "12345678",
        banco = "Banesco"
    )

    private val cuentaBanco = PaymentProfile(
        id = "cb-1",
        type = PaymentProfileType.CUENTA_BANCO,
        nombre = "Luis Gómez",
        documentoTipo = "E",
        documentoNumero = "87654321",
        banco = "Mercantil",
        numeroCuenta = "01050000000000000000"
    )

    @Test
    fun `pago movil shares type bank holder phone and document`() {
        assertEquals(
            listOf("Pago móvil · Banesco", "Ana Pérez · 04141234567", "V-12345678"),
            pagoMovil.shareLines()
        )
    }

    @Test
    fun `pago movil without a bank still names the type`() {
        assertEquals(
            listOf("Pago móvil", "Ana Pérez · 04141234567", "V-12345678"),
            pagoMovil.copy(banco = "").shareLines()
        )
    }

    @Test
    fun `pago movil without a document drops that line`() {
        assertEquals(
            listOf("Pago móvil · Banesco", "Ana Pérez · 04141234567"),
            pagoMovil.copy(documentoNumero = "").shareLines()
        )
    }

    @Test
    fun `blank fields are dropped from every shared block`() {
        assertEquals(
            listOf("Pago móvil · Banesco", "04141234567", "V-12345678"),
            pagoMovil.copy(nombre = "").shareLines()
        )
        assertEquals(
            listOf("Cuenta de banco · Mercantil", "01050000000000000000"),
            cuentaBanco.copy(nombre = "", documentoNumero = "").shareLines()
        )
    }

    @Test
    fun `bank account shares type bank holder account and document`() {
        assertEquals(
            listOf(
                "Cuenta de banco · Mercantil",
                "Luis Gómez · 01050000000000000000",
                "E-87654321"
            ),
            cuentaBanco.shareLines()
        )
    }

    @Test
    fun `a juridical document shares the J prefix`() {
        assertEquals(
            listOf("Pago móvil · Banesco", "Ana Pérez · 04141234567", "J-12345678"),
            pagoMovil.copy(documentoTipo = "J").shareLines()
        )
    }

    @Test
    fun `summary line lists the identifying fields and skips blanks`() {
        assertEquals("04141234567 · Banesco", pagoMovil.summaryLine)
        assertEquals("01050000000000000000 · Mercantil", cuentaBanco.summaryLine)
        assertEquals("", pagoMovil.copy(telefono = "", banco = "").summaryLine)
        assertEquals("Banesco", pagoMovil.copy(telefono = "").summaryLine)
    }

    @Test
    fun `type labels name each profile type`() {
        assertEquals("Pago móvil", pagoMovil.typeLabel)
        assertEquals("Cuenta de banco", cuentaBanco.typeLabel)
    }
}
