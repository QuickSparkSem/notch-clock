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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var switchEnable: SwitchCompat

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

        switchEnable = findViewById(R.id.switchEnable)
        val switchHands = findViewById<SwitchCompat>(R.id.switchHands)

        val tvX = findViewById<TextView>(R.id.tvValueX)
        val tvY = findViewById<TextView>(R.id.tvValueY)
        val tvSize = findViewById<TextView>(R.id.tvValueSize)

        val seekBarX = findViewById<SeekBar>(R.id.seekBarX)
        val seekBarY = findViewById<SeekBar>(R.id.seekBarY)
        val seekBarSize = findViewById<SeekBar>(R.id.seekBarSize)
        val btnReset = findViewById<Button>(R.id.btnReset)

        val btnHourColor = findViewById<Button>(R.id.btnHourColor)
        val btnMinuteColor = findViewById<Button>(R.id.btnMinuteColor)
        val btnSecondColor = findViewById<Button>(R.id.btnSecondColor)
        val btnTicksColor = findViewById<Button>(R.id.btnTicksColor)

        val initX = prefs.getInt("offsetX", 0)
        val initY = prefs.getInt("offsetY", 0)
        val initSize = prefs.getInt("size", 120)

        switchHands.isChecked = prefs.getBoolean("useHands", true)

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

        switchHands.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("useHands", isChecked).apply()
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

        btnReset.setOnClickListener {
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

        setupColorButton(btnHourColor, "hourColor", Color.WHITE, "Colore Ore")
        setupColorButton(btnMinuteColor, "minuteColor", Color.WHITE, "Colore Minuti")
        setupColorButton(btnSecondColor, "secondColor", Color.RED, "Colore Secondi")
        setupColorButton(btnTicksColor, "ticksColor", Color.GRAY, "Colore Tacche")
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
