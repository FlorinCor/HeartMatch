package com.example.heartmatch.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Pure-Kotlin procedural synthesizer for the game's sound effects.
 *
 * Every effect is rendered as 16-bit mono PCM. The design goals are a soft,
 * "glassy" modern casual-game feel: click-free attacks, bell-like partials
 * that decay faster than the fundamental, a pentatonic scale for anything
 * musical (so cascades always sound harmonious), sub-bass for impacts and
 * gentle soft-clipping instead of hard limiting.
 *
 * Has no Android dependencies so it can be unit-tested on the JVM.
 */
object SoundSynth {
    const val SAMPLE_RATE = 44100

    /** C-major pentatonic from C5 upwards – every combination sounds pleasant. */
    private val PENTATONIC = doubleArrayOf(
        523.25, 587.33, 659.25, 783.99, 880.00,
        1046.50, 1174.66, 1318.51, 1567.98, 1760.00,
        2093.00, 2349.32
    )

    fun render(effect: SoundEffect, comboIndex: Int = 0): ShortArray {
        val rng = Random(effect.ordinal * 31 + comboIndex)
        val samples = when (effect) {
            SoundEffect.BUTTON_CLICK -> buttonClick()
            SoundEffect.SELECT -> select()
            SoundEffect.SWAP -> swap()
            SoundEffect.INVALID_MOVE -> invalidMove()
            SoundEffect.MATCH -> match(comboIndex, rng)
            SoundEffect.CASCADE -> match(comboIndex + 2, rng)
            SoundEffect.SPECIAL_CREATED -> specialCreated()
            SoundEffect.SPECIAL_FIRE -> fire(rng)
            SoundEffect.SPECIAL_BOMB -> bomb(rng)
            SoundEffect.SPECIAL_RAINBOW -> rainbow(rng)
            SoundEffect.BLOCKER_HIT -> blockerHit(rng)
            SoundEffect.BLOCKER_DESTROY -> blockerDestroy(rng)
            SoundEffect.BOOSTER_USE -> boosterUse()
            SoundEffect.STAR_EARNED -> starEarned()
            SoundEffect.VICTORY -> victory()
            SoundEffect.GAME_OVER -> gameOver()
        }
        return toPcm(samples)
    }

    // ------------------------------------------------------------------
    // Effects
    // ------------------------------------------------------------------

    /** Soft UI "pop": a short downward pitch blip. */
    private fun buttonClick(): DoubleArray {
        val out = DoubleArray(ms(70))
        addGlide(out, 0, ms(70), 1400.0, 700.0, gain = 0.55, attack = 0.002, decay = 45.0)
        return out
    }

    /** Light glassy tick when a tile is picked. */
    private fun select(): DoubleArray {
        val out = DoubleArray(ms(60))
        addBell(out, 0, ms(60), 1760.0, gain = 0.35, attack = 0.001, decay = 40.0)
        addBell(out, 0, ms(45), 2637.0, gain = 0.15, attack = 0.001, decay = 60.0)
        return out
    }

    /** Quick rising "whoosh-pluck" for a successful swap. */
    private fun swap(): DoubleArray {
        val out = DoubleArray(ms(130))
        addGlide(out, 0, ms(130), 480.0, 720.0, gain = 0.45, attack = 0.004, decay = 22.0)
        addGlide(out, ms(10), ms(110), 960.0, 1440.0, gain = 0.15, attack = 0.004, decay = 30.0)
        return out
    }

    /** Muted low double-thud for a rejected move. */
    private fun invalidMove(): DoubleArray {
        val out = DoubleArray(ms(190))
        addGlide(out, 0, ms(90), 210.0, 150.0, gain = 0.6, attack = 0.004, decay = 26.0)
        addGlide(out, ms(95), ms(95), 190.0, 130.0, gain = 0.5, attack = 0.004, decay = 26.0)
        return out
    }

