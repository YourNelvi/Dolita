package com.example.erp.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.erp.MainActivity
import com.example.erp.R
import com.example.erp.notification.NotificationHelper
import kotlinx.coroutines.*
import kotlin.math.abs

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        /**
         * Kept in the 2000 block shared with BubbleService, away from the 1000 block that
         * NotificationHelper uses for rate announcements, so an ongoing foreground notification can
         * never overwrite a rate alert (or the other way around).
         */
        const val NOTIFICATION_ID = 2002
    }

    // Rate data
    private var usdRate = 0.0
    private var eurRate = 0.0
    private var usdtRate = 0.0
    private var usdChange = 0.0
    private var eurChange = 0.0
    private var usdtChange = 0.0

    // Selected rate (0=USD, 1=EUR, 2=USDT/Paralelo)
    private var selectedRate = 0

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        // BubbleService starts this service with startForegroundService(), so the platform kills it
        // with ForegroundServiceDidNotStartInTimeException unless startForeground() runs first.
        // It is the very first thing after super.onCreate(): inflating the window, adding the view
        // and kicking off the rate fetch must never eat into the 5 second budget.
        NotificationHelper.createChannels(this)
        startForegroundCompat(createNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_window, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            x = 0
            y = 150
        }

        // Make draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(overlayView, params)
        setupViews()
        scope.launch { fetchRates() }
        startRateUpdates()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_OVERLAY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Dolita superpuesta")
            .setContentText("Toca para cerrar la calculadora")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Since API 34 a service that declares a `foregroundServiceType` must use the typed
     * [Service.startForeground] overload, otherwise the platform throws
     * `MissingForegroundServiceTypeException`. `ServiceCompat` is not used here because the
     * resolved `androidx.core:core:1.10.1` does not expose a `startForeground` overload at all,
     * and this task may not add dependencies.
     *
     * Below API 34 the `specialUse` type does not exist yet, so the manifest attribute is ignored
     * and the untyped overload is the correct call.
     */
    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupViews() {
        val btnClose = overlayView?.findViewById<ImageButton>(R.id.btn_close)
        val btnOpenApp = overlayView?.findViewById<Button>(R.id.btn_open_app)
        val etAmount = overlayView?.findViewById<EditText>(R.id.et_amount)
        val tabUsd = overlayView?.findViewById<TextView>(R.id.tab_usd)
        val tabEur = overlayView?.findViewById<TextView>(R.id.tab_eur)
        val tabUsdt = overlayView?.findViewById<TextView>(R.id.tab_usdt)

        btnClose?.setOnClickListener {
            stopSelf()
        }

        btnOpenApp?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            stopSelf()
        }

        // Rate tab clicks
        tabUsd?.setOnClickListener { selectRate(0) }
        tabEur?.setOnClickListener { selectRate(1) }
        tabUsdt?.setOnClickListener { selectRate(2) }

        // Calculator input - force keyboard
        etAmount?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showKeyboard(etAmount)
        }
        etAmount?.setOnClickListener { showKeyboard(it as EditText) }

        etAmount?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateConversion(s?.toString())
            }
        })

        // Force keyboard on start
        etAmount?.postDelayed({ showKeyboard(etAmount) }, 300)
    }

    private fun showKeyboard(editText: EditText) {
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun selectRate(index: Int) {
        selectedRate = index
        val tabs = arrayOf(
            overlayView?.findViewById<TextView>(R.id.tab_usd),
            overlayView?.findViewById<TextView>(R.id.tab_eur),
            overlayView?.findViewById<TextView>(R.id.tab_usdt)
        )

        tabs.forEachIndexed { i, tab ->
            tab?.apply {
                if (i == index) {
                    setBackgroundResource(R.drawable.tab_selected)
                    setTextColor(0xFFFFFFFF.toInt())
                } else {
                    setBackgroundResource(android.R.color.transparent)
                    setTextColor(0x88FFFFFF.toInt())
                }
            }
        }

        updateRateDisplay()
        updateConversion(
            overlayView?.findViewById<EditText>(R.id.et_amount)?.text?.toString()
        )
    }

    private fun updateRateDisplay() {
        val tvLabel = overlayView?.findViewById<TextView>(R.id.tv_rate_label)
        val tvMain = overlayView?.findViewById<TextView>(R.id.tv_rate_main)
        val tvChange = overlayView?.findViewById<TextView>(R.id.tv_rate_change)

        when (selectedRate) {
            0 -> {
                tvLabel?.text = "Dólar BCV"
                tvMain?.text = "$${formatNumber(usdRate)}"
                tvChange?.text = if (usdChange >= 0) "↑ ${formatNumber(abs(usdChange))}%" else "↓ ${formatNumber(abs(usdChange))}%"
                tvChange?.setTextColor(if (usdChange >= 0) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
            }
            1 -> {
                tvLabel?.text = "Euro BCV"
                tvMain?.text = "€${formatNumber(eurRate)}"
                tvChange?.text = if (eurChange >= 0) "↑ ${formatNumber(abs(eurChange))}%" else "↓ ${formatNumber(abs(eurChange))}%"
                tvChange?.setTextColor(if (eurChange >= 0) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
            }
            2 -> {
                tvLabel?.text = "Paralelo"
                tvMain?.text = "$${formatNumber(usdtRate)}"
                tvChange?.text = if (usdtChange >= 0) "↑ ${formatNumber(abs(usdtChange))}%" else "↓ ${formatNumber(abs(usdtChange))}%"
                tvChange?.setTextColor(if (usdtChange >= 0) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
            }
        }
    }

    private fun updateConversion(amount: String?) {
        val tvResult = overlayView?.findViewById<TextView>(R.id.tv_result)
        if (tvResult == null) return

        if (amount.isNullOrBlank()) {
            tvResult.text = "Bs. 0,00"
            return
        }

        try {
            val amountValue = amount.replace(",", ".").toDouble()
            val rate = when (selectedRate) {
                0 -> usdRate
                1 -> eurRate
                2 -> usdtRate
                else -> usdRate
            }
            if (rate > 0) {
                val result = amountValue * rate
                tvResult.text = "Bs. ${formatNumber(result)}"
            } else {
                tvResult.text = "Bs. 0,00"
            }
        } catch (_: Exception) {
            tvResult.text = "Bs. 0,00"
        }
    }

    private fun formatNumber(value: Double): String {
        val bd = java.math.BigDecimal.valueOf(value)
            .setScale(2, java.math.RoundingMode.HALF_UP)
        val plain = bd.toPlainString().replace('.', ',')
        val parts = plain.split(',')
        val withThousands = parts[0].reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
        return if (parts.size > 1) "$withThousands,${parts[1]}" else "$withThousands,00"
    }

    private fun startRateUpdates() {
        job = scope.launch {
            while (isActive) {
                fetchRates()
                delay(60_000)
            }
        }
    }

    private suspend fun fetchRates() {
        try {
            withContext(Dispatchers.IO) {
                // Fetch USD
                val usdUrl = java.net.URL("https://ve.dolarapi.com/v1/dolares/oficial")
                val usdConn = usdUrl.openConnection() as java.net.HttpURLConnection
                usdConn.connectTimeout = 5000
                usdConn.readTimeout = 5000
                val usdResponse = usdConn.inputStream.bufferedReader().readText()
                val usdJson = org.json.JSONObject(usdResponse)
                usdRate = usdJson.getDouble("promedio")
                usdChange = usdJson.optDouble("variacion", 0.0)

                // Fetch EUR
                val eurUrl = java.net.URL("https://ve.dolarapi.com/v1/euros/oficial")
                val eurConn = eurUrl.openConnection() as java.net.HttpURLConnection
                eurConn.connectTimeout = 5000
                eurConn.readTimeout = 5000
                val eurResponse = eurConn.inputStream.bufferedReader().readText()
                val eurJson = org.json.JSONObject(eurResponse)
                eurRate = eurJson.getDouble("promedio")
                eurChange = eurJson.optDouble("variacion", 0.0)

                // Fetch Paralelo (was USDT)
                val parUrl = java.net.URL("https://ve.dolarapi.com/v1/dolares/paralelo")
                val parConn = parUrl.openConnection() as java.net.HttpURLConnection
                parConn.connectTimeout = 5000
                parConn.readTimeout = 5000
                val parResponse = parConn.inputStream.bufferedReader().readText()
                val parJson = org.json.JSONObject(parResponse)
                usdtRate = parJson.getDouble("promedio")
                usdtChange = parJson.optDouble("variacion", 0.0)

                withContext(Dispatchers.Main) {
                    updateRateDisplay()
                    updateConversion(
                        overlayView?.findViewById<EditText>(R.id.et_amount)?.text?.toString()
                    )
                }
            }
        } catch (e: Exception) {
            // Keep old values on error
        }
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        overlayView?.let { windowManager?.removeView(it) }
        stopForeground(true)
        super.onDestroy()
    }
}