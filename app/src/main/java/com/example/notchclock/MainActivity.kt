package com.example.notchclock

import android.content.Context
import android.content.Intent
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

        val prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)

        // Imposta stato iniziale dello switch
        switchService.isChecked = checkOverlayPermission()

        // Listener dello Switch ON/OFF
        switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkOverlayPermission()) {
                    startNotchService()
                } else {
                    switchService.isChecked = false
                    requestOverlayPermission()
                }
            } else {
                stopNotchService()
            }
        }

        // Listener per la calibrazione in tempo reale
        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().apply {
                    putInt("offsetX", seekX.progress - 100)
                    putInt("offsetY", seekY.progress - 100)
                    putInt("size", seekSize.progress)
                    apply()
                }
                
                // Invia broadcast per aggiornare l'overlay al volo
                val intent = Intent("com.example.notchclock.UPDATE_SETTINGS")
                sendBroadcast(intent)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekX.setOnSeekBarChangeListener(seekBarChangeListener)
        seekY.setOnSeekBarChangeListener(seekBarChangeListener)
        seekSize.setOnSeekBarChangeListener(seekBarChangeListener)
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, "Attiva l'autorizzazione 'Visualizzazione sopra altre app'", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startNotchService() {
        val intent = Intent(this, NotchService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopNotchService() {
        val intent = Intent(this, NotchService::class.java)
        stopService(intent)
    }

    override fun onResume() {
        super.onResume()
        // Se l'utente torna dalle impostazioni dopo aver dato il permesso, accendi lo switch
        if (checkOverlayPermission() && !switchService.isChecked) {
            switchService.isChecked = true
            startNotchService()
        }
    }
}
