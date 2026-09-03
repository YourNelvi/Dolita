package com.example.erp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.erp.data.ApiDolarRepository
import com.example.erp.data.CachedDolarRepository
import com.example.erp.data.DolarQuote
import com.example.erp.data.DolarRepository
import com.example.erp.data.Error
import com.example.erp.data.FileHistoryStore
import com.example.erp.data.QuoteScheduler
import com.example.erp.data.RateHistoryStore
import com.example.erp.data.RateSample
import com.example.erp.data.RateSamplingPolicy
import com.example.erp.data.ThemeMode
import com.example.erp.data.ThemePreferencesImpl
import com.example.erp.data.ThemeRepository
import com.example.erp.data.ThemeRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class DolarUiState(
    val quotes: List<DolarQuote> = emptyList(),
    val selectedFuente: String = "usd",
    val historial: List<RateSample> = emptyList(),
    val loading: Boolean = true,
    val error: Error? = null,
    val selectedDateRate: RateSample? = null,
    val selectedDateLabel: String? = null,
    val dateLookupDone: Boolean = false,
    val futureQuote: DolarQuote? = null
)

/**
 * @JvmOverloads is REQUIRED: the default `viewModel()` factory reflectively
 * looks for an exact `(Application)` constructor; without it the app crashes
 * at runtime with "cannot be constructed".
 */
