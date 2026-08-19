package com.example.erp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.erp.data.ApiDolarRepository
import com.example.erp.data.DolarQuote
import com.example.erp.data.DolarRepository
import com.example.erp.data.FileHistoryStore
import com.example.erp.data.RateHistoryStore
import com.example.erp.data.RateSample
import com.example.erp.data.RateSamplingPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DolarUiState(
    val quotes: List<DolarQuote> = emptyList(),
    val selectedFuente: String = "usd",
    val historial: List<RateSample> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

/**
 * @JvmOverloads is REQUIRED: the default `viewModel()` factory reflectively
 * looks for an exact `(Application)` constructor; without it the app crashes
 * at runtime with "cannot be constructed".
 */
class DolarViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: DolarRepository = ApiDolarRepository(),
    private val historyStore: RateHistoryStore = FileHistoryStore(application.filesDir)
) : AndroidViewModel(application) {

    /** One USDT sample per app-open window (flag lives as long as the ViewModel). */
    private var usdtSampledThisSession = false

    private val _uiState = MutableStateFlow(DolarUiState())
    val uiState: StateFlow<DolarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyStore.ensureSeeded()
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getQuotes() }
                .onSuccess { quotes ->
                    val selected = quotes.firstOrNull { it.fuente == _uiState.value.selectedFuente }
                        ?: quotes.firstOrNull()
                    val historial = selected
                        ?.let { sampleAndPersist(quotes).filter { sample -> sample.fuente == it.fuente } }
                        ?: emptyList()
                    _uiState.update {
                        it.copy(
                            quotes = quotes,
                            selectedFuente = selected?.fuente ?: it.selectedFuente,
                            historial = historial,
                            loading = false,
                            error = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            loading = false,
                            error = it.message ?: "Error de conexión"
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
            _uiState.update { it.copy(historial = historial) }
        }
    }

    /**
     * Samples the freshly fetched quotes into the store and returns the
     * current-year history. Runs only on load() success, so a failed fetch
     * never produces samples.
     */
    private suspend fun sampleAndPersist(quotes: List<DolarQuote>): List<RateSample> {
        val existing = historyStore.readCurrentYear()
        val newSamples = RateSamplingPolicy.shouldSample(
            existing = existing,
            quotes = quotes,
            nowEpochMillis = System.currentTimeMillis(),
            usdtSampledThisSession = usdtSampledThisSession
        )
        if (newSamples.isNotEmpty()) {
            historyStore.append(newSamples)
            if (newSamples.any { it.fuente == "usdt" }) usdtSampledThisSession = true
        }
        return historyStore.readCurrentYear()
    }
}
