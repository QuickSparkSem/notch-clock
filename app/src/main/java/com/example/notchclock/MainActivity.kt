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

class MainActivity : AppCompatActivity() {

    private val colors = arrayOf(
        Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
        Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GRAY, Color.parseColor("#FFA500")
    )
    private val colorNames = arrayOf("Bianco", "Nero", "Rosso", "Verde", "Blu", "Giallo", "Azzurro", "Fucsia", "Grigio", "Arancione")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissionsAndStart()

        val prefs = getSharedPreferences("ClockPrefs", Context.MODE_PRIVATE)

        setupSeekBar(R.id.seekX, "offset_x", prefs, -300, 300)
        setupSeekBar(R.id.seekY, "offset_y", prefs, -100, 500)
        setupSeekBar(R.id.seekRadius, "radius", prefs, 30, 300)

        findViewById<Button>(R.id.btnColorHour).setOnClickListener { showColorPicker("color_hour", prefs) }
        findViewById<Button>(R.id.btnColorMin).setOnClickListener { showColorPicker("color_minute", prefs) }
        findViewById<Button>(R.id.btnColorTicks).setOnClickListener { showColorPicker("color_ticks", prefs) }
    }

    private fun setupSeekBar(id: Int, key: String, prefs: android.content.SharedPreferences, min: Int, max: Int) {
        val seekBar = findViewById<SeekBar>(id)
        seekBar.max = max - min
        seekBar.progress = prefs.getInt(key, if (key == "radius") 100 else 0) - min

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt(key, progress + min).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showColorPicker(prefKey: String, prefs: android.content.SharedPreferences) {
        AlertDialog.Builder(this)
            .setTitle("Scegli Colore")
            .setItems(colorNames) { _, which ->
                prefs.edit().putInt(prefKey, colors[which]).apply()
            }
            .show()
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            val intent = Intent(this, ClockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
