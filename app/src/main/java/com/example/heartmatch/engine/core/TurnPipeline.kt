package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.GameState
import com.example.heartmatch.engine.model.GameStatus
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.MatchGroup
import com.example.heartmatch.engine.model.ObjectiveType
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile

class TurnPipeline(
    private val matchDetector: MatchDetector = MatchDetector(),
    private val specialEffectHandler: SpecialEffectHandler = SpecialEffectHandler(),
    private val blockerHandler: BlockerHandler = BlockerHandler(),
    private val gravityManager: GravityManager = GravityManager(),
    private val tileSpawner: TileSpawner,
    private val rng: DeterministicRng = DeterministicRng(),
    private val moveValidator: MoveValidator = MoveValidator(matchDetector, specialEffectHandler),
    private val boardReshuffler: BoardReshuffler = BoardReshuffler(matchDetector, moveValidator),
    private val allowedColors: List<HeartColor> = listOf(
        HeartColor.RED,
        HeartColor.PINK,
        HeartColor.BLUE,
        HeartColor.GREEN,
        HeartColor.YELLOW
    )
) {

    data class TurnExecutionResult(
        val events: List<EngineEvent>,
        val scoreDelta: Int,
        val combosAchieved: Int,
        val isSuccessfulMove: Boolean
    )

    fun executeSwap(
        gameState: GameState,
        from: Coord,
        to: Coord
    ): TurnExecutionResult {
        val board = gameState.board
        val events = mutableListOf<EngineEvent>()
        var totalScoreDelta = 0
        var darkHeartDamagedThisTurn = false

        val cellFrom = board[from]
        val cellTo = board[to]

        if (cellFrom == null || cellTo == null || !cellFrom.isPlayable || !cellTo.isPlayable) {
            return TurnExecutionResult(emptyList(), 0, 0, false)
        }

        val tileFrom = cellFrom.tile
        val tileTo = cellTo.tile

        if (tileFrom == null || tileTo == null || !tileFrom.isMovable || !tileTo.isMovable) {
            return TurnExecutionResult(emptyList(), 0, 0, false)
        }

        // 1. Check special interaction swap
        val isSpecialSwap = specialEffectHandler.isSpecialInteraction(tileFrom, tileTo)

        if (isSpecialSwap) {
            // Deduct move
            gameState.movesRemaining--
            gameState.status = GameStatus.RESOLVING

            events.add(EngineEvent.Swap(from, to, isRollback = false))

            // Execute special activation
            val specResult = specialEffectHandler.handleSpecialSwap(board, from, to, tileFrom, tileTo)
            events.addAll(specResult.triggeredEvents)

            // Direct damage to blockers
            val blockerResult = blockerHandler.applyBlockerDamage(board, emptySet(), specResult.clearedCoords)
            events.addAll(blockerResult.events)
            if (blockerResult.damagedBlockers.any { it.second.blockerType == BlockerType.DARK_HEART } ||
                blockerResult.destroyedBlockers.any { it.second.blockerType == BlockerType.DARK_HEART }
            ) {
                darkHeartDamagedThisTurn = true
            }

            for (damaged in blockerResult.damagedBlockers) {
                if (damaged.second.blockerType == BlockerType.BROKEN_HEART) {
                    updateBrokenHeartRepairedObjectives(gameState, 1)
                }
            }

            for (destroyed in blockerResult.destroyedBlockers) {
                updateBlockerObjectives(gameState, destroyed.second.blockerType)
                if (destroyed.second.blockerType == BlockerType.BROKEN_HEART) {
                    updateBrokenHeartRepairedObjectives(gameState, 1)
                }
            }

            // Update cell clearing and board clearing objectives
            updateSpecificCellObjectives(gameState, specResult.clearedCoords)
            updateClearBoardObjectives(gameState, specResult.clearedCoords.size)

            // Clear cleared tiles (except blockers with payload or remaining durability)
            for (coord in specResult.clearedCoords) {
                val tile = board.getTile(coord)
                if (tile !is Tile.Blocker) {
                    board.setTile(coord, null)
                }
            }

            val scoreFromSpecial = specResult.clearedCoords.size * 120 + specResult.bonusScore
            totalScoreDelta += scoreFromSpecial
            updateGiftObjectives(gameState, specResult.giftHeartsClearedCount)

            // Apply gravity and spawn
            val dropEvents = gravityManager.applyGravity(board)
            events.addAll(dropEvents)
            val spawnEvents = tileSpawner.spawnNewTiles(board)
            events.addAll(spawnEvents)

            // Continue to cascade loop
            val cascadeResult = runCascadeLoop(
                gameState = gameState,
                initialCombo = 1,
                playerSwappedCoords = null,
                initialDarkHeartDamaged = darkHeartDamagedThisTurn
            )
            events.addAll(cascadeResult.events)
            totalScoreDelta += cascadeResult.scoreDelta
            darkHeartDamagedThisTurn = cascadeResult.darkHeartDamaged

            finishTurn(gameState, events, totalScoreDelta, darkHeartDamagedThisTurn)
            return TurnExecutionResult(events, totalScoreDelta, cascadeResult.combos, true)
        }

        // 2. Normal swap: execute on board
        board.swap(from, to)
        val initialMatches = matchDetector.detectMatches(board, from to to)

        if (initialMatches.isEmpty()) {
            // Rollback swap
            board.swap(from, to)
            events.add(EngineEvent.Swap(from, to, isRollback = true))
            return TurnExecutionResult(events, 0, 0, false)
        }

        // Swap is valid!
        gameState.movesRemaining--
        gameState.status = GameStatus.RESOLVING
        events.add(EngineEvent.Swap(from, to, isRollback = false))

        // Run cascade loop
        val cascadeResult = runCascadeLoop(
            gameState = gameState,
            initialCombo = 0,
            playerSwappedCoords = from to to,
            initialDarkHeartDamaged = false
        )
        events.addAll(cascadeResult.events)
        totalScoreDelta += cascadeResult.scoreDelta
        darkHeartDamagedThisTurn = cascadeResult.darkHeartDamaged

        finishTurn(gameState, events, totalScoreDelta, darkHeartDamagedThisTurn)
        return TurnExecutionResult(events, totalScoreDelta, cascadeResult.combos, true)
    }

    private data class CascadeLoopResult(
        val events: List<EngineEvent>,
        val scoreDelta: Int,
        val combos: Int,
        val darkHeartDamaged: Boolean
    )

    private fun runCascadeLoop(
        gameState: GameState,
        initialCombo: Int,
        playerSwappedCoords: Pair<Coord, Coord>?,
        initialDarkHeartDamaged: Boolean
    ): CascadeLoopResult {
        val board = gameState.board
        val events = mutableListOf<EngineEvent>()
        var combo = initialCombo
        var totalScoreDelta = 0
        var darkHeartDamaged = initialDarkHeartDamaged
        var currentSwap = playerSwappedCoords

        var cascadeCount = 0
        val maxCascades = 100

        while (cascadeCount < maxCascades) {
            cascadeCount++
            val matches = matchDetector.detectMatches(board, currentSwap)
            if (matches.isEmpty()) break

            combo++
            var stepScore = 0
            val allMatchedCoords = mutableSetOf<Coord>()
            val createdSpecials = mutableMapOf<Coord, Tile.Special>()
            val specialsToTrigger = mutableListOf<Pair<Coord, Tile.Special>>()

            for (group in matches) {
                events.add(
                    EngineEvent.Match(
                        coords = group.matchedCoords,
                        color = group.color,
                        shape = group.shape,
                        comboIndex = combo
                    )
                )

                // Track objective for matched color
                updateHeartCollectionObjectives(gameState, group.color, group.matchedCoords.size)

                // Check special creation
                if (group.createdSpecial != null && group.specialSpawnCoord != null) {
                    val specialTile = Tile.Special(
                        specialType = group.createdSpecial,
                        baseColor = group.color,
                        fireDirection = group.createdSpecialDirection,
                        bombRadius = 1
                    )
                    createdSpecials[group.specialSpawnCoord] = specialTile
                    events.add(EngineEvent.SpecialCreated(group.specialSpawnCoord, group.createdSpecial))
                    updateSpecialCreationObjectives(gameState, group.createdSpecial)
                }

                allMatchedCoords.addAll(group.matchedCoords)
                stepScore += group.matchedCoords.size * 100
                if (group.createdSpecial != null) {
                    stepScore += 200
                }
            }

            // Check if any existing specials were inside the matched coords
            for (coord in allMatchedCoords) {
                val tile = board.getTile(coord)
                if (tile is Tile.Special && coord !in createdSpecials) {
                    specialsToTrigger.add(coord to tile)
                }
            }

            // Apply special effects if any specials were triggered
            val directHitCoords = mutableSetOf<Coord>()
            if (specialsToTrigger.isNotEmpty()) {
                val specResult = specialEffectHandler.triggerSpecials(board, specialsToTrigger, allMatchedCoords)
                events.addAll(specResult.triggeredEvents)
                directHitCoords.addAll(specResult.clearedCoords)
                stepScore += specResult.clearedCoords.size * 100 + specResult.bonusScore
                updateGiftObjectives(gameState, specResult.giftHeartsClearedCount)
            }

            // Apply blocker damage
            val blockerResult = blockerHandler.applyBlockerDamage(board, allMatchedCoords, directHitCoords)
            events.addAll(blockerResult.events)

            if (blockerResult.damagedBlockers.any { it.second.blockerType == BlockerType.DARK_HEART } ||
                blockerResult.destroyedBlockers.any { it.second.blockerType == BlockerType.DARK_HEART }
            ) {
                darkHeartDamaged = true
            }

            for (damaged in blockerResult.damagedBlockers) {
                if (damaged.second.blockerType == BlockerType.BROKEN_HEART) {
                    updateBrokenHeartRepairedObjectives(gameState, 1)
                }
            }

            for (destroyed in blockerResult.destroyedBlockers) {
                updateBlockerObjectives(gameState, destroyed.second.blockerType)
                if (destroyed.second.blockerType == BlockerType.BROKEN_HEART) {
                    updateBrokenHeartRepairedObjectives(gameState, 1)
                }
                stepScore += 150
            }

            // Update specific cell and clear board objectives
            val allAffected = allMatchedCoords + directHitCoords
            updateSpecificCellObjectives(gameState, allAffected)
            updateClearBoardObjectives(gameState, allAffected.size)

            // Remove cleared hearts from board (placing created specials in their designated positions)
            for (coord in allAffected) {
                if (coord in createdSpecials) {
                    board.setTile(coord, createdSpecials[coord])
                } else {
                    val currentTile = board.getTile(coord)
                    if (currentTile !is Tile.Blocker) {
                        board.setTile(coord, null)
                    }
                }
            }

            // Combo multiplier on step score
            val multiplier = 1.0 + (combo - 1) * 0.5
            val finalStepScore = (stepScore * multiplier).toInt()
            totalScoreDelta += finalStepScore

            // Apply gravity
            val dropEvents = gravityManager.applyGravity(board)
            events.addAll(dropEvents)

            // Spawn new tiles
            val spawnEvents = tileSpawner.spawnNewTiles(board)
            events.addAll(spawnEvents)

            currentSwap = null
        }

        return CascadeLoopResult(events, totalScoreDelta, combo, darkHeartDamaged)
    }

    private fun finishTurn(
        gameState: GameState,
        events: MutableList<EngineEvent>,
        scoreDelta: Int,
        darkHeartDamagedThisTurn: Boolean
    ) {
        // Dark heart spread check
        val spreadEvent = blockerHandler.processDarkHeartSpread(gameState.board, darkHeartDamagedThisTurn, rng)
        if (spreadEvent != null) {
            events.add(spreadEvent)
        }

        // Update score
        gameState.score += scoreDelta
        updateStars(gameState)
        events.add(EngineEvent.ScoreChanged(gameState.score, scoreDelta, gameState.comboCount))

        // Check score objectives
        updateScoreObjectives(gameState)

        // Check game status (won / lost / ready)
        when {
            gameState.isWon -> {
                gameState.status = GameStatus.OBJECTIVE_COMPLETED
                events.add(EngineEvent.GameWon(gameState.score, gameState.earnedStars))
            }
            gameState.isLost -> {
                gameState.status = GameStatus.GAME_OVER
                events.add(EngineEvent.GameOver(if (gameState.movesRemaining <= 0) "OUT_OF_MOVES" else "OUT_OF_TIME"))
            }
            else -> {
                gameState.status = GameStatus.READY_FOR_INPUT

                // Dead-board detection and automatic reshuffle
                if (!boardReshuffler.hasValidMoves(gameState.board)) {
                    val reshuffled = boardReshuffler.reshuffle(gameState.board, allowedColors, rng)
                    if (reshuffled) {
                        events.add(EngineEvent.BoardReshuffled("NO_VALID_MOVES"))
                    }
                }
            }
        }
    }

    private fun updateStars(gameState: GameState) {
        val (s1, s2, s3) = gameState.starThresholds
        gameState.earnedStars = when {
            gameState.score >= s3 -> 3
            gameState.score >= s2 -> 2
            gameState.score >= s1 -> 1
            else -> 0
        }
    }

    private fun updateHeartCollectionObjectives(gameState: GameState, color: HeartColor, count: Int) {
        for (obj in gameState.objectives) {
            if ((obj.config.type == ObjectiveType.COLLECT_COLOR || obj.config.type == ObjectiveType.COLLECT_HEARTS) &&
                (obj.config.targetColor == null || obj.config.targetColor == color)
            ) {
                obj.currentCount += count
            }
        }
    }

    private fun updateSpecialCreationObjectives(gameState: GameState, specialType: SpecialHeartType) {
        for (obj in gameState.objectives) {
            if ((obj.config.type == ObjectiveType.COLLECT_SPECIAL || obj.config.type == ObjectiveType.CREATE_SPECIALS) &&
                (obj.config.targetSpecial == null || obj.config.targetSpecial == specialType)
            ) {
                obj.currentCount++
            }
        }
    }

    private fun updateBlockerObjectives(gameState: GameState, blockerType: BlockerType) {
        for (obj in gameState.objectives) {
            if ((obj.config.type == ObjectiveType.DESTROY_BLOCKERS || obj.config.type == ObjectiveType.CLEAR_BLOCKER) &&
                (obj.config.targetBlocker == null || obj.config.targetBlocker == blockerType)
            ) {
                obj.currentCount++
            }
            if (obj.config.type == ObjectiveType.CLEAR_DARK_HEARTS && blockerType == BlockerType.DARK_HEART) {
                obj.currentCount++
            }
        }
    }

    private fun updateBrokenHeartRepairedObjectives(gameState: GameState, count: Int) {
        for (obj in gameState.objectives) {
            if (obj.config.type == ObjectiveType.REPAIR_BROKEN) {
                obj.currentCount += count
            }
        }
    }

    private fun updateSpecificCellObjectives(gameState: GameState, affectedCoords: Set<Coord>) {
        for (obj in gameState.objectives) {
            if (obj.config.type == ObjectiveType.CLEAR_SPECIFIC_CELLS) {
                val matched = affectedCoords.intersect(obj.config.targetCells)
                if (matched.isNotEmpty()) {
                    obj.clearedCells.addAll(matched)
                    obj.currentCount = obj.clearedCells.size
                }
            }
        }
    }

    private fun updateClearBoardObjectives(gameState: GameState, clearedCount: Int) {
        for (obj in gameState.objectives) {
            if (obj.config.type == ObjectiveType.CLEAR_BOARD) {
                obj.currentCount += clearedCount
            }
        }
    }

    private fun updateScoreObjectives(gameState: GameState) {
        for (obj in gameState.objectives) {
            if (obj.config.type == ObjectiveType.SCORE || obj.config.type == ObjectiveType.REACH_SCORE) {
                obj.currentCount = gameState.score
            }
        }
    }

    private fun updateGiftObjectives(gameState: GameState, count: Int) {
        if (count <= 0) return
        for (obj in gameState.objectives) {
            if (obj.config.type == ObjectiveType.COLLECT_GIFT ||
                ((obj.config.type == ObjectiveType.COLLECT_SPECIAL || obj.config.type == ObjectiveType.COLLECT_HEARTS) &&
                        obj.config.targetSpecial == SpecialHeartType.GIFT_HEART)
            ) {
                obj.currentCount += count
            }
        }
    }
}
