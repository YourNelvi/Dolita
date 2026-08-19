package com.example.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.erp.ui.DolarScreen
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.ERPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentTheme by remember { mutableStateOf(AppTheme.DOLAR_VERDE) }
            ERPTheme(dynamicColor = false, theme = currentTheme) {
                DolarScreen(onThemeChange = { currentTheme = it })
            }
        }
    }
}