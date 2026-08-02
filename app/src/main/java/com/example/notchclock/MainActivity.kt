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

    // Elenco colori ordinato alfabeticamente
    private val colorList = listOf(
        "Arancione" to Color.rgb(255, 165, 0),
        "Bianco" to Color.WHITE,
        "Blu" to Color.BLUE,
        "Ciano" to Color.CYAN,
        "Giallo" to Color.YELLOW,
        "Grigio" to Color.GRAY,
        "Magenta" to Color.MAGENTA,
        "Nero" to Color.BLACK,
        "Rosa" to Color.rgb(255, 192, 203),
        "Rosso" to Color.RED,
        "Verde" to Color.GREEN,
        "Viola" to Color.rgb(128, 0, 128)
    ).sortedBy { it.first }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchService = findViewById(R.id.switchService)
        seekX = findViewById(R.id.seekX)
        seekY = findViewById(R.id.seekY)
        seekSize = findViewById(R.id.seekSize)

        val prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)

        // Switch ON / OFF
        switchService.isChecked = checkOverlayPermission()
        switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkOverlayPermission()) {
                    toggleClockService(true)
                } else {
                    switchService.isChecked = false
                    requestOverlayPermission()
                }
            } else {
                toggleClockService(false)
            }
        }

        // Listener SeekBar unico
        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
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

        seekX.setOnSeekBarChangeListener(seekBarListener)
        seekY.setOnSeekBarChangeListener(seekBarListener)
        seekSize.setOnSeekBarChangeListener(seekBarListener)

        // Listener Pulsanti Colore
        findViewById<Button>(R.id.btnColorHour).setOnClickListener { showColorPicker("colorHour") }
        findViewById<Button>(R.id.btnColorMinute).setOnClickListener { showColorPicker("colorMinute") }
        findViewById<Button>(R.id.btnColorSecond).setOnClickListener { showColorPicker("colorSecond") }
        findViewById<Button>(R.id.btnColorTicks).setOnClickListener { showColorPicker("colorTicks") }
    }

    private fun showColorPicker(key: String) {
        val names = colorList.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Scegli Colore")
            .setItems(names) { _, which ->
                getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)
                    .edit().putInt(key, colorList[which].second).apply()
                sendBroadcast(Intent("com.example.notchclock.UPDATE_SETTINGS"))
            }
            .show()
    }

    private fun checkOverlayPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true

    private fun requestOverlayPermission() {
        Toast.makeText(this, "Attiva il permesso 'Visualizzazione sopra altre app'", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun toggleClockService(start: Boolean) {
        val intent = Intent(this, ClockService::class.java)
        if (start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        } else {
            stopService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkOverlayPermission() && !switchService.isChecked) {
            switchService.isChecked = true
            toggleClockService(true)
        }
    }
}
