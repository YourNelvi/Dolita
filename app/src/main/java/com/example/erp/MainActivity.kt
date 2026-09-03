package com.example.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.erp.ui.DolarScreen
import com.example.erp.ui.DolarViewModel
import com.example.erp.data.ThemeMode
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.ERPTheme
import com.example.erp.update.UpdateChecker
import com.example.erp.update.UpdateDialog
import com.example.erp.update.UpdateDownloader
import com.example.erp.update.DownloadProgressDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DolarViewModel = viewModel()
            val theme by viewModel.theme.collectAsState(initial = AppTheme.DOLAR_VERDE)
            val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by viewModel.dynamicColorEnabled.collectAsState(initial = true)

            val explicitDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> null
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Update state
            var updateInfo by remember { mutableStateOf<com.example.erp.update.UpdateInfo?>(null) }
            val downloadState by UpdateDownloader.downloadState.collectAsState()
            val scope = rememberCoroutineScope()

            // Check for updates on launch
            LaunchedEffect(Unit) {
                val info = UpdateChecker.checkForUpdate(applicationContext)
                updateInfo = info
            }

            ERPTheme(
                darkTheme = explicitDarkTheme,
                dynamicColor = dynamicColor,
                theme = theme
            ) {
                DolarScreen(
                    viewModel = viewModel
                )

                // Show update dialog
                updateInfo?.let { info ->
                    UpdateDialog(
                        updateInfo = info,
                        onDismiss = { updateInfo = null },
                        onDownload = {
                            UpdateDownloader.startDownload(
                                context = applicationContext,
                                url = info.downloadUrl,
                                versionName = info.versionName
                            )
                            updateInfo = null
                        }
                    )
                }

                // Show download progress
                if (downloadState.isDownloading) {
                    DownloadProgressDialog(
                        progress = downloadState.progress,
                        onDismiss = { }
                    )
                }
            }
        }
    }
}