open class DolarViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: DolarRepository = CachedDolarRepository(ApiDolarRepository(), application),
    private val historyStore: RateHistoryStore = FileHistoryStore(application.filesDir),
    private val themeRepository: ThemeRepository = ThemeRepositoryImpl(ThemePreferencesImpl(application))
) : AndroidViewModel(application) {

    /** One USDT sample per app-open window (flag lives as long as the ViewModel). */
    private var usdtSampledThisSession = false

    /** Raw API quotes before date transformation, for source switching. */
    private var rawApiQuotes: List<DolarQuote> = emptyList()

    private val _uiState = MutableStateFlow(DolarUiState())
    val uiState: StateFlow<DolarUiState> = _uiState.asStateFlow()

    // Expose theme flows for UI
    val theme = themeRepository.theme
    val themeMode = themeRepository.themeMode
    val dynamicColorEnabled = themeRepository.dynamicColorEnabled
    val highPrecisionEnabled = themeRepository.highPrecisionEnabled

    init {
        viewModelScope.launch {
            historyStore.ensureSeeded()
            // Fetch historical data on first launch (if store is empty)
            if (!historyStore.hasData()) {
                android.util.Log.d("DolarViewModel", "Fetching historical data from API...")
                historyStore.fetchAndPopulateHistorical()
                android.util.Log.d("DolarViewModel", "Historical data fetched successfully")
            }
            // Programar fetch diario a las 8 AM
            QuoteScheduler.scheduleDailyFetch(application)
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val rawQuotes = repository.getQuotes()
                rawApiQuotes = rawQuotes
                val today = java.time.LocalDate.now()
                val zoneId = java.time.ZoneId.systemDefault()

                // Separar cotizaciones: las de hoy vs las de manana
                val todayQuotes = mutableListOf<DolarQuote>()
                var futureUsd: DolarQuote? = null
                var futureEur: DolarQuote? = null

                rawQuotes.forEach { quote ->
                    val quoteDate = try {
                        java.time.LocalDate.parse(quote.fechaActualizacion)
                    } catch (e: Exception) { null }

                    if (quoteDate != null && quoteDate.isAfter(today)) {
                        // Es tasa de manana -> guardar como futura
                        when (quote.fuente) {
                            "usd" -> futureUsd = quote
                            "eur" -> futureEur = quote
                        }
                        // Construir la tasa de HOY usando el campo "anterior" del API
                        quote.anterior?.let { anterior ->
                            todayQuotes.add(
                                quote.copy(
                                    promedio = anterior,
                                    fechaActualizacion = today.toString(),
                                    anterior = null,
                                    variacion = null
                                )
                            )
                        }
                    } else {
                        todayQuotes.add(quote)
                    }
                }

                val selected = todayQuotes.firstOrNull { it.fuente == _uiState.value.selectedFuente }
                    ?: todayQuotes.firstOrNull()
                val historial = selected
                    ?.let { sampleAndPersist(todayQuotes).filter { sample -> sample.fuente == it.fuente } }
                    ?: emptyList()

                // La "proxima tasa" es el quote futuro de la fuente seleccionada
                val futureForSelected = when (_uiState.value.selectedFuente) {
                    "usd" -> futureUsd
                    "eur" -> futureEur
                    else -> null
                }

                _uiState.update {
                    it.copy(
                        quotes = todayQuotes,
                        selectedFuente = selected?.fuente ?: it.selectedFuente,
                        historial = historial,
                        loading = false,
                        error = null,
                        futureQuote = futureForSelected
                    )
                }

                // Notify user when a new "next rate" is available
                futureForSelected?.let { future ->
                    com.example.erp.notification.NotificationHelper.showNextRateNotification(
                        context = getApplication(),
                        nextUsdRate = future.promedio,
                        nextDate = future.fechaActualizacion
                    )
                }
            } catch (exception: Exception) {
                _uiState.update { state ->
                    state.copy(
                        loading = false,
                        error = mapExceptionToError(exception)
                    )
                }
            }
        }
    }

    fun select(fuente: String) {
        if (fuente == _uiState.value.selectedFuente) return
        viewModelScope.launch {
            _uiState.update { it.copy(selectedFuente = fuente) }
            val historial = historyStore.readCurrentYear().filter { it.fuente == fuente }
            // Reconstruir futureQuote para la nueva fuente
            val today = java.time.LocalDate.now()
            val futureForSelected = rawApiQuotes.firstOrNull { quote ->
                quote.fuente == fuente && try {
                    java.time.LocalDate.parse(quote.fechaActualizacion).isAfter(today)
                } catch (e: Exception) { false }
            }
            _uiState.update { it.copy(historial = historial, futureQuote = futureForSelected) }
        }
    }

    fun lookupDateRate(dateMillis: Long) {
        viewModelScope.launch {
            // El DatePicker retorna midnight UTC; convertir directamente a LocalDate via UTC
            val date = Instant.ofEpochMilli(dateMillis).atOffset(ZoneOffset.UTC).toLocalDate()
            val samples = historyStore.readCurrentYear()
            val selected = _uiState.value.selectedFuente
            val zoneId = java.time.ZoneId.systemDefault()
            val sample = samples.firstOrNull {
                it.fuente == selected &&
                com.example.erp.data.localDateOf(it.timestampEpochMillis, zoneId) == date
            }
            _uiState.update {
                it.copy(
                    selectedDateRate = sample,
                    selectedDateLabel = date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    dateLookupDone = true
                )
            }
        }
    }

    fun clearDateRate() {
        _uiState.update { it.copy(selectedDateRate = null, selectedDateLabel = null, dateLookupDone = false) }
    }

    fun setTheme(theme: com.example.erp.ui.theme.AppTheme) {
        viewModelScope.launch {
            themeRepository.setTheme(theme)
            // Si el usuario elige un tema explícito, desactivar Dynamic Color
            themeRepository.setDynamicColorEnabled(false)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themeRepository.setDynamicColorEnabled(enabled)
        }
    }

    fun setHighPrecisionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themeRepository.setHighPrecisionEnabled(enabled)
        }
    }

    internal fun mapExceptionToError(exception: Throwable): Error {
        return when (exception) {
            is java.io.IOException -> Error.NetworkError(exception.message ?: "Error de conexión")
            is org.json.JSONException -> Error.ParseError("json", exception.message ?: "Error de parseo")
            else -> Error.ApiError("unknown", -1, exception.message ?: "Error desconocido")
        }
    }

    /**
     * Samples the freshly fetched quotes into the store and returns the
     * current-year history. Runs only on load() success, so a failed fetch
     * never produces samples.
     */
    private suspend fun sampleAndPersist(quotes: List<DolarQuote>): List<RateSample> {
        val existing = historyStore.readCurrentYear()
        // Parse "previous" date from the BCV API to sample yesterday's rate
        val previousDateMillis = quotes.firstOrNull()?.fechaAnterior?.let { dateStr ->
            try {
                java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
            } catch (_: Exception) { null }
        }
        val newSamples = RateSamplingPolicy.shouldSample(
            existing = existing,
            quotes = quotes,
            nowEpochMillis = System.currentTimeMillis(),
            usdtSampledThisSession = usdtSampledThisSession,
            previousDateMillis = previousDateMillis
        )
        if (newSamples.isNotEmpty()) {
            historyStore.append(newSamples)
            if (newSamples.any { it.fuente == "usdt" }) usdtSampledThisSession = true
        }
        return historyStore.readCurrentYear()
    }
}