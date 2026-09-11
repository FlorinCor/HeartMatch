package com.example.heartmatch.graphics.animation

import kotlin.math.sin

/**
 * Easing curve definitions for decoupled animation systems.
 */
enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BOUNCE_OUT,
    OVERSHOOT
}

fun EasingType.interpolate(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return when (this) {
        EasingType.LINEAR -> clamped
        EasingType.EASE_IN -> clamped * clamped
        EasingType.EASE_OUT -> clamped * (2f - clamped)
        EasingType.EASE_IN_OUT -> if (clamped < 0.5f) 2f * clamped * clamped else -1f + (4f - 2f * clamped) * clamped
        EasingType.BOUNCE_OUT -> {
            var n = clamped
            if (n < 1 / 2.75f) {
                7.5625f * n * n
            } else if (n < 2 / 2.75f) {
                n -= 1.5f / 2.75f
                7.5625f * n * n + 0.75f
            } else if (n < 2.5 / 2.75f) {
                n -= 2.25f / 2.75f
                7.5625f * n * n + 0.9375f
            } else {
                n -= 2.625f / 2.75f
                7.5625f * n * n + 0.984375f
            }
        }
        EasingType.OVERSHOOT -> {
            val s = 1.70158f
            val n = clamped - 1f
            n * n * ((s + 1f) * n + s) + 1f
        }
    }
}

/**
 * A single keyframe for transform values.
 */
data class TransformKeyframe(
    val timeFraction: Float, // 0.0 to 1.0
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val alpha: Float = 1.0f,
    val rotationDeg: Float = 0.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val glowIntensity: Float = 0.0f,
    val shakeIntensity: Float = 0.0f
)

/**
 * Reusable animation definition completely decoupled from graphic assets.
 */
data class AnimationDefinition(
    val name: String,
    val durationMs: Long,
    val isLooping: Boolean = false,
    val easing: EasingType = EasingType.EASE_OUT,
    val keyframes: List<TransformKeyframe>
) {
    fun sample(progress: Float): TransformKeyframe {
        val clampedProgress = if (isLooping) (progress % 1.0f) else progress.coerceIn(0f, 1f)
        if (keyframes.isEmpty()) return TransformKeyframe(clampedProgress)
        if (keyframes.size == 1) return keyframes.first()

        // Find surrounding keyframes
        val sorted = keyframes.sortedBy { it.timeFraction }
        val rightIdx = sorted.indexOfFirst { it.timeFraction >= clampedProgress }.let { if (it == -1) sorted.size - 1 else it }
        val leftIdx = (rightIdx - 1).coerceAtLeast(0)

        val left = sorted[leftIdx]
        val right = sorted[rightIdx]

        if (left === right || left.timeFraction == right.timeFraction) {
            return left
        }

        val segmentDuration = right.timeFraction - left.timeFraction
        val segmentProgress = ((clampedProgress - left.timeFraction) / segmentDuration).coerceIn(0f, 1f)
        val eased = easing.interpolate(segmentProgress)

        return TransformKeyframe(
            timeFraction = clampedProgress,
            scaleX = left.scaleX + (right.scaleX - left.scaleX) * eased,
            scaleY = left.scaleY + (right.scaleY - left.scaleY) * eased,
            alpha = left.alpha + (right.alpha - left.alpha) * eased,
            rotationDeg = left.rotationDeg + (right.rotationDeg - left.rotationDeg) * eased,
            offsetX = left.offsetX + (right.offsetX - left.offsetX) * eased,
            offsetY = left.offsetY + (right.offsetY - left.offsetY) * eased,
            glowIntensity = left.glowIntensity + (right.glowIntensity - left.glowIntensity) * eased,
            shakeIntensity = left.shakeIntensity + (right.shakeIntensity - left.shakeIntensity) * eased
        )
    }
}

/**
 * Standard preset animation catalog for the game.
 */
object StandardHeartAnimations {

    val IDLE_BREATHE = AnimationDefinition(
        name = "idle-breathe",
        durationMs = 2000,
        isLooping = true,
        easing = EasingType.EASE_IN_OUT,
        keyframes = listOf(
            TransformKeyframe(0.0f, scaleX = 1.0f, scaleY = 1.0f, alpha = 1.0f),
            TransformKeyframe(0.5f, scaleX = 1.04f, scaleY = 1.04f, alpha = 1.0f, glowIntensity = 0.2f),
            TransformKeyframe(1.0f, scaleX = 1.0f, scaleY = 1.0f, alpha = 1.0f)
        )
    )

