package com.example.erp.data

/**
 * One bank a Pago Móvil can be sent to.
 *
 * The code is the four-digit identifier the Venezuelan banking system uses
 * (0102, 0134, ...), and it is part of [label] because the client needs it to
 * complete the transfer.
 */
data class VenezuelanBank(
    val code: String,
    val name: String
) {
    val label: String
        get() = "$code · $name"
}

/**
 * The complete Pago Móvil bank catalog, sorted by code.
 *
 * This is the ONLY source of the bank field: the picker writes a [label] from
 * here and the field cannot be typed into, because a half-written bank name
 * is a payment that never arrives.
 */
val venezuelanBanks: List<VenezuelanBank> = listOf(
    VenezuelanBank("0102", "Banco de Venezuela (BDV)"),
    VenezuelanBank("0104", "Banco Venezolano de Crédito (BVC)"),
    VenezuelanBank("0105", "Mercantil Banco Universal"),
    VenezuelanBank("0108", "Banco Provincial (BBVA)"),
    VenezuelanBank("0114", "Bancaribe"),
    VenezuelanBank("0115", "Banco Exterior"),
    VenezuelanBank("0128", "Banco Caroní"),
    VenezuelanBank("0134", "Banesco Banco Universal"),
    VenezuelanBank("0137", "Banco Sofitasa"),
    VenezuelanBank("0138", "Banco Plaza"),
    VenezuelanBank("0146", "Bangente"),
    VenezuelanBank("0151", "BFC Banco Fondo Común"),
    VenezuelanBank("0156", "100% Banco"),
    VenezuelanBank("0157", "Del Sur Banco Universal"),
    VenezuelanBank("0163", "Banco del Tesoro"),
    VenezuelanBank("0166", "Banco Agrícola de Venezuela"),
    VenezuelanBank("0168", "Bancrecer"),
    VenezuelanBank("0169", "Mi Banco"),
    VenezuelanBank("0171", "Banco Activo"),
    VenezuelanBank("0172", "Bancamiga"),
    VenezuelanBank("0173", "Banco Internacional de Desarrollo (BID)"),
    VenezuelanBank("0174", "Banplus"),
    VenezuelanBank("0175", "Banco Digital de los Trabajadores (BDT)"),
    VenezuelanBank("0177", "BANFANB"),
    VenezuelanBank("0178", "N58 Banco Digital"),
    VenezuelanBank("0191", "Banco Nacional de Crédito (BNC)")
)

/**
 * Whether [value] identifies a bank in the catalog.
 *
 * The label ("0134 · Banesco Banco Universal") is what the picker writes, but
 * a bare code ("0134") also identifies the bank: profiles saved before the
 * field became a picker kept whatever the user had typed, and the code is the
 * part a transfer actually needs. Free text is not accepted, because the whole
 * point of the picker is that the bank is one of the real ones.
 */
fun isKnownVenezuelanBank(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return false
    if (venezuelanBanks.any { it.label == trimmed }) return true
    return trimmed.matches(Regex("01\\d{2}")) && venezuelanBanks.any { it.code == trimmed }
}
