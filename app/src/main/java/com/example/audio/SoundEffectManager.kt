package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundEffectManager(
    var isSoundEnabled: Boolean = true
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    fun playClick() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(freq = 600f, durationMs = 30, maxVol = 0.3f)
        }
    }

    fun playPickup() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(freq = 520f, durationMs = 40, maxVol = 0.35f)
        }
    }

    fun playPlace(blockCount: Int = 4) {
        if (!isSoundEnabled) return
        scope.launch {
            playBlockPlacementAudio(blockCount)
        }
    }

    fun playInvalid() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(freq = 180f, durationMs = 80, maxVol = 0.4f)
        }
    }

    fun playLineClear(linesCount: Int) {
        if (!isSoundEnabled) return
        scope.launch {
            // Pentatonic arpeggio based on lines cleared
            val baseFreqs = listOf(523.25f, 659.25f, 783.99f, 1046.50f, 1318.51f) // C5, E5, G5, C6, E6
            val notesToPlay = minOf(linesCount + 2, baseFreqs.size)
            for (i in 0 until notesToPlay) {
                playTone(baseFreqs[i], durationMs = 70, maxVol = 0.5f)
            }
        }
    }

    fun playCombo(combo: Int) {
        if (!isSoundEnabled) return
        scope.launch {
            val comboFreq = 440f * (1f + (combo * 0.15f))
            playChirp(comboFreq, comboFreq * 1.5f, durationMs = 120, maxVol = 0.6f)
        }
    }

    fun playHighScore() {
        if (!isSoundEnabled) return
        scope.launch {
            val fanfare = listOf(523f, 659f, 783f, 1046f, 1318f, 1567f)
            for (f in fanfare) {
                playTone(f, durationMs = 90, maxVol = 0.55f)
            }
        }
    }

    fun playGameOver() {
        if (!isSoundEnabled) return
        scope.launch {
            playChirp(startFreq = 440f, endFreq = 160f, durationMs = 250, maxVol = 0.45f)
        }
    }

    fun playReward() {
        if (!isSoundEnabled) return
        scope.launch {
            playTone(880f, durationMs = 60, maxVol = 0.4f)
            playTone(1174f, durationMs = 90, maxVol = 0.5f)
        }
    }

    private fun playTone(freq: Float, durationMs: Int, maxVol: Float) {
        try {
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * PI * freq * t
                // Attack and decay envelope
                val envelope = when {
                    i < numSamples * 0.15 -> i / (numSamples * 0.15)
                    else -> (numSamples - i).toDouble() / (numSamples * 0.85)
                }
                val sample = (sin(angle) * envelope * maxVol * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 20)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // Audio output fallback
        }
    }

    private fun playChirp(startFreq: Float, endFreq: Float, durationMs: Int, maxVol: Float) {
        try {
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * PI * currentFreq * t

                val envelope = when {
                    i < numSamples * 0.1 -> i / (numSamples * 0.1)
                    else -> (numSamples - i).toDouble() / (numSamples * 0.9)
                }
                val sample = (sin(angle) * envelope * maxVol * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 20)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // Fallback
        }
    }

    private fun playBlockPlacementAudio(blockCount: Int) {
        try {
            val durationMs = 75
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            // Dynamic base frequency: larger shapes sound slightly deeper and weightier
            val baseFreq = when {
                blockCount <= 2 -> 300f
                blockCount in 3..4 -> 260f
                else -> 220f
            }

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                // Pitch glides down swiftly on contact, giving weight and physical impact
                val pitchMultiplier = 1.35 - (0.50 * kotlin.math.sqrt(progress))
                val currentFreq = baseFreq * pitchMultiplier
                val t = i.toDouble() / sampleRate

                val fundamentalAngle = 2.0 * PI * currentFreq * t
                val secondHarmonicAngle = 2.0 * PI * (currentFreq * 2.02) * t
                val thirdHarmonicAngle = 2.0 * PI * (currentFreq * 3.01) * t

                // Fast attack in first 3ms, then exponential resonance decay
                val attackSamples = (sampleRate * 0.003).toInt().coerceAtLeast(1)
                val envelope = if (i < attackSamples) {
                    i.toDouble() / attackSamples
                } else {
                    val decayT = (i - attackSamples).toDouble() / (numSamples - attackSamples)
                    kotlin.math.max(0.0, 1.0 - decayT).let { it * it * it }
                }

                // Initial 6ms transient click for tactile physical snap
                val clickTransient = if (i < (sampleRate * 0.006).toInt()) {
                    0.25 * sin(2.0 * PI * 1800.0 * t)
                } else {
                    0.0
                }

                val rawWave = (sin(fundamentalAngle) * 0.70 +
                        sin(secondHarmonicAngle) * 0.22 +
                        sin(thirdHarmonicAngle) * 0.08 +
                        clickTransient)

                val sample = (rawWave * envelope * 0.65 * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 15)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // Audio output fallback
        }
    }
}
