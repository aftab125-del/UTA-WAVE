package com.uta.wave.ui.splash

import kotlin.math.*

data class RippleWavePacket(
    val birthTimeMs: Long,
    val amplitude: Float,
    val wavelength: Float = 38f,
    val speed: Float = 540f // Propagation speed in pixels/second
)

data class RippleRingSpec(
    val radius: Float,
    val alpha: Float,
    val strokeWidth: Float,
    val chromaticShift: Float
)

class WaterPhysicsEngine {
    var impactTimeMs: Long = 0L
    private val wavePackets = mutableListOf<RippleWavePacket>()

    /**
     * Called at the exact moment the "U" strikes the still water surface.
     * Generates a primary shockwave followed by secondary and tertiary capillary rebounds.
     */
    fun triggerImpact(timeMs: Long) {
        impactTimeMs = timeMs
        wavePackets.clear()

        // 1. Primary shockwave: High amplitude, widest wavelength
        wavePackets.add(
            RippleWavePacket(
                birthTimeMs = timeMs,
                amplitude = 1.0f,
                wavelength = 42f,
                speed = 580f
            )
        )

        // 2. Secondary rebound ripple: Droplet bounce back effect (~140ms later)
        wavePackets.add(
            RippleWavePacket(
                birthTimeMs = timeMs + 140L,
                amplitude = 0.58f,
                wavelength = 34f,
                speed = 490f
            )
        )

        // 3. Tertiary trailing capillary resonance (~280ms later)
        wavePackets.add(
            RippleWavePacket(
                birthTimeMs = timeMs + 280L,
                amplitude = 0.32f,
                wavelength = 28f,
                speed = 430f
            )
        )
    }

    /**
     * Evaluates all active concentric wave rings for rendering at [currentTimeMs].
     * Applies:
     * - Circular wavefront expansion: radius = speed * deltaT
     * - Viscous damping: e^(-gamma * deltaT)
     * - Geometric energy spreading: 1 / sqrt(radius)
     * - Soft chromatic dispersion: chromaticShift = 3.5 * alpha
     */
    fun getActiveRings(currentTimeMs: Long): List<RippleRingSpec> {
        if (impactTimeMs == 0L || currentTimeMs < impactTimeMs) return emptyList()

        val rings = mutableListOf<RippleRingSpec>()

        for (packet in wavePackets) {
            val deltaT = (currentTimeMs - packet.birthTimeMs) / 1000f
            if (deltaT <= 0f) continue

            val radius = packet.speed * deltaT
            // Exponential fluid decay + geometric radial attenuation
            val attenuation = exp(-1.75f * deltaT) / sqrt(max(1f, radius / 40f))
            val alpha = (packet.amplitude * attenuation).coerceIn(0f, 1f)

            if (alpha > 0.008f) {
                // Wave crest stroke width slightly thins as circle expands
                val strokeWidth = (28f * exp(-0.75f * deltaT)).coerceAtLeast(3.5f)

                rings.add(
                    RippleRingSpec(
                        radius = radius,
                        alpha = alpha,
                        strokeWidth = strokeWidth,
                        chromaticShift = (3.5f * alpha).coerceAtLeast(0.5f)
                    )
                )
            }
        }

        return rings
    }
}
