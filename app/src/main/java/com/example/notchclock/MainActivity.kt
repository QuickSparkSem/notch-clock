package com.example.notchclock

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)

        val switchEnable = findViewById<SwitchCompat>(R.id.switchEnable)
        val switchHands = findViewById<SwitchCompat>(R.id.switchHands)
        val seekBarX = findViewById<SeekBar>(R.id.seekBarX)
        val seekBarY = findViewById<SeekBar>(R.id.seekBarY)
        val seekBarSize = findViewById<SeekBar>(R.id.seekBarSize)

        val btnHourColor = findViewById<Button>(R.id.btnHourColor)
        val btnMinuteColor = findViewById<Button>(R.id.btnMinuteColor)
        val btnSecondColor = findViewById<Button>(R.id.btnSecondColor)
        val btnTicksColor = findViewById<Button>(R.id.btnTicksColor)

        switchEnable.isChecked = isOverlayPermissionGranted()
        switchHands.isChecked = prefs.getBoolean("useHands", true)

        // Range X: -300px a +300px
        seekBarX.max = 600
        seekBarX.progress = prefs.getInt("offsetX", 0) + 300

        // Range Y: -300px a +500px (consente di salire nell'area notch)
        seekBarY.max = 800
        seekBarY.progress = prefs.getInt("offsetY", 0) + 300

        seekBarSize.max = 200
        seekBarSize.progress = prefs.getInt("size", 100)

        // Eventi Switch
        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkOverlayPermission()) {
                    startClockService()
                } else {
                    switchEnable.isChecked = false
                }
            } else {
                stopClockService()
            }
        }

        switchHands.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("useHands", isChecked).apply()
            notifyService()
        }

        // Eventi Seekbar
        seekBarX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt("offsetX", progress - 300).apply()
                notifyService()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        seekBarY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt("offsetY", progress - 300).apply()
                notifyService()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val realSize = if (progress < 20) 20 else progress
                prefs.edit().putInt("size", realSize).apply()
                notifyService()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Eventi Selezione Colori
        btnHourColor.setOnClickListener { showColorPicker("hourColor", "Seleziona Colore Ore") }
        btnMinuteColor.setOnClickListener { showColorPicker("minuteColor", "Seleziona Colore Minuti") }
        btnSecondColor.setOnClickListener { showColorPicker("secondColor", "Seleziona Colore Secondi") }
        btnTicksColor.setOnClickListener { showColorPicker("ticksColor", "Seleziona Colore Tacche") }
    }

    private fun showColorPicker(prefKey: String, title: String) {
        val colors = intArrayOf(
            Color.WHITE, Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GRAY,
            Color.BLACK, Color.parseColor("#FF9800")
        )
        val colorNames = arrayOf(
            "Bianco", "Rosso", "Verde", "Blu",
            "Giallo", "Ciano", "Magenta", "Grigio",
            "Nero", "Arancione"
        )

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(colorNames) { _, which ->
                prefs.edit().putInt(prefKey, colors[which]).apply()
                notifyService()
            }
            .show()
    }

    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return false
        }
        return true
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
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
        val intent = Intent(this, ClockService::class.java)
        stopService(intent)
    }

    private fun notifyService() {
        val intent = Intent("com.example.notchclock.UPDATE_SETTINGS")
        intent.setPackage(packageName) // Fondamentale per Android 13+ con RECEIVER_NOT_EXPORTED
        sendBroadcast(intent)
    }
}
