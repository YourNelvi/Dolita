package com.example.erp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPromptTest {

    @Test
    fun `the first day of use never shows the invitation`() {
        assertFalse(
            shouldShowCreatorPrompt(
                firstLaunchDay = 100L,
                lastShownDay = -1L,
                today = 100L
            )
        )
    }

    @Test
    fun `an app that was never launched shows nothing`() {
        assertFalse(
            shouldShowCreatorPrompt(
                firstLaunchDay = -1L,
                lastShownDay = -1L,
                today = 100L
            )
        )
    }

    @Test
    fun `the first opening of a later day shows it`() {
        assertTrue(
            shouldShowCreatorPrompt(
                firstLaunchDay = 100L,
                lastShownDay = -1L,
                today = 101L
            )
        )
    }

    @Test
    fun `it never shows twice on the same day`() {
        assertFalse(
            shouldShowCreatorPrompt(
                firstLaunchDay = 100L,
                lastShownDay = 101L,
                today = 101L
            )
        )
    }

    @Test
    fun `a new day allows it again`() {
        assertTrue(
            shouldShowCreatorPrompt(
                firstLaunchDay = 100L,
                lastShownDay = 101L,
                today = 102L
            )
        )
    }
}
