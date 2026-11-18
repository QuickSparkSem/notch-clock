package com.example.notchclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class NotchClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val calendar = Calendar.getInstance()

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

        val minSide = min(width, height).toFloat()
        val padding = (minSide * 0.06f).coerceAtLeast(2f)
        val radius = (minSide / 2f) - padding
        if (radius <= 0f) return

        val centerX = width / 2f
        val centerY = height / 2f

        calendar.timeInMillis = System.currentTimeMillis()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        val tickLength = (radius * 0.16f).coerceAtLeast(3f)
        paint.color = ticksColor
        paint.strokeWidth = (radius * 0.035f).coerceAtLeast(1.2f)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            canvas.drawLine(
                centerX + (radius - tickLength) * cosA,
                centerY + (radius - tickLength) * sinA,
                centerX + radius * cosA,
                centerY + radius * sinA,
                paint
            )
        }

        val hourAngle = Math.toRadians(((hours + minutes / 60f) * 30 - 90).toDouble())
        val minuteAngle = Math.toRadians(((minutes + seconds / 60f) * 6 - 90).toDouble())
        val secondAngle = Math.toRadians((seconds * 6 - 90).toDouble())

        if (useHands) {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND

            paint.color = hourColor
            paint.strokeWidth = (radius * 0.09f).coerceAtLeast(2.5f)
            canvas.drawLine(
                centerX, centerY,
                centerX + (radius * 0.50f) * cos(hourAngle).toFloat(),
                centerY + (radius * 0.50f) * sin(hourAngle).toFloat(),
                paint
            )

            paint.color = minuteColor
            paint.strokeWidth = (radius * 0.06f).coerceAtLeast(2f)
            canvas.drawLine(
                centerX, centerY,
                centerX + (radius * 0.75f) * cos(minuteAngle).toFloat(),
                centerY + (radius * 0.75f) * sin(minuteAngle).toFloat(),
                paint
            )

            paint.color = secondColor
            paint.strokeWidth = (radius * 0.03f).coerceAtLeast(1.2f)
            canvas.drawLine(
                centerX, centerY,
                centerX + (radius * 0.88f) * cos(secondAngle).toFloat(),
                centerY + (radius * 0.88f) * sin(secondAngle).toFloat(),
                paint
            )

            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, centerY, (radius * 0.065f).coerceAtLeast(2f), paint)
        } else {
            paint.style = Paint.Style.FILL

            paint.color = hourColor
            canvas.drawCircle(
                centerX + (radius * 0.55f) * cos(hourAngle).toFloat(),
                centerY + (radius * 0.55f) * sin(hourAngle).toFloat(),
                (radius * 0.11f).coerceAtLeast(3f),
                paint
            )

            paint.color = minuteColor
            canvas.drawCircle(
                centerX + (radius * 0.75f) * cos(minuteAngle).toFloat(),
                centerY + (radius * 0.75f) * sin(minuteAngle).toFloat(),
                (radius * 0.08f).coerceAtLeast(2.5f),
                paint
            )

            paint.color = secondColor
            canvas.drawCircle(
                centerX + (radius * 0.88f) * cos(secondAngle).toFloat(),
                centerY + (radius * 0.88f) * sin(secondAngle).toFloat(),
                (radius * 0.055f).coerceAtLeast(2f),
                paint
            )
        }
    }
}