    val SELECTION_PULSE = AnimationDefinition(
        name = "selection-pulse",
        durationMs = 800,
        isLooping = true,
        easing = EasingType.EASE_IN_OUT,
        keyframes = listOf(
            TransformKeyframe(0.0f, scaleX = 1.10f, scaleY = 1.10f, alpha = 1.0f, glowIntensity = 0.6f),
            TransformKeyframe(0.5f, scaleX = 1.20f, scaleY = 1.20f, alpha = 1.0f, glowIntensity = 1.0f),
            TransformKeyframe(1.0f, scaleX = 1.10f, scaleY = 1.10f, alpha = 1.0f, glowIntensity = 0.6f)
        )
    )

    val MATCH_VANISH = AnimationDefinition(
        name = "match-vanish",
        durationMs = 300,
        isLooping = false,
        easing = EasingType.EASE_OUT,
        keyframes = listOf(
            TransformKeyframe(0.0f, scaleX = 1.0f, scaleY = 1.0f, alpha = 1.0f, glowIntensity = 0.0f),
            TransformKeyframe(0.3f, scaleX = 1.35f, scaleY = 1.35f, alpha = 1.0f, glowIntensity = 1.0f),
            TransformKeyframe(1.0f, scaleX = 0.0f, scaleY = 0.0f, alpha = 0.0f, glowIntensity = 0.0f)
        )
    )

    val BLOCKER_DAMAGE_SHAKE = AnimationDefinition(
        name = "blocker-damage-shake",
        durationMs = 250,
        isLooping = false,
        easing = EasingType.LINEAR,
        keyframes = listOf(
            TransformKeyframe(0.0f, scaleX = 1.0f, scaleY = 1.0f, offsetX = 0f, shakeIntensity = 0f),
            TransformKeyframe(0.2f, scaleX = 1.05f, scaleY = 1.05f, offsetX = -8f, shakeIntensity = 1f),
            TransformKeyframe(0.4f, scaleX = 0.95f, scaleY = 0.95f, offsetX = 8f, shakeIntensity = 1f),
            TransformKeyframe(0.6f, scaleX = 1.02f, scaleY = 1.02f, offsetX = -4f, shakeIntensity = 0.5f),
            TransformKeyframe(0.8f, scaleX = 0.98f, scaleY = 0.98f, offsetX = 4f, shakeIntensity = 0.3f),
            TransformKeyframe(1.0f, scaleX = 1.0f, scaleY = 1.0f, offsetX = 0f, shakeIntensity = 0f)
        )
    )

    val BLOCKER_DESTROY_SHATTER = AnimationDefinition(
        name = "blocker-destroy-shatter",
        durationMs = 400,
        isLooping = false,
        easing = EasingType.EASE_OUT,
        keyframes = listOf(
            TransformKeyframe(0.0f, scaleX = 1.0f, scaleY = 1.0f, alpha = 1.0f),
            TransformKeyframe(0.3f, scaleX = 1.25f, scaleY = 1.25f, alpha = 0.9f, rotationDeg = 15f),
            TransformKeyframe(1.0f, scaleX = 0.2f, scaleY = 0.2f, alpha = 0.0f, rotationDeg = 45f)
        )
    )

    val SPECIAL_ACTIVATION = AnimationDefinition(
        name = "special-activation",
        durationMs = 450,
        isLooping = false,
        easing = EasingType.OVERSHOOT,
        keyframes = listOf(
            TransformKeyframe(0.0f, scaleX = 1.0f, scaleY = 1.0f, rotationDeg = 0f, glowIntensity = 0.2f),
            TransformKeyframe(0.5f, scaleX = 1.4f, scaleY = 1.4f, rotationDeg = 180f, glowIntensity = 1.0f),
            TransformKeyframe(1.0f, scaleX = 1.8f, scaleY = 1.8f, rotationDeg = 360f, alpha = 0.0f, glowIntensity = 1.0f)
        )
    )

    val FALL_LANDING_BOUNCE = AnimationDefinition(
        name = "fall-landing-bounce",
        durationMs = 300,
        isLooping = false,
        easing = EasingType.BOUNCE_OUT,
        keyframes = listOf(
            TransformKeyframe(0.0f, scaleX = 0.85f, scaleY = 1.20f),
            TransformKeyframe(0.5f, scaleX = 1.15f, scaleY = 0.85f),
            TransformKeyframe(0.75f, scaleX = 0.95f, scaleY = 1.05f),
            TransformKeyframe(1.0f, scaleX = 1.00f, scaleY = 1.00f)
        )
    )

    val CASCADE_WAVE = AnimationDefinition(
        name = "cascade-wave",
        durationMs = 500,
        isLooping = false,
        easing = EasingType.EASE_OUT,
        keyframes = listOf(
            TransformKeyframe(0.0f, scaleX = 1.0f, scaleY = 1.0f, glowIntensity = 0.0f),
            TransformKeyframe(0.4f, scaleX = 1.2f, scaleY = 1.2f, glowIntensity = 0.8f),
            TransformKeyframe(1.0f, scaleX = 1.0f, scaleY = 1.0f, glowIntensity = 0.0f)
        )
    )
}
