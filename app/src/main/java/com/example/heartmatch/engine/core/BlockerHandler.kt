package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.Tile

class BlockerHandler {

    data class BlockerResolutionResult(
        val damagedBlockers: List<Pair<Coord, Tile.Blocker>>,
        val destroyedBlockers: List<Pair<Coord, Tile.Blocker>>,
        val events: List<EngineEvent>
    )

    fun applyBlockerDamage(
        board: Board,
        matchedCoords: Set<Coord>,
        directHitCoords: Set<Coord>,
        extraLayerHits: Set<Coord> = emptySet(),
        includeAdjacentDamage: Boolean = true
    ): BlockerResolutionResult {
        val impact = matchedCoords + directHitCoords
        val candidates = impact.toMutableSet()
        if (includeAdjacentDamage) impact.forEach { candidates += it.orthogonalNeighbors().filter(board::isValid) }
        val original = candidates.mapNotNull { c -> (board.getTile(c) as? Tile.Blocker)?.let { c to it } }.toMap()
        val damaged = mutableListOf<Pair<Coord, Tile.Blocker>>()
        val destroyed = mutableListOf<Pair<Coord, Tile.Blocker>>()
        val events = mutableListOf<EngineEvent>()
        val healedNeighbors = mutableSetOf<Coord>()
        fun hit(coord: Coord, tile: Tile.Blocker, amount: Int) {
            val remaining = (tile.durability - amount).coerceAtLeast(0)
            events += EngineEvent.BlockerDamaged(coord, tile.blockerType, remaining, remaining == 0)
            if (remaining == 0) {
                destroyed += coord to tile
                events += EngineEvent.BlockerDestroyed(coord, tile.blockerType)
                val replacement = when (tile.blockerType) {
                    BlockerType.CHAINED_HEART -> tile.payloadTile ?: Tile.Normal(color = tile.color ?: HeartColor.RED)
                    BlockerType.ICE_HEART -> tile.payloadTile ?: tile.color?.let { Tile.Normal(color = it) }
                    BlockerType.BROKEN_HEART, BlockerType.STITCHED_HEART -> tile.payloadTile ?: Tile.Normal(color = tile.color ?: HeartColor.RED)
                    else -> null
                }
                board.setTile(coord, replacement)
                if (tile.blockerType == BlockerType.STITCHED_HEART) healedNeighbors += coord.orthogonalNeighbors().filter(board::isValid)
            } else {
                val updated = tile.copy(durability = remaining, turnsSurvived = if (tile.blockerType == BlockerType.DARK_HEART) 0 else tile.turnsSurvived)
                board.setTile(coord, updated)
                damaged += coord to updated
            }
        }
        original.forEach { (coord, tile) ->
            // Stone takes one hit per wave. Wood can shed two layers when surrounded by a match.
            val woodBonus = tile.blockerType == BlockerType.WOODEN_HEART && coord.orthogonalNeighbors().count { it in matchedCoords } >= 2
            val strength = 1 + (if (coord in extraLayerHits || woodBonus) 1 else 0)
            hit(coord, tile, strength)
        }
        val processedHealing = mutableSetOf<Coord>()
        while (true) {
            val coord = healedNeighbors.firstOrNull { it !in processedHealing } ?: break
            processedHealing += coord
            val tile = board.getTile(coord) as? Tile.Blocker ?: continue
            if (tile.blockerType in listOf(BlockerType.BROKEN_HEART, BlockerType.STITCHED_HEART)) hit(coord, tile, 1)
        }
        return BlockerResolutionResult(damaged, destroyed, events)
    }

    fun processDarkHeartSpread(
        board: Board,
        darkHeartDamagedThisTurn: Boolean,
        rng: DeterministicRng
    ): EngineEvent.DarkHeartSpread? {
        val darkHearts = mutableListOf<Pair<Coord, Tile.Blocker>>()
        board.forEachCell { cell ->
            val tile = cell.tile
            if (cell.isPlayable && tile is Tile.Blocker && tile.blockerType == BlockerType.DARK_HEART) {
                darkHearts.add(cell.coord to tile)
            }
        }

        if (darkHearts.isEmpty()) return null

        if (darkHeartDamagedThisTurn) {
            // Reset survival turn counter for all dark hearts
            for ((coord, dh) in darkHearts) {
                if (dh.turnsSurvived > 0) {
                    board.setTile(coord, dh.copy(turnsSurvived = 0))
                }
            }
            return null
        }

        val readyToSpreadCandidates = mutableListOf<Pair<Coord, Coord>>() // source to target
        for ((coord, dh) in darkHearts) {
            val newTurns = dh.turnsSurvived + 1
            if (newTurns >= dh.spreadIntervalTurns) {
                for (neighbor in coord.orthogonalNeighbors()) {
                    if (board.isValid(neighbor)) {
                        val cell = board[neighbor]
                        if (cell != null && cell.isPlayable && cell.tile is Tile.Normal) {
                            readyToSpreadCandidates.add(coord to neighbor)
                        }
                    }
                }
            }
            board.setTile(coord, dh.copy(turnsSurvived = newTurns))
        }

        if (readyToSpreadCandidates.isEmpty()) {
            return null
        }

        val (source, target) = rng.pickRandom(readyToSpreadCandidates)
        val sourceTile = board.getTile(source) as? Tile.Blocker
        val interval = sourceTile?.spreadIntervalTurns ?: 1
        val dur = sourceTile?.maxDurability ?: 1

        board.setTile(target, Tile.Blocker.createDark(spreadIntervalTurns = interval, durability = dur))
        if (sourceTile != null) {
            board.setTile(source, sourceTile.copy(turnsSurvived = 0))
        }

        return EngineEvent.DarkHeartSpread(source, target)
    }
}
