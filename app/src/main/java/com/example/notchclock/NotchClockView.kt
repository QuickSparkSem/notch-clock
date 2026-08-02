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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val prefs = context.getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        val colorHour = prefs.getInt("colorHour", Color.RED)
        val colorMinute = prefs.getInt("colorMinute", Color.GREEN)
        val colorSecond = prefs.getInt("colorSecond", Color.CYAN)
        val colorTicks = prefs.getInt("colorTicks", Color.WHITE)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) / 2f) - 20f
        if (radius <= 0) return

        // 1. Tacche radiali (12)
        paint.color = colorTicks
        for (i in 0 until 12) {
            val rad = Math.toRadians((i * 30 - 90).toDouble())
            canvas.drawCircle(
                centerX + (radius - 12) * cos(rad).toFloat(),
                centerY + (radius - 12) * sin(rad).toFloat(),
                4f,
                paint
            )
        }

        // 2. Calcolo orario corrente
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)

        // Helper interno per disegnare i puntini dell'orologio
        fun drawDot(angleDeg: Double, distOffset: Float, dotRadius: Float, color: Int) {
            val rad = Math.toRadians(angleDeg - 90)
            paint.color = color
            canvas.drawCircle(
                centerX + (radius - distOffset) * cos(rad).toFloat(),
                centerY + (radius - distOffset) * sin(rad).toFloat(),
                dotRadius,
                paint
            )
        }

        // Disegno puntini: Ore, Minuti, Secondi
        drawDot((h + m / 60f) * 30.0, 5f, 9f, colorHour)
        drawDot((m + s / 60f) * 6.0, 5f, 7f, colorMinute)
        drawDot(s * 6.0, 0f, 5f, colorSecond)
    }
}
