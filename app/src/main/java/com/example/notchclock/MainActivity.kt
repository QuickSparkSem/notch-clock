package com.example.notchclock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        val prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)

        // Slider X
        val labelX = TextView(this).apply { text = "Posizione X (Orizzontale)" }
        val seekX = SeekBar(this).apply {
            max = 400
            progress = prefs.getInt("offset_x", 0) + 200
        }

        // Slider Y
        val labelY = TextView(this).apply { text = "Posizione Y (Verticale)" }
        val seekY = SeekBar(this).apply {
            max = 400
            progress = prefs.getInt("offset_y", 0) + 200
        }

        // Slider Dimensioni
        val labelSize = TextView(this).apply { text = "Dimensione Orologio" }
        val seekSize = SeekBar(this).apply {
            max = 200
            progress = prefs.getInt("clock_size", 80)
        }

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val x = seekX.progress - 200
                val y = seekY.progress - 200
                val size = seekSize.progress

                prefs.edit()
                    .putInt("offset_x", x)
                    .putInt("offset_y", y)
                    .putInt("clock_size", size)
                    .apply()

                val updateIntent = Intent(this@MainActivity, ClockService::class.java).apply {
                    putExtra("offset_x", x)
                    putExtra("offset_y", y)
                    putExtra("clock_size", size)
                }
                try {
                    startService(updateIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekX.setOnSeekBarChangeListener(listener)
        seekY.setOnSeekBarChangeListener(listener)
        seekSize.setOnSeekBarChangeListener(listener)

        layout.addView(labelX)
        layout.addView(seekX)
        layout.addView(labelY)
        layout.addView(seekY)
        layout.addView(labelSize)
        layout.addView(seekSize)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermissionAndStart()
    }

    private fun checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            startClockService()
        }
    }

    private fun startClockService() {
        val intent = Intent(this, ClockService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
