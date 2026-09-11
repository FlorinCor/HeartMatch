package com.example.heartmatch.backend.model

import com.example.heartmatch.engine.loader.JsonObject

data class StartGameRequest(
    val userId: String,
    val levelId: Int
) {
    companion object {
        fun fromJsonObject(obj: JsonObject): StartGameRequest {
            val userId = obj.getString("userId") ?: "guest"
            val levelId = obj.getInt("levelId") ?: 1
            return StartGameRequest(userId, levelId)
        }
    }
}

data class StartGameResponse(
    val sessionId: String,
    val levelId: Int,
    val status: String,
    val movesRemaining: Int,
    val lives: Int,
    val level: LevelSummary
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sessionId" to sessionId,
        "levelId" to levelId,
        "status" to status,
        "movesRemaining" to movesRemaining,
        "lives" to lives,
        "level" to level.toMap()
    )
}

data class MakeMoveRequest(
    val sessionId: String,
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int
) {
    companion object {
        fun fromJsonObject(obj: JsonObject): MakeMoveRequest {
            val sessionId = obj.getString("sessionId") ?: error("Missing sessionId")
            val fromRow = obj.getInt("fromRow")
                ?: obj.getObject("from")?.getInt("row")
                ?: error("Missing fromRow")
            val fromCol = obj.getInt("fromCol")
                ?: obj.getObject("from")?.getInt("col")
                ?: error("Missing fromCol")
            val toRow = obj.getInt("toRow")
                ?: obj.getObject("to")?.getInt("row")
                ?: error("Missing toRow")
            val toCol = obj.getInt("toCol")
                ?: obj.getObject("to")?.getInt("col")
                ?: error("Missing toCol")

            return MakeMoveRequest(sessionId, fromRow, fromCol, toRow, toCol)
        }
    }
}

data class MakeMoveResponse(
    val sessionId: String,
    val isLegal: Boolean,
    val score: Int,
    val movesRemaining: Int,
    val earnedStars: Int,
    val status: String,
    val isWon: Boolean,
    val isLost: Boolean,
    val eventCount: Int
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sessionId" to sessionId,
        "isLegal" to isLegal,
        "score" to score,
        "movesRemaining" to movesRemaining,
        "earnedStars" to earnedStars,
        "status" to status,
        "isWon" to isWon,
        "isLost" to isLost,
        "eventCount" to eventCount
    )
}

data class CompleteGameRequest(
    val sessionId: String,
    val clientScore: Int? = null,
    val clientStars: Int? = null,
    val clientMovesUsed: Int? = null
) {
    companion object {
        fun fromJsonObject(obj: JsonObject): CompleteGameRequest {
            val sessionId = obj.getString("sessionId") ?: error("Missing sessionId")
            val clientScore = obj.getInt("clientScore") ?: obj.getInt("score")
            val clientStars = obj.getInt("clientStars") ?: obj.getInt("stars")
            val clientMovesUsed = obj.getInt("clientMovesUsed") ?: obj.getInt("movesUsed")
            return CompleteGameRequest(sessionId, clientScore, clientStars, clientMovesUsed)
        }
    }
}

data class CompleteGameResponse(
    val success: Boolean,
    val result: GameResult,
    val progress: PlayerProgress,
    val message: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "success" to success,
        "result" to result.toMap(),
        "progress" to progress.toMap(),
        "message" to message
    ).filterValues { it != null }
}

data class UpdateProgressRequest(
    val userId: String,
    val username: String? = null,
    val boosters: Map<String, Int>? = null,
    val addCoins: Int? = null
) {
    companion object {
        fun fromJsonObject(obj: JsonObject): UpdateProgressRequest {
            val userId = obj.getString("userId") ?: error("Missing userId")
            val username = obj.getString("username")
            val addCoins = obj.getInt("addCoins")
            val boostersMap = mutableMapOf<String, Int>()
            obj.getObject("boosters")?.members?.forEach { (k, v) ->
                val count = v.asPrimitive().content.toIntOrNull()
                if (count != null) boostersMap[k] = count
            }
            return UpdateProgressRequest(
                userId = userId,
                username = username,
                boosters = if (boostersMap.isNotEmpty()) boostersMap else null,
                addCoins = addCoins
            )
        }
    }
}

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "success" to success,
        "data" to data,
        "error" to error
    ).filterValues { it != null }

    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun <T> error(message: String): ApiResponse<T> = ApiResponse(success = false, error = message)
    }
}
