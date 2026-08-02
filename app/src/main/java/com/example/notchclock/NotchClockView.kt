package com.example.notchclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.min

class NotchClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val needlePath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val prefs = context.getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        val colorHour = prefs.getInt("colorHour", Color.RED)
        val colorMinute = prefs.getInt("colorMinute", Color.GREEN)
        val colorSecond = prefs.getInt("colorSecond", Color.CYAN)
        val colorTicks = prefs.getInt("colorTicks", Color.WHITE)
        val isHandsStyle = prefs.getBoolean("isHandsStyle", false)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) / 2f) - 20f
        if (radius <= 0) return

        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)

        // Angoli di rotazione orario
        val hourAngle = ((h % 12) + m / 60f) * 30f
        val minuteAngle = (m + s / 60f) * 6f
        val secondAngle = s * 6f

        // 1. Quadrante con 12 puntini
        paint.color = colorTicks
        paint.style = Paint.Style.FILL
        for (i in 0 until 12) {
            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(i * 30f)
            canvas.drawCircle(0f, -(radius - 10f), 4f, paint)
            canvas.restore()
        }

        if (isHandsStyle) {
            // --- MODALITÀ LANCETTE ---

            // Lancetta Ore (Corta e tozza)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = colorHour
            paint.strokeWidth = 10f
            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(hourAngle)
            canvas.drawLine(0f, 0f, 0f, -radius * 0.45f, paint)
            canvas.restore()

            // Lancetta Minuti (Più lunga)
            paint.color = colorMinute
            paint.strokeWidth = 5f
            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(minuteAngle)
            canvas.drawLine(0f, 0f, 0f, -radius * 0.72f, paint)
            canvas.restore()

            // Lancetta Secondi (A punta e sottile)
            paint.style = Paint.Style.FILL
            paint.color = colorSecond
            needlePath.reset()
            needlePath.moveTo(-2.5f, 0f)
            needlePath.lineTo(0f, -radius * 0.85f)
            needlePath.lineTo(2.5f, 0f)
            needlePath.close()

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(secondAngle)
            canvas.drawPath(needlePath, paint)
            canvas.restore()

            // Pernetto centrale
            paint.color = colorTicks
            canvas.drawCircle(centerX, centerY, 4f, paint)

        } else {
            // --- MODALITÀ PUNTINI ---

            fun drawDot(angle: Float, distOffset: Float, dotRadius: Float, color: Int) {
                canvas.save()
                canvas.translate(centerX, centerY)
                canvas.rotate(angle)
                paint.style = Paint.Style.FILL
                paint.color = color
                canvas.drawCircle(0f, -(radius - distOffset), dotRadius, paint)
                canvas.restore()
            }

            drawDot(hourAngle, 5f, 9f, colorHour)
            drawDot(minuteAngle, 5f, 7f, colorMinute)
            drawDot(secondAngle, 0f, 5f, colorSecond)
        }
    }
}
