package com.example.notchclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class ClockService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var clockView: NotchClockView
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (::clockView.isInitialized) {
                clockView.invalidate()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateViewParams()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        clockView = NotchClockView(this)

        val prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        val size = prefs.getInt("size", 100)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            size + 100,
            size + 100,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = prefs.getInt("offsetX", 0)
            y = prefs.getInt("offsetY", 0)

            // Cutout / Notch Layout Mode (API 28+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager.addView(clockView, params)
        handler.post(updateRunnable)

        val filter = IntentFilter("com.example.notchclock.UPDATE_SETTINGS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun updateViewParams() {
        val prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        val size = prefs.getInt("size", 100)
        params.width = size + 100
        params.height = size + 100
        params.x = prefs.getInt("offsetX", 0)
        params.y = prefs.getInt("offsetY", 0)

        if (::clockView.isInitialized) {
            clockView.reloadPreferences()
            windowManager.updateViewLayout(clockView, params)
        }
    }

    private fun startForegroundNotification() {
        val channelId = "NotchClockChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notch Clock Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Notch Clock")
            .setContentText("Orologio attivo sulla Notch")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        runCatching { unregisterReceiver(receiver) }
        if (::clockView.isInitialized) {
            windowManager.removeView(clockView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
