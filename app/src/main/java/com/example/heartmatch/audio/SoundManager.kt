package com.example.heartmatch.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

enum class SoundEffect {
    /** Tile picked up. */
    SELECT,
    /** Tiles swapped. */
    SWAP,
    /** Swap rejected. */
    INVALID_MOVE,
    MATCH,
    CASCADE,
    SPECIAL_CREATED,
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

/**
 * Facade for all audio + haptic feedback.
 *
 * Sounds are synthesised once by [SoundSynth], cached as PCM and played through
 * short-lived low-latency [AudioTrack]s. Up to [MAX_VOICES] effects can overlap
 * so cascades sound layered instead of queueing up; extra requests are dropped
 * rather than delayed. Identical effects fired within a few milliseconds of each
 * other (e.g. several matches resolved in the same frame) are de-duplicated.
 */
class SoundManager(context: Context) {

    private val haptics = HapticsManager(context)

    private val cache = ConcurrentHashMap<Int, ShortArray>()
    private val lastPlayedAt = ConcurrentHashMap<Int, Long>()

    private val renderExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sfx-render").apply { isDaemon = true }
    }

    private val playbackExecutor = ThreadPoolExecutor(
        0, MAX_VOICES, 2L, TimeUnit.SECONDS, SynchronousQueue(),
        { r -> Thread(r, "sfx-voice").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardPolicy()
    )

    private var isSfxEnabled = true

    init {
        preWarm()
    }

    fun updateSettings(sfxEnabled: Boolean, hapticsEnabled: Boolean) {
        isSfxEnabled = sfxEnabled
        haptics.setEnabled(hapticsEnabled)
    }

    fun playSound(effect: SoundEffect, comboIndex: Int = 0) {
        if (!isSfxEnabled) return
        val key = key(effect, comboIndex)
        val now = SystemClock.uptimeMillis()
        val last = lastPlayedAt[key] ?: 0L
        if (now - last < DEDUPE_WINDOW_MS) return
        lastPlayedAt[key] = now

        val cached = cache[key]
        if (cached != null) {
            playbackExecutor.execute { playPcm(cached) }
        } else {
            try {
                renderExecutor.execute {
                    try {
                        val samples = cache.getOrPut(key) { SoundSynth.render(effect, comboIndex) }
                        playbackExecutor.execute { playPcm(samples) }
                    } catch (_: Exception) {
                        // An uncaught exception on a worker thread would kill the whole app.
                    }
                }
            } catch (_: RejectedExecutionException) {
                // Manager already released.
            }
        }
    }

    fun haptic(effect: HapticEffect, intensity: Int = 0) {
        haptics.play(effect, intensity)
    }

    fun release() {
        renderExecutor.shutdownNow()
        playbackExecutor.shutdownNow()
        haptics.cancel()
    }

    // ------------------------------------------------------------------

    /** Renders the most common effects up-front so the first tap is instant. */
    private fun preWarm() {
        renderExecutor.execute {
            try {
                listOf(
                    SoundEffect.BUTTON_CLICK, SoundEffect.SELECT, SoundEffect.SWAP,
                    SoundEffect.INVALID_MOVE, SoundEffect.BLOCKER_HIT, SoundEffect.BLOCKER_DESTROY,
                    SoundEffect.SPECIAL_CREATED
                ).forEach { cache.getOrPut(key(it, 0)) { SoundSynth.render(it, 0) } }
                for (combo in 0..4) {
                    cache.getOrPut(key(SoundEffect.MATCH, combo)) { SoundSynth.render(SoundEffect.MATCH, combo) }
                }
            } catch (_: Exception) {}
        }
    }

    private fun key(effect: SoundEffect, comboIndex: Int): Int = effect.ordinal * 64 + comboIndex.coerceIn(0, 63)

    private fun playPcm(samples: ShortArray) {
        var track: AudioTrack? = null
        try {
            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SoundSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
            track = builder.build()
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep((samples.size * 1000L) / SoundSynth.SAMPLE_RATE + 30)
        } catch (_: Exception) {
            // Audio is best-effort; never crash the game because of it.
        } finally {
            try {
                track?.stop()
            } catch (_: Exception) {}
            track?.release()
        }
    }

    private companion object {
        const val MAX_VOICES = 4
        const val DEDUPE_WINDOW_MS = 40L
    }
}
