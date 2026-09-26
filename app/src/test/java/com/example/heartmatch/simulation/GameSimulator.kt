package com.example.heartmatch.simulation

import com.example.heartmatch.engine.core.DeterministicRng
import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.core.MatchDetector
import com.example.heartmatch.engine.core.MoveValidator
import com.example.heartmatch.engine.core.SpecialEffectHandler
import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.GameState
import com.example.heartmatch.engine.model.GameStatus
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.LevelConfig
import com.example.heartmatch.engine.model.ObjectiveType
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile

enum class AgentStrategy {
    RANDOM,
    GREEDY,
    STRATEGIC,
    BOOSTER_ASSISTED
}

data class GameSimulationResult(
    val levelId: Int,
    val seed: Long,
    val strategy: AgentStrategy,
    val won: Boolean,
    val lost: Boolean,
    val movesUsed: Int,
    val movesRemaining: Int,
    val initialMoveLimit: Int,
    val finalScore: Int,
    val earnedStars: Int,
    val turnsPlayed: Int,
    val reshuffleCount: Int,
    val specialsCreated: Map<SpecialHeartType, Int>,
    val specialsTriggered: Map<SpecialHeartType, Int>,
    val specialCombosTriggered: Int,
    val totalCascades: Int,
    val maxCascadeChain: Int,
    val blockersDamaged: Map<BlockerType, Int>,
    val blockersDestroyed: Map<BlockerType, Int>,
    val darkHeartsSpreadCount: Int,
    val objectiveCompletionRatio: Double,
    val averageAvailableMoves: Double,
    val boostersUsed: Map<String, Int>
)

data class LevelSimulationSummary(
    val levelId: Int,
    val levelName: String,
    val sampleSize: Int,
    val strategy: AgentStrategy,
    val winRate: Double,
    val averageMovesUsed: Double,
    val averageMovesRemainingOnWin: Double,
    val averageScore: Double,
    val starDistribution: Triple<Double, Double, Double>, // % 1-star, % 2-star, % 3-star
    val zeroStarLossRate: Double,
    val averageObjectiveCompletion: Double,
    val deadBoardRatePerGame: Double,
    val averageSpecialsCreatedPerGame: Double,
    val averageSpecialCombosPerGame: Double,
    val averageCascadesPerTurn: Double,
    val averageAvailableMovesPerTurn: Double,
    val darkHeartSpreadRate: Double,
    val luckDependenceVariance: Double,
    val difficultyDiagnosis: String
)

class GameSimulator {

    private val matchDetector = MatchDetector()
    private val specialEffectHandler = SpecialEffectHandler()
    private val moveValidator = MoveValidator(matchDetector, specialEffectHandler)

