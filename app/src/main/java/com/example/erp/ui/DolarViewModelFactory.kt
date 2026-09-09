package com.example.erp.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.erp.data.ApiDolarRepository
import com.example.erp.data.CachedDolarRepository
import com.example.erp.data.DolarRepository
import com.example.erp.data.FileHistoryStore
import com.example.erp.data.RateHistoryStore
import com.example.erp.data.ThemePreferencesImpl
import com.example.erp.data.ThemeRepository
import com.example.erp.data.ThemeRepositoryImpl

/**
 * ViewModelFactory that properly constructs DolarViewModel with shared
 * dependencies. Prevents duplicate OkHttpClient / SharedPreferences instances.
 *
 * Usage:
 *   val viewModel: DolarViewModel = viewModel(
 *       factory = DolarViewModelFactory(application)
 *   )
 */
class DolarViewModelFactory(
    private val application: Application,
    private val repository: DolarRepository? = null,
    private val historyStore: RateHistoryStore? = null,
    private val themeRepository: ThemeRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DolarViewModel::class.java)) {
            return DolarViewModel(
                application = application,
                repository = repository ?: CachedDolarRepository(ApiDolarRepository(), application),
                historyStore = historyStore ?: FileHistoryStore(application.filesDir),
                themeRepository = themeRepository ?: ThemeRepositoryImpl(ThemePreferencesImpl(application))
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
