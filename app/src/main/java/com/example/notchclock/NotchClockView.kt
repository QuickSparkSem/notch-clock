package com.example.notchclock

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class NotchClockView(context: Context, private val prefs: SharedPreferences) : View(context) {

    private val paintTicks = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    private val paintHourDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintMinuteDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 10000) // Aggiorna ogni 10 secondi per risparmio batteria
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(ticker)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val radiusStr = prefs.getInt("radius", 100).toFloat()
        val tickColor = prefs.getInt("color_ticks", Color.WHITE)
        val hourColor = prefs.getInt("color_hour", Color.RED)
        val minColor = prefs.getInt("color_minute", Color.CYAN)

        paintTicks.color = tickColor
        paintHourDot.color = hourColor
        paintMinuteDot.color = minColor

        val cx = width / 2f
        val cy = height / 2f
        
        // Disegna le 12 tacche
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val startX = cx + (radiusStr - 15f) * cos(angle).toFloat()
            val startY = cy + (radiusStr - 15f) * sin(angle).toFloat()
            val stopX = cx + radiusStr * cos(angle).toFloat()
            val stopY = cy + radiusStr * sin(angle).toFloat()
            canvas.drawLine(startX, startY, stopX, stopY, paintTicks)
        }

        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)

        // Puntino ORE (più interno, più grande)
        val hourAngle = Math.toRadians((hours % 12 + minutes / 60.0) * 30.0 - 90.0)
        val hX = cx + (radiusStr * 0.55f) * cos(hourAngle).toFloat()
        val hY = cy + (radiusStr * 0.55f) * sin(hourAngle).toFloat()
        canvas.drawCircle(hX, hY, 12f, paintHourDot)

        // Puntino MINUTI (più esterno, più piccolo)
        val minAngle = Math.toRadians(minutes * 6.0 - 90.0)
        val mX = cx + (radiusStr * 0.85f) * cos(minAngle).toFloat()
        val mY = cy + (radiusStr * 0.85f) * sin(minAngle).toFloat()
        canvas.drawCircle(mX, mY, 8f, paintMinuteDot)
    }
}
