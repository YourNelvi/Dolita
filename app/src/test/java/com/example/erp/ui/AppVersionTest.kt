package com.example.erp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The version row is the only place a reader can find out which build they are
 * running, so the format is pinned: a bug report is only useful when the number
 * in it is the number on screen.
 */
class AppVersionTest {

    @Test
    fun `the label carries the version the app reports`() {
        assertEquals("Versión 1.29.0", versionLabel("1.29.0"))
    }

    @Test
    fun `surrounding whitespace is trimmed rather than rendered`() {
        assertEquals("Versión 1.29.0", versionLabel("  1.29.0\n"))
    }

    @Test
    fun `a blank version yields no row at all`() {
        // "Versión " with nothing after it reads as a layout bug, so the row is
        // dropped instead of rendered empty.
        assertNull(versionLabel(""))
        assertNull(versionLabel("   "))
    }
}
