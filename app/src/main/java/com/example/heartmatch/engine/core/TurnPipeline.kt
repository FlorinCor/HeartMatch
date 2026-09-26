package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.*

/** All clears, including free tools, share objective accounting and cascade resolution. */
class TurnPipeline(
    private val matchDetector: MatchDetector = MatchDetector(),
    private val specialEffectHandler: SpecialEffectHandler = SpecialEffectHandler(),
    private val blockerHandler: BlockerHandler = BlockerHandler(),
    private val gravityManager: GravityManager = GravityManager(),
    private val tileSpawner: TileSpawner,
    private val rng: DeterministicRng = DeterministicRng(),
    private val moveValidator: MoveValidator = MoveValidator(matchDetector, specialEffectHandler),
    private val boardReshuffler: BoardReshuffler = BoardReshuffler(matchDetector, moveValidator),
    private val allowedColors: List<HeartColor> = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)
) {
    data class TurnExecutionResult(val events: List<EngineEvent>, val scoreDelta: Int, val combosAchieved: Int, val isSuccessfulMove: Boolean)
    private data class Impact(val points: Int, val damagedDark: Boolean)

    fun executeSwap(state: GameState, from: Coord, to: Coord): TurnExecutionResult {
        if (state.status != GameStatus.READY_FOR_INPUT || state.movesRemaining <= 0 || !from.isAdjacentTo(to))
            return TurnExecutionResult(emptyList(), 0, 0, false)
        val board = state.board
        val a = board[from]?.takeIf { it.isPlayable }?.tile
        val b = board[to]?.takeIf { it.isPlayable }?.tile
        if (a == null || b == null || !a.isMovable || !b.isMovable) return TurnExecutionResult(emptyList(), 0, 0, false)
        prepareTargets(state)
        val events = mutableListOf<EngineEvent>()
        val startingScore = state.score
        state.comboCount = 0
        var darkDamaged = false
        if (specialEffectHandler.isSpecialInteraction(a, b)) {
            state.movesRemaining--
            state.status = GameStatus.RESOLVING
            events += EngineEvent.Swap(from, to, false)
            val before = board.clone()
            val effect = specialEffectHandler.handleSpecialSwap(board, from, to, a, b)
            val impact = resolveImpact(state, before, emptySet(), emptyMap(), effect, 1, events)
            darkDamaged = impact.damagedDark
            state.comboCount = 1
            drop(state, events)
        } else {
            board.swap(from, to)
            if (matchDetector.detectMatches(board, from to to).isEmpty()) {
                board.swap(from, to)
                return TurnExecutionResult(listOf(EngineEvent.Swap(from, to, true)), 0, 0, false)
            }
            state.movesRemaining--
            state.status = GameStatus.RESOLVING
            events += EngineEvent.Swap(from, to, false)
        }
        darkDamaged = cascade(state, events, if (state.comboCount == 0) from to to else null) || darkDamaged
        finishTurn(state, events, startingScore, darkDamaged, advanceHazards = true)
        return TurnExecutionResult(events, state.score - startingScore, state.comboCount, true)
    }

    fun executeHammer(state: GameState, coord: Coord): List<EngineEvent> {
        val tile = state.board[coord]?.takeIf { it.isPlayable }?.tile ?: return emptyList()
        if (state.status != GameStatus.READY_FOR_INPUT) return emptyList()
        prepareTargets(state)
        val events = mutableListOf<EngineEvent>()
        val startScore = state.score
        state.status = GameStatus.RESOLVING
        state.comboCount = 1
        val before = state.board.clone()
        val effect = if (tile is Tile.Special) specialEffectHandler.triggerSpecials(state.board, listOf(coord to tile))
            else SpecialEffectHandler.SpecialActivationResult(setOf(coord), emptyList())
        // A hammer strikes exactly its target; a struck special still produces its usual blast.
        resolveImpact(state, before, emptySet(), emptyMap(), effect, 1, events, adjacentDamage = tile is Tile.Special)
        drop(state, events)
        cascade(state, events, null)
        finishTurn(state, events, startScore, false, advanceHazards = false)
        return events
    }

    fun reshuffleCurrentBoard(board: Board): Boolean = boardReshuffler.reshuffle(board, allowedColors, rng)

    private fun prepareTargets(state: GameState) {
        specialEffectHandler.objectives = state.objectives.filterNot { it.isFulfilled }.map { it.config }
    }

    private fun cascade(state: GameState, events: MutableList<EngineEvent>, swapped: Pair<Coord, Coord>?): Boolean {
        var swap = swapped
        var darkDamaged = false
        repeat(100) {
            val matches = matchDetector.detectMatches(state.board, swap)
            if (matches.isEmpty()) return darkDamaged
            state.comboCount++
            val before = state.board.clone()
            val matched = matches.flatMap { it.matchedCoords }.toSet()
            val created = mutableMapOf<Coord, Tile.Special>()
            for (group in matches) {
                events += EngineEvent.Match(group.matchedCoords, group.color, group.shape, state.comboCount)
                val type = group.createdSpecial
                val coord = group.specialSpawnCoord?.takeIf { before.getTile(it) is Tile.Normal }
                    ?: group.matchedCoords.firstOrNull { before.getTile(it) is Tile.Normal }
                if (type != null && coord != null) {
                    created[coord] = Tile.Special(specialType = type, baseColor = group.color, fireDirection = group.createdSpecialDirection)
                    events += EngineEvent.SpecialCreated(coord, type)
                    state.objectives.filter { it.config.type in listOf(ObjectiveType.CREATE_SPECIALS, ObjectiveType.COLLECT_SPECIAL) && it.config.targetSpecial != SpecialHeartType.GIFT_HEART && (it.config.targetSpecial == null || it.config.targetSpecial == type) }.forEach { advance(it, 1) }
                }
            }
            val triggers = matched.filterNot { it in created }.mapNotNull { c -> (before.getTile(c) as? Tile.Special)?.let { c to it } }
            val effect = specialEffectHandler.triggerSpecials(state.board, triggers, matched)
            val impact = resolveImpact(state, before, matched, created, effect, state.comboCount, events)
            darkDamaged = darkDamaged || impact.damagedDark
            drop(state, events)
            swap = null
        }
        // An unusually long chain must leave a stable board, not an unrelated free match.
        boardReshuffler.reshuffle(state.board, allowedColors, rng)
        events += EngineEvent.BoardReshuffled("CASCADE_LIMIT")
        return darkDamaged
    }

    private fun resolveImpact(
        state: GameState, before: Board, matched: Set<Coord>, created: Map<Coord, Tile.Special>,
        effect: SpecialEffectHandler.SpecialActivationResult, combo: Int, events: MutableList<EngineEvent>,
        adjacentDamage: Boolean = true
    ): Impact {
        events += effect.triggeredEvents
        val lightHits = effect.triggeredEvents.filter { it.specialType == SpecialHeartType.LIGHT_HEART }.flatMap { it.affectedCoords }.toSet()
        effect.triggeredEvents.filter { it.specialType == SpecialHeartType.LIGHT_HEART }.forEach { events += EngineEvent.LightHeartActivated(it.affectedCoords.toList()) }
        val affected = matched + effect.clearedCoords
        val damage = blockerHandler.applyBlockerDamage(state.board, matched, effect.clearedCoords, lightHits, adjacentDamage)
        events += damage.events
        val hearts = affected.filter { before.getTile(it) != null && before.getTile(it) !is Tile.Blocker }
        // Read colours before rainbow conversion and count each affected heart once.
        for (coord in hearts) {
            val tile = before.getTile(coord)!!
            state.objectives.filter {
                it.config.type in listOf(ObjectiveType.COLLECT_COLOR, ObjectiveType.COLLECT_HEARTS) && it.config.targetSpecial == null &&
                    (it.config.targetColor == null || it.config.targetColor == tile.matchColor)
            }.forEach { advance(it, 1) }
        }
        for ((_, blocker) in damage.destroyedBlockers) {
            state.objectives.filter {
                when (it.config.type) {
                    ObjectiveType.DESTROY_BLOCKERS, ObjectiveType.CLEAR_BLOCKER -> it.config.targetBlocker == null || it.config.targetBlocker == blocker.blockerType
                    ObjectiveType.CLEAR_DARK_HEARTS -> blocker.blockerType == BlockerType.DARK_HEART
                    ObjectiveType.REPAIR_BROKEN -> blocker.blockerType in listOf(BlockerType.BROKEN_HEART, BlockerType.STITCHED_HEART)
                    else -> false
                }
            }.forEach { advance(it, 1) }
        }
        val gifts = hearts.count { (before.getTile(it) as? Tile.Special)?.specialType == SpecialHeartType.GIFT_HEART }
        state.objectives.filter { it.config.type == ObjectiveType.COLLECT_GIFT || it.config.targetSpecial == SpecialHeartType.GIFT_HEART }.forEach { advance(it, gifts) }
        val cleared = hearts.toSet() + damage.destroyedBlockers.map { it.first }
        state.objectives.forEach { obj ->
            when (obj.config.type) {
                ObjectiveType.CLEAR_SPECIFIC_CELLS -> { obj.clearedCells += cleared.intersect(obj.config.targetCells); obj.currentCount = obj.clearedCells.size }
                ObjectiveType.CLEAR_BOARD -> advance(obj, cleared.size)
                else -> Unit
            }
        }
        // Preserve released payloads and repaired hearts, which are separate from the destroyed shell.
        events += EngineEvent.TilesCleared(hearts.toSet())
        hearts.forEach { state.board.setTile(it, created[it]) }
        created.forEach { (coord, tile) -> state.board.setTile(coord, tile) }
        val heartPoints = hearts.size * 100
        val creationPoints = created.size * 200
        val blockerPoints = damage.destroyedBlockers.size * 150
        val base = heartPoints + creationPoints + blockerPoints + effect.bonusScore
        val multiplier = 1.0 + (combo - 1).coerceAtLeast(0) * 0.5
        val total = (base * multiplier).toInt()
        state.score += total
        state.scoreBreakdown = state.scoreBreakdown.plus(ScoreBreakdown(heartPoints, creationPoints, blockerPoints, effect.bonusScore, total - base))
        events += EngineEvent.ScoreStep(affected.firstOrNull() ?: Coord(0, 0), total, multiplier)
        return Impact(total, (damage.damagedBlockers + damage.destroyedBlockers).any { it.second.blockerType == BlockerType.DARK_HEART })
    }

    private fun drop(state: GameState, events: MutableList<EngineEvent>) {
        events += gravityManager.applyGravity(state.board)
        events += tileSpawner.spawnNewTiles(state.board)
    }

    private fun finishTurn(state: GameState, events: MutableList<EngineEvent>, startingScore: Int, darkDamaged: Boolean, advanceHazards: Boolean) {
        state.objectives.filter { it.config.type in listOf(ObjectiveType.SCORE, ObjectiveType.REACH_SCORE) }.forEach { it.currentCount = state.score.coerceAtMost(it.config.targetCount) }
        if (state.isWon) {
            // Purchased extra moves are excluded from the efficiency reward.
            val bonus = (state.movesRemaining - state.extraMovesGranted).coerceAtLeast(0) * 100
            state.score += bonus
            state.scoreBreakdown = state.scoreBreakdown.copy(remainingMoves = bonus)
            state.status = GameStatus.OBJECTIVE_COMPLETED
        } else if (state.isLost) state.status = GameStatus.GAME_OVER
        else {
            if (advanceHazards) blockerHandler.processDarkHeartSpread(state.board, darkDamaged, rng)?.let { events += it }
            state.status = GameStatus.READY_FOR_INPUT
            if (!boardReshuffler.hasValidMoves(state.board) && boardReshuffler.reshuffle(state.board, allowedColors, rng)) events += EngineEvent.BoardReshuffled()
        }
        val (_, two, three) = state.starThresholds
        state.earnedStars = when {
            state.score >= three -> 3
            state.score >= two -> 2
            state.isWon || state.score >= state.starThresholds.first -> 1
            else -> 0
        }
        events += EngineEvent.ScoreChanged(state.score, state.score - startingScore, state.comboCount)
        state.objectives.forEach { events += EngineEvent.ObjectiveUpdated(it) }
        when (state.status) {
            GameStatus.OBJECTIVE_COMPLETED -> events += EngineEvent.GameWon(state.score, state.earnedStars)
            GameStatus.GAME_OVER -> events += EngineEvent.GameOver(if (state.movesRemaining <= 0) "OUT_OF_MOVES" else "OUT_OF_TIME")
            else -> Unit
        }
    }

    private fun advance(objective: Objective, count: Int) { objective.currentCount = (objective.currentCount + count).coerceAtMost(objective.config.targetCount) }
}
