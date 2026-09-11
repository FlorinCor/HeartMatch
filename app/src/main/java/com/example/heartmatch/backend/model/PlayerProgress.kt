package com.example.heartmatch.backend.model

import com.example.heartmatch.engine.loader.JsonObject

data class PlayerProgress(
    val userId: String,
    val currentLevel: Int = 1,
    val completedLevels: List<Int> = emptyList(),
    val starsPerLevel: Map<Int, Int> = emptyMap(),
    val totalScore: Int = 0,
    val boosters: Map<String, Int> = mapOf(
        "HAMMER" to 3,
        "BOMB" to 3,
        "RAINBOW" to 2,
        "SHUFFLE" to 3,
        "EXTRA_MOVES" to 3
    ),
    val lives: Int = 5,
    val maxLives: Int = 5,
    val lastLifeRefillTime: Long = System.currentTimeMillis(),
    val highScoresPerLevel: Map<Int, Int> = emptyMap()
) {
    fun toMap(): Map<String, Any> {
        val starsStrMap = starsPerLevel.mapKeys { it.key.toString() }
        val highScoresStrMap = highScoresPerLevel.mapKeys { it.key.toString() }
        return mapOf(
            "userId" to userId,
            "currentLevel" to currentLevel,
            "completedLevels" to completedLevels,
            "starsPerLevel" to starsStrMap,
            "totalScore" to totalScore,
            "boosters" to boosters,
            "lives" to lives,
            "maxLives" to maxLives,
            "lastLifeRefillTime" to lastLifeRefillTime,
            "highScoresPerLevel" to highScoresStrMap
        )
    }

    companion object {
        fun fromJsonObject(obj: JsonObject): PlayerProgress {
            val userId = obj.getString("userId") ?: ""
            val currentLevel = obj.getInt("currentLevel") ?: 1
            val completedLevels = obj.getArray("completedLevels")?.elements?.mapNotNull {
                it.asPrimitive().content.toIntOrNull()
            } ?: emptyList()

            val starsMap = mutableMapOf<Int, Int>()
            obj.getObject("starsPerLevel")?.members?.forEach { (k, v) ->
                val levelId = k.toIntOrNull()
                val stars = v.asPrimitive().content.toIntOrNull()
                if (levelId != null && stars != null) {
                    starsMap[levelId] = stars
                }
            }

            val highScoresMap = mutableMapOf<Int, Int>()
            obj.getObject("highScoresPerLevel")?.members?.forEach { (k, v) ->
                val levelId = k.toIntOrNull()
                val score = v.asPrimitive().content.toIntOrNull()
                if (levelId != null && score != null) {
                    highScoresMap[levelId] = score
                }
            }

            val boostersMap = mutableMapOf<String, Int>()
            obj.getObject("boosters")?.members?.forEach { (k, v) ->
                val count = v.asPrimitive().content.toIntOrNull()
                if (count != null) {
                    boostersMap[k] = count
                }
            }

            val totalScore = obj.getInt("totalScore") ?: 0
            val lives = obj.getInt("lives") ?: 5
            val maxLives = obj.getInt("maxLives") ?: 5
            val lastLifeRefillTime = obj.getLong("lastLifeRefillTime") ?: System.currentTimeMillis()

            return PlayerProgress(
                userId = userId,
                currentLevel = currentLevel,
                completedLevels = completedLevels,
                starsPerLevel = starsMap,
                totalScore = totalScore,
                boosters = if (boostersMap.isNotEmpty()) boostersMap else mapOf(
                    "HAMMER" to 3,
                    "BOMB" to 3,
                    "RAINBOW" to 2,
                    "SHUFFLE" to 3,
                    "EXTRA_MOVES" to 3
                ),
                lives = lives,
                maxLives = maxLives,
                lastLifeRefillTime = lastLifeRefillTime,
                highScoresPerLevel = highScoresMap
            )
        }
    }
}
