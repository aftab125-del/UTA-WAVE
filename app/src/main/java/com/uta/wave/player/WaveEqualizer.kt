package com.uta.wave.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.uta.wave.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EqualizerState(
    val isEnabled: Boolean = false,
    val presetName: String = "FLAT",
    val bandGains: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), // in dB (-12f to +12f)
    val bassBoost: Int = 0, // 0 to 1000
    val virtualizer: Int = 0, // 0 to 1000
    val mode: Int = 0, // 0 = Simple (Bass/Mid/Treble), 1 = Advanced 10-Band
    val hardwareBandsCount: Int = 5
)

data class EqualizerPreset(
    val name: String,
    val displayName: String,
    val gains: List<Float>
)

object WaveEqualizer {
    val PRESETS = listOf(
        EqualizerPreset("FLAT", "Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqualizerPreset("BASS_BOOST", "Bass Boost", listOf(7f, 6f, 5f, 3f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqualizerPreset("BASS_REDUCER", "Bass Reducer", listOf(-7f, -6f, -5f, -3f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqualizerPreset("TREBLE_BOOST", "Treble Boost", listOf(0f, 0f, 0f, 0f, 0f, 0f, 2f, 4f, 6f, 7f)),
        EqualizerPreset("VOCAL_BOOST", "Vocal Boost", listOf(-2f, -1f, 0f, 2f, 4f, 5f, 4f, 2f, 0f, -1f)),
        EqualizerPreset("ROCK", "Rock", listOf(5f, 4f, 3f, 1f, -1f, 0f, 2f, 4f, 5f, 5f)),
        EqualizerPreset("POP", "Pop", listOf(2f, 3f, 4f, 2f, 1f, 1f, 2f, 3f, 3f, 2f)),
        EqualizerPreset("JAZZ", "Jazz", listOf(3f, 3f, 2f, 1f, 2f, 2f, 1f, 2f, 3f, 4f)),
        EqualizerPreset("ELECTRONIC", "Electronic", listOf(6f, 5f, 3f, 0f, -1f, 1f, 2f, 4f, 5f, 6f)),
        EqualizerPreset("HIP_HOP", "Hip-Hop", listOf(7f, 6f, 4f, 2f, 0f, 1f, 2f, 3f, 4f, 4f)),
        EqualizerPreset("CLASSICAL", "Classical", listOf(4f, 3f, 3f, 2f, 0f, 0f, 1f, 2f, 3f, 4f)),
        EqualizerPreset("ACOUSTIC", "Acoustic", listOf(3f, 2f, 1f, 2f, 3f, 3f, 3f, 4f, 4f, 3f))
    )

    val FREQUENCIES = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

    private val _state = MutableStateFlow(EqualizerState())
    val state = _state.asStateFlow()

    private var equalizerEffect: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null

    private var currentSessionId: Int = 0
    private var userPreferences: UserPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun initialize(context: Context) {
        val prefs = UserPreferences.getInstance(context)
        userPreferences = prefs

        val settings = prefs.settingsFlow.value
        val parsedGains = parseGains(settings.equalizerBands)

        _state.value = EqualizerState(
            isEnabled = settings.equalizerEnabled,
            presetName = settings.equalizerPreset,
            bandGains = parsedGains,
            bassBoost = settings.equalizerBassBoost,
            virtualizer = settings.equalizerVirtualizer,
            mode = settings.equalizerMode
        )

        // Observe preference changes to keep sync
        scope.launch {
            prefs.settingsFlow.collect { s ->
                val newGains = parseGains(s.equalizerBands)
                _state.update {
                    it.copy(
                        isEnabled = s.equalizerEnabled,
                        presetName = s.equalizerPreset,
                        bandGains = newGains,
                        bassBoost = s.equalizerBassBoost,
                        virtualizer = s.equalizerVirtualizer,
                        mode = s.equalizerMode
                    )
                }
                applyToHardware()
            }
        }
    }

    @Synchronized
    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        currentSessionId = audioSessionId

        releaseEffects()

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizerEffect = eq
            val bandsCount = eq.numberOfBands.toInt().coerceAtLeast(1)
            _state.update { it.copy(hardwareBandsCount = bandsCount) }
            AppLogger.i("Equalizer attached to audioSession $audioSessionId with $bandsCount hardware bands")
        } catch (e: Exception) {
            AppLogger.e("Failed to init hardware Equalizer on session $audioSessionId", e)
            equalizerEffect = null
        }

        try {
            val bb = BassBoost(0, audioSessionId)
            bassBoostEffect = bb
        } catch (e: Exception) {
            AppLogger.w("BassBoost not supported: ${e.message}")
            bassBoostEffect = null
        }

        try {
            val virt = Virtualizer(0, audioSessionId)
            virtualizerEffect = virt
        } catch (e: Exception) {
            AppLogger.w("Virtualizer not supported: ${e.message}")
            virtualizerEffect = null
        }

