package com.example.erp.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The favorite source is persisted by name, so the stored value is untrusted:
 * it can be missing, blank, written by an older build, or edited by hand. The
 * normalizer is the only thing standing between that text and a `selectedFuente`
 * the screen would render as "no tab active", so every fallback is pinned here.
 */
class SourcePreferencesTest {

    @Test
    fun `a missing stored value falls back to the dollar`() {
        assertEquals("usd", normalizeFavoriteSource(null))
    }

    @Test
    fun `a blank stored value falls back to the dollar`() {
        assertEquals("usd", normalizeFavoriteSource(""))
        assertEquals("usd", normalizeFavoriteSource("   "))
    }

    @Test
    fun `a case variant of a valid source is normalized`() {
        assertEquals("usd", normalizeFavoriteSource("USD"))
        assertEquals("eur", normalizeFavoriteSource("eur"))
        assertEquals("usdt", normalizeFavoriteSource(" USDT "))
    }

    @Test
    fun `an unknown source falls back to the dollar`() {
        assertEquals("usd", normalizeFavoriteSource("btc"))
    }
}
