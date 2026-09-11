package com.example.heartmatch.data

import android.content.Context
import android.content.SharedPreferences

data class LevelRecord(
    val levelId: Int,
    val stars: Int,
    val highScore: Int,
    val isCompleted: Boolean
)

data class PlayerProfile(
    val coins: Int = 500,
    val highestUnlockedLevel: Int = 1,
    val hammerCount: Int = 3,
    val bombBoosterCount: Int = 3,
    val rainbowBoosterCount: Int = 2,
    val shuffleCount: Int = 3,
    val extraMovesCount: Int = 3,
    val sfxEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val dailyRewardStreak: Int = 0,
    val lastDailyClaimDay: Long = 0L,
    val totalMatchesMade: Int = 0,
    val totalStarsEarned: Int = 0
)

class PlayerRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("heart_match_prefs", Context.MODE_PRIVATE)

    fun getProfile(): PlayerProfile {
        return PlayerProfile(
            coins = prefs.getInt("coins", 500),
            highestUnlockedLevel = prefs.getInt("highestUnlockedLevel", 1),
            hammerCount = prefs.getInt("hammerCount", 3),
            bombBoosterCount = prefs.getInt("bombBoosterCount", 3),
            rainbowBoosterCount = prefs.getInt("rainbowBoosterCount", 2),
            shuffleCount = prefs.getInt("shuffleCount", 3),
            extraMovesCount = prefs.getInt("extraMovesCount", 3),
            sfxEnabled = prefs.getBoolean("sfxEnabled", true),
            musicEnabled = prefs.getBoolean("musicEnabled", true),
            hapticsEnabled = prefs.getBoolean("hapticsEnabled", true),
            dailyRewardStreak = prefs.getInt("dailyRewardStreak", 0),
            lastDailyClaimDay = prefs.getLong("lastDailyClaimDay", 0L),
            totalMatchesMade = prefs.getInt("totalMatchesMade", 0),
            totalStarsEarned = calculateTotalStars()
        )
    }

    fun saveProfile(profile: PlayerProfile) {
        prefs.edit()
            .putInt("coins", profile.coins)
            .putInt("highestUnlockedLevel", profile.highestUnlockedLevel)
            .putInt("hammerCount", profile.hammerCount)
            .putInt("bombBoosterCount", profile.bombBoosterCount)
            .putInt("rainbowBoosterCount", profile.rainbowBoosterCount)
            .putInt("shuffleCount", profile.shuffleCount)
            .putInt("extraMovesCount", profile.extraMovesCount)
            .putBoolean("sfxEnabled", profile.sfxEnabled)
            .putBoolean("musicEnabled", profile.musicEnabled)
            .putBoolean("hapticsEnabled", profile.hapticsEnabled)
            .putInt("dailyRewardStreak", profile.dailyRewardStreak)
            .putLong("lastDailyClaimDay", profile.lastDailyClaimDay)
            .putInt("totalMatchesMade", profile.totalMatchesMade)
            .apply()
    }

    fun getLevelRecord(levelId: Int): LevelRecord {
        val stars = prefs.getInt("level_${levelId}_stars", 0)
        val highScore = prefs.getInt("level_${levelId}_score", 0)
        val isCompleted = prefs.getBoolean("level_${levelId}_completed", false)
        return LevelRecord(levelId, stars, highScore, isCompleted)
    }

    fun saveLevelProgress(levelId: Int, stars: Int, score: Int) {
        val currentRecord = getLevelRecord(levelId)
        val maxStars = maxOf(currentRecord.stars, stars)
        val maxScore = maxOf(currentRecord.highScore, score)

        val editor = prefs.edit()
        editor.putInt("level_${levelId}_stars", maxStars)
        editor.putInt("level_${levelId}_score", maxScore)
        editor.putBoolean("level_${levelId}_completed", true)

        val currentUnlocked = prefs.getInt("highestUnlockedLevel", 1)
        if (levelId >= currentUnlocked && levelId < 100) {
            editor.putInt("highestUnlockedLevel", levelId + 1)
        }

        editor.apply()
    }

    fun consumeBooster(boosterType: String): Boolean {
        val profile = getProfile()
        val updated = when (boosterType) {
            "HAMMER" -> if (profile.hammerCount > 0) profile.copy(hammerCount = profile.hammerCount - 1) else null
            "BOMB" -> if (profile.bombBoosterCount > 0) profile.copy(bombBoosterCount = profile.bombBoosterCount - 1) else null
            "RAINBOW" -> if (profile.rainbowBoosterCount > 0) profile.copy(rainbowBoosterCount = profile.rainbowBoosterCount - 1) else null
            "SHUFFLE" -> if (profile.shuffleCount > 0) profile.copy(shuffleCount = profile.shuffleCount - 1) else null
            "EXTRA_MOVES" -> if (profile.extraMovesCount > 0) profile.copy(extraMovesCount = profile.extraMovesCount - 1) else null
            else -> null
        }
        return if (updated != null) {
            saveProfile(updated)
            true
        } else {
            false
        }
    }

    fun addCoins(amount: Int) {
        val profile = getProfile()
        saveProfile(profile.copy(coins = profile.coins + amount))
    }

    fun addBooster(boosterType: String, count: Int = 1) {
        val profile = getProfile()
        val updated = when (boosterType) {
            "HAMMER" -> profile.copy(hammerCount = profile.hammerCount + count)
            "BOMB" -> profile.copy(bombBoosterCount = profile.bombBoosterCount + count)
            "RAINBOW" -> profile.copy(rainbowBoosterCount = profile.rainbowBoosterCount + count)
            "SHUFFLE" -> profile.copy(shuffleCount = profile.shuffleCount + count)
            "EXTRA_MOVES" -> profile.copy(extraMovesCount = profile.extraMovesCount + count)
            else -> profile
        }
        saveProfile(updated)
    }

    fun claimDailyReward(day: Int, rewardCoins: Int, boosterType: String? = null) {
        val profile = getProfile()
        var updated = profile.copy(
            coins = profile.coins + rewardCoins,
            dailyRewardStreak = day,
            lastDailyClaimDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        )
        if (boosterType != null) {
            updated = when (boosterType) {
                "HAMMER" -> updated.copy(hammerCount = updated.hammerCount + 1)
                "BOMB" -> updated.copy(bombBoosterCount = updated.bombBoosterCount + 1)
                "RAINBOW" -> updated.copy(rainbowBoosterCount = updated.rainbowBoosterCount + 1)
                "SHUFFLE" -> updated.copy(shuffleCount = updated.shuffleCount + 1)
                "EXTRA_MOVES" -> updated.copy(extraMovesCount = updated.extraMovesCount + 1)
                else -> updated
            }
        }
        saveProfile(updated)
    }

    fun resetAllData() {
        prefs.edit().clear().apply()
    }

    private fun calculateTotalStars(): Int {
        var total = 0
        for (i in 1..100) {
            total += prefs.getInt("level_${i}_stars", 0)
        }
        return total
    }
}