        applyToHardware()
    }

    private fun releaseEffects() {
        try {
            equalizerEffect?.release()
        } catch (ignored: Exception) {}
        equalizerEffect = null

        try {
            bassBoostEffect?.release()
        } catch (ignored: Exception) {}
        bassBoostEffect = null

        try {
            virtualizerEffect?.release()
        } catch (ignored: Exception) {}
        virtualizerEffect = null
    }

    fun release() {
        releaseEffects()
        currentSessionId = 0
    }

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(isEnabled = enabled) }
        userPreferences?.updateEqualizerEnabled(enabled)
        applyToHardware()
    }

    fun setPreset(presetName: String) {
        val preset = PRESETS.firstOrNull { it.name == presetName } ?: return
        val newGains = preset.gains
        _state.update { it.copy(presetName = presetName, bandGains = newGains) }
        userPreferences?.updateEqualizerPreset(presetName)
        userPreferences?.updateEqualizerBands(newGains.joinToString(",") { it.toString() })
        applyToHardware()
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0..9) return
        val currentGains = _state.value.bandGains.toMutableList()
        currentGains[bandIndex] = gainDb.coerceIn(-12f, 12f)
        _state.update { it.copy(presetName = "CUSTOM", bandGains = currentGains) }
        userPreferences?.updateEqualizerPreset("CUSTOM")
        userPreferences?.updateEqualizerBands(currentGains.joinToString(",") { it.toString() })
        applyToHardware()
    }

    fun setSimpleBands(bass: Float, mid: Float, treble: Float) {
        val b = bass.coerceIn(-12f, 12f)
        val m = mid.coerceIn(-12f, 12f)
        val t = treble.coerceIn(-12f, 12f)

        // Interpolate across 10 bands
        val newGains = listOf(
            b,
            b * 0.9f,
            b * 0.7f,
            m * 0.8f,
            m,
            m,
            m * 0.8f,
            t * 0.7f,
            t * 0.9f,
            t
        )
        _state.update { it.copy(presetName = "CUSTOM", bandGains = newGains) }
        userPreferences?.updateEqualizerPreset("CUSTOM")
        userPreferences?.updateEqualizerBands(newGains.joinToString(",") { it.toString() })
        applyToHardware()
    }

    fun setBassBoost(strength: Int) {
        val s = strength.coerceIn(0, 1000)
        _state.update { it.copy(bassBoost = s) }
        userPreferences?.updateEqualizerBassBoost(s)
        applyToHardware()
    }

    fun setVirtualizer(strength: Int) {
        val s = strength.coerceIn(0, 1000)
        _state.update { it.copy(virtualizer = s) }
        userPreferences?.updateEqualizerVirtualizer(s)
        applyToHardware()
    }

    fun setMode(mode: Int) {
        _state.update { it.copy(mode = mode) }
        userPreferences?.updateEqualizerMode(mode)
    }

    private fun applyToHardware() {
        val current = _state.value

        // 1. Equalizer effect
        equalizerEffect?.let { eq ->
            try {
                eq.enabled = current.isEnabled
                if (current.isEnabled) {
                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange // e.g. [-1500, 1500] in millibels
                    val minMb = range[0].toFloat()
                    val maxMb = range[1].toFloat()

                    // Map our 10-band model to available hardware bands
                    for (hBand in 0 until numBands) {
                        val tenBandIndex = ((hBand.toFloat() / (numBands.coerceAtLeast(2) - 1)) * 9f)
                            .toInt()
                            .coerceIn(0, 9)
                        val db = current.bandGains[tenBandIndex]
                        // Convert dB to millibels (-12dB = -1200mB, +12dB = +1200mB)
                        val mb = (db * 100f).coerceIn(minMb, maxMb).toInt().toShort()
                        eq.setBandLevel(hBand.toShort(), mb)
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("Error applying Equalizer hardware settings: ${e.message}")
            }
        }

        // 2. BassBoost effect
        bassBoostEffect?.let { bb ->
            try {
                bb.enabled = current.isEnabled && current.bassBoost > 0
                if (bb.strengthSupported) {
                    bb.setStrength(current.bassBoost.toShort())
                }
            } catch (e: Exception) {
                AppLogger.w("Error applying BassBoost: ${e.message}")
            }
        }

        // 3. Virtualizer effect
        virtualizerEffect?.let { virt ->
            try {
                virt.enabled = current.isEnabled && current.virtualizer > 0
                if (virt.strengthSupported) {
                    virt.setStrength(current.virtualizer.toShort())
                }
            } catch (e: Exception) {
                AppLogger.w("Error applying Virtualizer: ${e.message}")
            }
        }
    }

    private fun parseGains(csv: String): List<Float> {
        val parts = csv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        return if (parts.size >= 10) {
            parts.take(10)
        } else {
            List(10) { index -> parts.getOrElse(index) { 0f } }
        }
    }
}
