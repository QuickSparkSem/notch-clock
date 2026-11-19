package com.example.notchclock

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.BatteryManager
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
    private val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val calendar = Calendar.getInstance()
    private val shapePath = Path()
    private val arcRect = RectF()

    // Interruttori principali
    private var analogEnabled = true
    private var leftTimeEnabled = true
    private var rightBatteryEnabled = true

    // Dimensione e posizione base
    private var clockSize = 120
    private var offsetY = 0

    // Stili orologio tondo
    private var dialStyle = 0
    private var handStyle = 1
    private var tickShape = 0

    // Colori orologio tondo
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

    // Elementi laterali (Orario impilato SX • Batteria DX)
    private var sideDistance = 12
    private var sideSize = 14
    private var sideY = 42
    private var sideTransparency = 0
    private var sideColor = Color.WHITE

    // Cache lettura batteria (aggiornata ogni 3 secondi)
    private var lastBatteryReadMs = 0L
    private var batteryPct = 100
    private var isCharging = false

    init {
        reloadPreferences()
    }

    fun reloadPreferences() {
        val prefs = context.getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
        val fallbackHandStyle = if (prefs.getBoolean("useHands", true)) 1 else 2

        analogEnabled = prefs.getBoolean("analogEnabled", true)
        leftTimeEnabled = prefs.getBoolean("leftTimeEnabled", true)
        rightBatteryEnabled = prefs.getBoolean("rightBatteryEnabled", true)

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

        sideDistance = prefs.getInt("sideDistance", 12)
        sideSize = prefs.getInt("sideSize", 14)
        sideY = prefs.getInt("sideY", 42)
        sideTransparency = prefs.getInt("sideTransparency", 0)
        sideColor = prefs.getInt("sideColor", Color.WHITE)

        lastBatteryReadMs = 0L
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val hasSideElements = leftTimeEnabled || rightBatteryEnabled
        if (!analogEnabled && !hasSideElements) return

        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sideSize.toFloat(),
            resources.displayMetrics
        )
        val sideHalfH = (textSizePx * 1.35f).toInt() + 8
        val winTop = when {
            analogEnabled && hasSideElements -> min(offsetY, sideY - sideHalfH)
            hasSideElements -> sideY - sideHalfH
            else -> offsetY
        }

        val centerX = width / 2f
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now

        // 1. Orologio tondo analogico (se attivato)
        if (analogEnabled) {
            val analogCenterY = (offsetY - winTop) + (clockSize / 2f)
            drawAnalogClock(canvas, centerX, analogCenterY)
        }

        // 2. Elementi laterali: Orario impilato SX • Batteria DX
        if (hasSideElements && sideTransparency < 100) {
            val sideCenterY = (sideY - winTop).toFloat()
            val edgeOffset = (clockSize / 2f) + sideDistance

            if (leftTimeEnabled) {
                drawStackedTimeLeft(canvas, centerX - edgeOffset, sideCenterY, textSizePx)
            }
            if (rightBatteryEnabled) {
                refreshBatteryIfNeeded(now)
                drawBatteryRight(canvas, centerX + edgeOffset, sideCenterY, textSizePx)
            }
        }
    }

    private fun drawAnalogClock(canvas: Canvas, centerX: Float, centerY: Float) {
        val minSide = clockSize.toFloat()
        val padding = (minSide * 0.06f).coerceAtLeast(3f)
        val radius = (minSide / 2f) - padding
        if (radius <= 0f) return

        val scale = radius / 100f
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        drawDial(canvas, centerX, centerY, radius, scale)

        val hourProgress = ((hours % 12) + minutes / 60f) / 12f
        val minuteProgress = (minutes + seconds / 60f) / 60f
        val secondProgress = seconds / 60f

        drawHandElement(
            canvas, centerX, centerY, radius, scale,
            Math.toRadians((hourProgress * 360f - 90f).toDouble()),
            hourProgress, hourColor, hourTransparency, hourLength, hourThickness
        )
        drawHandElement(
            canvas, centerX, centerY, radius, scale,
            Math.toRadians((minuteProgress * 360f - 90f).toDouble()),
            minuteProgress, minuteColor, minuteTransparency, minuteLength, minuteThickness
        )
        drawHandElement(
            canvas, centerX, centerY, radius, scale,
            Math.toRadians((secondProgress * 360f - 90f).toDouble()),
            secondProgress, secondColor, secondTransparency, secondLength, secondThickness
        )

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
    }

    private fun drawStackedTimeLeft(
        canvas: Canvas,
        rightEdgeX: Float,
        centerY: Float,
        textSizePx: Float
    ) {
        val textColor = applyTransparency(sideColor, sideTransparency)
        if (Color.alpha(textColor) == 0) return

        val shadowAlpha = ((100 - sideTransparency) * 215) / 100
        val shadowRadius = (textSizePx * 0.14f).coerceAtLeast(1.8f)
        val shadowDy = (textSizePx * 0.08f).coerceAtLeast(1.2f)
        val shadowColor = Color.argb(shadowAlpha, 0, 0, 0)

        sidePaint.style = Paint.Style.FILL
        sidePaint.textSize = textSizePx
        sidePaint.color = textColor
        sidePaint.textAlign = Paint.Align.CENTER
        sidePaint.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)

        val hourVal = if (DateFormat.is24HourFormat(context)) {
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            val h12 = calendar.get(Calendar.HOUR)
            if (h12 == 0) 12 else h12
        }
        val hourStr = String.format(Locale.US, "%02d", hourVal)
        val minStr = String.format(Locale.US, "%02d", calendar.get(Calendar.MINUTE))

        val colWidth = sidePaint.measureText("00")
        val colCenterX = rightEdgeX - (colWidth / 2f)
        val fm = sidePaint.fontMetrics
        val gap = textSizePx * 0.18f

        // Ore sopra
        val hourBaseline = centerY - gap - (fm.descent * 0.2f)
        canvas.drawText(hourStr, colCenterX, hourBaseline, sidePaint)

        // Minuti sotto
        val minBaseline = centerY + gap - (fm.ascent * 0.82f)
        canvas.drawText(minStr, colCenterX, minBaseline, sidePaint)

        // Barretta orizzontale al centro tra ore e minuti
        sidePaint.style = Paint.Style.STROKE
        sidePaint.strokeCap = Paint.Cap.ROUND
        sidePaint.strokeWidth = (textSizePx * 0.11f).coerceAtLeast(1.5f)
        val barHalfW = colWidth * 0.42f
        canvas.drawLine(colCenterX - barHalfW, centerY, colCenterX + barHalfW, centerY, sidePaint)
    }

    private fun drawBatteryRight(
        canvas: Canvas,
        leftEdgeX: Float,
        centerY: Float,
        textSizePx: Float
    ) {
        val textColor = applyTransparency(sideColor, sideTransparency)
        if (Color.alpha(textColor) == 0) return

        val shadowAlpha = ((100 - sideTransparency) * 215) / 100
        val shadowRadius = (textSizePx * 0.14f).coerceAtLeast(1.8f)
        val shadowDy = (textSizePx * 0.08f).coerceAtLeast(1.2f)
        val shadowColor = Color.argb(shadowAlpha, 0, 0, 0)

        // Numero batteria (1..100 senza simbolo %)
        sidePaint.style = Paint.Style.FILL
        sidePaint.textSize = textSizePx
        sidePaint.color = textColor
        sidePaint.textAlign = Paint.Align.LEFT
        sidePaint.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)

        val pctStr = batteryPct.toString()
        val fm = sidePaint.fontMetrics
        val baseline = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText(pctStr, leftEdgeX, baseline, sidePaint)

        // Fulmine accanto (giallo in carica, bianco fuori carica)
        val numWidth = sidePaint.measureText(pctStr)
        val boltH = textSizePx * 0.85f
        val boltW = boltH * 0.56f
        val boltCx = leftEdgeX + numWidth + (boltW * 0.72f)
        val hw = boltW / 2f
        val hh = boltH / 2f

        val boltBaseColor = if (isCharging) Color.parseColor("#FFD600") else Color.WHITE
        sidePaint.color = applyTransparency(boltBaseColor, sideTransparency)

        shapePath.reset()
        shapePath.moveTo(boltCx + hw * 0.15f, centerY - hh)
        shapePath.lineTo(boltCx - hw, centerY + hh * 0.08f)
        shapePath.lineTo(boltCx - hw * 0.08f, centerY + hh * 0.08f)
        shapePath.lineTo(boltCx - hw * 0.25f, centerY + hh)
        shapePath.lineTo(boltCx + hw, centerY - hh * 0.08f)
        shapePath.lineTo(boltCx + hw * 0.08f, centerY - hh * 0.08f)
        shapePath.close()
        canvas.drawPath(shapePath, sidePaint)
    }

    private fun refreshBatteryIfNeeded(nowMs: Long) {
        if (nowMs - lastBatteryReadMs < 3000L) return
        lastBatteryReadMs = nowMs

        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) {
            batteryPct = ((level * 100) / scale).coerceIn(1, 100)
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            (status == BatteryManager.BATTERY_STATUS_FULL && plugged != 0)
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
                canvas.drawLine(centerX, centerY, centerX + len * cosA, centerY + len * sinA, paint)
            }
            1 -> {
                paint.style = Paint.Style.FILL
                val perpX = -sinA
                val perpY = cosA
                val halfW = (thickness * scale * 0.85f).coerceAtLeast(1.6f)
                val shoulderDist = len * 0.22f
                val tailLen = (len * 0.16f).coerceAtMost(radius * 0.15f)

                shapePath.reset()
                shapePath.moveTo(centerX + len * cosA, centerY + len * sinA)
                shapePath.lineTo(
                    centerX + shoulderDist * cosA + halfW * perpX,
                    centerY + shoulderDist * sinA + halfW * perpY
                )
                shapePath.lineTo(centerX - tailLen * cosA, centerY - tailLen * sinA)
                shapePath.lineTo(
                    centerX + shoulderDist * cosA - halfW * perpX,
                    centerY + shoulderDist * sinA - halfW * perpY
                )
                shapePath.close()
                canvas.drawPath(shapePath, paint)
            }
            2 -> {
                paint.style = Paint.Style.FILL
                val dotRadius = (thickness * scale * 1.15f).coerceAtLeast(2.2f)
                canvas.drawCircle(centerX + len * cosA, centerY + len * sinA, dotRadius, paint)
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
