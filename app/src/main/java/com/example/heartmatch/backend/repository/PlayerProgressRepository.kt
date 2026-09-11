package com.example.heartmatch.backend.repository

import com.example.heartmatch.backend.model.PlayerProgress
import java.util.concurrent.ConcurrentHashMap

class PlayerProgressRepository(
    private val lifeRegenIntervalMs: Long = 20 * 60 * 1000L // 20 minutes
) {
    private val progressMap = ConcurrentHashMap<String, PlayerProgress>()

    fun getProgress(userId: String): PlayerProgress {
        return progressMap.compute(userId) { _, existing ->
            val base = existing ?: PlayerProgress(userId = userId)
            recalculateLives(base)
        }!!
    }

    fun saveProgress(progress: PlayerProgress): PlayerProgress {
        val updated = recalculateLives(progress)
        progressMap[progress.userId] = updated
        return updated
    }

    fun recordLevelCompletion(
        userId: String,
        levelId: Int,
        stars: Int,
        score: Int
    ): PlayerProgress {
        return progressMap.compute(userId) { _, current ->
            val base = current ?: PlayerProgress(userId = userId)
            val updatedBase = recalculateLives(base)

            val newCompleted = if (updatedBase.completedLevels.contains(levelId)) {
                updatedBase.completedLevels
            } else {
                (updatedBase.completedLevels + levelId).sorted()
            }

            val currentStars = updatedBase.starsPerLevel[levelId] ?: 0
            val newStars = maxOf(currentStars, stars)
            val updatedStarsMap = updatedBase.starsPerLevel + (levelId to newStars)

            val currentHighScore = updatedBase.highScoresPerLevel[levelId] ?: 0
            val newHighScore = maxOf(currentHighScore, score)
            val updatedHighScoresMap = updatedBase.highScoresPerLevel + (levelId to newHighScore)

            val newTotalScore = updatedHighScoresMap.values.sum()
            val nextLevel = if (levelId >= updatedBase.currentLevel) levelId + 1 else updatedBase.currentLevel

            updatedBase.copy(
                currentLevel = nextLevel,
                completedLevels = newCompleted,
                starsPerLevel = updatedStarsMap,
                highScoresPerLevel = updatedHighScoresMap,
                totalScore = newTotalScore
            )
        }!!
    }

    fun deductLife(userId: String): Boolean {
        var deducted = false
        progressMap.compute(userId) { _, current ->
            val base = recalculateLives(current ?: PlayerProgress(userId = userId))
            if (base.lives > 0) {
                deducted = true
                val newLives = base.lives - 1
                val lastRefill = if (base.lives == base.maxLives) System.currentTimeMillis() else base.lastLifeRefillTime
                base.copy(lives = newLives, lastLifeRefillTime = lastRefill)
            } else {
                deducted = false
                base
            }
        }
        return deducted
    }

    fun addLives(userId: String, amount: Int): PlayerProgress {
        return progressMap.compute(userId) { _, current ->
            val base = recalculateLives(current ?: PlayerProgress(userId = userId))
            val newLives = minOf(base.maxLives, base.lives + amount)
            base.copy(lives = newLives)
        }!!
    }

    fun consumeBooster(userId: String, boosterType: String): Boolean {
        var success = false
        progressMap.compute(userId) { _, current ->
            val base = recalculateLives(current ?: PlayerProgress(userId = userId))
            val currentCount = base.boosters[boosterType] ?: 0
            if (currentCount > 0) {
                success = true
                val updatedBoosters = base.boosters + (boosterType to currentCount - 1)
                base.copy(boosters = updatedBoosters)
            } else {
                success = false
                base
            }
        }
        return success
    }

    fun addBooster(userId: String, boosterType: String, count: Int = 1): PlayerProgress {
        return progressMap.compute(userId) { _, current ->
            val base = recalculateLives(current ?: PlayerProgress(userId = userId))
            val currentCount = base.boosters[boosterType] ?: 0
            val updatedBoosters = base.boosters + (boosterType to currentCount + count)
            base.copy(boosters = updatedBoosters)
        }!!
    }

    private fun recalculateLives(progress: PlayerProgress): PlayerProgress {
        if (progress.lives >= progress.maxLives) {
            return progress.copy(lives = progress.maxLives, lastLifeRefillTime = System.currentTimeMillis())
        }

        val now = System.currentTimeMillis()
        val elapsed = now - progress.lastLifeRefillTime
        if (elapsed <= 0) return progress

        val livesToAdd = (elapsed / lifeRegenIntervalMs).toInt()
        if (livesToAdd <= 0) return progress

        val newLives = minOf(progress.maxLives, progress.lives + livesToAdd)
        val remainingTime = elapsed % lifeRegenIntervalMs
        val newRefillTime = if (newLives >= progress.maxLives) now else now - remainingTime

        return progress.copy(
            lives = newLives,
            lastLifeRefillTime = newRefillTime
        )
    }

    fun clear() {
        progressMap.clear()
    }
}
