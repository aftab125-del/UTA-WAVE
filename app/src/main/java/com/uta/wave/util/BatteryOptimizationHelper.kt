package com.uta.wave.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.uta.wave.player.AppLogger

/**
 * Utility to check and request exemption from Android Doze Mode / Battery Optimization.
 *
 * Essential for music applications: without exemption, Android restricts network access
 * and ignores wake locks when the screen is turned off for >10-15 minutes (Deep Doze).
 */
object BatteryOptimizationHelper {

    /**
     * Checks if UTA-wave is currently whitelisted / exempt from battery optimization.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    /**
     * Prompts the user with the official system dialog to ignore battery optimizations for UTA-wave.
     * Falls back to general battery optimization settings if direct intent cannot be handled.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            AppLogger.i("Prompted user for ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
        } catch (e: Exception) {
            AppLogger.w("Direct battery optimization request failed, falling back to settings list: ${e.message}")
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                AppLogger.e("Failed to open battery optimization settings", e2)
            }
        }
    }
}
