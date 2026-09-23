package com.example.heartmatch.audio

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SoundSynthTest {

    @Test
    fun everyEffectRendersAudibleClipFreeAudio() {
        for (effect in SoundEffect.values()) {
            val pcm = SoundSynth.render(effect)
            assertTrue("$effect is empty", pcm.isNotEmpty())

            val peak = pcm.maxOf { abs(it.toInt()) }
            assertTrue("$effect is silent (peak=$peak)", peak > Short.MAX_VALUE * 0.15)
            // Soft clipping must leave headroom – no hard clipping at full scale.
            assertTrue("$effect hard-clips (peak=$peak)", peak < Short.MAX_VALUE * 0.95)
        }
    }

    @Test
    fun effectsStartAndEndSilentToAvoidClicks() {
        for (effect in SoundEffect.values()) {
            val pcm = SoundSynth.render(effect)
            val threshold = Short.MAX_VALUE * 0.02
            assertTrue("$effect has a hard attack", abs(pcm.first().toInt()) < threshold)
            assertTrue("$effect has a hard cut-off", abs(pcm.last().toInt()) < threshold)
        }
    }

    @Test
    fun effectDurationsFitTheirGameplayRole() {
        fun durationMs(effect: SoundEffect) = SoundSynth.render(effect).size * 1000 / SoundSynth.SAMPLE_RATE

        // Quick, non-intrusive UI/board feedback.
        assertTrue(durationMs(SoundEffect.BUTTON_CLICK) <= 100)
        assertTrue(durationMs(SoundEffect.SELECT) <= 100)
        assertTrue(durationMs(SoundEffect.SWAP) <= 200)
        assertTrue(durationMs(SoundEffect.MATCH) <= 400)

        // Big moments are allowed to ring out.
        assertTrue(durationMs(SoundEffect.VICTORY) >= 1000)
        assertTrue(durationMs(SoundEffect.GAME_OVER) >= 800)
    }

    @Test
    fun matchPitchRisesWithComboIndex() {
        val low = dominantFrequency(SoundSynth.render(SoundEffect.MATCH, 0))
        val high = dominantFrequency(SoundSynth.render(SoundEffect.MATCH, 4))
        assertTrue("combo pitch should rise ($low -> $high)", high > low * 1.3)
    }

    @Test
    fun renderingIsDeterministic() {
        val a = SoundSynth.render(SoundEffect.SPECIAL_BOMB)
        val b = SoundSynth.render(SoundEffect.SPECIAL_BOMB)
        assertArrayEquals(a, b)
    }

    /** Crude zero-crossing estimate of the fundamental over the first 60 ms. */
    private fun dominantFrequency(pcm: ShortArray): Double {
        val window = minOf(pcm.size, SoundSynth.SAMPLE_RATE * 60 / 1000)
        var crossings = 0
        for (i in 1 until window) {
            if ((pcm[i - 1] < 0) != (pcm[i] < 0)) crossings++
        }
        return crossings / 2.0 / (window.toDouble() / SoundSynth.SAMPLE_RATE)
    }
}
