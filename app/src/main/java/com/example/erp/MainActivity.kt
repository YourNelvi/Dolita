package com.example.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.erp.ui.DolarScreen
import com.example.erp.ui.DolarViewModel
import com.example.erp.ui.DolarViewModelFactory
import com.example.erp.data.ThemeMode
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.ERPTheme
import com.example.erp.ui.theme.LocalIsDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DolarViewModel = viewModel(
                factory = DolarViewModelFactory(application)
            )
            val theme by viewModel.theme.collectAsState(initial = AppTheme.DOLAR_VERDE)
            val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by viewModel.dynamicColorEnabled.collectAsState(initial = true)

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
                // Update status bar icons based on current theme
                val isDark = LocalIsDarkTheme.current
                SideEffect {
                    val window = this@MainActivity.window
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }

                DolarScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
