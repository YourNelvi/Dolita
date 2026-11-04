package com.example.erp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The AMOLED variant is only worth anything if the whole surface stack is
 * genuinely black. Separation comes from the translucent hairline borders the
 * cards and dialogs draw, not from a lifted container colour.
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
    fun `amoled surface is pure black`() {
        // The whole point of the mode: a card at the same value as the background
        // is not a bug here, it is the requirement. The hairline border is what
        // keeps the card readable.
        assertEquals(Color(0xFF000000), AmoledSurface)
        assertEquals(0f, AmoledSurface.red, 0f)
        assertEquals(0f, AmoledSurface.green, 0f)
        assertEquals(0f, AmoledSurface.blue, 0f)
    }

    @Test
    fun `amoled capsule is pure black`() {
        // Segmented tracks and inset controls are surfaces too; on AMOLED they
        // must switch off as well instead of keeping a lifted grey.
        assertEquals(Color(0xFF000000), AmoledSurfaceCapsule)
        assertEquals(0f, AmoledSurfaceCapsule.red, 0f)
        assertEquals(0f, AmoledSurfaceCapsule.green, 0f)
        assertEquals(0f, AmoledSurfaceCapsule.blue, 0f)
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
