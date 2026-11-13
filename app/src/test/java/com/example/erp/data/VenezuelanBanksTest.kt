package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Pago Móvil bank catalog is data a client reads to send money, so a typo
 * in a code is a wrong payment, not a cosmetic defect. These tests pin the
 * complete list, the four-digit shape and the codes of the banks most people
 * use, instead of trusting a hand-typed dropdown.
 */
class VenezuelanBanksTest {

    @Test
    fun `the catalog contains the full list of banks`() {
        assertEquals(26, venezuelanBanks.size)
    }

    @Test
    fun `every bank code is unique and starts with 01`() {
        assertEquals(
            "a duplicated code would make two banks indistinguishable",
            venezuelanBanks.size,
            venezuelanBanks.map { it.code }.distinct().size
        )
        assertTrue(venezuelanBanks.all { it.code.matches(Regex("01\\d{2}")) })
    }

    @Test
    fun `a label carries the code and the name the client needs`() {
        assertEquals("0102 · Banco de Venezuela (BDV)", venezuelanBanks.first().label)
        assertEquals("0191 · Banco Nacional de Crédito (BNC)", venezuelanBanks.last().label)
    }

    @Test
    fun `the anchors keep their codes`() {
        assertEquals(
            "Mercantil Banco Universal",
            venezuelanBanks.first { it.code == "0105" }.name
        )
        assertEquals(
            "Banco Provincial (BBVA)",
            venezuelanBanks.first { it.code == "0108" }.name
        )
        assertEquals(
            "Banesco Banco Universal",
            venezuelanBanks.first { it.code == "0134" }.name
        )
    }

    @Test
    fun `a label from the catalog is a known bank`() {
        assertTrue(isKnownVenezuelanBank("0134 · Banesco Banco Universal"))
    }

    @Test
    fun `a bare code is a known bank`() {
        // Profiles saved before the field became a picker kept whatever the
        // user had typed, and the code is the part a transfer actually needs.
        assertTrue(isKnownVenezuelanBank("0134"))
        assertTrue(isKnownVenezuelanBank(" 0102 "))
    }

    @Test
    fun `a free text bank is not a known bank`() {
        assertFalse(isKnownVenezuelanBank("Banesco"))
        assertFalse(isKnownVenezuelanBank("el de mi tia"))
    }

    @Test
    fun `a code that is not in the catalog is not a known bank`() {
        assertFalse(isKnownVenezuelanBank("9999"))
    }

    @Test
    fun `an empty bank is not a known bank`() {
        assertFalse(isKnownVenezuelanBank(""))
        assertFalse(isKnownVenezuelanBank("   "))
    }
}
