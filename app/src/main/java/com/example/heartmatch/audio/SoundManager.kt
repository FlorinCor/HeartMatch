package com.example.heartmatch.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect {
    SWAP,
    MATCH,
    CASCADE,
    SPECIAL_FIRE,
    SPECIAL_BOMB,
    SPECIAL_RAINBOW,
    BLOCKER_HIT,
    BLOCKER_DESTROY,
    BOOSTER_USE,
    BUTTON_CLICK,
    VICTORY,
    GAME_OVER,
    STAR_EARNED
}

class SoundManager(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val sampleRate = 44100
    private var isSfxEnabled = true
    private var isHapticsEnabled = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun updateSettings(sfxEnabled: Boolean, hapticsEnabled: Boolean) {
        this.isSfxEnabled = sfxEnabled
        this.isHapticsEnabled = hapticsEnabled
    }

    fun playSound(effect: SoundEffect, comboIndex: Int = 0) {
        if (!isSfxEnabled) return
        executor.execute {
            try {
                val samples = generateSamples(effect, comboIndex)
                playPcm(samples)
            } catch (_: Exception) {
                // Ignore audio playback errors gracefully
            }
        }
    }

    fun vibrate(durationMs: Long = 30, amplitude: Int = 120) {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val clampedAmp = amplitude.coerceIn(1, 255)
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmp))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun playPcm(samples: ShortArray) {
        val bufferSize = samples.size * 2
        val audioTrack = AudioTrack.Builder()
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
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        Thread.sleep((samples.size * 1000L) / sampleRate + 20)
        audioTrack.stop()
        audioTrack.release()
    }

    private fun generateSamples(effect: SoundEffect, comboIndex: Int): ShortArray {
        return when (effect) {
            SoundEffect.BUTTON_CLICK -> generateTone(frequency = 700.0, durationMs = 50, decay = 20.0)
            SoundEffect.SWAP -> generateTone(frequency = 420.0, durationMs = 80, decay = 15.0)
            SoundEffect.MATCH -> {
                val baseFreq = 480.0 + (comboIndex.coerceAtMost(6) * 70.0)
                generateChord(listOf(baseFreq, baseFreq * 1.25), durationMs = 150, decay = 8.0)
            }
            SoundEffect.CASCADE -> {
                val baseFreq = 540.0 + (comboIndex.coerceAtMost(8) * 85.0)
                generateTone(frequency = baseFreq, durationMs = 120, decay = 12.0)
            }
            SoundEffect.SPECIAL_FIRE -> generateNoiseSweep(startFreq = 300.0, endFreq = 900.0, durationMs = 280)
            SoundEffect.SPECIAL_BOMB -> generateExplosion(durationMs = 350)
            SoundEffect.SPECIAL_RAINBOW -> generateArpeggio(listOf(523.25, 659.25, 783.99, 1046.50), durationMs = 300)
            SoundEffect.BLOCKER_HIT -> generateTone(frequency = 260.0, durationMs = 100, decay = 16.0)
            SoundEffect.BLOCKER_DESTROY -> generateExplosion(durationMs = 200)
            SoundEffect.BOOSTER_USE -> generateArpeggio(listOf(440.0, 554.37, 659.25), durationMs = 200)
            SoundEffect.STAR_EARNED -> generateChord(listOf(659.25, 830.61, 987.77, 1318.51), durationMs = 350, decay = 5.0)
            SoundEffect.VICTORY -> generateVictoryFanfare()
            SoundEffect.GAME_OVER -> generateGameOverTone()
        }
    }

    private fun generateTone(frequency: Double, durationMs: Int, decay: Double): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val envelope = exp(-decay * time)
            val sample = sin(2.0 * PI * frequency * time) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateChord(frequencies: List<Double>, durationMs: Int, decay: Double): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val envelope = exp(-decay * time)
            var sample = 0.0
            for (freq in frequencies) {
                sample += sin(2.0 * PI * freq * time)
            }
            sample = (sample / frequencies.size) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.75).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateArpeggio(notes: List<Double>, durationMs: Int): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        val noteLength = numSamples / notes.size
        for (i in 0 until numSamples) {
            val noteIdx = (i / noteLength).coerceAtMost(notes.size - 1)
            val noteTime = (i % noteLength).toDouble() / sampleRate
            val envelope = exp(-10.0 * noteTime)
            val sample = sin(2.0 * PI * notes[noteIdx] * noteTime) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateNoiseSweep(startFreq: Double, endFreq: Double, durationMs: Int): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * currentFreq / sampleRate
            val envelope = (1.0 - progress) * (if (progress < 0.1) progress * 10 else 1.0)
            val sample = (sin(phase) + (Math.random() * 0.4 - 0.2)) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateExplosion(durationMs: Int): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val envelope = exp(-6.0 * progress)
            val noise = (Math.random() * 2.0 - 1.0)
            val bass = sin(2.0 * PI * (90.0 - 60.0 * progress) * (i.toDouble() / sampleRate))
            val sample = (noise * 0.6 + bass * 0.4) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.75).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateVictoryFanfare(): ShortArray {
        val notes = listOf(523.25, 659.25, 783.99, 1046.50)
        return generateArpeggio(notes, 500)
    }

    private fun generateGameOverTone(): ShortArray {
        val notes = listOf(440.0, 392.0, 349.23, 293.66)
        return generateArpeggio(notes, 600)
    }
}