    /**
     * Bright bell chord that climbs the pentatonic scale with the combo index,
     * layered with a short sparkle so bigger combos feel more rewarding.
     */
    private fun match(comboIndex: Int, rng: Random): DoubleArray {
        val idx = comboIndex.coerceIn(0, PENTATONIC.size - 5)
        val root = PENTATONIC[idx]
        val third = PENTATONIC[idx + 2]
        val octave = PENTATONIC[idx + 4]
        val out = DoubleArray(ms(320))
        addBell(out, 0, ms(300), root, gain = 0.42, attack = 0.003, decay = 9.0)
        addBell(out, ms(6), ms(280), third, gain = 0.28, attack = 0.003, decay = 10.0)
        addBell(out, ms(12), ms(260), octave, gain = 0.20, attack = 0.003, decay = 12.0)
        if (comboIndex >= 1) {
            val sparkleGain = min(0.25, 0.08 + comboIndex * 0.04)
            addSparkle(out, ms(20), ms(240), octave * 2, gain = sparkleGain, rng = rng)
        }
        addShimmerTail(out, delayMs = 38, feedback = 0.32)
        return out
    }

    /** Short ascending glissando with a twinkle when a special heart is forged. */
    private fun specialCreated(): DoubleArray {
        val out = DoubleArray(ms(420))
        val notes = doubleArrayOf(PENTATONIC[2], PENTATONIC[4], PENTATONIC[6], PENTATONIC[8])
        notes.forEachIndexed { i, f ->
            addBell(out, ms(i * 55), ms(260), f, gain = 0.32, attack = 0.003, decay = 9.0)
        }
        addBell(out, ms(220), ms(200), PENTATONIC[10], gain = 0.18, attack = 0.002, decay = 14.0)
        addShimmerTail(out, delayMs = 45, feedback = 0.35)
        return out
    }

    /** Fiery sweep: filtered noise whoosh plus a rising harmonic tone. */
    private fun fire(rng: Random): DoubleArray {
        val out = DoubleArray(ms(380))
        addNoiseSweep(out, 0, ms(380), gain = 0.45, rng = rng, attack = 0.02, brightnessStart = 0.15, brightnessEnd = 0.75)
        addGlide(out, ms(20), ms(340), 220.0, 1300.0, gain = 0.30, attack = 0.02, decay = 5.0)
        addGlide(out, ms(40), ms(300), 440.0, 2600.0, gain = 0.10, attack = 0.02, decay = 7.0)
        return out
    }

    /** Punchy explosion: sub-bass drop, a fast noise crack and a low rumble tail. */
    private fun bomb(rng: Random): DoubleArray {
        val out = DoubleArray(ms(520))
        addGlide(out, 0, ms(420), 140.0, 38.0, gain = 0.9, attack = 0.003, decay = 6.0)
        addNoiseSweep(out, 0, ms(160), gain = 0.6, rng = rng, attack = 0.002, brightnessStart = 0.9, brightnessEnd = 0.2)
        addNoiseSweep(out, ms(60), ms(460), gain = 0.30, rng = rng, attack = 0.01, brightnessStart = 0.12, brightnessEnd = 0.04)
        return out
    }

    /** Magical rainbow: fast pentatonic run with heavy shimmer. */
    private fun rainbow(rng: Random): DoubleArray {
        val out = DoubleArray(ms(560))
        for (i in 0 until 7) {
            val f = PENTATONIC[i + 1]
            addBell(out, ms(i * 42), ms(240), f, gain = 0.28, attack = 0.002, decay = 10.0)
        }
        addSparkle(out, ms(120), ms(400), PENTATONIC[9], gain = 0.16, rng = rng)
        addShimmerTail(out, delayMs = 52, feedback = 0.42)
        return out
    }

    /** Short percussive knock. */
    private fun blockerHit(rng: Random): DoubleArray {
        val out = DoubleArray(ms(110))
        addGlide(out, 0, ms(100), 260.0, 170.0, gain = 0.6, attack = 0.002, decay = 28.0)
        addNoiseSweep(out, 0, ms(40), gain = 0.35, rng = rng, attack = 0.001, brightnessStart = 0.7, brightnessEnd = 0.3)
        return out
    }

