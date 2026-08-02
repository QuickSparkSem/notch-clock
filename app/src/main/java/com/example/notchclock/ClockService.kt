package com.example.notchclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class ClockService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var windowManager: WindowManager
    private var clockView: NotchClockView? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("ClockPrefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)
        
        startForegroundService()
        showOverlay()
    }

    private fun startForegroundService() {
        val channelId = "clock_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notch Clock Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Notch Clock")
            .setContentText("Orologio in sovrimpressione attivo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        clockView = NotchClockView(this, prefs)

        // FLAG_NOT_TOUCHABLE e FLAG_NOT_FOCUSABLE permettono al tocco di passare attraverso
        layoutParams = WindowManager.LayoutParams(
            300, 300,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        updatePositionParams()

        try {
            windowManager.addView(clockView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePositionParams() {
        layoutParams.x = prefs.getInt("offset_x", 0)
        layoutParams.y = prefs.getInt("offset_y", 50)
        // La View è quadrata. Il radius massimo gestibile dentro i 300x300 pixel è gestito dal View stesso.
        // Se si espande oltre, allarghiamo i layoutParams
        val size = prefs.getInt("radius", 100) * 2 + 50
        layoutParams.width = size
        layoutParams.height = size
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in listOf("offset_x", "offset_y", "radius")) {
            updatePositionParams()
            if (clockView != null) {
                windowManager.updateViewLayout(clockView, layoutParams)
            }
        }
        clockView?.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (clockView != null) {
            windowManager.removeView(clockView)
            clockView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
