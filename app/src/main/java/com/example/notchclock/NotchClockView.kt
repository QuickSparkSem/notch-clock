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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var useHands = true
    private var hourColor = Color.WHITE
    private var minuteColor = Color.WHITE
    private var secondColor = Color.RED
    private var ticksColor = Color.GRAY

    init {
        reloadPreferences()
    }

    fun reloadPreferences() {
        val prefs = context.getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        useHands = prefs.getBoolean("useHands", true)
        hourColor = prefs.getInt("hourColor", Color.WHITE)
        minuteColor = prefs.getInt("minuteColor", Color.WHITE)
        secondColor = prefs.getInt("secondColor", Color.RED)
        ticksColor = prefs.getInt("ticksColor", Color.GRAY)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) / 2f) - 10f

        if (radius <= 0) return

        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        // Disegna le tacche del quadrante
        paint.color = ticksColor
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE

        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val startX = centerX + (radius - 12) * cos(angle).toFloat()
            val startY = centerY + (radius - 12) * sin(angle).toFloat()
            val endX = centerX + radius * cos(angle).toFloat()
            val endY = centerY + radius * sin(angle).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }

        // Calcolo angoli lancette/puntini
        val hourAngle = Math.toRadians(((hours + minutes / 60f) * 30 - 90).toDouble())
        val minuteAngle = Math.toRadians(((minutes + seconds / 60f) * 6 - 90).toDouble())
        val secondAngle = Math.toRadians((seconds * 6 - 90).toDouble())

        if (useHands) {
            // RENDERING A LANCETTE
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND

            // Ore
            paint.color = hourColor
            paint.strokeWidth = 8f
            canvas.drawLine(
                centerX, centerY,
                centerX + (radius * 0.5f) * cos(hourAngle).toFloat(),
                centerY + (radius * 0.5f) * sin(hourAngle).toFloat(),
                paint
            )

            // Minuti
            paint.color = minuteColor
            paint.strokeWidth = 5f
            canvas.drawLine(
                centerX, centerY,
                centerX + (radius * 0.75f) * cos(minuteAngle).toFloat(),
                centerY + (radius * 0.75f) * sin(minuteAngle).toFloat(),
                paint
            )

            // Secondi
            paint.color = secondColor
            paint.strokeWidth = 3f
            canvas.drawLine(
                centerX, centerY,
                centerX + (radius * 0.9f) * cos(secondAngle).toFloat(),
                centerY + (radius * 0.9f) * sin(secondAngle).toFloat(),
                paint
            )
        } else {
            // RENDERING A PUNTINI
            paint.style = Paint.Style.FILL

            // Puntino Ore
            paint.color = hourColor
            val hX = centerX + (radius * 0.55f) * cos(hourAngle).toFloat()
            val hY = centerY + (radius * 0.55f) * sin(hourAngle).toFloat()
            canvas.drawCircle(hX, hY, 7f, paint)

            // Puntino Minuti
            paint.color = minuteColor
            val mX = centerX + (radius * 0.75f) * cos(minuteAngle).toFloat()
            val mY = centerY + (radius * 0.75f) * sin(minuteAngle).toFloat()
            canvas.drawCircle(mX, mY, 5f, paint)

            // Puntino Secondi
            paint.color = secondColor
            val sX = centerX + (radius * 0.9f) * cos(secondAngle).toFloat()
            val sY = centerY + (radius * 0.9f) * sin(secondAngle).toFloat()
            canvas.drawCircle(sX, sY, 3.5f, paint)
        }
    }
}