    /** Crunchy crack with a thump underneath. */
    private fun blockerDestroy(rng: Random): DoubleArray {
        val out = DoubleArray(ms(260))
        addGlide(out, 0, ms(220), 130.0, 55.0, gain = 0.7, attack = 0.002, decay = 12.0)
        addNoiseSweep(out, 0, ms(180), gain = 0.55, rng = rng, attack = 0.001, brightnessStart = 0.85, brightnessEnd = 0.25)
        addBell(out, ms(15), ms(140), 1046.5, gain = 0.12, attack = 0.002, decay = 24.0)
        return out
    }

    /** Magical power-up rise. */
    private fun boosterUse(): DoubleArray {
        val out = DoubleArray(ms(360))
        addGlide(out, 0, ms(260), 330.0, 990.0, gain = 0.30, attack = 0.01, decay = 6.0)
        addBell(out, ms(120), ms(220), PENTATONIC[5], gain = 0.28, attack = 0.003, decay = 10.0)
        addBell(out, ms(180), ms(180), PENTATONIC[7], gain = 0.22, attack = 0.003, decay = 12.0)
        addShimmerTail(out, delayMs = 40, feedback = 0.3)
        return out
    }

    /** Bright, long-ringing reward chime. */
    private fun starEarned(): DoubleArray {
        val out = DoubleArray(ms(620))
        val chord = doubleArrayOf(659.25, 830.61, 987.77, 1318.51)
        chord.forEachIndexed { i, f ->
            addBell(out, ms(i * 18), ms(560), f, gain = 0.26 - i * 0.03, attack = 0.003, decay = 5.5)
        }
        addBell(out, ms(90), ms(400), 2637.02, gain = 0.08, attack = 0.002, decay = 9.0)
        addShimmerTail(out, delayMs = 60, feedback = 0.4)
        return out
    }

    /** Celebratory fanfare: rising run into a sustained major chord. */
    private fun victory(): DoubleArray {
        val out = DoubleArray(ms(1400))
        val run = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        run.forEachIndexed { i, f ->
            addBell(out, ms(i * 110), ms(300), f, gain = 0.34, attack = 0.004, decay = 8.0)
        }
        val chord = doubleArrayOf(1046.50, 1318.51, 1567.98, 2093.00)
        chord.forEachIndexed { i, f ->
            addBell(out, ms(440 + i * 12), ms(900), f, gain = 0.26 - i * 0.04, attack = 0.006, decay = 3.2)
        }
        addBell(out, ms(440), ms(900), 261.63, gain = 0.18, attack = 0.01, decay = 3.0)
        addShimmerTail(out, delayMs = 70, feedback = 0.42)
        return out
    }

    /** Soft descending pad – disappointed but not harsh. */
    private fun gameOver(): DoubleArray {
        val out = DoubleArray(ms(1100))
        val notes = doubleArrayOf(440.0, 392.0, 349.23, 293.66)
        notes.forEachIndexed { i, f ->
            val start = ms(i * 210)
            addBell(out, start, ms(420), f, gain = 0.30, attack = 0.03, decay = 4.5)
            addBell(out, start, ms(420), f / 2, gain = 0.16, attack = 0.03, decay = 4.0)
        }
        addShimmerTail(out, delayMs = 90, feedback = 0.35)
        return out
    }

    // ------------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------------

    private fun ms(millis: Int): Int = (SAMPLE_RATE * millis) / 1000

    /** Smooth attack / release envelope multiplied by an exponential decay. */
    private fun envelope(t: Double, duration: Double, attack: Double, decay: Double): Double {
        val a = if (attack <= 0.0) 1.0 else min(1.0, t / attack)
        val release = 0.012
        val r = min(1.0, max(0.0, (duration - t) / release))
        // Smoothstep both edges to avoid zipper noise.
        val aS = a * a * (3 - 2 * a)
        val rS = r * r * (3 - 2 * r)
        return aS * rS * exp(-decay * t)
    }

