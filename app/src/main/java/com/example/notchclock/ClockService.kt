package com.example.notchclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.util.Calendar

class ClockService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ClockView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var isOverlayAdded = false

    private var offsetX = 0
    private var offsetY = 0
    private var clockSize = 80

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        offsetX = prefs.getInt("offset_x", 0)
        offsetY = prefs.getInt("offset_y", 0)
        clockSize = prefs.getInt("clock_size", 80)

        createNotificationChannel()
        try {
            startForeground(1, createNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        overlayView = ClockView(this)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        tryAddOverlay()
        handler.post(updateClockRunnable)
    }

    private fun tryAddOverlay() {
        if (!isOverlayAdded && overlayView != null &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
        ) {
            try {
                windowManager.addView(overlayView, layoutParams)
                isOverlayAdded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            offsetX = it.getIntExtra("offset_x", offsetX)
            offsetY = it.getIntExtra("offset_y", offsetY)
            clockSize = it.getIntExtra("clock_size", clockSize)

            tryAddOverlay()
            if (isOverlayAdded) {
                overlayView?.invalidate()
            }
        }
        return START_STICKY
    }

    private val updateClockRunnable = object : Runnable {
        override fun run() {
            tryAddOverlay()
            if (isOverlayAdded) {
                overlayView?.invalidate()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private inner class ClockView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width / 2f + offsetX
            val centerY = clockSize.toFloat() + offsetY
            val radius = clockSize.toFloat()

            // Cerchio di sfondo
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            canvas.drawCircle(centerX, centerY, radius, paint)

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            // Lancetta Ore
            paint.color = Color.WHITE
            paint.strokeWidth = 6f
            val hourAngle = Math.toRadians(((hour + minute / 60.0) * 30 - 90))
            canvas.drawLine(
                centerX, centerY,
                (centerX + Math.cos(hourAngle) * (radius * 0.5f)).toFloat(),
                (centerY + Math.sin(hourAngle) * (radius * 0.5f)).toFloat(),
                paint
            )

            // Lancetta Minuti
            paint.strokeWidth = 4f
            val minuteAngle = Math.toRadians(((minute + second / 60.0) * 6 - 90))
            canvas.drawLine(
                centerX, centerY,
                (centerX + Math.cos(minuteAngle) * (radius * 0.75f)).toFloat(),
                (centerY + Math.sin(minuteAngle) * (radius * 0.75f)).toFloat(),
                paint
            )

            // Lancetta Secondi
            paint.color = Color.RED
            paint.strokeWidth = 2f
            val secondAngle = Math.toRadians((second * 6 - 90).toDouble())
            canvas.drawLine(
                centerX, centerY,
                (centerX + Math.cos(secondAngle) * (radius * 0.85f)).toFloat(),
                (centerY + Math.sin(secondAngle) * (radius * 0.85f)).toFloat(),
                paint
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "notch_clock_channel",
                "Notch Clock Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager.class)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "notch_clock_channel")
            .setContentTitle("Notch Clock")
            .setContentText("L'orologio in overlay è attivo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateClockRunnable)
        if (isOverlayAdded && overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