    fun simulateGame(
        config: LevelConfig,
        seed: Long,
        strategy: AgentStrategy = AgentStrategy.STRATEGIC,
        maxTurns: Int = 150
    ): GameSimulationResult {
        val simConfig = config.copy(randomSeed = seed)
        val engine = HeartMatchEngine(simConfig)
        val rng = DeterministicRng(seed)

        var reshuffleCount = 0
        val specialsCreated = mutableMapOf<SpecialHeartType, Int>()
        val specialsTriggered = mutableMapOf<SpecialHeartType, Int>()
        var specialCombosTriggered = 0
        var totalCascades = 0
        var maxCascadeChain = 0
        val blockersDamaged = mutableMapOf<BlockerType, Int>()
        val blockersDestroyed = mutableMapOf<BlockerType, Int>()
        var darkHeartsSpread = 0
        var turnCount = 0
        var totalAvailableMovesSum = 0
        val boostersUsed = mutableMapOf<String, Int>()

        engine.addEventListener { event ->
            when (event) {
                is EngineEvent.BoardReshuffled -> reshuffleCount++
                is EngineEvent.SpecialCreated -> {
                    specialsCreated[event.specialType] = (specialsCreated[event.specialType] ?: 0) + 1
                }
                is EngineEvent.SpecialTriggered -> {
                    specialsTriggered[event.specialType] = (specialsTriggered[event.specialType] ?: 0) + 1
                }
                is EngineEvent.Match -> {
                    if (event.comboIndex > 1) {
                        totalCascades++
                        if (event.comboIndex > maxCascadeChain) {
                            maxCascadeChain = event.comboIndex
                        }
                    }
                }
                is EngineEvent.BlockerDamaged -> {
                    blockersDamaged[event.blockerType] = (blockersDamaged[event.blockerType] ?: 0) + 1
                }
                is EngineEvent.BlockerDestroyed -> {
                    blockersDestroyed[event.blockerType] = (blockersDestroyed[event.blockerType] ?: 0) + 1
                }
                is EngineEvent.DarkHeartSpread -> darkHeartsSpread++
                else -> {}
            }
        }

        val initialMoves = config.moveLimit ?: 30
        var simulatedBoostersAvailable = mutableMapOf(
            "HAMMER" to 3,
            "BOMB" to 2,
            "RAINBOW" to 1,
            "EXTRA_MOVES" to 2
        )

        while (engine.getState().status == GameStatus.READY_FOR_INPUT && turnCount < maxTurns) {
            val state = engine.getState()
            val possibleMoves = engine.getPossibleMoves()
            totalAvailableMovesSum += possibleMoves.size

            if (possibleMoves.isEmpty()) {
                // No moves available; engine should have reshuffled or game is stuck
                break
            }

            // Booster logic for BOOSTER_ASSISTED strategy
            if (strategy == AgentStrategy.BOOSTER_ASSISTED) {
                val completion = calculateObjectiveRatio(state)
                if (state.movesRemaining <= 3 && completion >= 0.70 && (simulatedBoostersAvailable["EXTRA_MOVES"] ?: 0) > 0) {
                    engine.addExtraMoves()
                    simulatedBoostersAvailable["EXTRA_MOVES"] = simulatedBoostersAvailable["EXTRA_MOVES"]!! - 1
                    boostersUsed["EXTRA_MOVES"] = (boostersUsed["EXTRA_MOVES"] ?: 0) + 1
                } else if (state.movesRemaining <= 5 && completion in 0.5..0.95 && (simulatedBoostersAvailable["HAMMER"] ?: 0) > 0) {
                    // Try to hammer an annoying blocker
                    val targetBlockerCoord = findBestHammerTarget(state)
                    if (targetBlockerCoord != null) {
                        engine.applyHammer(targetBlockerCoord)
                        simulatedBoostersAvailable["HAMMER"] = simulatedBoostersAvailable["HAMMER"]!! - 1
                        boostersUsed["HAMMER"] = (boostersUsed["HAMMER"] ?: 0) + 1
                    }
                }
            }

            if (engine.getState().status != GameStatus.READY_FOR_INPUT) break
            val refreshedMoves = engine.getPossibleMoves()
            if (refreshedMoves.isEmpty()) break
            val chosenMove = when (strategy) {
                AgentStrategy.RANDOM -> selectRandomMove(refreshedMoves, rng)
                AgentStrategy.GREEDY -> selectGreedyMove(engine, state, refreshedMoves)
                AgentStrategy.STRATEGIC, AgentStrategy.BOOSTER_ASSISTED -> engine.getRankedMoves().first()
            }

            val result = engine.swap(chosenMove.first, chosenMove.second)
            turnCount++

            if (!result.isSuccessfulMove) {
                // If swap failed (e.g. invalid fallback), break to avoid infinite loop
                break
            }
        }

        val finalState = engine.getState()
        val won = finalState.isWon
        val lost = finalState.isLost
        val movesRemaining = finalState.movesRemaining
        val movesUsed = initialMoves - movesRemaining
        val objectiveRatio = calculateObjectiveRatio(finalState)
        val avgAvailableMoves = if (turnCount > 0) totalAvailableMovesSum.toDouble() / turnCount else 0.0

        return GameSimulationResult(
            levelId = config.id,
            seed = seed,
            strategy = strategy,
            won = won,
            lost = lost,
            movesUsed = movesUsed.coerceAtLeast(0),
            movesRemaining = movesRemaining.coerceAtLeast(0),
            initialMoveLimit = initialMoves,
            finalScore = finalState.score,
            earnedStars = finalState.earnedStars,
            turnsPlayed = turnCount,
            reshuffleCount = reshuffleCount,
            specialsCreated = specialsCreated,
            specialsTriggered = specialsTriggered,
            specialCombosTriggered = specialCombosTriggered,
            totalCascades = totalCascades,
            maxCascadeChain = maxCascadeChain,
            blockersDamaged = blockersDamaged,
            blockersDestroyed = blockersDestroyed,
            darkHeartsSpreadCount = darkHeartsSpread,
            objectiveCompletionRatio = objectiveRatio,
            averageAvailableMoves = avgAvailableMoves,
            boostersUsed = boostersUsed
        )
    }

