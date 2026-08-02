package com.example.notchclock

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var switchService: SwitchMaterial
    private lateinit var seekX: SeekBar
    private lateinit var seekY: SeekBar
    private lateinit var seekSize: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchService = findViewById(R.id.switchService)
        seekX = findViewById(R.id.seekX)
        seekY = findViewById(R.id.seekY)
        seekSize = findViewById(R.id.seekSize)

        val btnColorHour: Button = findViewById(R.id.btnColorHour)
        val btnColorMinute: Button = findViewById(R.id.btnColorMinute)
        val btnColorSecond: Button = findViewById(R.id.btnColorSecond)
        val btnColorTicks: Button = findViewById(R.id.btnColorTicks)

        val prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)

        switchService.isChecked = checkOverlayPermission()

        switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkOverlayPermission()) {
                    startClockService()
                } else {
                    switchService.isChecked = false
                    requestOverlayPermission()
                }
            } else {
                stopClockService()
            }
        }

        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().apply {
                    putInt("offsetX", seekX.progress - 100)
                    putInt("offsetY", seekY.progress - 100)
                    putInt("size", seekSize.progress)
                    apply()
                }
                sendBroadcast(Intent("com.example.notchclock.UPDATE_SETTINGS"))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekX.setOnSeekBarChangeListener(seekBarChangeListener)
        seekY.setOnSeekBarChangeListener(seekBarChangeListener)
        seekSize.setOnSeekBarChangeListener(seekBarChangeListener)

        btnColorHour.setOnClickListener { showColorPicker("colorHour", Color.RED) }
        btnColorMinute.setOnClickListener { showColorPicker("colorMinute", Color.GREEN) }
        btnColorSecond.setOnClickListener { showColorPicker("colorSecond", Color.CYAN) }
        btnColorTicks.setOnClickListener { showColorPicker("colorTicks", Color.WHITE) }
    }

    private fun showColorPicker(key: String, defaultColor: Int) {
        val colors = arrayOf("Rosso", "Verde", "Blu", "Ciano", "Giallo", "Magenta", "Bianco")
        val colorValues = intArrayOf(
            Color.RED, Color.GREEN, Color.BLUE,
            Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.WHITE
        )

        AlertDialog.Builder(this)
            .setTitle("Scegli Colore")
            .setItems(colors) { _, which ->
                getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
                    .edit().putInt(key, colorValues[which]).apply()
                sendBroadcast(Intent("com.example.notchclock.UPDATE_SETTINGS"))
            }
            .show()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, "Attiva il permesso 'Visualizzazione sopra altre app'", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
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

    override fun onResume() {
        super.onResume()
        if (checkOverlayPermission() && !switchService.isChecked) {
            switchService.isChecked = true
            startClockService()
        }
    }
}
