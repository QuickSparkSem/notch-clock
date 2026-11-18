package com.example.notchclock

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var switchEnable: SwitchCompat

    private val dialStyles = arrayOf(
        "Classico (12 tacche)",
        "Minimal (4 punti: 12-3-6-9)",
        "Completo (60 tacche + 12 ore)",
        "Anello continuo",
        "Solo lancette (nessun quadrante)"
    )

    private val handStyles = arrayOf(
        "Classiche (lineari)",
        "A punta (forma lancetta)",
        "Tonde (puntini orbitanti)",
        "Archi concentrici"
    )

    private val tickShapes = arrayOf(
        "Lineari",
        "Tonde"
    )

    private val colorPalette = listOf(
        "Bianco" to Color.WHITE,
        "Rosso" to Color.parseColor("#FF5252"),
        "Verde" to Color.parseColor("#69F0AE"),
        "Blu" to Color.parseColor("#448AFF"),
        "Giallo" to Color.parseColor("#FFD740"),
        "Ciano" to Color.parseColor("#18FFFF"),
        "Magenta" to Color.parseColor("#FF4081"),
        "Arancione" to Color.parseColor("#FF9800"),
        "Grigio" to Color.parseColor("#9E9E9E"),
        "Nero" to Color.BLACK
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)

        listOf(
            R.id.cardStatus,
            R.id.cardPosition,
            R.id.cardDigital,
            R.id.cardHours,
            R.id.cardMinutes,
            R.id.cardSeconds,
            R.id.cardTicks
        ).forEach { styleCard(findViewById(it)) }

        switchEnable = findViewById(R.id.switchEnable)
        val btnDialStyle = findViewById<Button>(R.id.btnDialStyle)
        val btnHandStyle = findViewById<Button>(R.id.btnHandStyle)
        val btnTickShape = findViewById<Button>(R.id.btnTickShape)

        val tvX = findViewById<TextView>(R.id.tvValueX)
        val tvY = findViewById<TextView>(R.id.tvValueY)
        val tvSize = findViewById<TextView>(R.id.tvValueSize)
        val seekBarX = findViewById<SeekBar>(R.id.seekBarX)
        val seekBarY = findViewById<SeekBar>(R.id.seekBarY)
        val seekBarSize = findViewById<SeekBar>(R.id.seekBarSize)
        val btnResetPosition = findViewById<Button>(R.id.btnResetPosition)

        val seekTicksLength = findViewById<SeekBar>(R.id.seekTicksLength)
        val tvTicksLength = findViewById<TextView>(R.id.tvTicksLength)

        fun refreshStyleButtons() {
            val dIdx = prefs.getInt("dialStyle", 0).coerceIn(0, dialStyles.lastIndex)
            val hIdx = prefs.getInt("handStyle", 1).coerceIn(0, handStyles.lastIndex)
            val tIdx = prefs.getInt("tickShape", 0).coerceIn(0, tickShapes.lastIndex)
            btnDialStyle.text = "Quadrante: ${dialStyles[dIdx]}"
            btnHandStyle.text = "Lancette: ${handStyles[hIdx]}"
            btnTickShape.text = "Forma tacche: ${tickShapes[tIdx]}"
        }

        refreshStyleButtons()

        btnDialStyle.setOnClickListener {
            val current = prefs.getInt("dialStyle", 0).coerceIn(0, dialStyles.lastIndex)
            AlertDialog.Builder(this)
                .setTitle("Scegli stile quadrante")
                .setSingleChoiceItems(dialStyles, current) { dialog, which ->
                    prefs.edit().putInt("dialStyle", which).apply()
                    refreshStyleButtons()
                    dialog.dismiss()
                }
                .show()
        }

        btnHandStyle.setOnClickListener {
            val current = prefs.getInt("handStyle", 1).coerceIn(0, handStyles.lastIndex)
            AlertDialog.Builder(this)
                .setTitle("Scegli forma lancette")
                .setSingleChoiceItems(handStyles, current) { dialog, which ->
                    val editor = prefs.edit().putInt("handStyle", which)
                    if (which == 2) {
                        editor.putInt("tickShape", 0)
                        val curTickLen = prefs.getInt("ticksLength", 18)
                        if (curTickLen < 25) {
                            editor.putInt("ticksLength", 26)
                            seekTicksLength.progress = 26 - 5
                            tvTicksLength.text = "26%"
                        }
                        Toast.makeText(
                            this,
                            "Tacche impostate su lineari lunghe per non confonderle con le lancette tonde",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    editor.apply()
                    refreshStyleButtons()
                    dialog.dismiss()
                }
                .show()
        }

        btnTickShape.setOnClickListener {
            val current = prefs.getInt("tickShape", 0).coerceIn(0, tickShapes.lastIndex)
            AlertDialog.Builder(this)
                .setTitle("Scegli forma tacche")
                .setSingleChoiceItems(tickShapes, current) { dialog, which ->
                    val editor = prefs.edit().putInt("tickShape", which)
                    if (which == 1 && prefs.getInt("handStyle", 1) == 2) {
                        editor.putInt("handStyle", 1)
                        Toast.makeText(
                            this,
                            "Lancette impostate 'A punta' per distinguerle dalle tacche tonde",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    editor.apply()
                    refreshStyleButtons()
                    dialog.dismiss()
                }
                .show()
        }

        // Posizione e dimensione orologio analogico
        val initX = prefs.getInt("offsetX", 0)
        val initY = prefs.getInt("offsetY", 0)
        val initSize = prefs.getInt("size", 120)

        seekBarX.max = 600
        seekBarX.progress = initX + 300
        tvX.text = "${initX} px"

        seekBarY.max = 800
        seekBarY.progress = initY + 300
        tvY.text = "${initY} px"

        seekBarSize.max = 260
        seekBarSize.progress = (initSize - 40).coerceIn(0, 260)
        tvSize.text = "${initSize} px"

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.edit().putBoolean("isEnabled", true).apply()
                if (hasOverlayPermission()) {
                    startClockService()
                } else {
                    requestOverlayPermission()
                }
            } else {
                prefs.edit().putBoolean("isEnabled", false).apply()
                stopClockService()
            }
        }

        seekBarX.onUserChange { progress ->
            val offsetX = progress - 300
            tvX.text = "${offsetX} px"
            prefs.edit().putInt("offsetX", offsetX).apply()
        }

        seekBarY.onUserChange { progress ->
            val offsetY = progress - 300
            tvY.text = "${offsetY} px"
            prefs.edit().putInt("offsetY", offsetY).apply()
        }

        seekBarSize.onUserChange { progress ->
            val size = progress + 40
            tvSize.text = "${size} px"
            prefs.edit().putInt("size", size).apply()
        }

        btnResetPosition.setOnClickListener {
            seekBarX.progress = 300
            seekBarY.progress = 300
            seekBarSize.progress = 80
            tvX.text = "0 px"
            tvY.text = "0 px"
            tvSize.text = "120 px"
            prefs.edit()
                .putInt("offsetX", 0)
                .putInt("offsetY", 0)
                .putInt("size", 120)
                .apply()
        }

        // Controlli NUMERI LATERALI (Ore SX, Minuti DX)
        val switchDigital = findViewById<SwitchCompat>(R.id.switchDigital)
        switchDigital.isChecked = prefs.getBoolean("digitalEnabled", true)
        switchDigital.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("digitalEnabled", isChecked).apply()
        }

        setupColorButton(findViewById(R.id.btnDigitalColor), "digitalColor", Color.WHITE, "Colore caratteri")
        bindSlider("digitalDistance", 14, -20, 200, " px", findViewById(R.id.seekDigitalDistance), findViewById(R.id.tvDigitalDistance))
        bindSlider("digitalY", 45, 0, 500, " px", findViewById(R.id.seekDigitalY), findViewById(R.id.tvDigitalY))
        bindSlider("digitalSize", 22, 10, 72, "", findViewById(R.id.seekDigitalSize), findViewById(R.id.tvDigitalSize))
        bindSlider("digitalTransparency", 0, 0, 100, "%", findViewById(R.id.seekDigitalAlpha), findViewById(R.id.tvDigitalAlpha))

        // Controlli ORE
        setupColorButton(findViewById(R.id.btnHourColor), "hourColor", Color.WHITE, "Colore Ore")
        bindSlider("hourLength", 50, 20, 95, "%", findViewById(R.id.seekHourLength), findViewById(R.id.tvHourLength))
        bindSlider("hourThickness", 8, 1, 20, "", findViewById(R.id.seekHourThickness), findViewById(R.id.tvHourThickness))
        bindSlider("hourTransparency", 0, 0, 100, "%", findViewById(R.id.seekHourAlpha), findViewById(R.id.tvHourAlpha))

        // Controlli MINUTI
        setupColorButton(findViewById(R.id.btnMinuteColor), "minuteColor", Color.WHITE, "Colore Minuti")
        bindSlider("minuteLength", 75, 20, 95, "%", findViewById(R.id.seekMinuteLength), findViewById(R.id.tvMinuteLength))
        bindSlider("minuteThickness", 5, 1, 20, "", findViewById(R.id.seekMinuteThickness), findViewById(R.id.tvMinuteThickness))
        bindSlider("minuteTransparency", 0, 0, 100, "%", findViewById(R.id.seekMinuteAlpha), findViewById(R.id.tvMinuteAlpha))

        // Controlli SECONDI
        setupColorButton(findViewById(R.id.btnSecondColor), "secondColor", Color.parseColor("#FF5252"), "Colore Secondi")
        bindSlider("secondLength", 88, 20, 100, "%", findViewById(R.id.seekSecondLength), findViewById(R.id.tvSecondLength))
        bindSlider("secondThickness", 3, 1, 20, "", findViewById(R.id.seekSecondThickness), findViewById(R.id.tvSecondThickness))
        bindSlider("secondTransparency", 0, 0, 100, "%", findViewById(R.id.seekSecondAlpha), findViewById(R.id.tvSecondAlpha))

        // Controlli TACCHE
        setupColorButton(findViewById(R.id.btnTicksColor), "ticksColor", Color.parseColor("#9E9E9E"), "Colore Tacche")
        bindSlider("ticksLength", 18, 5, 45, "%", seekTicksLength, tvTicksLength)
        bindSlider("ticksThickness", 3, 1, 15, "", findViewById(R.id.seekTicksThickness), findViewById(R.id.tvTicksThickness))
        bindSlider("ticksTransparency", 0, 0, 100, "%", findViewById(R.id.seekTicksAlpha), findViewById(R.id.tvTicksAlpha))

        // Ripristina tutti gli stili predefiniti
        findViewById<Button>(R.id.btnResetAll).setOnClickListener {
            prefs.edit()
                .putInt("dialStyle", 0)
                .putInt("handStyle", 1)
                .putInt("tickShape", 0)
                .putBoolean("digitalEnabled", true)
                .putInt("digitalColor", Color.WHITE)
                .putInt("digitalDistance", 14)
                .putInt("digitalY", 45)
                .putInt("digitalSize", 22)
                .putInt("digitalTransparency", 0)
                .putInt("hourColor", Color.WHITE)
                .putInt("minuteColor", Color.WHITE)
                .putInt("secondColor", Color.parseColor("#FF5252"))
                .putInt("ticksColor", Color.parseColor("#9E9E9E"))
                .putInt("hourLength", 50)
                .putInt("hourThickness", 8)
                .putInt("hourTransparency", 0)
                .putInt("minuteLength", 75)
                .putInt("minuteThickness", 5)
                .putInt("minuteTransparency", 0)
                .putInt("secondLength", 88)
                .putInt("secondThickness", 3)
                .putInt("secondTransparency", 0)
                .putInt("ticksLength", 18)
                .putInt("ticksThickness", 3)
                .putInt("ticksTransparency", 0)
                .apply()
            recreate()
        }
    }

    override fun onResume() {
        super.onResume()
        val wantEnabled = prefs.getBoolean("isEnabled", false)
        if (wantEnabled && hasOverlayPermission()) {
            switchEnable.isChecked = true
            startClockService()
        } else {
            if (wantEnabled && !hasOverlayPermission()) {
                prefs.edit().putBoolean("isEnabled", false).apply()
            }
            switchEnable.isChecked = false
        }
    }

    private fun bindSlider(
        prefKey: String,
        defaultValue: Int,
        minValue: Int,
        maxValue: Int,
        suffix: String,
        seekBar: SeekBar,
        valueText: TextView
    ) {
        val current = prefs.getInt(prefKey, defaultValue).coerceIn(minValue, maxValue)
        seekBar.max = maxValue - minValue
        seekBar.progress = current - minValue
        valueText.text = "$current$suffix"

        seekBar.onUserChange { progress ->
            val value = progress + minValue
            valueText.text = "$value$suffix"
            prefs.edit().putInt(prefKey, value).apply()
        }
    }

    private fun styleCard(view: View) {
        val density = resources.displayMetrics.density
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f * density
            setColor(Color.parseColor("#1E1E1E"))
            setStroke((1f * density).toInt().coerceAtLeast(1), Color.parseColor("#2C2C2C"))
        }
    }

    private fun setupColorButton(button: Button, prefKey: String, defaultColor: Int, title: String) {
        updateButtonSwatch(button, prefs.getInt(prefKey, defaultColor))
        button.setOnClickListener {
            showColorPicker(title) { selectedColor ->
                prefs.edit().putInt(prefKey, selectedColor).apply()
                updateButtonSwatch(button, selectedColor)
            }
        }
    }

    private fun updateButtonSwatch(button: Button, color: Int) {
        val sizePx = (20 * resources.displayMetrics.density).toInt()
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(sizePx, sizePx)
            setColor(color)
            setStroke((1.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#66FFFFFF"))
        }
        button.setCompoundDrawablesWithIntrinsicBounds(null, null, dot, null)
    }

    private fun showColorPicker(title: String, onSelected: (Int) -> Unit) {
        val adapter = object : ArrayAdapter<Pair<String, Int>>(
            this,
            android.R.layout.select_dialog_item,
            colorPalette
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val (name, color) = colorPalette[position]
                view.text = name
                view.textSize = 16f
                val sizePx = (22 * resources.displayMetrics.density).toInt()
                val dot = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setSize(sizePx, sizePx)
                    setColor(color)
                    setStroke(2, Color.GRAY)
                }
                view.compoundDrawablePadding = (14 * resources.displayMetrics.density).toInt()
                view.setCompoundDrawablesWithIntrinsicBounds(dot, null, null, null)
                return view
            }
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setAdapter(adapter) { _, which ->
                onSelected(colorPalette[which].second)
            }
            .show()
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun startClockService() {
        val intent = Intent(this, ClockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopClockService() {
        stopService(Intent(this, ClockService::class.java))
    }

    private inline fun SeekBar.onUserChange(crossinline action: (Int) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) action(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
}
