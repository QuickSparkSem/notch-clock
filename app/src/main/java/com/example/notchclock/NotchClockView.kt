package com.example.notchclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class NotchClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val minutePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val prefs = context.getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        hourPaint.color = prefs.getInt("colorHour", Color.RED)
        minutePaint.color = prefs.getInt("colorMinute", Color.GREEN)
        secondPaint.color = prefs.getInt("colorSecond", Color.CYAN)
        tickPaint.color = prefs.getInt("colorTicks", Color.WHITE)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) / 2f) - 20f

        if (radius <= 0) return

        // 1. Tacche radiali (12)
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val startX = centerX + (radius - 12) * cos(angle).toFloat()
            val startY = centerY + (radius - 12) * sin(angle).toFloat()
            canvas.drawCircle(startX, startY, 4f, tickPaint)
        }

        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        // 2. Puntino Ore
        val hourAngle = Math.toRadians(((hours + minutes / 60f) * 30 - 90).toDouble())
        val hourX = centerX + (radius - 5) * cos(hourAngle).toFloat()
        val hourY = centerY + (radius - 5) * sin(hourAngle).toFloat()
        canvas.drawCircle(hourX, hourY, 9f, hourPaint)

        // 3. Puntino Minuti
        val minuteAngle = Math.toRadians(((minutes + seconds / 60f) * 6 - 90).toDouble())
        val minuteX = centerX + (radius - 5) * cos(minuteAngle).toFloat()
        val minuteY = centerY + (radius - 5) * sin(minuteAngle).toFloat()
        canvas.drawCircle(minuteX, minuteY, 7f, minutePaint)

        // 4. Puntino Secondi
        val secondAngle = Math.toRadians((seconds * 6 - 90).toDouble())
        val secondX = centerX + radius * cos(secondAngle).toFloat()
        val secondY = centerY + radius * sin(secondAngle).toFloat()
        canvas.drawCircle(secondX, secondY, 5f, secondPaint)
    }
}
