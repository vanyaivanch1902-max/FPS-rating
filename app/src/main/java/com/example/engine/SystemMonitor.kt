package com.example.engine

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

/**
 * Monitors system memory (RAM) and battery temperature for gamer telemetry.
 */
class SystemMonitor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()

    @Volatile
    var batteryTempCelsius: Float = 32.0f
        private set

    private var receiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            intent?.let {
                val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                if (temp > 0) {
                    batteryTempCelsius = temp / 10.0f
                }
            }
        }
    }

    fun start() {
        if (!receiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = context.registerReceiver(batteryReceiver, filter)
            sticky?.let {
                val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                if (temp > 0) {
                    batteryTempCelsius = temp / 10.0f
                }
            }
            receiverRegistered = true
        }
    }

    fun stop() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    fun getMemoryStats(): Triple<Int, Int, Int> {
        // Returns Triple(usedPercent, usedMb, totalMb)
        activityManager?.getMemoryInfo(memoryInfo)
        val totalMb = (memoryInfo.totalMem / (1024 * 1024)).toInt()
        val availMb = (memoryInfo.availMem / (1024 * 1024)).toInt()
        val usedMb = (totalMb - availMb).coerceAtLeast(0)
        val percent = if (totalMb > 0) ((usedMb.toDouble() / totalMb) * 100).toInt() else 0
        return Triple(percent, usedMb, totalMb)
    }
}
