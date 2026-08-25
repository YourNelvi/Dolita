package com.example.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.erp.ui.DolarScreen
import com.example.erp.ui.DolarViewModel
import com.example.erp.data.ThemeMode
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.ERPTheme
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DolarViewModel = viewModel()
            val theme by viewModel.theme.collectAsState(initial = AppTheme.DOLAR_VERDE)
            val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by viewModel.dynamicColorEnabled.collectAsState(initial = true)

            // For SYSTEM mode, let ERPTheme use isSystemInDarkTheme() default
            // For LIGHT/DARK modes, override explicitly
            val explicitDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> null
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            ERPTheme(
                darkTheme = explicitDarkTheme,
                dynamicColor = dynamicColor,
                theme = theme
            ) {
                DolarScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}