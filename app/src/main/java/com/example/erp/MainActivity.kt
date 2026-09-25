package com.example.erp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.erp.notification.NotificationHelper
import com.example.erp.ui.DolarScreen
import com.example.erp.ui.DolarViewModel
import com.example.erp.ui.DolarViewModelFactory
import com.example.erp.data.ThemeMode
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.ERPTheme
import com.example.erp.ui.theme.LocalIsDarkTheme

private const val TAG = "MainActivity"

private const val PERMISSION_PREFS_NAME = "notification_permission_prefs"
private const val KEY_PERMISSION_REQUESTED = "post_notifications_requested"

class MainActivity : ComponentActivity() {

    /**
     * Registered as a property initializer, so the registration happens while the activity is
     * still being constructed, before onCreate() runs. That is the window
     * [androidx.activity.result.registry.ActivityResultRegistry] requires, and it is why this
     * cannot be a `rememberLauncherForActivityResult` call inside setContent: that one only
     * registers on first composition, which is after the request already has to be made.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // A denial is a normal outcome, never an error. Nothing throws, no error state is
            // published and no dialog is shown: the app just stops posting notifications.
            Log.i(
                TAG,
                "POST_NOTIFICATIONS ${if (granted) "granted" else "denied; rate alerts stay silent"}"
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Order matters. Channels are created first: a runtime grant for channels that do not
        // exist yet is a grant the user never sees the effect of. createChannels() is idempotent,
        // so the per-notify calls inside NotificationHelper are left exactly as they are.
        NotificationHelper.createChannels(this)
        requestNotificationPermissionIfNeeded()

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

    /**
     * Asks for POST_NOTIFICATIONS at most once per installation.
     *
     * Three guards, in order, each one short-circuiting the request:
     *  1. Below API 33 the permission is granted implicitly at install time, so asking is
     *     meaningless and the dialog would never appear.
     *  2. If it is already granted, asking again is penalised by the platform and pointless.
     *  3. If the user already answered once, they are not asked again on this or any later launch.
     *     A persistent flag is the only way to honour that, because
     *     `shouldShowRequestPermissionRationale` returns `false` both for "never asked" and for
     *     "permanently denied", so on its own it cannot tell those two states apart.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (isNotificationPermissionGranted()) {
            Log.d(TAG, "POST_NOTIFICATIONS already granted, nothing to ask")
            return
        }

        if (wasNotificationPermissionRequested()) {
            // Nothing to do: the user already answered. The platform signal only tells us *how*
            // they answered, for diagnostics: `true` means a plain denial where a re-ask is still
            // legal, `false` means "don't ask again" and any further request would be ignored.
            val denialKind = if (canStillAskNotificationPermission()) "denied, re-ask allowed" else "permanently denied"
            Log.d(TAG, "POST_NOTIFICATIONS already answered ($denialKind), not asking again")
            return
        }

        // Recorded before launching, not in the result callback, so a rotation or a process death
        // while the system dialog is open cannot re-trigger the request on the next onCreate().
        permissionPrefs().edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
        Log.d(TAG, "Requesting POST_NOTIFICATIONS")
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun isNotificationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun canStillAskNotificationPermission(): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )

    private fun wasNotificationPermissionRequested(): Boolean =
        permissionPrefs().getBoolean(KEY_PERMISSION_REQUESTED, false)

    private fun permissionPrefs() =
        getSharedPreferences(PERMISSION_PREFS_NAME, Context.MODE_PRIVATE)
}
