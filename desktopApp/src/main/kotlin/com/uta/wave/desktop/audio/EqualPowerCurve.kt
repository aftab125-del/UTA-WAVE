package com.uta.wave.desktop.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mathematical Equal-Power crossfade gain calculator.
 *
 * Direct parity with UTA-WAVE Mobile (EchoMusic standard).
 * Uses quarter-sine and quarter-cosine volume curves where:
 * cos²(θ) + sin²(θ) = 1
 *
 * Guarantees that the perceived acoustic sound power output
 * across the stereo field remains constant throughout the transition,
 * completely eliminating the audible mid-fade volume drop of linear crossfading.
 */
object EqualPowerCurve {
    data class Gains(val outgoingGain: Float, val incomingGain: Float)

    fun calculateGains(progress: Float): Gains {
        val safeProgress = if (progress.isFinite()) progress else 0.0f
        val clamped = safeProgress.coerceIn(0.0f, 1.0f)
        val angle = clamped * (PI.toFloat() / 2.0f)
        val outgoing = cos(angle)
        val incoming = sin(angle)
        return Gains(outgoingGain = outgoing, incomingGain = incoming)
    }
}
