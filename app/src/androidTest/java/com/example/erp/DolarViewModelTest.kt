package com.example.erp

import com.example.erp.data.DolarQuote
import com.example.erp.data.DolarRepository
import com.example.erp.data.Error
import com.example.erp.data.RateHistoryStore
import com.example.erp.data.RateSample
import com.example.erp.data.ThemeMode
import com.example.erp.data.ThemeRepository
import com.example.erp.ui.DolarViewModel
import com.example.erp.ui.theme.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import org.json.JSONException

@ExperimentalCoroutinesApi
class DolarViewModelTest {

    private lateinit var viewModel: DolarViewModel

    // Fake repository for testing
    private class FakeRepository(
        private val quotes: List<DolarQuote> = emptyList(),
        private val exception: Exception? = null
    ) : DolarRepository {
        override suspend fun getQuotes(): List<DolarQuote> {
            if (exception != null) throw exception
            return quotes
        }
    }

    // Fake history store
    private class FakeHistoryStore : RateHistoryStore {
        override suspend fun append(samples: List<RateSample>) {}
        override suspend fun readCurrentYear(): List<RateSample> = emptyList()
        override suspend fun ensureSeeded() {}
    }

    // Fake theme repository
    private class FakeThemeRepository : ThemeRepository {
        override val theme = flowOf(AppTheme.AZUL_BANCARIO)
        override suspend fun setTheme(theme: AppTheme) {}
        override val themeMode = flowOf(ThemeMode.SYSTEM)
        override suspend fun setThemeMode(mode: ThemeMode) {}
        override val dynamicColorEnabled = flowOf(false)
        override suspend fun setDynamicColorEnabled(enabled: Boolean) {}
    }

    @Before
    fun setup() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val fakeRepo = FakeRepository()
        val fakeHistory = FakeHistoryStore()
        val fakeThemeRepo = FakeThemeRepository()

        viewModel = DolarViewModel(
            application = context,
            repository = fakeRepo,
            historyStore = fakeHistory,
            themeRepository = fakeThemeRepo
        )
    }

    @Test
    fun `mapExceptionToError maps IOException to NetworkError`() = runBlocking {
        val exception = IOException("Connection timeout")
        val error = viewModel.mapExceptionToError(exception)

        assertTrue(error is Error.NetworkError)
        assertEquals("Connection timeout", (error as Error.NetworkError).message)
    }

    @Test
    fun `mapExceptionToError maps JSONException to ParseError`() = runBlocking {
        val exception = JSONException("Invalid JSON")
        val error = viewModel.mapExceptionToError(exception)

        assertTrue(error is Error.ParseError)
        assertEquals("json", (error as Error.ParseError).field)
        assertEquals("Invalid JSON", (error as Error.ParseError).message)
    }

    @Test
    fun `mapExceptionToError maps other exceptions to ApiError`() = runBlocking {
        val exception = RuntimeException("Server error")
        val error = viewModel.mapExceptionToError(exception)

        assertTrue(error is Error.ApiError)
        assertEquals("unknown", (error as Error.ApiError).source)
        assertEquals(-1, (error as Error.ApiError).code)
        assertEquals("Server error", (error as Error.ApiError).message)
    }
}