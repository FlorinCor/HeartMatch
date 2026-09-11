package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.HeartColor
import java.util.Random

class DeterministicRng(val seed: Long = 0L) {
    private val random = Random(seed)

    fun nextInt(bound: Int): Int = random.nextInt(bound)

    fun nextDouble(): Double = random.nextDouble()

    fun <T> pickRandom(list: List<T>): T {
        require(list.isNotEmpty()) { "List cannot be empty" }
        return list[random.nextInt(list.size)]
    }

    fun pickWeightedColor(colors: List<HeartColor>, weights: Map<HeartColor, Int>? = null): HeartColor {
        if (weights == null || weights.isEmpty()) {
            return pickRandom(colors)
        }
        val totalWeight = colors.sumOf { weights[it] ?: 1 }
        if (totalWeight <= 0) return pickRandom(colors)
        var roll = random.nextInt(totalWeight)
        for (color in colors) {
            val weight = weights[color] ?: 1
            if (roll < weight) {
                return color
            }
            roll -= weight
        }
        return colors.last()
    }
}
