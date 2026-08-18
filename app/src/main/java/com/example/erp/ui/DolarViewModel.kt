package com.example.erp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.erp.data.ApiDolarRepository
import com.example.erp.data.DolarQuote
import com.example.erp.data.DolarRepository
import com.example.erp.data.PricePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DolarUiState(
    val quotes: List<DolarQuote> = emptyList(),
    val selectedFuente: String = "usd",
    val historial: List<PricePoint> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class DolarViewModel(
    private val repository: DolarRepository = ApiDolarRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DolarUiState())
    val uiState: StateFlow<DolarUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.getQuotes() }
                .onSuccess { quotes ->
                    val selected = quotes.firstOrNull { it.fuente == _uiState.value.selectedFuente }
                        ?: quotes.firstOrNull()
                    val historial = selected?.let { repository.getHistorial(it) } ?: emptyList()
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
            _uiState.value.quotes.firstOrNull { it.fuente == fuente }?.let { quote ->
                _uiState.update { it.copy(historial = repository.getHistorial(quote)) }
            }
        }
    }
}