package com.example.notchclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class ClockService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var clockView: NotchClockView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var prefs: SharedPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var isScreenOn = true

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isScreenOn) return
            if (::clockView.isInitialized && clockView.windowToken != null) {
                clockView.invalidate()
            }
            val delay = 1000L - (System.currentTimeMillis() % 1000L)
            handler.postDelayed(this, delay)
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        updateViewParams()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    handler.removeCallbacks(updateRunnable)
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    handler.removeCallbacks(updateRunnable)
                    handler.post(updateRunnable)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        startForegroundNotification()

        prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        clockView = NotchClockView(this)

        val size = prefs.getInt("size", 120)
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            size,
            size,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = prefs.getInt("offsetX", 0)
            y = prefs.getInt("offsetY", 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val added = runCatching { windowManager.addView(clockView, params) }.isSuccess
        if (!added) {
            stopSelf()
            return
        }

        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, screenFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, screenFilter)
        }

        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateViewParams()
        return START_STICKY
    }

    private fun updateViewParams() {
        if (!::prefs.isInitialized || !::clockView.isInitialized) return

        val size = prefs.getInt("size", 120)
        params.width = size
        params.height = size
        params.x = prefs.getInt("offsetX", 0)
        params.y = prefs.getInt("offsetY", 0)

        clockView.reloadPreferences()
        if (clockView.windowToken != null) {
            runCatching { windowManager.updateViewLayout(clockView, params) }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "NotchClockChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notch Clock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Orologio attivo • Tocca per configurare")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        if (::prefs.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        }
        runCatching { unregisterReceiver(screenReceiver) }
        if (::clockView.isInitialized && clockView.windowToken != null) {
            runCatching { windowManager.removeView(clockView) }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
