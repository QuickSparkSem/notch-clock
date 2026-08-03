package com.example.notchclock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val switchEnable = findViewById<SwitchCompat>(R.id.switchEnable)
        val switchHands = findViewById<SwitchCompat>(R.id.switchHands)
        val seekBarX = findViewById<SeekBar>(R.id.seekBarX)
        val seekBarY = findViewById<SeekBar>(R.id.seekBarY)
        val seekBarSize = findViewById<SeekBar>(R.id.seekBarSize)

        val prefs = getSharedPreferences("NotchClockPrefs", Context.MODE_PRIVATE)

        switchEnable.isChecked = isOverlayPermissionGranted()
        switchHands.isChecked = prefs.getBoolean("useHands", true)

        // Range X: da -300px a +300px
        seekBarX.max = 600
        seekBarX.progress = prefs.getInt("offsetX", 0) + 300

        // Range Y: da -300px a +500px (valori negativi per salire sopra la barra di stato / notch)
        seekBarY.max = 800
        seekBarY.progress = prefs.getInt("offsetY", 0) + 300

        seekBarSize.max = 200
        seekBarSize.progress = prefs.getInt("size", 100)

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

        seekBarX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val realX = progress - 300
                prefs.edit().putInt("offsetX", realX).apply()
                notifyService()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        seekBarY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val realY = progress - 300 // Permette di spingere l'orologio fino in cima al notch
                prefs.edit().putInt("offsetY", realY).apply()
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
        sendBroadcast(Intent("com.example.notchclock.UPDATE_SETTINGS"))
    }
}
