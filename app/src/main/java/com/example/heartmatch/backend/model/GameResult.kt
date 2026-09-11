package com.example.heartmatch.backend.model

import com.example.heartmatch.engine.loader.JsonObject

data class GameResult(
    val levelId: Int,
    val score: Int,
    val movesUsed: Int,
    val objectivesCompleted: Boolean,
    val stars: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val sessionId: String? = null,
    val isVerified: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "levelId" to levelId,
        "score" to score,
        "movesUsed" to movesUsed,
        "objectivesCompleted" to objectivesCompleted,
        "stars" to stars,
        "completedAt" to completedAt,
        "userId" to userId,
        "sessionId" to sessionId,
        "isVerified" to isVerified
    ).filterValues { it != null }

    companion object {
        fun fromJsonObject(obj: JsonObject): GameResult {
            val levelId = obj.getInt("levelId") ?: 1
            val score = obj.getInt("score") ?: 0
            val movesUsed = obj.getInt("movesUsed") ?: 0
            val objectivesCompleted = obj.getBoolean("objectivesCompleted") ?: false
            val stars = obj.getInt("stars") ?: 0
            val completedAt = obj.getLong("completedAt") ?: System.currentTimeMillis()
            val userId = obj.getString("userId")
            val sessionId = obj.getString("sessionId")
            val isVerified = obj.getBoolean("isVerified") ?: true

            return GameResult(
                levelId = levelId,
                score = score,
                movesUsed = movesUsed,
                objectivesCompleted = objectivesCompleted,
                stars = stars,
                completedAt = completedAt,
                userId = userId,
                sessionId = sessionId,
                isVerified = isVerified
            )
        }
    }
}
