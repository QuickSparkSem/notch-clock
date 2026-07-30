package com.example.notchclock

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import java.util.Calendar

class ClockService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: AnalogClockView
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            overlayView.invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = AnalogClockView(this)

        val params = WindowManager.LayoutParams(
            200, 200,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }

        windowManager.addView(overlayView, params)
        handler.post(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    private inner class AnalogClockView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = Math.min(cx, cy) - 10f

            val calendar = Calendar.getInstance()
            val hours = calendar.get(Calendar.HOUR)
            val minutes = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)

            // Cerchio esterno del notch
            paint.color = Color.CYAN
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            canvas.drawCircle(cx, cy, radius, paint)

            // Lancetta ore
            val hourAngle = Math.toRadians(((hours + minutes / 60.0) * 30.0 - 90.0))
            val hx = cx + (radius * 0.5 * Math.cos(hourAngle)).toFloat()
            val hy = cy + (radius * 0.5 * Math.sin(hourAngle)).toFloat()
            paint.color = Color.WHITE
            paint.strokeWidth = 8f
            canvas.drawLine(cx, cy, hx, hy, paint)

            // Lancetta minuti
            val minAngle = Math.toRadians(((minutes + seconds / 60.0) * 6.0 - 90.0))
            val mx = cx + (radius * 0.7 * Math.cos(minAngle)).toFloat()
            val my = cy + (radius * 0.7 * Math.sin(minAngle)).toFloat()
            paint.strokeWidth = 5f
            canvas.drawLine(cx, cy, mx, my, paint)

            // Lancetta secondi
            val secAngle = Math.toRadians((seconds * 6.0 - 90.0))
            val sx = cx + (radius * 0.85 * Math.cos(secAngle)).toFloat()
            val sy = cy + (radius * 0.85 * Math.sin(secAngle)).toFloat()
            paint.color = Color.RED
            paint.strokeWidth = 3f
            canvas.drawLine(cx, cy, sx, sy, paint)
        }
    }
}
