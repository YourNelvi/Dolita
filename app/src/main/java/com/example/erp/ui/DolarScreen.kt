package com.example.erp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.erp.BuildConfig
import com.example.erp.data.ApkDownloadResult
import com.example.erp.data.ApkDownloader
import com.example.erp.data.ApkInstallResult
import com.example.erp.data.ApkInstaller
import com.example.erp.data.AvailableUpdate
import com.example.erp.data.CreatorPromptStore
import com.example.erp.data.DolarQuote
import com.example.erp.data.Error as AppError
import com.example.erp.data.HISTORICO_PUNTOS
import com.example.erp.data.PaymentProfile
import com.example.erp.data.PaymentProfileStore
import com.example.erp.data.RateSample
import com.example.erp.data.ShareImage
import com.example.erp.data.ThemeMode
import com.example.erp.data.UpdateCheckResult
import com.example.erp.data.UpdateChecker
import com.example.erp.data.parseSha256Digest
import com.example.erp.data.updateApkFileName
import com.example.erp.ui.components.CalculatorCard
import com.example.erp.ui.components.CreatorDialog
import com.example.erp.ui.components.CreatorFooter
import com.example.erp.ui.components.Emphasized
import com.example.erp.ui.components.Entrance
import com.example.erp.ui.components.EvolutionChart
import com.example.erp.ui.components.InstallPermissionDialog
import com.example.erp.ui.components.MotionDurations
import com.example.erp.ui.components.OpeningScreen
import com.example.erp.ui.components.PaymentProfilesScreen
import com.example.erp.ui.components.PaymentProfilePickerDialog
import com.example.erp.ui.components.PulseDot
import com.example.erp.ui.components.ShimmerBox
import com.example.erp.ui.components.SettingsScreen
import com.example.erp.ui.components.TickerNumber
import com.example.erp.ui.components.UpdateDialog
import com.example.erp.ui.components.UpdateDownloadState
import com.example.erp.ui.components.UsdtCountdownCard
import com.example.erp.ui.components.UsdtRatesCard
import com.example.erp.ui.components.openExternal
import com.example.erp.ui.components.openingVisible
import com.example.erp.ui.theme.AppTheme
import com.example.erp.ui.theme.DownRedLight
import com.example.erp.ui.theme.ERPTheme
import com.example.erp.ui.theme.FintechSignalRed
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.positiveColor
import com.example.erp.ui.theme.cardBadgeColor
import com.example.erp.ui.theme.cardBorder
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.cardContainerColor
import com.example.erp.ui.theme.isDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import android.os.Build
import android.widget.Toast

private val priceFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private fun formatPrice(value: Double): String = "Bs ${priceFormatter.format(value)}"

private val updatedFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

private fun formatUpdated(value: String): String {
    try {
        return LocalDate.parse(value).format(dateFormatter)
    } catch (e: Exception) {
        // not a date-only value, try full timestamp below
    }
    return try {
        OffsetDateTime.parse(value).format(updatedFormatter)
    } catch (e: Exception) {
        value
    }
}

// Positive signal: always green (independent of the palette accent).
@Composable
private fun trendColor(): Color = positiveColor()

// Negative signal: always semantic red (never the accent green).
@Composable
private fun downColor(): Color = if (isDarkTheme()) FintechSignalRed else DownRedLight

/**
 * The calculator's amount fields as the card holds them, in the digit encoding
 * the fields use ("123456" is 1.234,56). One holder for both sides so the two
 * published amounts can never be read from two different points in time.
 */
private data class CalculatorAmounts(
    val bolivaresDigits: String = "",
    val divisaDigits: String = ""
)

/**
 * Keeps the update notice alive across a rotation.
 *
 * The launch check marks its timestamp before the dialog is shown, so a
 * configuration change that dropped this state would hide the notice until the
 * next day. The changelog is a few hundred characters; saved instance state is
 * the right place for it.
 *
 * Optional fields travel as empty values instead of nulls: every entry has to
 * be something the saved-state bundle can carry.
 */
private val availableUpdateSaver = listSaver<AvailableUpdate?, Any?>(
    save = { update ->
        if (update == null) {
            emptyList()
        } else {
            listOf(
                update.versionName,
                update.releaseNotes,
                update.releaseUrl,
                update.apkSizeBytes ?: 0L,
                update.apkUrl.orEmpty(),
                update.apkDigest.orEmpty(),
                update.apkName.orEmpty()
            )
        }
    },
    restore = { values ->
        if (values.size != 7) return@listSaver null
        AvailableUpdate(
            versionName = values[0] as? String ?: return@listSaver null,
            releaseNotes = values[1] as? String ?: return@listSaver null,
            releaseUrl = values[2] as? String ?: return@listSaver null,
            apkSizeBytes = (values[3] as? Long)?.takeIf { it > 0L },
            apkUrl = (values[4] as? String)?.takeIf { it.isNotEmpty() },
            apkDigest = (values[5] as? String)?.takeIf { it.isNotEmpty() },
            apkName = (values[6] as? String)?.takeIf { it.isNotEmpty() }
        )
    }
)

