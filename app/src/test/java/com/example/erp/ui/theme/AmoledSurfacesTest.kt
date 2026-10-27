package com.example.erp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The AMOLED variant is only worth anything if the background is genuinely black
 * AND the surfaces stay distinguishable from it. Both halves are asserted here
 * because getting either wrong produces a setting that either saves nothing or
 * makes the card layout disappear.
 */
class AmoledSurfacesTest {

    @Test
    fun `amoled background is pure black`() {
        assertEquals(Color(0xFF000000), AmoledBackground)
        assertEquals(0f, AmoledBackground.red, 0f)
        assertEquals(0f, AmoledBackground.green, 0f)
        assertEquals(0f, AmoledBackground.blue, 0f)
    }

    @Test
    fun `amoled surface is lifted off the background so cards stay visible`() {
        // Not equal: at the same value as the background a card is invisible and
        // the whole screen reads as one flat sheet.
        assertNotEquals(AmoledBackground, AmoledSurface)
        assertTrue(
            "a card must be lighter than a pure-black background",
            AmoledSurface.red > AmoledBackground.red
        )
    }

    @Test
    fun `amoled capsule is lifted above the card surface`() {
        // Segmented tracks and inset controls sit inside cards, so they have to
        // step up again or they vanish into the card they are drawn on.
        assertTrue(
            "inset controls must be lighter than the card they sit on",
            AmoledSurfaceCapsule.red > AmoledSurface.red
        )
    }

    @Test
    fun `fintech background is not pure black so the amoled variant is a real change`() {
        // If the default were already #000000 the toggle would do nothing and
        // look broken. It must differ.
        assertNotEquals(Color(0xFF000000), FintechBackground)
        assertTrue(FintechBackground.red > 0f)
    }

    @Test
    fun `both surface sets carry their own three roles`() {
        // Only the three surface roles change with the toggle. Text colours are
        // not part of DarkSurfaces on purpose, so they cannot drift with it and
        // silently shift the contrast ratio when the setting flips.
        assertEquals(FintechBackground, FintechSurfaces.background)
        assertEquals(FintechSurface, FintechSurfaces.surface)
        assertEquals(FintechSurfaceCapsule, FintechSurfaces.capsule)
        assertEquals(AmoledBackground, AmoledSurfaces.background)
        assertEquals(AmoledSurface, AmoledSurfaces.surface)
        assertEquals(AmoledSurfaceCapsule, AmoledSurfaces.capsule)
    }
}
