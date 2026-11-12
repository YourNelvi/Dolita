package com.example.erp.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Calculator suggestions are an optional visual shortcut, so two things have to
 * stay true: the amounts are exactly the requested sequence, and the preference
 * that shows them is persisted instead of reset on every launch.
 */
class CalculatorSuggestionsTest {

    private val prefs = FakeSharedPreferences()

    /**
     * `ContextWrapper` is concrete, so only `getSharedPreferences` needs a body.
     * That keeps this a JVM test with no Android runtime involved.
     */
    private val context: Context = object : ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    private fun repository(): ThemeRepository =
        ThemeRepositoryImpl(ThemePreferencesImpl(context, prefs))

    @Test
    fun `the suggestions are the requested sequence`() {
        assertEquals(listOf(2, 5, 10, 20, 50), CalculatorSuggestions.amounts)
    }

    @Test
    fun `a suggested amount is encoded as the dollar field expects`() {
        assertEquals("200", CalculatorSuggestions.digits(2))
        assertEquals("500", CalculatorSuggestions.digits(5))
        assertEquals("1000", CalculatorSuggestions.digits(10))
        assertEquals("2000", CalculatorSuggestions.digits(20))
        assertEquals("5000", CalculatorSuggestions.digits(50))
    }

    @Test
    fun `suggestions are enabled until the user turns them off`() = runBlocking {
        assertTrue(repository().calculatorSuggestionsEnabled.first())
    }

    @Test
    fun `turning suggestions off updates the flow and survives a reload`() = runBlocking {
        val repository = repository()

        repository.setCalculatorSuggestionsEnabled(false)

        assertFalse(repository.calculatorSuggestionsEnabled.first())
        assertFalse(
            "a new repository must read the stored value, not the default",
            repository().calculatorSuggestionsEnabled.first()
        )
    }

    @Test
    fun `turning suggestions back on survives a reload`() = runBlocking {
        repository().setCalculatorSuggestionsEnabled(false)
        repository().setCalculatorSuggestionsEnabled(true)

        assertTrue(repository().calculatorSuggestionsEnabled.first())
    }
}