    private fun calculateObjectiveRatio(state: GameState): Double {
        if (state.objectives.isEmpty()) return 1.0
        val ratios = state.objectives.map { obj ->
            if (obj.config.targetCount <= 0) 1.0
            else (obj.currentCount.toDouble() / obj.config.targetCount).coerceAtMost(1.0)
        }
        return ratios.average()
    }

    private fun findBestHammerTarget(state: GameState): Coord? {
        val board = state.board
        var bestCoord: Coord? = null
        var bestPriority = -1

        board.forEachCell { cell ->
            val tile = cell.tile
            if (cell.isPlayable && tile is Tile.Blocker) {
                val priority = when (tile.blockerType) {
                    BlockerType.DARK_HEART -> 100
                    BlockerType.STONE_HEART -> 80
                    BlockerType.BARBED_HEART -> 70
                    BlockerType.WOODEN_HEART -> 60
                    BlockerType.ICE_HEART -> 50
                    BlockerType.CHAINED_HEART -> 40
                    BlockerType.BROKEN_HEART -> 30
                    BlockerType.STITCHED_HEART -> 30
                }
                if (priority > bestPriority) {
                    bestPriority = priority
                    bestCoord = cell.coord
                }
            }
        }
        return bestCoord
    }

    private fun selectRandomMove(possibleMoves: List<Pair<Coord, Coord>>, rng: DeterministicRng): Pair<Coord, Coord> {
        val idx = rng.nextInt(possibleMoves.size)
        return possibleMoves[idx]
    }

