package com.example.heartmatch.backend

import com.example.heartmatch.backend.model.CompleteGameRequest
import com.example.heartmatch.backend.model.MakeMoveRequest
import com.example.heartmatch.backend.model.StartGameRequest
import com.example.heartmatch.backend.repository.GameResultRepository
import com.example.heartmatch.backend.repository.GameSessionRepository
import com.example.heartmatch.backend.repository.PlayerProgressRepository
import com.example.heartmatch.backend.repository.UserRepository
import com.example.heartmatch.backend.service.GameService
import com.example.heartmatch.backend.service.LevelService
import com.example.heartmatch.backend.service.ValidationService
import com.example.heartmatch.engine.loader.LevelRepository
import com.example.heartmatch.engine.model.Coord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServerSideEngineValidationTest {

    private lateinit var userRepo: UserRepository
    private lateinit var progressRepo: PlayerProgressRepository
    private lateinit var sessionRepo: GameSessionRepository
    private lateinit var resultRepo: GameResultRepository
    private lateinit var levelService: LevelService
    private lateinit var validationService: ValidationService
    private lateinit var gameService: GameService

    @Before
    fun setUp() {
        userRepo = UserRepository()
        progressRepo = PlayerProgressRepository()
        sessionRepo = GameSessionRepository()
        resultRepo = GameResultRepository()
        levelService = LevelService(LevelRepository())
        validationService = ValidationService()
        gameService = GameService(
            userRepository = userRepo,
            progressRepository = progressRepo,
            sessionRepository = sessionRepo,
            resultRepository = resultRepo,
            levelService = levelService,
            validationService = validationService
        )
    }

    @Test
    fun testServerSideEngineSessionSimulation() {
        // Start level 1
        val startRes = gameService.startGame(StartGameRequest(userId = "player_val_1", levelId = 1))
        assertNotNull(startRes.sessionId)
        assertEquals(1, startRes.levelId)
        assertEquals(4, startRes.lives) // 1 life deducted from default 5

        val session = gameService.getSession(startRes.sessionId)
        assertNotNull(session)
        val possibleMoves = session!!.engine.getPossibleMoves()
        assertTrue(possibleMoves.isNotEmpty())

        // Execute a legal move found by the engine
        val (from, to) = possibleMoves.first()
        val moveRes = gameService.makeMove(
            MakeMoveRequest(
                sessionId = startRes.sessionId,
                fromRow = from.row,
                fromCol = from.col,
                toRow = to.row,
                toCol = to.col
            )
        )
        assertTrue(moveRes.isLegal)
        assertTrue(moveRes.score > 0)
        assertEquals(session.currentScore, moveRes.score)

        // Complete game with manipulated client score
        val completeRes = gameService.completeGame(
            CompleteGameRequest(
                sessionId = startRes.sessionId,
                clientScore = 9999999, // Fake score sent by client!
                clientStars = 3
            )
        )

        // Server must reject fake score and store true engine-computed score!
        assertEquals(moveRes.score, completeRes.result.score)
        assertFalse(completeRes.result.isVerified) // flagged as unverified / discrepancy
        assertNotNull(completeRes.message)
        assertTrue(completeRes.message!!.contains("does not match server-calculated score"))
    }

    @Test
    fun testOfflineReplayValidation() {
        val config = levelService.getLevelConfig(1)
        val seed = 42L

        // Find a sequence of legal moves on this level
        val configWithSeed = config.copy(randomSeed = seed)
        val testEngine = com.example.heartmatch.engine.core.HeartMatchEngine(configWithSeed)
        val movesList = mutableListOf<Pair<Coord, Coord>>()

        for (i in 1..5) {
            val moves = testEngine.getPossibleMoves()
            if (moves.isEmpty()) break
            val move = moves.first()
            movesList.add(move)
            testEngine.swap(move.first, move.second)
        }

        val expectedScore = testEngine.getState().score

        // Validate replay with accurate score
        val accurateOutcome = validationService.validateReplay(
            levelConfig = config,
            randomSeed = seed,
            moves = movesList,
            claimedScore = expectedScore
        )
        assertEquals(expectedScore, accurateOutcome.verifiedResult.score)

        // Validate replay with fraudulent score
        val fraudOutcome = validationService.validateReplay(
            levelConfig = config,
            randomSeed = seed,
            moves = movesList,
            claimedScore = expectedScore + 50000
        )
        assertFalse(fraudOutcome.isValid)
        assertNotNull(fraudOutcome.discrepancyReason)
    }

    @Test
    fun testLockedLevelAccessPrevention() {
        // Player starts at level 1, tries to start level 5 immediately
        var threwException = false
        try {
            gameService.startGame(StartGameRequest(userId = "new_player", levelId = 5))
        } catch (e: IllegalStateException) {
            threwException = true
            assertTrue(e.message!!.contains("is locked"))
        }
        assertTrue(threwException)
    }

    @Test
    fun testZeroLivesStartGameRejection() {
        val progress = progressRepo.getProgress("out_of_lives_user")
        progressRepo.saveProgress(progress.copy(lives = 0))

        var threwException = false
        try {
            gameService.startGame(StartGameRequest(userId = "out_of_lives_user", levelId = 1))
        } catch (e: IllegalStateException) {
            threwException = true
            assertTrue(e.message!!.contains("No lives remaining"))
        }
        assertTrue(threwException)
    }
}