@Composable
fun DolarScreen(
    viewModel: DolarViewModel = viewModel(),
    requestedSource: String? = null,
    onSourceRequestHandled: () -> Unit = {},
    /**
     * True when this composition is the app being opened, not a recreation.
     *
     * The cached rate is painted before the first frame reaches the screen, so
     * asking the state "are you still loading?" would skip the opening
     * animation exactly when the user is watching the system splash. The
     * activity owns this answer: a rotation or a restored task is not an app
     * open.
     */
    isColdStart: Boolean = true,
    /**
     * How many times the app has been opened today, counting this one. The
     * creator invitation shows on the second one.
     */
    creatorOpenNumber: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoriteSource by viewModel.favoriteSource.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showPaymentProfiles by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // The calculator stays composed underneath these overlays, so a focused
    // amount field keeps its IME connection and the keyboard floats over
    // Settings. Drop focus and hide the keyboard before the overlay takes over.
    fun openSettings() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        showSettings = true
    }

    fun openPaymentProfiles() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        showPaymentProfiles = true
    }

    val context = LocalContext.current
    val paymentProfileStore = remember(context) { PaymentProfileStore(context) }
    var paymentProfiles by remember { mutableStateOf(paymentProfileStore.load()) }

    fun savePaymentProfile(profile: PaymentProfile) {
        val next = if (paymentProfiles.any { it.id == profile.id }) {
            paymentProfiles.map { if (it.id == profile.id) profile else it }
        } else {
            paymentProfiles + profile
        }
        paymentProfiles = next
        paymentProfileStore.save(next)
    }

    fun deletePaymentProfile(id: String) {
        val next = paymentProfiles.filterNot { it.id == id }
        paymentProfiles = next
        paymentProfileStore.save(next)
    }

    val scope = rememberCoroutineScope()
    val inPreview = LocalInspectionMode.current

    // The opening curtain belongs to the app being opened, not to the data
    // being slow: the cached rate is painted before the first frame, so the
    // state would say "loaded" while the user is still looking at the system
    // splash. `remember` without keys is intentional: it freezes the answer
    // from the first frame.
    val coldStart = remember { isColdStart && !inPreview }
    var openingMinElapsed by remember { mutableStateOf(!coldStart) }
    var openingMaxElapsed by remember { mutableStateOf(!coldStart) }
    LaunchedEffect(coldStart) {
        if (!coldStart) return@LaunchedEffect
        delay(MotionDurations.OPENING_MIN.toLong())
        openingMinElapsed = true
        delay((MotionDurations.OPENING_MAX - MotionDurations.OPENING_MIN).toLong())
        openingMaxElapsed = true
    }
    val showOpening = openingVisible(
        coldStart = coldStart,
        loading = uiState.loading,
        hasQuotes = uiState.quotes.isNotEmpty(),
        minElapsed = openingMinElapsed,
        maxElapsed = openingMaxElapsed
    )

    // The creator invitation. The reader asks for it from the footer, and the
    // app may offer it on its own: at most once a day, and only after the app
    // has been used for more than one day. The dice are rolled once per app
    // open, never per recomposition.
    val creatorPromptStore = remember(context) { CreatorPromptStore(context) }
    var showCreatorDialog by rememberSaveable { mutableStateOf(false) }

    fun openCreatorInfo() {
        showCreatorDialog = true
        // Opening it by hand burns the day too: the invitation must not come
        // back on its own later the same day.
        creatorPromptStore.markShownToday()
    }

    // The invitation shows on the FIRST opening of the day and not again
    // until the next one. The key is the opening counter: it changes on
    // every opening, which is what makes this effect re-check the rule.
    LaunchedEffect(creatorOpenNumber) {
        if (inPreview) return@LaunchedEffect
        if (!creatorPromptStore.shouldShowNow()) return@LaunchedEffect
        // Wait for the opening curtain to be gone: the invitation and the
        // brand moment must not fight for the same frame. The state is read
        // INSIDE the flow: reading a captured boolean would never observe the
        // change and the invitation would wait forever.
        snapshotFlow { openingMinElapsed || openingMaxElapsed }.first { it }
        delay(500)
        showCreatorDialog = true
        creatorPromptStore.markShownToday()
    }
    val updateChecker = remember(context, inPreview) {
        if (inPreview) null else UpdateChecker(context.applicationContext)
    }
    val apkDownloader = remember(context, inPreview) {
        if (inPreview) null else ApkDownloader(context.applicationContext)
    }
    var availableUpdate by rememberSaveable(stateSaver = availableUpdateSaver) {
        mutableStateOf<AvailableUpdate?>(null)
    }
    var checkingUpdate by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var showInstallPermission by rememberSaveable { mutableStateOf(false) }
    // Blank means "no verified APK is waiting for the installer". A path
    // survives rotation; a File does not.
    var pendingInstallPath by rememberSaveable { mutableStateOf("") }

    fun checkForUpdates(force: Boolean) {
        val checker = updateChecker ?: return
        if (checkingUpdate) return

        scope.launch {
            checkingUpdate = true
            val result = checker.check(force)
            checkingUpdate = false

            when (result) {
                is UpdateCheckResult.Available -> {
                    // A new answer invalidates whatever the previous one left
                    // behind: an APK for 1.30.0 must never be offered for 1.31.0.
                    downloadState = UpdateDownloadState.Idle
                    pendingInstallPath = ""
                    apkDownloader?.clearPending()
                    availableUpdate = result.update
                }

                UpdateCheckResult.UpToDate -> if (force) {
                    Toast.makeText(
                        context,
                        "Ya tenés la última versión (${BuildConfig.VERSION_NAME}).",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is UpdateCheckResult.Failed -> if (force) {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }

                UpdateCheckResult.Skipped -> Unit
            }
        }
    }

    fun dismissUpdateDialog() {
        downloadJob?.cancel()
        downloadState = UpdateDownloadState.Idle
        pendingInstallPath = ""
        apkDownloader?.clearPending()
        availableUpdate = null
    }

    fun installDownloaded(file: File) {
        when (val result = ApkInstaller.install(context, file)) {
            ApkInstallResult.Started -> {
                // The system installer owns the screen from here. The APK stays
                // on disk because it is the file the installer was handed.
                pendingInstallPath = ""
                downloadState = UpdateDownloadState.Idle
                availableUpdate = null
            }

            ApkInstallResult.NeedsPermission -> {
                pendingInstallPath = file.absolutePath
                showInstallPermission = true
            }

            is ApkInstallResult.Failed -> {
                downloadState = UpdateDownloadState.Failed(result.message)
            }
        }
    }

    fun startDownload(update: AvailableUpdate) {
        val url = update.apkUrl ?: return
        val digest = parseSha256Digest(update.apkDigest) ?: return
        val downloader = apkDownloader ?: return

        downloadJob?.cancel()
        downloadJob = scope.launch {
            try {
                downloadState = UpdateDownloadState.Downloading(
                    bytesRead = 0L,
                    totalBytes = update.apkSizeBytes ?: 0L
                )

                val result = downloader.download(
                    url = url,
                    expectedSha256 = digest,
                    fileName = updateApkFileName(update.versionName),
                    onProgress = { bytesRead, totalBytes ->
                        // The downloader reports from its IO dispatcher; state
                        // belongs to the main thread.
                        withContext(Dispatchers.Main.immediate) {
                            downloadState = UpdateDownloadState.Downloading(bytesRead, totalBytes)
                        }
                    }
                )

                when (result) {
                    is ApkDownloadResult.Success -> installDownloaded(result.file)

                    is ApkDownloadResult.Failure ->
                        downloadState = UpdateDownloadState.Failed(result.message)
                }
            } finally {
                // A cancelled download must not leave the dialog claiming that
                // it is still running.
                if (!isActive) downloadState = UpdateDownloadState.Idle
            }
        }
    }

    fun requestDownload(update: AvailableUpdate) {
        if (!ApkInstaller.canInstall(context)) {
            // Ask before spending 48 MB: downloading first and failing at the
            // installer would waste the user's data for nothing.
            downloadState = UpdateDownloadState.Idle
            showInstallPermission = true
            return
        }
        startDownload(update)
    }

    // One silent check per day, at launch. The checker persists its last
    // successful timestamp, so rotations and quick restarts cost no request.
    LaunchedEffect(Unit) {
        if (!inPreview) checkForUpdates(force = false)
    }

    // A previous install may have left the downloaded APK behind. It is only
    // useful until the installer has read it, and 48 MB of it should never
    // outlive the update it was downloaded for.
    LaunchedEffect(Unit) {
        if (!inPreview) {
            withContext(Dispatchers.IO) { apkDownloader?.clearPending() }
        }
    }

    // A tapped notification names the rate it was about. Honour it once and
    // report back, so a recomposition does not keep steering the user.
    LaunchedEffect(requestedSource) {
        if (requestedSource != null) {
            viewModel.select(requestedSource)
            onSourceRequestHandled()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DolarScreenContent(
            uiState = uiState,
            onSelectCasa = viewModel::select,
            // Explicit lambda, not `viewModel::load`: a method reference to a
            // function with a default argument binds force=false, so the refresh
            // button was silently asking the cache to render itself instead of
            // asking the network — and "Reintentar" inherited the same no-op.
            onRefresh = { viewModel.load(force = true) },
            onToggleFutureRate = viewModel::toggleFutureRate,
            onToggleSelectedDateRate = viewModel::toggleSelectedDateRate,
            onOpenSettings = { openSettings() },
            paymentProfiles = paymentProfiles,
            onOpenPaymentProfiles = { openPaymentProfiles() },
            onOpenCreatorInfo = { openCreatorInfo() },
            viewModel = viewModel
        )

        // Settings is a window, not a sheet: it covers the screen, owns its own
        // scroll and leaves through the back arrow or Back. The main screen stays
        // composed underneath, so its scroll position survives the trip.
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(animationSpec = tween(MotionDurations.BASE, easing = Emphasized)) +
                slideInHorizontally(
                    animationSpec = tween(MotionDurations.SLOW, easing = Emphasized),
                    initialOffsetX = { it }
                ),
            exit = fadeOut(animationSpec = tween(MotionDurations.FAST)) +
                slideOutHorizontally(
                    animationSpec = tween(MotionDurations.BASE, easing = Emphasized),
                    targetOffsetX = { it }
                )
        ) {
            val currentTheme by viewModel.theme.collectAsState(initial = AppTheme.AZUL_BANCARIO)
            val currentMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val currentDynamicColor by viewModel.dynamicColorEnabled.collectAsState(initial = false)
            val currentHighPrecision by viewModel.highPrecisionEnabled.collectAsState(initial = false)
            val currentCalculatorSuggestions by
                viewModel.calculatorSuggestionsEnabled.collectAsState(initial = true)
            val currentShareAsImage by
                viewModel.shareAsImageEnabled.collectAsState(initial = true)
            val currentAmoledBlack by viewModel.amoledBlackEnabled.collectAsState(initial = false)

            SettingsScreen(
                currentTheme = currentTheme,
                currentMode = currentMode,
                currentDynamicColor = currentDynamicColor,
                currentHighPrecision = currentHighPrecision,
                currentCalculatorSuggestions = currentCalculatorSuggestions,
                currentAmoledBlack = currentAmoledBlack,
                onThemeChange = { viewModel.setTheme(it) },
                onModeChange = { viewModel.setThemeMode(it) },
                onDynamicColorChange = { viewModel.setDynamicColorEnabled(it) },
                onHighPrecisionChange = { viewModel.setHighPrecisionEnabled(it) },
                onCalculatorSuggestionsChange = { viewModel.setCalculatorSuggestionsEnabled(it) },
                onAmoledBlackChange = { viewModel.setAmoledBlackEnabled(it) },
                isDynamicColorAvailable = Build.VERSION.SDK_INT >= 31,
                onBack = { showSettings = false },
                onCheckForUpdates = { checkForUpdates(force = true) },
                checkingUpdate = checkingUpdate,
                paymentProfileCount = paymentProfiles.size,
                onOpenPaymentProfiles = { openPaymentProfiles() },
                currentFavoriteSource = favoriteSource,
                onFavoriteSourceChange = viewModel::setFavoriteSource,
                currentShareAsImage = currentShareAsImage,
                onShareAsImageChange = { viewModel.setShareAsImageEnabled(it) }
            )
        }

        // Payment profiles cover Settings like Settings covers the rate screen.
        // Composed last so its BackHandler is the one Back reaches first.
        AnimatedVisibility(
            visible = showPaymentProfiles,
            enter = fadeIn(animationSpec = tween(MotionDurations.BASE, easing = Emphasized)) +
                slideInHorizontally(
                    animationSpec = tween(MotionDurations.SLOW, easing = Emphasized),
                    initialOffsetX = { it }
                ),
            exit = fadeOut(animationSpec = tween(MotionDurations.FAST)) +
                slideOutHorizontally(
                    animationSpec = tween(MotionDurations.BASE, easing = Emphasized),
                    targetOffsetX = { it }
                )
        ) {
            PaymentProfilesScreen(
                profiles = paymentProfiles,
                onSave = { savePaymentProfile(it) },
                onDelete = { deletePaymentProfile(it) },
                onBack = { showPaymentProfiles = false }
            )
        }

        availableUpdate?.let { update ->
            UpdateDialog(
                update = update,
                installedVersion = BuildConfig.VERSION_NAME,
                downloadState = downloadState,
                onDownload = { requestDownload(update) },
                onCancelDownload = { downloadJob?.cancel() },
                onOpenRelease = {
                    downloadJob?.cancel()
                    if (openExternal(context, update.releaseUrl)) {
                        downloadState = UpdateDownloadState.Idle
                        availableUpdate = null
                    } else {
                        Toast.makeText(
                            context,
                            "No se pudo abrir el navegador.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onDismiss = { dismissUpdateDialog() }
            )
        }

        if (showInstallPermission) {
            InstallPermissionDialog(
                onOpenSettings = {
                    if (!ApkInstaller.openInstallPermissionSettings(context)) {
                        Toast.makeText(
                            context,
                            "No se pudieron abrir los ajustes de permisos.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onRetry = {
                    if (!ApkInstaller.canInstall(context)) {
                        Toast.makeText(
                            context,
                            "Todavía falta habilitar «Instalar apps desconocidas».",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        showInstallPermission = false
                        val pending = pendingInstallPath
                            .takeIf { it.isNotBlank() }
                            ?.let(::File)
                            ?.takeIf { it.isFile }

                        if (pending != null) {
                            installDownloaded(pending)
                        } else {
                            availableUpdate?.let(::requestDownload)
                        }
                    }
                },
                onDismiss = { showInstallPermission = false }
            )
        }

        // Last child: the cold-start curtain covers every other surface while
        // the process is still resolving its first rate. It never outlives the
        // cap, so a slow network falls back to the skeleton underneath.
        AnimatedVisibility(
            visible = showOpening,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(MotionDurations.SLOW, easing = Emphasized)),
            modifier = Modifier.fillMaxSize()
        ) {
            OpeningScreen(modifier = Modifier.fillMaxSize())
        }

        // A dialog lives in its own window, so it does not compete with the
        // curtain for the z-order: it simply opens on top of whatever is
        // showing.
        if (showCreatorDialog) {
            CreatorDialog(onDismiss = { showCreatorDialog = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DolarScreenContent(
    uiState: DolarUiState,
    onSelectCasa: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleFutureRate: () -> Unit,
    onToggleSelectedDateRate: () -> Unit = {},
    onOpenSettings: () -> Unit,
    viewModel: DolarViewModel,
    paymentProfiles: List<PaymentProfile> = emptyList(),
    onOpenPaymentProfiles: () -> Unit = {},
    onOpenCreatorInfo: () -> Unit = {}
) {
    val currentHighPrecision by viewModel.highPrecisionEnabled.collectAsState(initial = false)
    val calculatorSuggestionsEnabled by
        viewModel.calculatorSuggestionsEnabled.collectAsState(initial = true)
    val shareAsImageEnabled by
        viewModel.shareAsImageEnabled.collectAsState(initial = true)
    // The refresh glyph completes one turn per fetch and parks there — a spin
    // that keeps turning forever pretends to be working.
    val refreshTurn by animateFloatAsState(
        targetValue = if (uiState.loading) 1f else 0f,
        animationSpec = if (uiState.loading) {
            tween(durationMillis = 700, easing = LinearEasing)
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "refreshTurn"
    )

    Scaffold(
        topBar = {
            Entrance(index = 0) {
                CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CurrencyExchange,
                            contentDescription = null,
                            // Chrome stays neutral: the accent belongs to the numbers.
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Dolita",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(refreshTurn * 360f)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
                )
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()

        when {
            uiState.loading && uiState.quotes.isEmpty() -> {
                LoadingSkeleton(modifier = contentModifier)
            }

            uiState.error != null && uiState.quotes.isEmpty() -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = onRefresh,
                    modifier = contentModifier
                )
            }

            else -> {
                DolarContent(
                    uiState = uiState,
                    onSelectCasa = onSelectCasa,
                    onToggleFutureRate = onToggleFutureRate,
                    onToggleSelectedDateRate = onToggleSelectedDateRate,
                    viewModel = viewModel,
                    highPrecision = currentHighPrecision,
                    calculatorSuggestionsEnabled = calculatorSuggestionsEnabled,
                    shareAsImageEnabled = shareAsImageEnabled,
                    paymentProfiles = paymentProfiles,
                    onOpenPaymentProfiles = onOpenPaymentProfiles,
                    onOpenCreatorInfo = onOpenCreatorInfo,
                    modifier = contentModifier
                )
            }
        }
    }
}

/**
 * Loading state that mirrors the real layout: a rate board of the right shape,
 * not a spinner floating in a void. The silhouette stays stable so nothing
 * jumps when the data lands.
 */
@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp),
                shape = RoundedCornerShape(16.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Cold-start failure with nothing stored to show. The raw exception belongs to
 * the developer, not the reader: "Unable to resolve host …" is a stack trace
 * wearing a UI costume. This states what is missing and what unblocks it, and
 * nothing else.
 */
@Composable
private fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offline = error is AppError.NetworkError
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (offline) Icons.Rounded.CloudOff else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (offline) "Sin conexión" else "No pudimos cargar la tasa",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (offline) {
                "Conectate a internet y volvé a intentar para ver la tasa de hoy."
            } else {
                "El servidor no respondió. Probá de nuevo en un momento."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        HairlineAction(label = "Reintentar", onClick = onRetry)
    }
}

/**
 * Quiet action for empty and failure states. A filled M3 pill shouts in a
 * layout that is otherwise flat and hairline-bordered; this keeps the same
 * voice as every other affordance in the app.
 */
@Composable
private fun HairlineAction(
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(shape)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, accentColor().copy(alpha = 0.4f)),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = accentColor(),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor()
        )
    }
}

/**
 * The share affordance itself: a flat square with a hairline border and no
 * tonal fill, in the same voice as [HairlineAction]. A filled M3 pill next to
 * these numbers would outshout them.
 *
 * The tap does nothing when there is nothing to send. An empty share sheet is a
 * dead control, not a harmless one, so callers build the payload first and
 * return before launching anything.
 */
@Composable
private fun ShareIconButton(
    onShare: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        shape = shape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor()),
        modifier = modifier
            // Clip before the click so the touch area is the rounded square the
            // eye sees, not the untrimmed bounds behind it.
            .clip(shape)
            .clickable(onClick = onShare)
    ) {
        Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                // 16 + 16 on each side keeps the touch target at 48dp, the
                // minimum Material asks for, without a filled affordance around it.
                .padding(16.dp)
                .size(16.dp)
        )
    }
}

/**
 * The share affordance for a rate.
 *
 * It only forwards the tap: the screen builds the payload, so the payment
 * profile choice can slot in between the tap and [ShareImage]. The card is what
 * makes the number read as the day's rate instead of a line of chat; the
 * caption is what keeps the number selectable, because the reader still wants
 * to paste it into a calculator.
 */
@Composable
private fun ShareRateAction(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ShareIconButton(
        onShare = onClick,
        contentDescription = contentDescription,
        modifier = modifier
    )
}

/**
 * The share affordance for what the calculator produced, not just the rate it
 * converted with.
 *
 * Both amounts travel, because a rate on its own is already shareable from the
 * hero and this control exists for the RESULT the user typed. The day comes
 * from the rate that did the conversion, so a next-day or calendar-selected rate
 * says which day it belongs to instead of letting the message read as today's.
 */
@Composable
private fun ShareCalculatorResultAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ShareIconButton(
        onShare = onClick,
        contentDescription = "Compartir el cálculo",
        modifier = modifier
    )
}

/**
 * Honesty strip for a snapshot the app could not refresh. Known-good numbers
 * with a clear "these are old" note beat an empty screen — the reader always
 * knows which of the two they are looking at.
 */
@Composable
private fun StaleBanner(
    timestampMillis: Long?,
    modifier: Modifier = Modifier
) {
    val stamp = remember(timestampMillis) {
        timestampMillis?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM · HH:mm"))
        }
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardContainerColor())
            .border(cardBorder(), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sin conexión",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stamp?.let { "Mostrando la tasa del $it" }
                    ?: "Mostrando la última tasa guardada",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * What a share tap is about, held until the payment-profile choice resolves.
 *
 * The request carries the exact numbers the old click handler read at tap time,
 * so a refresh under the dialog cannot change what gets shared.
 */
private sealed interface ShareRequest {
    data class Rate(val quote: DolarQuote) : ShareRequest

    data class Calculation(
        val quote: DolarQuote,
        val amounts: CalculatorAmounts,
        val highPrecision: Boolean
    ) : ShareRequest
}

@Composable
private fun DolarContent(
    uiState: DolarUiState,
    onSelectCasa: (String) -> Unit,
    onToggleFutureRate: () -> Unit,
    onToggleSelectedDateRate: () -> Unit,
    viewModel: DolarViewModel,
    highPrecision: Boolean,
    calculatorSuggestionsEnabled: Boolean = true,
    shareAsImageEnabled: Boolean = true,
    paymentProfiles: List<PaymentProfile> = emptyList(),
    onOpenPaymentProfiles: () -> Unit = {},
    onOpenCreatorInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selected = uiState.quotes.firstOrNull { it.fuente == uiState.selectedFuente }
        ?: uiState.quotes.firstOrNull()
    val selectedIndex = uiState.quotes.indexOfFirst { it.fuente == selected?.fuente }
    val officialUsdRate = uiState.quotes.firstOrNull { it.fuente == "usd" }?.promedio

    // The hero rate is a horizontal carousel. Swiping it moves between the
    // available casas; the chip selector below stays in sync through the same
    // onSelectCasa callback used by the taps.
    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceAtLeast(0),
        pageCount = { uiState.quotes.size.coerceAtLeast(1) }
    )

    LaunchedEffect(selectedIndex, uiState.quotes.size) {
        if (selectedIndex >= 0 && pagerState.currentPage != selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    // Only a settled page changes the selected casa. Comparing against the
    // displayed quote avoids switching sources when the selected one is missing
    // (for example, USDT unavailable while the BCV quotes still load).
    LaunchedEffect(pagerState.settledPage) {
        // A programmatic scroll is already moving toward the selected source;
        // ignore intermediate settles so a chip tap cannot be reverted by the
        // page it happens to pass through.
        if (pagerState.targetPage == selectedIndex) return@LaunchedEffect

        val pageQuote = uiState.quotes.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        if (pageQuote.fuente != selected?.fuente) {
            onSelectCasa(pageQuote.fuente)
        }
    }

    // The calculator's two amount fields, lifted out of the card so the share
    // control can report exactly what is on screen. Empty until the card
    // publishes them, which also makes an early share a no-op rather than a
    // message about nothing.
    var calculatorAmounts by remember { mutableStateOf(CalculatorAmounts()) }

    // Sharing is a two-step decision only when the user keeps payment profiles:
    // the tap names WHAT to share, and the dialog decides whether a payment
    // block rides along. With no saved profiles the tap shares immediately.
    val context = LocalContext.current
    var pendingShare by remember { mutableStateOf<ShareRequest?>(null) }

    fun share(request: ShareRequest, payment: PaymentProfile?) {
        val chooserTitle = when (request) {
            is ShareRequest.Rate -> "Compartir tasa"
            is ShareRequest.Calculation -> "Compartir cálculo"
        }
        val content = when (request) {
            is ShareRequest.Rate -> RateShareText.content(
                quote = request.quote,
                highPrecision = highPrecision,
                payment = payment
            )

            is ShareRequest.Calculation -> CalculatorShareText.content(
                quote = request.quote,
                bolivaresDigits = request.amounts.bolivaresDigits,
                divisaDigits = request.amounts.divisaDigits,
                highPrecision = request.highPrecision,
                payment = payment
            )
        } ?: return

        ShareImage.share(
            context = context,
            card = content.toCard(),
            caption = content.toText(),
            chooserTitle = chooserTitle,
            asImage = shareAsImageEnabled
        )
    }

    fun requestShare(request: ShareRequest) {
        if (paymentProfiles.isEmpty()) {
            share(request, null)
        } else {
            pendingShare = request
        }
    }

    fun selectRelativeRate(delta: Int) {
        if (uiState.quotes.size < 2) return
        val current = uiState.quotes.indexOfFirst { it.fuente == selected?.fuente }
            .let { if (it >= 0) it else 0 }
        val target = (current + delta).coerceIn(0, uiState.quotes.lastIndex)
        if (target != current) {
            onSelectCasa(uiState.quotes[target].fuente)
        }
    }

    // The launch sequence lives here, on the container: one reveal for the whole
    // board, 60ms behind the top bar. Animating individual items would replay
    // every time they re-enter the viewport.
    Entrance(index = 1) {
    // The hero pager owns its own swipe. This wrapper extends the same gesture
    // to the calculator and every section below it, so the whole board responds
    // to a horizontal swipe instead of only the top card.
    RateSwipeBox(
        enabled = uiState.quotes.size > 1,
        onSwipeLeft = { selectRelativeRate(1) },
        onSwipeRight = { selectRelativeRate(-1) }
    ) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sits above the hero rate on purpose: the reader learns these numbers
        // are old before they read them, not after.
        if (uiState.isStale) {
            item { StaleBanner(timestampMillis = uiState.staleSinceEpochMillis) }
        }

        item {
            // No Entrance() here on purpose: per-item entrance animations
            // re-run every time an item scrolls back into view, which reads as
            // the list sticking under your thumb.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 12.dp,
                userScrollEnabled = uiState.quotes.size > 1
            ) { page ->
                val quote = uiState.quotes.getOrNull(page)
                FeaturedCard(
                    quote = quote,
                    highPrecision = highPrecision,
                    // The future rate belongs to the currently selected source,
                    // not to whichever adjacent page is being previewed.
                    futureQuote = if (quote?.fuente == selected?.fuente) uiState.futureQuote else null,
                    officialUsdRate = officialUsdRate,
                    onShareRate = { requestShare(ShareRequest.Rate(it)) }
                )
            }
        }

        item {
            CasaChips(
                quotes = uiState.quotes,
                selectedCasa = uiState.selectedFuente,
                onSelect = onSelectCasa
            )
        }

        // The countdown belongs to the parallel market only: BCV publishes on
        // its own schedule and has no hourly worker to count down to.
        if (selected?.fuente == "usdt") {
            item {
                UsdtCountdownCard(
                    lastFetch = selected.fechaActualizacion,
                    refreshing = uiState.usdtRefreshing,
                    error = uiState.usdtRefreshError,
                    onRefresh = viewModel::refreshUsdt
                )
            }
        }

        // The two alternative rates live together right above the section they
        // change: separated, they read as independent read-only cards, and the
        // calculator's actual input is invisible.
        item {
            CalculatorRateBars(
                uiState = uiState,
                highPrecision = highPrecision,
                onToggleFutureRate = onToggleFutureRate,
                onToggleSelectedDateRate = onToggleSelectedDateRate
            )
        }

        // The quote is resolved once, above both items, because the header row
        // needs it for the share action and the card needs it to convert with.
        // Resolving it inside the card item left the header unable to offer the
        // share, which is what pushed the button into its own row and the
        // asymmetry the reader saw.
        val calcQuote = CalculatorRateResolver.resolve(
            today = selected,
            future = uiState.futureQuote,
            futureSource = uiState.calcRateSource,
            selectedSample = uiState.selectedDateRate,
            selectedLabel = uiState.selectedDateLabel,
            dateSource = uiState.calcRateSource
        )

        // The USDT buy/sell choice applies ONLY to today's rate: a historical
        // or next-day sample has no sides, and the hero/history keep the
        // canonical average. The derived quote exists for the calculator and
        // its share alone.
        val calcQuoteForUsdt = if (
            calcQuote?.fuente == "usdt" &&
            uiState.calcRateSource == CalculatorRateSource.TODAY
        ) {
            calcQuote.conTasaUsdt(uiState.usdtRateMode)
        } else {
            calcQuote
        }

        // Title left, action right, on one line. A share button parked above the
        // card left a column of empty space between the two, which read as a
        // layout slip rather than as two things on the same row.
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader("Calculadora")
                if (calcQuoteForUsdt != null) {
                    ShareCalculatorResultAction(
                        onClick = {
                            // The USDT-specific quote, not the canonical
                            // average: the reader must receive the same side of
                            // the Compra/Venta/Promedio choice the calculator
                            // is showing.
                            requestShare(
                                ShareRequest.Calculation(
                                    quote = calcQuoteForUsdt,
                                    amounts = calculatorAmounts,
                                    highPrecision = highPrecision
                                )
                            )
                        }
                    )
                }
            }
        }
        item {
            calcQuoteForUsdt?.let { quote ->
                CalculatorCard(
                    quote = quote,
                    quotes = uiState.quotes,
                    suggestionsEnabled = calculatorSuggestionsEnabled,
                    onAmountsChanged = { bolivares, divisa ->
                        calculatorAmounts = CalculatorAmounts(bolivares, divisa)
                    }
                )
            }
        }

        // Just below the calculator it re-prices: the rates and the control
        // that chooses between them belong to the same decision.
        if (selected?.fuente == "usdt" && uiState.calcRateSource == CalculatorRateSource.TODAY) {
            item {
                UsdtRatesCard(
                    quote = selected,
                    selected = uiState.usdtRateMode,
                    onSelect = viewModel::setUsdtRateMode
                )
            }
        }

        item { SectionHeader("Histórico") }
        when (val historico = historicoState(uiState.historial)) {
            is HistoricoState.SinDatos -> item { SinDatosCard() }
            is HistoricoState.ConDatos -> {
                item { HistoricoChartCard(samples = historico.samples) }
            }
        }

        // Calendario: consultar tasa por fecha
        item { CalendarLookupSection(uiState = uiState, viewModel = viewModel) }

        item { SectionHeader("Cotizaciones") }
        items(uiState.quotes, key = { it.fuente }) { quote ->
            QuoteRow(
                quote = quote,
                selected = quote.fuente == uiState.selectedFuente,
                highPrecision = highPrecision,
                onClick = { onSelectCasa(quote.fuente) },
                onShareRate = { requestShare(ShareRequest.Rate(it)) }
            )
        }

        item {
                Text(
                    text = "Precios: rates.dolarvzla.com (BCV)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
        }

        // The last thing on the board: who made it, and the door to the
        // invitation. It sits after the prices note because it is a sign-off,
        // not part of the rates.
        item {
            CreatorFooter(onOpenCreatorInfo = onOpenCreatorInfo)
        }
    }
    }
    }

    // The picker is the second half of a share tap. Choosing a profile shares
    // exactly what the tap named; dismissing shares nothing, which is the only
    // safe default for data the client did not ask for.
    pendingShare?.let { request ->
        PaymentProfilePickerDialog(
            profiles = paymentProfiles,
            onSelect = { profile ->
                pendingShare = null
                share(request, profile)
            },
            onDismiss = { pendingShare = null },
            onAddProfile = {
                pendingShare = null
                onOpenPaymentProfiles()
            }
        )
    }
}

/**
 * Adds a horizontal fling-to-change-casa gesture around a section. The callback
 * references are kept current so a rate refresh does not restart the pointer
 * input in the middle of a drag.
 */
@Composable
private fun RateSwipeBox(
    enabled: Boolean,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentOnSwipeLeft = rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight = rememberUpdatedState(onSwipeRight)

    Box(
        modifier = if (enabled) {
            Modifier.pointerInput(enabled) {
                val threshold = 72.dp.toPx()
                var totalDrag = 0f

                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        if (abs(totalDrag) >= threshold) {
                            if (totalDrag < 0) {
                                currentOnSwipeLeft.value()
                            } else {
                                currentOnSwipeRight.value()
                            }
                        }
                        totalDrag = 0f
                    }
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                }
            }
        } else {
            Modifier
        }
    ) {
        content()
    }
}