    private fun selectGreedyMove(
        engine: HeartMatchEngine,
        state: GameState,
        possibleMoves: List<Pair<Coord, Coord>>
    ): Pair<Coord, Coord> {
        var bestMove = possibleMoves.first()
        var bestScore = -1.0

        for (move in possibleMoves) {
            val score = evaluateMoveBasic(state.board, move.first, move.second)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun selectStrategicMove(
        engine: HeartMatchEngine,
        state: GameState,
        possibleMoves: List<Pair<Coord, Coord>>,
        config: LevelConfig
    ): Pair<Coord, Coord> {
        var bestMove = possibleMoves.first()
        var bestScore = -Double.MAX_VALUE

        val unfulfilledObjectives = state.objectives.filter { !it.isFulfilled }
        val targetColors = unfulfilledObjectives
            .filter { it.config.type == ObjectiveType.COLLECT_COLOR }
            .mapNotNull { it.config.targetColor }
            .toSet()
        val targetBlockers = unfulfilledObjectives
            .filter { it.config.type in setOf(ObjectiveType.DESTROY_BLOCKERS, ObjectiveType.CLEAR_DARK_HEARTS, ObjectiveType.REPAIR_BROKEN) }
            .mapNotNull { it.config.targetBlocker }
            .toSet()

        for (move in possibleMoves) {
            val from = move.first
            val to = move.second
            val tileA = state.board.getTile(from)
            val tileB = state.board.getTile(to)

            var score = 0.0

            // 1. Special combination priority
            if (tileA is Tile.Special && tileB is Tile.Special) {
                val tA = tileA.specialType
                val tB = tileB.specialType
                score += when {
                    tA == SpecialHeartType.RAINBOW_HEART && tB == SpecialHeartType.RAINBOW_HEART -> 5000.0
                    (tA == SpecialHeartType.RAINBOW_HEART && tB == SpecialHeartType.BOMB_HEART) ||
                    (tB == SpecialHeartType.RAINBOW_HEART && tA == SpecialHeartType.BOMB_HEART) -> 4000.0
                    (tA == SpecialHeartType.RAINBOW_HEART && tB == SpecialHeartType.FIRE_HEART) ||
                    (tB == SpecialHeartType.RAINBOW_HEART && tA == SpecialHeartType.FIRE_HEART) -> 3500.0
                    (tA == SpecialHeartType.BOMB_HEART && tB == SpecialHeartType.BOMB_HEART) -> 3000.0
                    (tA == SpecialHeartType.FIRE_HEART && tB == SpecialHeartType.BOMB_HEART) ||
                    (tB == SpecialHeartType.FIRE_HEART && tA == SpecialHeartType.BOMB_HEART) -> 2800.0
                    (tA == SpecialHeartType.FIRE_HEART && tB == SpecialHeartType.FIRE_HEART) -> 2500.0
                    else -> 2000.0
                }
            } else if (tileA is Tile.Special && tileA.specialType == SpecialHeartType.RAINBOW_HEART && tileB is Tile.Normal) {
                // Rainbow with target color
                score += if (tileB.color in targetColors) 2500.0 else 1500.0
            } else if (tileB is Tile.Special && tileB.specialType == SpecialHeartType.RAINBOW_HEART && tileA is Tile.Normal) {
                score += if (tileA.color in targetColors) 2500.0 else 1500.0
            }

            // 2. Clone board and simulate match detection
            val clonedBoard = state.board.clone()
            clonedBoard.swap(from, to)
            val matches = matchDetector.detectMatches(clonedBoard, from to to)

            for (match in matches) {
                val matchSize = match.matchedCoords.size
                // Special creation bonus
                when {
                    matchSize >= 5 -> score += 1200.0 // Rainbow
                    match.shape == com.example.heartmatch.engine.model.MatchShape.T_SHAPE || match.shape == com.example.heartmatch.engine.model.MatchShape.L_SHAPE -> score += 900.0 // Bomb
                    matchSize == 4 -> score += 700.0 // Fire
                    else -> score += 200.0
                }

                // Objective color match bonus
                if (match.color in targetColors) {
                    score += 400.0 * matchSize
                }

                // Blocker damage potential around match coords
                for (coord in match.matchedCoords) {
                    for (neighborCoord in coord.orthogonalNeighbors()) {
                        val neighborCell = state.board[neighborCoord]
                        val neighborTile = neighborCell?.tile
                        if (neighborTile is Tile.Blocker) {
                            if (neighborTile.blockerType == BlockerType.DARK_HEART) {
                                score += 800.0 // Urgent dark heart suppression
                            } else if (neighborTile.blockerType in targetBlockers || targetBlockers.isEmpty()) {
                                score += 450.0
                            } else {
                                score += 200.0
                            }
                        }
                    }
                }
            }

            // 3. Lower board bias for cascade gravity potential (higher row number = lower on board)
            val avgRow = (from.row + to.row) / 2.0
            score += avgRow * 15.0

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        return bestMove
    }

    private fun evaluateMoveBasic(board: Board, from: Coord, to: Coord): Double {
        val clonedBoard = board.clone()
        clonedBoard.swap(from, to)
        val matches = matchDetector.detectMatches(clonedBoard, from to to)
        return matches.sumOf { it.matchedCoords.size * 100 }.toDouble() + (from.row + to.row) * 5.0
    }

    fun analyzeLevel(
        config: LevelConfig,
        sampleSize: Int = 50,
        strategy: AgentStrategy = AgentStrategy.STRATEGIC
    ): LevelSimulationSummary {
        val results = (1..sampleSize).map { seedOffset ->
            val seed = config.randomSeed * 1000L + seedOffset
            simulateGame(config, seed, strategy)
        }

        val wins = results.filter { it.won }
        val winRate = wins.size.toDouble() / sampleSize
        val avgMovesUsed = results.map { it.movesUsed }.average()
        val avgMovesRemainingOnWin = if (wins.isNotEmpty()) wins.map { it.movesRemaining }.average() else 0.0
        val avgScore = results.map { it.finalScore }.average()

        val starCounts = results.map { it.earnedStars }
        val oneStars = starCounts.count { it >= 1 }.toDouble() / sampleSize
        val twoStars = starCounts.count { it >= 2 }.toDouble() / sampleSize
        val threeStars = starCounts.count { it >= 3 }.toDouble() / sampleSize
        val zeroStarLossRate = starCounts.count { it == 0 }.toDouble() / sampleSize

        val avgObjRatio = results.map { it.objectiveCompletionRatio }.average()
        val deadBoardRate = results.map { it.reshuffleCount }.average()
        val avgSpecials = results.map { it.specialsCreated.values.sum() }.average()
        val avgCombos = results.map { it.specialCombosTriggered }.average()
        val avgCascadesPerTurn = results.map {
            if (it.turnsPlayed > 0) it.totalCascades.toDouble() / it.turnsPlayed else 0.0
        }.average()
        val avgAvailableMoves = results.map { it.averageAvailableMoves }.average()
        val darkHeartSpread = results.map { it.darkHeartsSpreadCount }.average()

        // Calculate variance / luck dependence (spread of moves remaining on win and score variance)
        val winScores = if (wins.isNotEmpty()) wins.map { it.finalScore.toDouble() } else listOf(0.0)
        val meanWinScore = winScores.average()
        val scoreStdDev = Math.sqrt(winScores.map { Math.pow(it - meanWinScore, 2.0) }.average())
        val luckVariance = if (meanWinScore > 0) (scoreStdDev / meanWinScore) else 1.0

        val diagnosis = when {
            winRate == 0.0 -> "IMPOSSIBLE"
            winRate < 0.35 -> "TOO_DIFFICULT"
            winRate > 0.98 && avgMovesRemainingOnWin > config.moveLimit!! * 0.5 -> "TOO_EASY"
            avgAvailableMoves < 2.0 -> "BOTTLE_NECK_FRUSTRATING"
            darkHeartSpread > 6.0 && winRate < 0.50 -> "DARK_HEART_RUNAWAY"
            luckVariance > 0.65 && winRate in 0.35..0.65 -> "LUCK_DEPENDENT"
            else -> "WELL_BALANCED"
        }

        return LevelSimulationSummary(
            levelId = config.id,
            levelName = config.name,
            sampleSize = sampleSize,
            strategy = strategy,
            winRate = winRate,
            averageMovesUsed = avgMovesUsed,
            averageMovesRemainingOnWin = avgMovesRemainingOnWin,
            averageScore = avgScore,
            starDistribution = Triple(oneStars, twoStars, threeStars),
            zeroStarLossRate = zeroStarLossRate,
            averageObjectiveCompletion = avgObjRatio,
            deadBoardRatePerGame = deadBoardRate,
            averageSpecialsCreatedPerGame = avgSpecials,
            averageSpecialCombosPerGame = avgCombos,
            averageCascadesPerTurn = avgCascadesPerTurn,
            averageAvailableMovesPerTurn = avgAvailableMoves,
            darkHeartSpreadRate = darkHeartSpread,
            luckDependenceVariance = luckVariance,
            difficultyDiagnosis = diagnosis
        )
    }
}
