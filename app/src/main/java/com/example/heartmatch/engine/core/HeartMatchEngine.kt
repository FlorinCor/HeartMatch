package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.loader.LevelJsonParser
import com.example.heartmatch.engine.loader.LevelValidationResult
import com.example.heartmatch.engine.loader.LevelValidator
import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.GameState
import com.example.heartmatch.engine.model.GameStatus
import com.example.heartmatch.engine.model.LevelConfig
import com.example.heartmatch.engine.model.Objective
import com.example.heartmatch.engine.model.ObjectiveType

class HeartMatchEngine(
    private val levelJsonParser: LevelJsonParser = LevelJsonParser(),
    private val levelValidator: LevelValidator = LevelValidator()
) {

    constructor(config: LevelConfig) : this() {
        loadLevel(config)
    }

    private var rng = DeterministicRng(0L)
    private var matchDetector = MatchDetector()
    private var specialEffectHandler = SpecialEffectHandler(rng)
    private var blockerHandler = BlockerHandler()
    private var gravityManager = GravityManager()
    private lateinit var boardReshuffler: BoardReshuffler
    private lateinit var tileSpawner: TileSpawner
    private lateinit var moveValidator: MoveValidator
    private lateinit var turnPipeline: TurnPipeline

    private var currentGameState: GameState? = null
    private val eventListeners = mutableListOf<(EngineEvent) -> Unit>()

    fun parseLevelJson(jsonString: String): LevelConfig {
        return levelJsonParser.parse(jsonString)
    }

    fun validateLevel(config: LevelConfig): LevelValidationResult {
        return levelValidator.validate(config)
    }

    fun validateLevelJson(jsonString: String): LevelValidationResult {
        val config = levelJsonParser.parse(jsonString)
        return levelValidator.validate(config)
    }

    fun loadLevelFromJson(jsonString: String) {
        val config = levelJsonParser.parse(jsonString)
        loadLevel(config)
    }

    fun loadLevel(config: LevelConfig) {
        // 1. Validate level configuration
        val validation = levelValidator.validate(config)
        validation.assertValid()

        // 2. Initialize engine subcomponents
        rng = DeterministicRng(config.randomSeed)
        matchDetector = MatchDetector()
        specialEffectHandler = SpecialEffectHandler(rng)
        blockerHandler = BlockerHandler()
        gravityManager = GravityManager()
        moveValidator = MoveValidator(matchDetector, specialEffectHandler)
        boardReshuffler = BoardReshuffler(matchDetector, moveValidator)
        tileSpawner = TileSpawner(config.allowedColors, config.colorWeights, rng, boardReshuffler)
        turnPipeline = TurnPipeline(
            matchDetector = matchDetector,
            specialEffectHandler = specialEffectHandler,
            blockerHandler = blockerHandler,
            gravityManager = gravityManager,
            tileSpawner = tileSpawner,
            rng = rng,
            moveValidator = moveValidator,
            boardReshuffler = boardReshuffler,
            allowedColors = config.allowedColors
        )

        // 3. Create board layout
        val board = Board.createWithLayout(
            rows = config.rows,
            cols = config.cols,
            cellStates = config.cellStates,
            initialTiles = config.initialTiles
        )

        // 4. Fill playable cells if needed and guarantee valid initial board without matches and with legal moves
        val hasEmptyPlayableCells = board.getAllPlayableCoords().any { board.getTile(it) == null }
        if (hasEmptyPlayableCells) {
            tileSpawner.fillInitialBoardWithoutMatches(board)
            if (!boardReshuffler.hasValidMoves(board)) {
                boardReshuffler.reshuffle(board, config.allowedColors, rng)
            }
        }

        // 5. Initialize objectives
        var totalInitialTilesOnBoard = 0
        board.forEachCell { cell ->
            if (cell.isPlayable && cell.tile != null) {
                totalInitialTilesOnBoard++
            }
        }

        val objectives = config.objectives.map { objConfig ->
            val adjustedConfig = when {
                objConfig.type == ObjectiveType.CLEAR_BOARD && objConfig.targetCount <= 0 -> {
                    objConfig.copy(targetCount = totalInitialTilesOnBoard)
                }
                objConfig.type == ObjectiveType.CLEAR_SPECIFIC_CELLS && objConfig.targetCount <= 0 && objConfig.targetCells.isNotEmpty() -> {
                    objConfig.copy(targetCount = objConfig.targetCells.size)
                }
                else -> objConfig
            }
            Objective(adjustedConfig)
        }

        currentGameState = GameState(
            levelId = config.id,
            status = GameStatus.READY_FOR_INPUT,
            movesRemaining = config.moveLimit ?: 30,
            timeRemainingSeconds = config.timeLimitSeconds,
            score = 0,
            comboCount = 0,
            earnedStars = 0,
            board = board,
            objectives = objectives,
            starThresholds = config.starThresholds
        )
    }

    fun swap(from: Coord, to: Coord): TurnPipeline.TurnExecutionResult {
        val state = currentGameState ?: error("No level loaded. Call loadLevel first.")
        if (state.status != GameStatus.READY_FOR_INPUT) {
            return TurnPipeline.TurnExecutionResult(emptyList(), 0, 0, false)
        }

        val result = turnPipeline.executeSwap(state, from, to)
        result.events.forEach { event ->
            notifyListeners(event)
        }
        return result
    }

    fun getState(): GameState {
        return currentGameState ?: error("No level loaded. Call loadLevel first.")
    }

    fun getPossibleMoves(): List<Pair<Coord, Coord>> {
        val state = currentGameState ?: return emptyList()
        return moveValidator.findPossibleMoves(state.board)
    }

    fun addEventListener(listener: (EngineEvent) -> Unit): () -> Unit {
        eventListeners.add(listener)
        return { eventListeners.remove(listener) }
    }

    private fun notifyListeners(event: EngineEvent) {
        eventListeners.forEach { it(event) }
    }
}
