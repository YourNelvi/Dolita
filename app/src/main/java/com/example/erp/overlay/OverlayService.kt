package com.example.erp.overlay

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.erp.MainActivity
import com.example.erp.R
import kotlinx.coroutines.*

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
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
            y = 100
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
        startRateUpdates()
    }

    private fun setupViews() {
        val btnClose = overlayView?.findViewById<ImageButton>(R.id.btn_close)
        val btnOpenApp = overlayView?.findViewById<Button>(R.id.btn_open_app)
        val etAmount = overlayView?.findViewById<EditText>(R.id.et_amount)
        val tvResult = overlayView?.findViewById<TextView>(R.id.tv_result)

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

        etAmount?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateConversion(s?.toString())
            }
        })
    }

    private fun updateConversion(amount: String?) {
        val tvResult = overlayView?.findViewById<TextView>(R.id.tv_result)
        val tvRateUsd = overlayView?.findViewById<TextView>(R.id.tv_rate_usd)

        if (amount.isNullOrBlank()) {
            tvResult?.text = "Bs. 0,00"
            return
        }

        try {
            val usdAmount = amount.replace(",", ".").toDouble()
            val rateText = tvRateUsd?.text?.toString() ?: ""
            val rate = extractRate(rateText)
            if (rate > 0) {
                val result = usdAmount * rate
                tvResult?.text = "Bs. ${formatNumber(result)}"
            }
        } catch (_: Exception) {
            tvResult?.text = "Bs. 0,00"
        }
    }

    private fun extractRate(rateText: String): Double {
        return try {
            val clean = rateText
                .replace("USD:", "")
                .replace("$", "")
                .replace(".", "")
                .replace(",", ".")
                .trim()
            clean.toDouble()
        } catch (_: Exception) {
            0.0
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
                delay(60_000) // Update every minute
            }
        }
    }

    private suspend fun fetchRates() {
        try {
            val tvUsd = overlayView?.findViewById<TextView>(R.id.tv_rate_usd)
            val tvEur = overlayView?.findViewById<TextView>(R.id.tv_rate_eur)
            val tvUsdt = overlayView?.findViewById<TextView>(R.id.tv_rate_usdt)

            withContext(Dispatchers.IO) {
                // Fetch USD
                val usdUrl = java.net.URL("https://ve.dolarapi.com/v1/dolares/oficial")
                val usdConn = usdUrl.openConnection() as java.net.HttpURLConnection
                val usdResponse = usdConn.inputStream.bufferedReader().readText()
                val usdJson = org.json.JSONObject(usdResponse)
                val usdRate = usdJson.getDouble("promedio")

                withContext(Dispatchers.Main) {
                    tvUsd?.text = "USD: $${formatNumber(usdRate)}"
                }

                // Fetch EUR
                val eurUrl = java.net.URL("https://ve.dolarapi.com/v1/euros/oficial")
                val eurConn = eurUrl.openConnection() as java.net.HttpURLConnection
                val eurResponse = eurConn.inputStream.bufferedReader().readText()
                val eurJson = org.json.JSONObject(eurResponse)
                val eurRate = eurJson.getDouble("promedio")

                withContext(Dispatchers.Main) {
                    tvEur?.text = "EUR: €${formatNumber(eurRate)}"
                }

                // Fetch USDT
                val usdtUrl = java.net.URL("https://ve.dolarapi.com/v1/dolares/usdt")
                val usdtConn = usdtUrl.openConnection() as java.net.HttpURLConnection
                val usdtResponse = usdtConn.inputStream.bufferedReader().readText()
                val usdtJson = org.json.JSONObject(usdtResponse)
                val usdtRate = usdtJson.getDouble("promedio")

                withContext(Dispatchers.Main) {
                    tvUsdt?.text = "USDT: $$${formatNumber(usdtRate)}"
                    updateConversion(
                        overlayView?.findViewById<EditText>(R.id.et_amount)?.text?.toString()
                    )
                }
            }
        } catch (_: Exception) {
            // Keep old values on error
        }
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        overlayView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }
}
