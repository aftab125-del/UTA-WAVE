package com.uta.wave.player

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager

/**
 * Manages CPU WakeLocks and Wi-Fi Locks to ensure seamless, uninterrupted
 * background audio playback when the device screen is off and in deep sleep (Doze Mode).
 *
 * Bridges the transition gap between tracks when ExoPlayer temporarily releases
 * its internal wake lock at Player.STATE_ENDED.
 */
class PlaybackWakeLockHelper(context: Context) {

    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var transitionWakeLock: PowerManager.WakeLock? = null
    private var continuousWakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    init {
        try {
            transitionWakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "utawave:PlaybackTransitionLock"
            )?.apply {
                setReferenceCounted(false)
            }

            continuousWakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "utawave:PlaybackContinuousLock"
            )?.apply {
                setReferenceCounted(false)
            }

            @Suppress("DEPRECATION")
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "utawave:PlaybackWifiLock"
            )?.apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to initialize PlaybackWakeLockHelper locks", e)
        }
    }

    /**
     * Acquires a CPU WakeLock specifically across track transitions.
     * Prevents the Linux kernel from suspending the CPU to RAM while resolving
     * stream URLs and preparing the next ExoPlayer track.
     *
     * @param timeoutMs Safety timeout to prevent indefinite battery drain (default 45s).
     */
    @Synchronized
    fun acquireTransitionLock(timeoutMs: Long = 45_000L) {
        try {
            if (transitionWakeLock?.isHeld != true) {
                transitionWakeLock?.acquire(timeoutMs)
                AppLogger.i("⚡ Transition WakeLock acquired (${timeoutMs}ms safety timeout)")
            }
            acquireWifiLock()
        } catch (e: Exception) {
            AppLogger.w("Failed to acquire transition WakeLock: ${e.message}")
        }
    }

    /**
     * Releases the transition WakeLock once the incoming track has successfully
     * entered Player.STATE_READY and is actively playing.
     */
    @Synchronized
    fun releaseTransitionLock() {
        try {
            if (transitionWakeLock?.isHeld == true) {
                transitionWakeLock?.release()
                AppLogger.i("⚡ Transition WakeLock released")
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to release transition WakeLock: ${e.message}")
        }
    }

    /**
     * Acquires continuous locks while media is active to prevent Doze network throttle.
     */
    @Synchronized
    fun acquireContinuousPlaybackLock() {
        try {
            if (continuousWakeLock?.isHeld != true) {
                // Continuous lock with 1 hour safety timeout renewed during playback
                continuousWakeLock?.acquire(60 * 60 * 1000L)
            }
            acquireWifiLock()
        } catch (e: Exception) {
            AppLogger.w("Failed to acquire continuous WakeLock: ${e.message}")
        }
    }

    @Synchronized
    fun releaseContinuousPlaybackLock() {
        try {
            if (continuousWakeLock?.isHeld == true) {
                continuousWakeLock?.release()
            }
            releaseWifiLock()
        } catch (e: Exception) {
            AppLogger.w("Failed to release continuous WakeLock: ${e.message}")
        }
    }

    @Synchronized
    private fun acquireWifiLock() {
        try {
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
                AppLogger.i("⚡ High-performance WifiLock acquired")
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to acquire WifiLock: ${e.message}")
        }
    }

    @Synchronized
    private fun releaseWifiLock() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                AppLogger.i("⚡ WifiLock released")
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to release WifiLock: ${e.message}")
        }
    }

    @Synchronized
    fun releaseAll() {
        releaseTransitionLock()
        releaseContinuousPlaybackLock()
        releaseWifiLock()
    }
}
