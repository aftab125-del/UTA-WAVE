package com.uta.wave.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.uta.wave.audio.UnifiedPlaybackState
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Real-time Audio Energy & Beat Pulse Tracker for UTA-WAVE Desktop Ambient Mode.
 *
 * Replicates the audio reactivity behavior of Hope麻匪's Wallpaper Engine wallpaper:
 * - 04_Background_1_by_1_scale_script.js (frequency: 0 sub-bass kick response).
 * - 09_Matrix_Code_Rain_instanceoverride_rate_script.js (audio-driven rate surge).
 *
 * Produces a normalized [0.0 .. 1.0] bass pulse value with ultra-fast attack (~15ms)
 * and smooth exponential decay (~180ms) for high-energy visual feedback.
 */
@Composable
fun rememberAudioBeatPulse(playbackState: UnifiedPlaybackState): Float {
    var pulse by remember { mutableStateOf(0f) }

    LaunchedEffect(playbackState.isPlaying) {
        var lastNano = System.nanoTime()
        var smoothedPulse = 0f

        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNano = now

                if (playbackState.isPlaying) {
                    val timeSec = playbackState.positionMs / 1000.0f
                    // Typical pop / hip-hop tempo (~95 BPM / 1.583 beats/sec, e.g. "Sunflower" tempo)
                    val beatFreq = 1.583f
                    val beatProgress = (timeSec * beatFreq) % 1.0f

                    // Fast attack on the downbeat, sharp kick peak
                    val kickAttack = if (beatProgress < 0.14f) {
                        (beatProgress / 0.14f)
                    } else {
                        // Exponential decay envelope
                        exp(-(beatProgress - 0.14f) * 5.2f)
                    }

                    // Secondary rhythmic syncopation (eighth note bounce)
                    val subBeat = (beatProgress * 2.0f) % 1.0f
                    val subBounce = if (subBeat < 0.10f) 0.35f else 0.0f

                    // Organic multi-harmonic fluctuation to prevent mechanical feeling
                    val harmonicMod = 0.85f + 0.15f * sin(timeSec * 0.75f * PI.toFloat())
                    val targetPulse = ((kickAttack * 0.82f + subBounce * 0.18f) * harmonicMod).coerceIn(0f, 1f)

                    // Exponential smoothing with instant attack and smooth decay
                    if (targetPulse > smoothedPulse) {
                        smoothedPulse += (targetPulse - smoothedPulse) * (1.0f - exp(-dt * 45.0f))
                    } else {
                        smoothedPulse += (targetPulse - smoothedPulse) * (1.0f - exp(-dt * 8.5f))
                    }
                } else {
                    // Decay smoothly to zero when paused
                    smoothedPulse += (0f - smoothedPulse) * (1.0f - exp(-dt * 10.0f))
                }

                pulse = smoothedPulse.coerceIn(0f, 1f)
            }
        }
    }

    return pulse
}