    /**
     * Bell timbre: fundamental plus 2nd and 3rd partials that decay faster,
     * plus a slightly detuned copy of the fundamental for a chorus-like warmth.
     */
    private fun addBell(out: DoubleArray, start: Int, length: Int, freq: Double, gain: Double, attack: Double, decay: Double) {
        val end = min(out.size, start + length)
        val duration = (end - start).toDouble() / SAMPLE_RATE
        for (i in start until end) {
            val t = (i - start).toDouble() / SAMPLE_RATE
            val env = envelope(t, duration, attack, decay)
            val w = 2.0 * PI * t
            var s = sin(w * freq)
            s += 0.55 * sin(w * freq * 1.003)
            s += 0.35 * sin(w * freq * 2.0) * exp(-decay * 0.9 * t)
            s += 0.16 * sin(w * freq * 3.0) * exp(-decay * 1.6 * t)
            out[i] += s * env * gain / 1.4
        }
    }

    /** Sine with linear-in-log pitch glide; harmonics added for body. */
    private fun addGlide(out: DoubleArray, start: Int, length: Int, fromHz: Double, toHz: Double, gain: Double, attack: Double, decay: Double) {
        val end = min(out.size, start + length)
        val duration = (end - start).toDouble() / SAMPLE_RATE
        var phase = 0.0
        for (i in start until end) {
            val t = (i - start).toDouble() / SAMPLE_RATE
            val progress = if (duration <= 0.0) 0.0 else t / duration
            val f = fromHz * (toHz / fromHz).pow(progress)
            phase += 2.0 * PI * f / SAMPLE_RATE
            val env = envelope(t, duration, attack, decay)
            val s = sin(phase) + 0.25 * sin(2 * phase) * exp(-decay * 0.7 * t)
            out[i] += s * env * gain
        }
    }

    /**
     * Noise "whoosh" through a simple one-pole low-pass whose cut-off sweeps
     * between two brightness values (0 = very dull, 1 = white).
     */
    private fun addNoiseSweep(out: DoubleArray, start: Int, length: Int, gain: Double, rng: Random, attack: Double, brightnessStart: Double, brightnessEnd: Double) {
        val end = min(out.size, start + length)
        val duration = (end - start).toDouble() / SAMPLE_RATE
        var lp = 0.0
        for (i in start until end) {
            val t = (i - start).toDouble() / SAMPLE_RATE
            val progress = if (duration <= 0.0) 0.0 else t / duration
            val brightness = brightnessStart + (brightnessEnd - brightnessStart) * progress
            val alpha = brightness.coerceIn(0.02, 1.0)
            val white = rng.nextDouble() * 2.0 - 1.0
            lp += alpha * (white - lp)
            val env = envelope(t, duration, attack, decay = 2.5) * (1.0 - progress).pow(1.4)
            out[i] += lp * env * gain
        }
    }

    /** Rapid twinkling grains above the given pitch. */
    private fun addSparkle(out: DoubleArray, start: Int, length: Int, baseHz: Double, gain: Double, rng: Random) {
        val grains = max(3, length / ms(45))
        for (g in 0 until grains) {
            val gStart = start + rng.nextInt(max(1, length - ms(60)))
            val ratio = 1.0 + rng.nextInt(4) * 0.5 // 1, 1.5, 2, 2.5 -> harmonic sparkle
            addBell(out, gStart, ms(70), baseHz * ratio, gain = gain * (0.5 + rng.nextDouble() * 0.5), attack = 0.001, decay = 35.0)
        }
    }

    /** Very small feedback delay that adds a sense of space without smearing transients. */
    private fun addShimmerTail(out: DoubleArray, delayMs: Int, feedback: Double) {
        val d = ms(delayMs)
        if (d <= 0 || d >= out.size) return
        for (i in d until out.size) {
            out[i] += out[i - d] * feedback * 0.5
        }
    }

    /** Soft-clips and converts to 16-bit PCM with a small headroom. */
    private fun toPcm(samples: DoubleArray): ShortArray {
        val result = ShortArray(samples.size)
        for (i in samples.indices) {
            val v = tanh(samples[i] * 1.15) * 0.9
            result[i] = (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return result
    }
}
