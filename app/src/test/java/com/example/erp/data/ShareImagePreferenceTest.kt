package com.example.erp.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sharing a picture is the behavior the app had before the switch existed, so
 * it starts on. The stored choice has to survive a reload: a reader who asks
 * for text only should not get the card back on the next launch.
 */
class ShareImagePreferenceTest {

    private val prefs = FakeSharedPreferences()

    private val context: Context = object : ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    private fun repository(): ThemeRepository =
        ThemeRepositoryImpl(ThemePreferencesImpl(context, prefs))

    @Test
    fun `sharing as an image is enabled until the user turns it off`() = runBlocking {
        assertTrue(repository().shareAsImageEnabled.first())
    }

    @Test
    fun `turning the image off updates the flow and survives a reload`() = runBlocking {
        val repository = repository()

        repository.setShareAsImageEnabled(false)

        assertFalse(repository.shareAsImageEnabled.first())
        assertFalse(
            "a new repository must read the stored value, not the default",
            repository().shareAsImageEnabled.first()
        )
    }

    @Test
    fun `turning the image back on survives a reload`() = runBlocking {
        repository().setShareAsImageEnabled(false)
        repository().setShareAsImageEnabled(true)

        assertTrue(repository().shareAsImageEnabled.first())
    }
}
