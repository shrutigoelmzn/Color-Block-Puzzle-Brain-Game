package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackManager(
    context: Context,
    var isHapticsEnabled: Boolean = true
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun vibratePickup() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    fun vibratePlace() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25)
        }
    }

    fun vibrateInvalid() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }

    fun vibrateLineClear() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(55)
        }
    }

    /**
     * Tactile punch for simultaneous multi-line clears (double, triple, quad+).
     */
    fun vibrateMultiLineClear(linesCount: Int) {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = when (linesCount) {
                2 -> longArrayOf(0, 35, 35, 60)
                3 -> longArrayOf(0, 35, 25, 45, 25, 75)
                else -> longArrayOf(0, 40, 20, 50, 20, 70, 20, 95)
            }
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(85)
        }
    }

    /**
     * Tiered haptic vibration for combo completions:
     * Escalates in tactile rhythm and intensity as combo increases.
     */
    fun vibrateCombo(combo: Int = 1) {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when {
                combo <= 1 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    } else {
                        vibrator.vibrate(45)
                    }
                }
                combo == 2 -> {
                    // Double crisp pulse
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 35, 45, 55), -1))
                }
                combo == 3 -> {
                    // Ascending triple pulse
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 45, 40, 70), -1))
                }
                else -> {
                    // Energized multi-pulse rumble for mega combos (4+)
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 35, 30, 50, 30, 70, 30, 110), -1))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate((50L + combo * 30L).coerceAtMost(220L))
        }
    }

    /**
     * Heavy distinctive descending pulse pattern for Game Over states.
     */
    fun vibrateGameOver() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 70, 90, 80, 180), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(260)
        }
    }

    fun vibrateHighScore() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40, 50, 80), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(180)
        }
    }
}
