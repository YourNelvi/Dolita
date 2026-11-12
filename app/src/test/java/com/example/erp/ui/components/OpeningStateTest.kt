package com.example.erp.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningStateTest {

    @Test
    fun `warm start never shows the opening animation`() {
        assertFalse(
            openingVisible(
                coldStart = false,
                loading = false,
                hasQuotes = true,
                minElapsed = true,
                maxElapsed = false
            )
        )
    }

    @Test
    fun `cold start holds the animation while the first load runs`() {
        assertTrue(
            openingVisible(
                coldStart = true,
                loading = true,
                hasQuotes = false,
                minElapsed = false,
                maxElapsed = false
            )
        )
    }

    @Test
    fun `cold start holds the animation until the minimum even when data lands early`() {
        assertTrue(
            openingVisible(
                coldStart = true,
                loading = false,
                hasQuotes = true,
                minElapsed = false,
                maxElapsed = false
            )
        )
    }

    @Test
    fun `cold start releases the animation after the minimum once data is ready`() {
        assertFalse(
            openingVisible(
                coldStart = true,
                loading = false,
                hasQuotes = true,
                minElapsed = true,
                maxElapsed = false
            )
        )
    }

    @Test
    fun `error before the minimum still holds the animation`() {
        assertTrue(
            openingVisible(
                coldStart = true,
                loading = false,
                hasQuotes = false,
                minElapsed = false,
                maxElapsed = false
            )
        )
    }

    @Test
    fun `cold start falls back to the skeleton after the maximum`() {
        assertFalse(
            openingVisible(
                coldStart = true,
                loading = true,
                hasQuotes = false,
                minElapsed = true,
                maxElapsed = true
            )
        )
    }
}