@Composable
private fun FeaturedCard(
    quote: DolarQuote?,
    highPrecision: Boolean = false,
    futureQuote: DolarQuote? = null,
    officialUsdRate: Double? = null,
    onShareRate: (DolarQuote) -> Unit = {}
) {
    if (quote == null) return
    val shape = RoundedCornerShape(20.dp)
    val fracDigits = if (highPrecision) 4 else 2
    val featPriceFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = fracDigits
        maximumFractionDigits = fracDigits
    }
    fun featFormatPrice(value: Double): String = "Bs ${featPriceFormatter.format(value)}"

    // The gap between the parallel market and the official rate is a MAGNITUDE,
    // not a direction: a wider gap is not a "gain" and must not wear the green
    // that means "subió". The sign carries the information, the accent carries
    // the emphasis, and it only appears when both rates are actually known.
    val spreadPercent = if (quote.fuente == "usdt" && officialUsdRate != null && officialUsdRate > 0.0) {
        (quote.promedio - officialUsdRate) / officialUsdRate * 100.0
    } else {
        null
    }
    // Fintech hero card: flat surface + hairline border (no M3 elevation, no
    // full-background color fill). The accent green is spent on the big number.
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = quote.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            // The signature moment: the rate rolls into place like a board
            // instead of blinking between two values.
            TickerNumber(
                value = featFormatPrice(quote.promedio),
                style = MaterialTheme.typography.displayLarge,
                color = accentColor(),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = cardBadgeColor(),
                modifier = Modifier.clip(RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quote.variacion?.let { variacion ->
                        val signalColor = if (variacion >= 0) trendColor() else downColor()
                        TrendIcon(variacion, signalColor)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${if (variacion >= 0) "+" else ""}${"%.2f".format(variacion)}%",
                            color = signalColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    // Liveness, not direction: the dot wears the theme accent.
                    // Direction (the +/- values) keeps semantic green/red.
                    PulseDot(color = accentColor())
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Act. ${formatUpdated(quote.fechaActualizacion)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Only the parallel market has a gap worth stating, and only while
            // both rates are known.
            spreadPercent?.let { spread ->
                val gapPerDollar = spreadPercent?.let { percent ->
                    officialUsdRate?.let { it * percent / 100.0 }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (spread >= 0) "+" else ""}${"%.2f".format(spread)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor()
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "sobre el oficial",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                gapPerDollar?.let { gap ->
                    Spacer(Modifier.height(2.dp))
                    // Bolívares, never dollars: the gap is what one extra dollar
                    // costs on the parallel, so the "$" of featFormatPrice read as
                    // "105,43 dollars" — off by the price of a dollar, and the
                    // calculator states the very same figure as "Bs 105,43".
                    Text(
                        text = "Son Bs ${featPriceFormatter.format(abs(gap))} por dólar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeaturedStat(
                    label = "Fuente",
                    value = quote.fuente.uppercase()
                )
                FeaturedStat(label = "Promedio", value = featFormatPrice(quote.promedio))
                quote.anterior?.let {
                    FeaturedStat(label = "Anterior", value = featFormatPrice(it))
                }
                quote.anterior?.let { anterior ->
                    val cambio = quote.promedio - anterior
                    val signo = if (cambio >= 0) "+" else ""
                    val color = if (cambio >= 0) trendColor() else downColor()
                    FeaturedStatColored(
                        label = "Cambio",
                        value = "${signo}${featPriceFormatter.format(cambio)} Bs",
                        color = color
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // The share affordance sits directly under the number it shares:
            // on the hero card the rate is the subject, so the control belongs
            // with the number, not out in a corner competing with the chrome.
            ShareRateAction(
                contentDescription = "Compartir ${quote.nombre}",
                onClick = { onShareRate(quote) }
            )
        }
    }
}

@Composable
private fun FeaturedStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FeaturedStatColored(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * The calculator's rate choice, shown as a stack of toggles right above the
 * calculator. Only rates that actually exist are rendered: a next-day quote
 * that the API never published and a calendar lookup that found nothing are
 * not options, so offering them would promise a switch that cannot happen.
 */
@Composable
private fun CalculatorRateBars(
    uiState: DolarUiState,
    highPrecision: Boolean = false,
    onToggleFutureRate: () -> Unit = {},
    onToggleSelectedDateRate: () -> Unit = {}
) {
    val future = uiState.futureQuote
    val sample = uiState.selectedDateRate
    if (future == null && sample == null) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (future != null) {
            ProximaTasaCard(
                future = future,
                highPrecision = highPrecision,
                isActive = uiState.calcRateSource == CalculatorRateSource.NEXT_RATE,
                onClick = onToggleFutureRate
            )
        }
        if (sample != null) {
            TasaPorFechaCard(
                sample = sample,
                label = uiState.selectedDateLabel,
                highPrecision = highPrecision,
                isActive = uiState.calcRateSource == CalculatorRateSource.SELECTED_DATE,
                onClick = onToggleSelectedDateRate
            )
        }
    }
}

@Composable
private fun ProximaTasaCard(
    future: DolarQuote,
    highPrecision: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    val fracDigits = if (highPrecision) 4 else 2
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = fracDigits
        maximumFractionDigits = fracDigits
    }
    // Thin 1dp hairline in every state; the accent border only marks the
    // active toggle, so the card reads as a control and not a colored banner.
    val borderColor = if (isActive) accentColor() else cardBorderColor()
    val titleColor = if (isActive) accentColor() else MaterialTheme.colorScheme.onSurface
    val valueColor = if (isActive) accentColor() else MaterialTheme.colorScheme.onSurface

    // What tomorrow's rate costs against the rate it replaces. The arithmetic
    // and the missing-value rules live in rateChangeOf(); this composable only
    // decides how the answer reads.
    val change = rateChangeOf(future)
    // Neutral for zero AND for absent, the same way EvolutionChart reads a
    // missing or flat variation: an unknown move must not wear a green.
    val changeColor = when (change?.direction) {
        RateChangeDirection.UP -> trendColor()
        RateChangeDirection.DOWN -> downColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // Each half is printed only if the quote published it, so a rate that
    // arrives without a previous value shows no delta at all rather than a
    // confident "+0,00 Bs" the API never claimed.
    val changeText = change?.let { rateChange ->
        buildList {
            rateChange.bolivaresDelta?.let { add(formatBolivaresDelta(it)) }
            rateChange.percent?.let { add(formatRatePercent(it)) }
        }.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isActive) "Usando tasa futura" else "Proxima tasa",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = formatUpdated(future.fechaActualizacion),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Same voice as the line above: no pill, no fill, just the
                // number in its signal colour.
                changeText?.let { text ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = changeColor
                    )
                }
            }
            Text(
                text = "Bs ${fmt.format(future.promedio)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

/**
 * Same visual language as the next-day bar, because it fills the same role: a
 * selectable alternative rate for the calculator. The calendar section keeps
 * showing the lookup result; this bar is the control that applies it.
 */
@Composable
private fun TasaPorFechaCard(
    sample: RateSample,
    label: String?,
    highPrecision: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    val fracDigits = if (highPrecision) 4 else 2
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = fracDigits
        maximumFractionDigits = fracDigits
    }
    val borderColor = if (isActive) accentColor() else cardBorderColor()
    val titleColor = if (isActive) accentColor() else MaterialTheme.colorScheme.onSurface
    val valueColor = if (isActive) accentColor() else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isActive) "Usando tasa del ${label ?: "ese dia"}" else "Tasa de ${label ?: "esa fecha"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = sample.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Bs ${fmt.format(sample.precio)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

/**
 * Source selector as a fintech capsule segmented control: shared track with a
 * hairline border, a contrasting active item and a small accent indicator dot
 * bar. Replaces the Material FilterChip row (no ripple, no tonal fills).
 */
@Composable
private fun CasaChips(
    quotes: List<DolarQuote>,
    selectedCasa: String,
    onSelect: (String) -> Unit
) {
    val trackShape = RoundedCornerShape(50)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, cardBorderColor(), trackShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        quotes.forEach { quote ->
            val selected = quote.fuente == selectedCasa
            val activeBackground =
                if (isDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.White
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(trackShape)
                    .background(if (selected) activeBackground else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                        shape = trackShape
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onSelect(quote.fuente) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = quote.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                // Subtle active-state indicator under the selected segment: it
                // slides out from nothing instead of popping into existence.
                val indicatorWidth by animateDpAsState(
                    targetValue = if (selected) 16.dp else 0.dp,
                    animationSpec = tween(MotionDurations.BASE, easing = Emphasized),
                    label = "casaIndicator"
                )
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(2.dp)
                        .clip(trackShape)
                        .background(accentColor())
                )
            }
        }
    }
}

@Composable
private fun HistoricoChartCard(samples: List<com.example.erp.data.RateSample>) {
    // The header states the coverage the samples actually have. It used to be a
    // hardcoded "Últimos 15 días", which lied on a fresh install and permanently
    // for the parallel series — nothing fetches a historical P2P rate, so that
    // chart can only ever hold what was sampled live.
    val title = remember(samples) { historicoTitle(samples) }
    val isHourly = remember(samples) { samples.any { it.fuente == "usdt" } }
    val maxPoints = remember(samples) {
        if (isHourly) HISTORICO_MUESTRAS_HORARIAS else HISTORICO_PUNTOS
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            EvolutionChart(
                samples = samples,
                maxPoints = maxPoints,
                showHours = isHourly,
                // No fixed height: the chart also renders the dated grid of every
                // drawn point, so the card has to grow with the series.
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SinDatosCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sin datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "El histórico se genera con cada carga exitosa de cotizaciones.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarLookupSection(
    uiState: DolarUiState,
    viewModel: DolarViewModel
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // Limite: midnight UTC de hoy. The rule for which days exist at all lives in
    // CalendarioDateRules, so the picker and its unit test cannot drift apart.
    val todayUtc = remember { java.time.LocalDate.now() }
    val todayUtcMillis = remember(todayUtc) {
        todayUtc.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    // El dia publicado: el unico dia futuro que el calendario ofrece, y el unico
    // que lleva el punto verde. Viene de la cotizacion, no de un reloj: la tasa
    // "de manana" del BCV es para el proximo dia que publica, que no siempre es
    // manana (los fines de semana no publica).
    val publishedDay = remember(uiState.futureQuote) { publishedDayOf(uiState.futureQuote) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayUtcMillis,
        selectableDates = object : SelectableDates {
            // No weekends, and nothing older than the last 3 months. A weekend is
            // not a day without a rate, it is not a day at all, so offering it
            // could only ever answer "Sin datos para el …".
            //
            // The one exception is the published day: its rate exists, so the
            // "nothing after today" bound does not apply to it.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                CalendarioDateRules.isSelectableUtc(utcTimeMillis, todayUtc, publishedDay)

            // Around New Year the published day falls in the next year, so the
            // year gate cannot simply stop at the current one.
            override fun isSelectableYear(year: Int): Boolean =
                year <= todayUtc.year || year == publishedDay?.year
        }
    )
    var calendarRejection by remember { mutableStateOf(CalendarioRejection.NONE) }
    val priceFmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    } }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Consultar por fecha",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Selecciona un dia para ver la tasa de ese momento",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Button(onClick = { calendarRejection = CalendarioRejection.NONE; showDatePicker = true }) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Elegir fecha")
            }

            // Aviso: la fecha elegida no existe en el rango que el calendario ofrece
            val calendarErrorText = when (calendarRejection) {
                CalendarioRejection.NONE -> null
                CalendarioRejection.AFTER_TODAY ->
                    "Solo se pueden consultar fechas anteriores o iguales a hoy"
                CalendarioRejection.WEEKEND ->
                    "El BCV no publica tasas los fines de semana"
                // Light on purpose: the reader asked for a rate, not a rejection
                // notice, and three months is a window, not a failure.
                CalendarioRejection.TOO_OLD ->
                    "Por ahora guardamos los últimos 3 meses, probá con una fecha más reciente"
            }
            calendarErrorText?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Resultado de la busqueda
            if (uiState.dateLookupDone) {
                Spacer(Modifier.height(12.dp))
                val sample = uiState.selectedDateRate
                if (sample != null) {
                    // The row grows into place so the tap that produced it has a
                    // visible consequence.
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(MotionDurations.BASE, easing = Emphasized)
                        ) + expandVertically(
                            animationSpec = tween(MotionDurations.SLOW, easing = Emphasized)
                        )
                    ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        // Nested inset control: capsule fill + hairline border.
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedDateLabel ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sample.nombre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                TickerNumber(
                                    value = "Bs ${priceFmt.format(sample.precio)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = accentColor(),
                                    textAlign = TextAlign.End
                                )
                                sample.variacion?.let { variacion ->
                                    val signo = if (variacion >= 0) "+" else ""
                                    val color = if (variacion >= 0) trendColor() else downColor()
                                    Text(
                                        text = "${signo}${"%.2f".format(variacion)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                    }
                } else {
                    // Sin datos para esa fecha
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sin datos para el ${uiState.selectedDateLabel ?: "ese dia"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                viewModel.clearDateRate()
            },
            // Pure black in AMOLED; the hairline keeps the dialog readable
            // against the black scrim.
            modifier = Modifier.border(1.dp, cardBorderColor(), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Same rule the picker renders with, so the confirm button
                        // cannot accept a day the calendar just refused.
                        when (
                            val rejection = CalendarioDateRules.rejectionUtc(
                                millis,
                                todayUtc,
                                publishedDay
                            )
                        ) {
                            CalendarioRejection.NONE -> {
                                calendarRejection = CalendarioRejection.NONE
                                viewModel.lookupDateRate(millis)
                                showDatePicker = false
                            }
                            else -> calendarRejection = rejection
                        }
                    }
                }) {
                    Text("Ver tasa")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDatePicker = false
                    calendarRejection = CalendarioRejection.NONE
                    viewModel.clearDateRate()
                }) {
                    Text("Cancelar")
                }
            }
        ) {
            // The marker cannot live inside a Material3 day cell (there is no
            // dayContent slot), so it is drawn over the picker. matchParentSize
            // keeps it exactly as wide and tall as the picker it sits on, which
            // is what the overlay's grid arithmetic is measured against.
            Box {
                DatePicker(state = datePickerState)
                CalendarioPublishedDayMarker(
                    displayedMonthMillis = datePickerState.displayedMonthMillis,
                    // To verify the overlay geometry against a day that is
                    // guaranteed visible, substitute a fixed visible date here
                    // (today works) — the real marked day only exists once BCV
                    // publishes it, which cannot be forced.
                    markedDate = publishedDay,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

@Composable
private fun HistoricoTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Fecha",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Fuente",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.4f)
        )
        Text(
            text = "Precio",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Variación",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HistoricoRowItem(row: HistoricoRow) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.fecha,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = row.fuente,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.4f)
            )
            Text(
                text = row.precio,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = row.variacion,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun QuoteRow(
    quote: DolarQuote,
    selected: Boolean,
    highPrecision: Boolean = false,
    onClick: () -> Unit,
    onShareRate: (DolarQuote) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        // Selected rows are marked with an accent hairline instead of a tonal fill.
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) accentColor().copy(alpha = 0.55f) else cardBorderColor()
        ),
        // Fintech cards sit flat: no Material elevation/shadow.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quote.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Fuente: ${quote.fuente.uppercase()} · Act. ${formatUpdated(quote.fechaActualizacion)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(quote.promedio),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                quote.variacion?.let { variacion ->
                    TrendIndicator(variacion = variacion)
                }
            }
            Spacer(Modifier.width(10.dp))
            // Nested inside the row's own click: the child consumes the tap, so
            // sharing never reads as a source switch.
            ShareRateAction(
                contentDescription = "Compartir ${quote.nombre}",
                onClick = { onShareRate(quote) }
            )
        }
    }
}

