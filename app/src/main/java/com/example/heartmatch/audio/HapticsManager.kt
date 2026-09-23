package com.example.heartmatch.audio

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.min

/**
 * Semantic haptic events. The UI/ViewModel asks for an *intent* and the
 * manager decides how to render it on the current device.
 */
enum class HapticEffect {
    /** Generic UI button press. */
    BUTTON,
    /** A tile was picked up / selected. */
    SELECT,
    /** Tiles were swapped successfully. */
    SWAP,
    /** The swap was rejected and rolled back. */
    INVALID_MOVE,
    /** A match was cleared; scales with the combo index. */
    MATCH,
    /** A special heart was created. */
    SPECIAL_CREATED,
    /** A special heart detonated. */
    SPECIAL_TRIGGERED,
    /** A blocker took a hit but survived. */
    BLOCKER_HIT,
    /** A blocker was destroyed. */
    BLOCKER_DESTROY,
    /** A booster was activated. */
    BOOSTER_USE,
    /** Reward collected (daily reward, star). */
    REWARD,
    /** Level won. */
    VICTORY,
    /** Level lost. */
    GAME_OVER
}

/**
 * Renders [HapticEffect]s using the richest API the device supports:
 *
 * 1. `VibrationEffect.Composition` primitives (API 30+, when the hardware
 *    reports support) – crisp, designed haptics with rises, thuds and ticks.
 * 2. Predefined system effects (API 29+) – `EFFECT_TICK`, `EFFECT_CLICK`, …
 * 3. Amplitude-controlled waveforms (API 26+) as a universal fallback.
 */
class HapticsManager(private val context: Context) {

