package com.example.heartmatch.backend.model

import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.GameState
import com.example.heartmatch.engine.model.GameStatus
import com.example.heartmatch.engine.model.LevelConfig

data class MoveRecord(
    val moveIndex: Int,
    val from: Coord,
    val to: Coord,
    val isLegal: Boolean,
    val scoreDelta: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class GameSession(
    val sessionId: String,
    val userId: String,
    val levelId: Int,
    val levelConfig: LevelConfig,
    val engine: HeartMatchEngine,
    val startedAt: Long = System.currentTimeMillis()
) {
    val moveHistory = mutableListOf<MoveRecord>()
    val eventHistory = mutableListOf<EngineEvent>()
    var movesUsed: Int = 0
        private set
    var isCompleted: Boolean = false
        private set
    var completedResult: GameResult? = null
        private set

    init {
        engine.addEventListener { event ->
            eventHistory.add(event)
        }
    }

    val currentScore: Int
        get() = engine.getState().score

    val earnedStars: Int
        get() = engine.getState().earnedStars

    val movesRemaining: Int
        get() = engine.getState().movesRemaining

    val status: GameStatus
        get() = engine.getState().status

    val isWon: Boolean
        get() = engine.getState().isWon

    val isLost: Boolean
        get() = engine.getState().isLost

    val isObjectivesCompleted: Boolean
        get() = engine.getState().objectives.isNotEmpty() && engine.getState().objectives.all { it.isFulfilled }

    fun executeMove(from: Coord, to: Coord): Pair<Boolean, List<EngineEvent>> {
        check(!isCompleted) { "Cannot make moves on a completed game session." }

        val startEventCount = eventHistory.size
        val scoreBefore = currentScore
        val result = engine.swap(from, to)

        if (result.isSuccessfulMove) {
            movesUsed++
        }

        val scoreDelta = currentScore - scoreBefore
        moveHistory.add(
            MoveRecord(
                moveIndex = moveHistory.size + 1,
                from = from,
                to = to,
                isLegal = result.isSuccessfulMove,
                scoreDelta = scoreDelta
            )
        )

        val newEvents = eventHistory.subList(startEventCount, eventHistory.size).toList()
        return Pair(result.isSuccessfulMove, newEvents)
    }

    fun completeSession(isVerified: Boolean = true): GameResult {
        if (isCompleted && completedResult != null) {
            return completedResult!!
        }

        isCompleted = true
        val state = engine.getState()
        val result = GameResult(
            levelId = levelId,
            score = state.score,
            movesUsed = movesUsed,
            objectivesCompleted = isObjectivesCompleted || state.isWon,
            stars = state.earnedStars,
            completedAt = System.currentTimeMillis(),
            userId = userId,
            sessionId = sessionId,
            isVerified = isVerified
        )
        completedResult = result
        return result
    }

    fun getGameState(): GameState = engine.getState()
}
