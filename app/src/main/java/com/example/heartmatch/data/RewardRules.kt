package com.example.heartmatch.data

/** Earned coins are the only currency. Replays remain useful without replacing first clears. */
object RewardRules {
    val prices = linkedMapOf("HAMMER" to 120, "BOMB" to 180, "RAINBOW" to 240, "SHUFFLE" to 100, "EXTRA_MOVES" to 200,
        "ROSE_GARDEN" to 600, "MOON_GARDEN" to 600)
    fun completionCoins(completed: Boolean, bestStars: Int, stars: Int): Int =
        (if (completed) 15 else 75) + (stars - bestStars).coerceAtLeast(0) * 25
    fun nextClaim(streak: Int): Int = streak % 7 + 1
    fun milestone(level: Int, completed: Boolean): Boolean = !completed && level % 10 == 0
}