    private var isEnabled = true

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private val attributes: VibrationAttributes? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA)
        } else {
            null
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * @param intensity optional 0-based strength hint (e.g. combo index).
     */
    fun play(effect: HapticEffect, intensity: Int = 0) {
        if (!isEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            val vibration = buildEffect(v, effect, intensity.coerceAtLeast(0))
            val attrs = attributes
            if (attrs != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                v.vibrate(vibration, attrs)
            } else {
                v.vibrate(vibration)
            }
        } catch (_: Exception) {
            // Haptics are best-effort.
        }
    }

    fun cancel() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------------

    private fun buildEffect(v: Vibrator, effect: HapticEffect, intensity: Int): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            composed(v, effect, intensity)?.let { return it }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            predefined(effect, intensity)?.let { return it }
        }
        return waveform(effect, intensity)
    }

    /** Designed primitive compositions – the "modern" path. */
    private fun composed(v: Vibrator, effect: HapticEffect, intensity: Int): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val s = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val tick = VibrationEffect.Composition.PRIMITIVE_TICK
        val click = VibrationEffect.Composition.PRIMITIVE_CLICK
        val quickRise = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
        val slowRise = VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
        val quickFall = VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
        val lowTick = if (s) VibrationEffect.Composition.PRIMITIVE_LOW_TICK else tick
        val thud = if (s) VibrationEffect.Composition.PRIMITIVE_THUD else click
        val spin = if (s) VibrationEffect.Composition.PRIMITIVE_SPIN else quickRise

        val steps: List<Step> = when (effect) {
            HapticEffect.BUTTON -> listOf(Step(tick, 0.6f))
            HapticEffect.SELECT -> listOf(Step(tick, 0.45f))
            HapticEffect.SWAP -> listOf(Step(click, 0.65f))
            HapticEffect.INVALID_MOVE -> listOf(Step(lowTick, 1f), Step(lowTick, 1f, 70))
            HapticEffect.MATCH -> {
                val scale = min(1f, 0.55f + intensity * 0.12f)
                if (intensity >= 2) listOf(Step(click, scale), Step(tick, 0.7f, 45))
                else listOf(Step(click, scale))
            }
            HapticEffect.SPECIAL_CREATED -> listOf(Step(quickRise, 0.7f), Step(click, 0.9f, 20))
            HapticEffect.SPECIAL_TRIGGERED -> listOf(Step(thud, 1f), Step(quickFall, 0.6f, 40))
            HapticEffect.BLOCKER_HIT -> listOf(Step(lowTick, 0.85f))
            HapticEffect.BLOCKER_DESTROY -> listOf(Step(thud, 0.9f), Step(tick, 0.5f, 35))
            HapticEffect.BOOSTER_USE -> listOf(Step(slowRise, 0.6f), Step(click, 0.85f, 10))
            HapticEffect.REWARD -> listOf(Step(tick, 0.6f), Step(tick, 0.8f, 55), Step(click, 1f, 55))
            HapticEffect.VICTORY -> listOf(
                Step(quickRise, 0.8f),
                Step(click, 1f, 30),
                Step(tick, 0.8f, 90),
                Step(tick, 0.8f, 70),
                Step(spin, 0.9f, 60),
                Step(click, 1f, 40)
            )
            // Slowing "heartbeat" that fades away.
            HapticEffect.GAME_OVER -> listOf(
                Step(lowTick, 1f),
                Step(lowTick, 0.9f, 110),
                Step(lowTick, 0.7f, 220),
                Step(lowTick, 0.5f, 260)
            )
        }

        return buildComposition(v, steps)
    }

    // The primitive ids in [steps] always originate from Composition.PRIMITIVE_* constants;
    // lint cannot follow them through the data class.
    @Suppress("WrongConstant")
    private fun buildComposition(v: Vibrator, steps: List<Step>): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val ids = steps.map { it.primitive }.distinct().toIntArray()
        if (!v.areAllPrimitivesSupported(*ids)) return null

        val composition = VibrationEffect.startComposition()
        steps.forEach { composition.addPrimitive(it.primitive, it.scale, it.delayMs) }
        return composition.compose()
    }

    /** System-tuned single effects (API 29+). Multi-step patterns fall through to waveforms. */
    private fun predefined(effect: HapticEffect, intensity: Int): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val id = when (effect) {
            HapticEffect.BUTTON, HapticEffect.SELECT, HapticEffect.BLOCKER_HIT -> VibrationEffect.EFFECT_TICK
            HapticEffect.SWAP -> VibrationEffect.EFFECT_CLICK
            HapticEffect.MATCH -> if (intensity >= 2) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_CLICK
            HapticEffect.INVALID_MOVE -> VibrationEffect.EFFECT_DOUBLE_CLICK
            HapticEffect.SPECIAL_CREATED, HapticEffect.SPECIAL_TRIGGERED,
            HapticEffect.BLOCKER_DESTROY, HapticEffect.BOOSTER_USE -> VibrationEffect.EFFECT_HEAVY_CLICK
            HapticEffect.REWARD, HapticEffect.VICTORY, HapticEffect.GAME_OVER -> return null
        }
        return VibrationEffect.createPredefined(id)
    }

    /** Amplitude waveforms – works on every supported API level. */
    private fun waveform(effect: HapticEffect, intensity: Int): VibrationEffect {
        val (timings, amplitudes) = when (effect) {
            HapticEffect.BUTTON -> longArrayOf(12) to intArrayOf(110)
            HapticEffect.SELECT -> longArrayOf(10) to intArrayOf(80)
            HapticEffect.SWAP -> longArrayOf(18) to intArrayOf(160)
            HapticEffect.INVALID_MOVE -> longArrayOf(30, 60, 30) to intArrayOf(140, 0, 140)
            HapticEffect.MATCH -> {
                val amp = min(255, 150 + intensity * 25)
                val len = min(60L, 24L + intensity * 8)
                if (intensity >= 2) longArrayOf(len, 40, 14) to intArrayOf(amp, 0, 120)
                else longArrayOf(len) to intArrayOf(amp)
            }
            HapticEffect.SPECIAL_CREATED -> longArrayOf(15, 15, 20, 15, 35) to intArrayOf(70, 0, 140, 0, 230)
            HapticEffect.SPECIAL_TRIGGERED -> longArrayOf(60, 30, 30) to intArrayOf(255, 0, 140)
            HapticEffect.BLOCKER_HIT -> longArrayOf(18) to intArrayOf(200)
            HapticEffect.BLOCKER_DESTROY -> longArrayOf(35, 25, 15) to intArrayOf(255, 0, 120)
            HapticEffect.BOOSTER_USE -> longArrayOf(20, 15, 20, 15, 30) to intArrayOf(60, 0, 120, 0, 220)
            HapticEffect.REWARD -> longArrayOf(15, 45, 15, 45, 35) to intArrayOf(120, 0, 170, 0, 255)
            HapticEffect.VICTORY -> longArrayOf(30, 40, 50, 60, 20, 50, 20, 60, 90) to
                intArrayOf(160, 0, 255, 0, 140, 0, 140, 0, 255)
            HapticEffect.GAME_OVER -> longArrayOf(45, 110, 45, 200, 40, 250, 35) to
                intArrayOf(220, 0, 200, 0, 150, 0, 100)
        }
        return VibrationEffect.createWaveform(timings, amplitudes, -1)
    }

    private data class Step(val primitive: Int, val scale: Float, val delayMs: Int = 0)
}
