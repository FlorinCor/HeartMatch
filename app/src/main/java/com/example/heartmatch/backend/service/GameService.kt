package com.example.heartmatch.backend.service

import com.example.heartmatch.backend.model.CompleteGameRequest
import com.example.heartmatch.backend.model.CompleteGameResponse
import com.example.heartmatch.backend.model.GameSession
import com.example.heartmatch.backend.model.MakeMoveRequest
import com.example.heartmatch.backend.model.MakeMoveResponse
import com.example.heartmatch.backend.model.StartGameRequest
import com.example.heartmatch.backend.model.StartGameResponse
import com.example.heartmatch.backend.repository.GameResultRepository
import com.example.heartmatch.backend.repository.GameSessionRepository
import com.example.heartmatch.backend.repository.PlayerProgressRepository
import com.example.heartmatch.backend.repository.UserRepository
import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.model.Coord
import java.util.UUID

class GameService(
    private val userRepository: UserRepository,
    private val progressRepository: PlayerProgressRepository,
    private val sessionRepository: GameSessionRepository,
    private val resultRepository: GameResultRepository,
    private val levelService: LevelService,
    private val validationService: ValidationService = ValidationService()
) {

    fun startGame(request: StartGameRequest): StartGameResponse {
        userRepository.getOrCreateUser(request.userId)
        val progress = progressRepository.getProgress(request.userId)

        require(request.levelId in 1..100) { "Invalid levelId: ${request.levelId}" }
        if (request.levelId > progress.currentLevel) {
            error("Level ${request.levelId} is locked. Current unlocked level is ${progress.currentLevel}.")
        }

        if (progress.lives <= 0) {
            error("No lives remaining. Please wait for lives to recharge or refill.")
        }

        progressRepository.deductLife(request.userId)
        val currentLives = progressRepository.getProgress(request.userId).lives

        val levelConfig = levelService.getLevelConfig(request.levelId)
        val engine = HeartMatchEngine(levelConfig)

        val sessionId = UUID.randomUUID().toString()
        val session = GameSession(
            sessionId = sessionId,
            userId = request.userId,
            levelId = request.levelId,
            levelConfig = levelConfig,
            engine = engine
        )
        sessionRepository.save(session)

        val levelSummary = com.example.heartmatch.backend.model.LevelSummary.fromConfig(levelConfig)

        return StartGameResponse(
            sessionId = sessionId,
            levelId = request.levelId,
            status = session.status.name,
            movesRemaining = session.movesRemaining,
            lives = currentLives,
            level = levelSummary
        )
    }

    fun makeMove(request: MakeMoveRequest): MakeMoveResponse {
        val session = sessionRepository.get(request.sessionId)
            ?: error("Game session '${request.sessionId}' not found.")

        if (session.isCompleted) {
            error("Game session '${request.sessionId}' is already completed.")
        }

        val from = Coord(request.fromRow, request.fromCol)
        val to = Coord(request.toRow, request.toCol)

        val (isLegal, events) = session.executeMove(from, to)
        val gameState = session.getGameState()

        return MakeMoveResponse(
            sessionId = session.sessionId,
            isLegal = isLegal,
            score = gameState.score,
            movesRemaining = gameState.movesRemaining,
            earnedStars = gameState.earnedStars,
            status = gameState.status.name,
            isWon = gameState.isWon,
            isLost = gameState.isLost,
            eventCount = events.size
        )
    }

    fun completeGame(request: CompleteGameRequest): CompleteGameResponse {
        val session = sessionRepository.get(request.sessionId)
            ?: error("Game session '${request.sessionId}' not found.")

        val validation = validationService.validateSession(
            session = session,
            clientScore = request.clientScore,
            clientStars = request.clientStars,
            clientMovesUsed = request.clientMovesUsed
        )

        val result = validation.verifiedResult
        resultRepository.save(result)

        val updatedProgress = if (result.objectivesCompleted) {
            progressRepository.recordLevelCompletion(
                userId = session.userId,
                levelId = session.levelId,
                stars = result.stars,
                score = result.score
            )
        } else {
            progressRepository.getProgress(session.userId)
        }

        return CompleteGameResponse(
            success = true,
            result = result,
            progress = updatedProgress,
            message = validation.discrepancyReason
        )
    }

    fun getSession(sessionId: String): GameSession? = sessionRepository.get(sessionId)
}
