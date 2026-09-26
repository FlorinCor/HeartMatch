package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.loader.LevelJsonParser
import com.example.heartmatch.engine.loader.LevelValidationResult
import com.example.heartmatch.engine.loader.LevelValidator
import com.example.heartmatch.engine.model.Tile
import com.example.heartmatch.engine.model.SpecialHeartType
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

    /** Reshuffles the current board without resetting moves, score, or objectives. */
    fun reshuffleCurrentBoard(): Boolean {
        val state = currentGameState ?: return false
        if (state.status != GameStatus.READY_FOR_INPUT) return false
        return turnPipeline.reshuffleCurrentBoard(state.board)
    }

    fun applyHammer(coord: Coord): List<EngineEvent> {
        val state = currentGameState ?: return emptyList()
        val events = turnPipeline.executeHammer(state, coord)
        events.forEach(::notifyListeners)
        return events
    }

    fun addExtraMoves(): Boolean {
        val state = currentGameState ?: return false
        if (state.status !in listOf(GameStatus.READY_FOR_INPUT, GameStatus.GAME_OVER) || state.isWon ||
            (state.timeRemainingSeconds != null && state.timeRemainingSeconds!! <= 0)) return false
        state.movesRemaining += 5
        state.extraMovesGranted += 5
        state.status = GameStatus.READY_FOR_INPUT
        return true
    }

    fun placeBooster(type: SpecialHeartType, coord: Coord): Boolean {
        val state = currentGameState ?: return false
        if (state.status != GameStatus.READY_FOR_INPUT || type !in listOf(SpecialHeartType.BOMB_HEART, SpecialHeartType.RAINBOW_HEART)) return false
        val tile = state.board[coord]?.takeIf { it.isPlayable }?.tile as? Tile.Normal ?: return false
        state.board.setTile(coord, Tile.Special(specialType = type, baseColor = tile.color))
        return true
    }

    fun previewTarget(coord: Coord): Set<Coord> {
        val state = getState()
        val tile = state.board.getTile(coord) as? Tile.Special ?: return setOf(coord)
        val handler = SpecialEffectHandler(DeterministicRng(0))
        handler.objectives = state.objectives.filterNot { it.isFulfilled }.map { it.config }
        val cleared = handler.triggerSpecials(state.board.clone(), listOf(coord to tile)).clearedCoords
        return cleared + cleared.flatMap { it.orthogonalNeighbors() }.filter { state.board.getTile(it) is Tile.Blocker }
    }

    /** Evaluate immediate objective progress without consuming the live random stream. */
    fun getRankedMoves(): List<Pair<Coord, Coord>> {
        val state = getState()
        val handler = SpecialEffectHandler(DeterministicRng(0))
        handler.objectives = state.objectives.filterNot { it.isFulfilled }.map { it.config }
        return getPossibleMoves().sortedByDescending { (from, to) ->
            val board = state.board.clone()
            val a = board.getTile(from)!!
            val b = board.getTile(to)!!
            var creationValue = 0
            val affected = if (handler.isSpecialInteraction(a, b)) {
                handler.handleSpecialSwap(board, from, to, a, b).clearedCoords
            } else {
                board.swap(from, to)
                val matches = matchDetector.detectMatches(board, from to to)
                creationValue = matches.sumOf { group ->
                    if (group.createdSpecial == null) 0 else 20 + if (state.objectives.any {
                        !it.isFulfilled && it.config.type in listOf(ObjectiveType.CREATE_SPECIALS, ObjectiveType.COLLECT_SPECIAL) &&
                            (it.config.targetSpecial == null || it.config.targetSpecial == group.createdSpecial)
                    }) 100 else 0
                }
                matches.flatMap { it.matchedCoords }.toSet()
            }
            creationValue + affected.sumOf { handler.targetPriority(board, it) + 1 } +
                affected.flatMap { listOf(Coord(it.row - 1, it.col), Coord(it.row + 1, it.col), Coord(it.row, it.col - 1), Coord(it.row, it.col + 1)) }.distinct().filter { board.getTile(it) is Tile.Blocker }
                    .sumOf { handler.targetPriority(board, it) + 20 }
        }
    }

    fun addEventListener(listener: (EngineEvent) -> Unit): () -> Unit {
        eventListeners.add(listener)
        return { eventListeners.remove(listener) }
    }

    private fun notifyListeners(event: EngineEvent) {
        eventListeners.forEach { it(event) }
    }
}
