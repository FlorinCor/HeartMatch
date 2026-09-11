package com.example.heartmatch.backend.service

import com.example.heartmatch.backend.model.GameResult
import com.example.heartmatch.backend.model.GameSession
import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.GameStatus
import com.example.heartmatch.engine.model.LevelConfig

data class ValidationOutcome(
    val isValid: Boolean,
    val verifiedResult: GameResult,
    val discrepancyReason: String? = null
)

class ValidationService {

    /**
     * Validates a live session by comparing client-provided metrics against the server-side engine state.
     * Never trusts client score or stars; always enforces server-side authoritative state.
     */
    fun validateSession(
        session: GameSession,
        clientScore: Int? = null,
        clientStars: Int? = null,
        clientMovesUsed: Int? = null
    ): ValidationOutcome {
        val serverState = session.getGameState()
        val serverScore = serverState.score
        val serverStars = serverState.earnedStars
        val serverMovesUsed = session.movesUsed
        val serverWon = serverState.isWon || session.isObjectivesCompleted

        var discrepancy: String? = null
        var isValid = true

        if (clientScore != null && clientScore != serverScore) {
            discrepancy = "Client reported score ($clientScore) does not match server-calculated score ($serverScore)."
            isValid = false
        } else if (clientStars != null && clientStars != serverStars) {
            discrepancy = "Client reported stars ($clientStars) do not match server-calculated stars ($serverStars)."
            isValid = false
        } else if (clientMovesUsed != null && clientMovesUsed != serverMovesUsed) {
            discrepancy = "Client reported moves used ($clientMovesUsed) does not match server moves ($serverMovesUsed)."
            isValid = false
        }

        val verifiedResult = session.completeSession(isVerified = isValid)

        return ValidationOutcome(
            isValid = isValid,
            verifiedResult = verifiedResult,
            discrepancyReason = discrepancy
        )
    }

    /**
     * Runs an offline replay simulation using the server-side HeartMatchEngine to validate a move sequence.
     */
    fun validateReplay(
        levelConfig: LevelConfig,
        randomSeed: Long,
        moves: List<Pair<Coord, Coord>>,
        claimedScore: Int? = null,
        claimedStars: Int? = null
    ): ValidationOutcome {
        val configWithSeed = levelConfig.copy(randomSeed = randomSeed)
        val engine = HeartMatchEngine(configWithSeed)

        var legalMovesCount = 0
        for ((from, to) in moves) {
            val res = engine.swap(from, to)
            if (res.isSuccessfulMove) {
                legalMovesCount++
            }
        }

        val state = engine.getState()
        val isWon = state.isWon || (state.objectives.isNotEmpty() && state.objectives.all { it.isFulfilled })
        val isScoreMatch = claimedScore == null || claimedScore == state.score
        val isStarsMatch = claimedStars == null || claimedStars == state.earnedStars

        val isValid = isWon && isScoreMatch && isStarsMatch
        val discrepancy = when {
            !isWon && claimedScore != null -> "Simulation did not fulfill level objectives or win condition."
            !isScoreMatch -> "Claimed score $claimedScore differs from simulated score ${state.score}."
            !isStarsMatch -> "Claimed stars $claimedStars differs from simulated stars ${state.earnedStars}."
            else -> null
        }

        val verifiedResult = GameResult(
            levelId = levelConfig.id,
            score = state.score,
            movesUsed = legalMovesCount,
            objectivesCompleted = isWon,
            stars = state.earnedStars,
            completedAt = System.currentTimeMillis(),
            isVerified = isValid
        )

        return ValidationOutcome(
            isValid = isValid,
            verifiedResult = verifiedResult,
            discrepancyReason = discrepancy
        )
    }
}