@Composable
private fun TrendIndicator(variacion: Double) {
    val isUp = variacion >= 0
    val color = if (isUp) trendColor() else downColor()
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrendIcon(variacion, color)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${if (isUp) "+" else ""}${"%.2f".format(variacion)}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TrendIcon(variacion: Double, tint: Color) {
    val icon: ImageVector = when {
        variacion > 0 -> Icons.AutoMirrored.Rounded.TrendingUp
        variacion < 0 -> Icons.AutoMirrored.Rounded.TrendingDown
        else -> Icons.AutoMirrored.Rounded.TrendingFlat
    }
    Icon(
        imageVector = icon,
        contentDescription = if (variacion > 0) "Sube" else if (variacion < 0) "Baja" else "Estable",
        tint = tint,
        modifier = Modifier.size(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun DolarScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fakeViewModel = FakePreviewViewModel(context)
    ERPTheme {
        DolarScreenContent(
            uiState = DolarUiState(
                quotes = previewQuotes(),
                selectedFuente = "usd",
                historial = listOf(
                    RateSample(
                        fuente = "usd",
                        nombre = "Dólar (BCV)",
                        precio = 772.54,
                        timestampEpochMillis = 1_787_068_800_000L,
                        anterior = 771.9,
                        variacion = 0.083
                    ),
                    RateSample(
                        fuente = "usd",
                        nombre = "Dólar (BCV)",
                        precio = 773.31,
                        timestampEpochMillis = 1_787_155_200_000L,
                        anterior = 772.54,
                        variacion = 0.099
                    )
                ),
                loading = false
            ),
            onSelectCasa = {},
            onRefresh = {},
            onToggleFutureRate = {},
            onOpenSettings = {},
            viewModel = fakeViewModel
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DolarScreenSinDatosPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fakeViewModel = FakePreviewViewModel(context)
    ERPTheme {
        DolarScreenContent(
            uiState = DolarUiState(
                quotes = previewQuotes(),
                selectedFuente = "usdt",
                historial = emptyList(),
                loading = false
            ),
            onSelectCasa = {},
            onRefresh = {},
            onToggleFutureRate = {},
            onOpenSettings = {},
            viewModel = fakeViewModel
        )
    }
}

private class FakePreviewViewModel(
    private val context: android.content.Context
) : DolarViewModel(
    application = context.applicationContext as android.app.Application,
    repository = com.example.erp.data.ApiDolarRepository(),
    historyStore = com.example.erp.data.FileHistoryStore(com.example.erp.data.FileHistoryStore.defaultDir(context)),
    themeRepository = com.example.erp.data.ThemeRepositoryImpl(com.example.erp.data.ThemePreferencesImpl(context))
)

private fun previewQuotes(): List<DolarQuote> = listOf(
    DolarQuote("usd", "Dólar (BCV)", 773.31, 772.54, 0.099, "2026-08-18"),
    DolarQuote("eur", "Euro (BCV)", 896.03, 894.49, 0.172, "2026-08-18"),
    DolarQuote("usdt", "USDT (P2P)", 889.5, null, null, "2026-08-18T10:30:00")
)