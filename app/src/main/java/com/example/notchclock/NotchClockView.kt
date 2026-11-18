package com.example.notchclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.format.DateFormat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class NotchClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val calendar = Calendar.getInstance()
    private val handPath = Path()
    private val arcRect = RectF()

    // Dimensione e posizione base
    private var clockSize = 120
    private var offsetY = 0

    // Stili:
    // dialStyle: 0 = Classico (12), 1 = Minimal (4), 2 = Completo (60), 3 = Anello, 4 = Nessuno
    // handStyle: 0 = Classiche lineari, 1 = A punta (lancetta), 2 = Tonde (puntini), 3 = Archi
    // tickShape: 0 = Lineari, 1 = Tonde
    private var dialStyle = 0
    private var handStyle = 1
    private var tickShape = 0

    // Colori
    private var hourColor = Color.WHITE
    private var minuteColor = Color.WHITE
    private var secondColor = Color.parseColor("#FF5252")
    private var ticksColor = Color.parseColor("#9E9E9E")

    // Lunghezze (% del raggio)
    private var hourLength = 50
    private var minuteLength = 75
    private var secondLength = 88
    private var ticksLength = 18

    // Spessori
    private var hourThickness = 8
    private var minuteThickness = 5
    private var secondThickness = 3
    private var ticksThickness = 3

    // Trasparenze (0 = opaco, 100 = trasparente)
    private var hourTransparency = 0
    private var minuteTransparency = 0
    private var secondTransparency = 0
    private var ticksTransparency = 0

    // Numeri laterali (ore a sinistra, minuti a destra)
    private var digitalEnabled = true
    private var digitalDistance = 14
    private var digitalSize = 22
    private var digitalY = 45
    private var digitalTransparency = 0
    private var digitalColor = Color.WHITE

    init {
        reloadPreferences()
    }

    fun reloadPreferences() {
        val prefs = context.getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        val fallbackHandStyle = if (prefs.getBoolean("useHands", true)) 1 else 2

        clockSize = prefs.getInt("size", 120)
        offsetY = prefs.getInt("offsetY", 0)

        dialStyle = prefs.getInt("dialStyle", 0)
        handStyle = prefs.getInt("handStyle", fallbackHandStyle)
        tickShape = prefs.getInt("tickShape", 0)

        hourColor = prefs.getInt("hourColor", Color.WHITE)
        minuteColor = prefs.getInt("minuteColor", Color.WHITE)
        secondColor = prefs.getInt("secondColor", Color.parseColor("#FF5252"))
        ticksColor = prefs.getInt("ticksColor", Color.parseColor("#9E9E9E"))

        hourLength = prefs.getInt("hourLength", 50)
        minuteLength = prefs.getInt("minuteLength", 75)
        secondLength = prefs.getInt("secondLength", 88)
        ticksLength = prefs.getInt("ticksLength", 18)

        hourThickness = prefs.getInt("hourThickness", 8)
        minuteThickness = prefs.getInt("minuteThickness", 5)
        secondThickness = prefs.getInt("secondThickness", 3)
        ticksThickness = prefs.getInt("ticksThickness", 3)

        hourTransparency = prefs.getInt("hourTransparency", 0)
        minuteTransparency = prefs.getInt("minuteTransparency", 0)
        secondTransparency = prefs.getInt("secondTransparency", 0)
        ticksTransparency = prefs.getInt("ticksTransparency", 0)

        digitalEnabled = prefs.getBoolean("digitalEnabled", true)
        digitalDistance = prefs.getInt("digitalDistance", 14)
        digitalSize = prefs.getInt("digitalSize", 22)
        digitalY = prefs.getInt("digitalY", 45)
        digitalTransparency = prefs.getInt("digitalTransparency", 0)
        digitalColor = prefs.getInt("digitalColor", Color.WHITE)

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val minSide = clockSize.toFloat()
        val padding = (minSide * 0.06f).coerceAtLeast(3f)
        val radius = (minSide / 2f) - padding
        if (radius <= 0f) return

        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            digitalSize.toFloat(),
            resources.displayMetrics
        )
        val winTop = if (digitalEnabled) min(offsetY, (digitalY - textSizePx).toInt()) else offsetY

        val centerX = width / 2f
        val centerY = if (digitalEnabled) {
            (offsetY - winTop) + (clockSize / 2f)
        } else {
            height / 2f
        }
        val scale = radius / 100f

        calendar.timeInMillis = System.currentTimeMillis()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        // 1. Disegna il quadrante / tacche
        drawDial(canvas, centerX, centerY, radius, scale)

        // 2. Calcola angoli e progressi
        val hourProgress = ((hours % 12) + minutes / 60f) / 12f
        val minuteProgress = (minutes + seconds / 60f) / 60f
        val secondProgress = seconds / 60f

        val hourAngle = Math.toRadians((hourProgress * 360f - 90f).toDouble())
        val minuteAngle = Math.toRadians((minuteProgress * 360f - 90f).toDouble())
        val secondAngle = Math.toRadians((secondProgress * 360f - 90f).toDouble())

        // 3. Disegna Ore, Minuti, Secondi analogici
        drawHandElement(
            canvas, centerX, centerY, radius, scale,
            hourAngle, hourProgress, hourColor, hourTransparency, hourLength, hourThickness
        )
        drawHandElement(
            canvas, centerX, centerY, radius, scale,
            minuteAngle, minuteProgress, minuteColor, minuteTransparency, minuteLength, minuteThickness
        )
        drawHandElement(
            canvas, centerX, centerY, radius, scale,
            secondAngle, secondProgress, secondColor, secondTransparency, secondLength, secondThickness
        )

        // 4. Perno centrale per lancette classiche o a punta
        if (handStyle == 0 || handStyle == 1) {
            val pinColor = if (secondTransparency < 100) {
                applyTransparency(secondColor, secondTransparency)
            } else {
                applyTransparency(hourColor, hourTransparency)
            }
            if (Color.alpha(pinColor) > 0) {
                paint.style = Paint.Style.FILL
                paint.color = pinColor
                canvas.drawCircle(centerX, centerY, (3.2f * scale).coerceAtLeast(2f), paint)
            }
        }

        // 5. Numeri laterali: Ore a sinistra, Minuti a destra
        if (digitalEnabled && digitalTransparency < 100) {
            drawSideDigits(canvas, centerX, winTop, textSizePx, minutes)
        }
    }

    private fun drawSideDigits(
        canvas: Canvas,
        centerX: Float,
        winTop: Int,
        textSizePx: Float,
        minutes: Int
    ) {
        val finalTextColor = applyTransparency(digitalColor, digitalTransparency)
        if (Color.alpha(finalTextColor) == 0) return

        val digitalCenterY = (digitalY - winTop).toFloat()
        val shadowAlpha = ((100 - digitalTransparency) * 215) / 100

        textPaint.textSize = textSizePx
        textPaint.color = finalTextColor
        textPaint.setShadowLayer(
            (textSizePx * 0.14f).coerceAtLeast(2f),
            0f,
            (textSizePx * 0.08f).coerceAtLeast(1.5f),
            Color.argb(shadowAlpha, 0, 0, 0)
        )

        val fontMetrics = textPaint.fontMetrics
        val baselineY = digitalCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2f

        val hourValue = if (DateFormat.is24HourFormat(context)) {
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            val h12 = calendar.get(Calendar.HOUR)
            if (h12 == 0) 12 else h12
        }

        val hourStr = String.format(Locale.US, "%02d", hourValue)
        val minuteStr = String.format(Locale.US, "%02d", minutes)
        val edgeOffset = (clockSize / 2f) + digitalDistance

        // Ore a sinistra
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(hourStr, centerX - edgeOffset, baselineY, textPaint)

        // Minuti a destra
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(minuteStr, centerX + edgeOffset, baselineY, textPaint)
    }

    private fun drawDial(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        scale: Float
    ) {
        if (dialStyle == 4 || ticksTransparency >= 100) return

        val finalTickColor = applyTransparency(ticksColor, ticksTransparency)
        if (Color.alpha(finalTickColor) == 0) return

        paint.color = finalTickColor
        val baseStroke = (ticksThickness * scale * 0.9f).coerceAtLeast(1.2f)
        val baseLen = (radius * (ticksLength / 100f)).coerceAtLeast(3f)

        if (dialStyle == 3) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = baseStroke
            canvas.drawCircle(centerX, centerY, radius - baseStroke / 2f, paint)
            return
        }

        val totalTicks = when (dialStyle) {
            1 -> 4
            2 -> 60
            else -> 12
        }
        val stepDegrees = 360.0 / totalTicks

        for (i in 0 until totalTicks) {
            val isMajor = (dialStyle != 2) || (i % 5 == 0)
            val angle = Math.toRadians(i * stepDegrees - 90.0)
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            if (tickShape == 1) {
                paint.style = Paint.Style.FILL
                val dotRadius = if (isMajor) {
                    (ticksThickness * scale * 0.95f).coerceAtLeast(2f)
                } else {
                    (ticksThickness * scale * 0.50f).coerceAtLeast(1.2f)
                }
                val dist = radius - dotRadius
                canvas.drawCircle(centerX + dist * cosA, centerY + dist * sinA, dotRadius, paint)
            } else {
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = if (isMajor) baseStroke else (baseStroke * 0.55f).coerceAtLeast(1f)
                val len = if (isMajor) baseLen else (baseLen * 0.55f).coerceAtLeast(2f)
                canvas.drawLine(
                    centerX + (radius - len) * cosA,
                    centerY + (radius - len) * sinA,
                    centerX + radius * cosA,
                    centerY + radius * sinA,
                    paint
                )
            }
        }
    }

    private fun drawHandElement(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        scale: Float,
        angle: Double,
        progress: Float,
        baseColor: Int,
        transparency: Int,
        lengthPercent: Int,
        thickness: Int
    ) {
        if (transparency >= 100) return
        val finalColor = applyTransparency(baseColor, transparency)
        if (Color.alpha(finalColor) == 0) return

        paint.color = finalColor
        val cosA = cos(angle).toFloat()
        val sinA = sin(angle).toFloat()
        val len = radius * (lengthPercent / 100f)

        when (handStyle) {
            0 -> {
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = (thickness * scale * 1.05f).coerceAtLeast(1.3f)
                canvas.drawLine(
                    centerX,
                    centerY,
                    centerX + len * cosA,
                    centerY + len * sinA,
                    paint
                )
            }
            1 -> {
                paint.style = Paint.Style.FILL
                val perpX = -sinA
                val perpY = cosA
                val halfW = (thickness * scale * 0.85f).coerceAtLeast(1.6f)
                val shoulderDist = len * 0.22f
                val tailLen = (len * 0.16f).coerceAtMost(radius * 0.15f)

                handPath.reset()
                handPath.moveTo(centerX + len * cosA, centerY + len * sinA)
                handPath.lineTo(
                    centerX + shoulderDist * cosA + halfW * perpX,
                    centerY + shoulderDist * sinA + halfW * perpY
                )
                handPath.lineTo(centerX - tailLen * cosA, centerY - tailLen * sinA)
                handPath.lineTo(
                    centerX + shoulderDist * cosA - halfW * perpX,
                    centerY + shoulderDist * sinA - halfW * perpY
                )
                handPath.close()
                canvas.drawPath(handPath, paint)
            }
            2 -> {
                paint.style = Paint.Style.FILL
                val dotRadius = (thickness * scale * 1.15f).coerceAtLeast(2.2f)
                canvas.drawCircle(
                    centerX + len * cosA,
                    centerY + len * sinA,
                    dotRadius,
                    paint
                )
            }
            3 -> {
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                val strokeW = (thickness * scale * 1.05f).coerceAtLeast(1.5f)
                paint.strokeWidth = strokeW
                val arcR = len.coerceAtLeast(strokeW)
                arcRect.set(centerX - arcR, centerY - arcR, centerX + arcR, centerY + arcR)
                val sweep = (progress * 360f).coerceAtLeast(3f)
                canvas.drawArc(arcRect, -90f, sweep, false, paint)
            }
        }
    }

    private fun applyTransparency(color: Int, transparencyPercent: Int): Int {
        val clamped = transparencyPercent.coerceIn(0, 100)
        val alpha = ((100 - clamped) * 255) / 100
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
