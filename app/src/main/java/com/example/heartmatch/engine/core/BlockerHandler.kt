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
        directHitCoords: Set<Coord>
    ): BlockerResolutionResult {
        val allImpactCoords = matchedCoords + directHitCoords
        val damagedList = mutableListOf<Pair<Coord, Tile.Blocker>>()
        val destroyedList = mutableListOf<Pair<Coord, Tile.Blocker>>()
        val events = mutableListOf<EngineEvent>()
        val processedCoords = mutableSetOf<Coord>()

        // 1. Direct hits on blockers (e.g. hit by special blast or direct match)
        for (coord in allImpactCoords) {
            val tile = board.getTile(coord)
            if (tile is Tile.Blocker && coord !in processedCoords) {
                processedCoords.add(coord)
                val newDurability = tile.durability - 1
                val isDestroyed = newDurability <= 0

                events.add(
                    EngineEvent.BlockerDamaged(
                        coord = coord,
                        blockerType = tile.blockerType,
                        remainingDurability = maxOf(0, newDurability),
                        isDestroyed = isDestroyed
                    )
                )

                if (isDestroyed) {
                    destroyedList.add(coord to tile)
                    events.add(EngineEvent.BlockerDestroyed(coord, tile.blockerType))

                    // Replace with payload or repaired normal heart or clear
                    when (tile.blockerType) {
                        BlockerType.CHAINED_HEART -> {
                            val replacement = tile.payloadTile ?: tile.color?.let { Tile.Normal(color = it) } ?: Tile.Normal(color = HeartColor.RED)
                            board.setTile(coord, replacement)
                        }
                        BlockerType.ICE_HEART -> {
                            val replacement = tile.payloadTile ?: tile.color?.let { Tile.Normal(color = it) }
                            board.setTile(coord, replacement)
                        }
                        BlockerType.BROKEN_HEART,
                        BlockerType.STITCHED_HEART -> {
                            val replacement = tile.payloadTile ?: Tile.Normal(color = tile.color ?: HeartColor.RED)
                            board.setTile(coord, replacement)
                        }
                        else -> {
                            board.setTile(coord, null)
                        }
                    }
                } else {
                    val updatedBlocker = tile.copy(
                        durability = newDurability,
                        turnsSurvived = if (tile.blockerType == BlockerType.DARK_HEART) 0 else tile.turnsSurvived
                    )
                    board.setTile(coord, updatedBlocker)
                    damagedList.add(coord to updatedBlocker)
                }
            }
        }

        // 2. Adjacent damage from matches / direct hits
        val adjacentCandidates = mutableSetOf<Coord>()
        for (coord in allImpactCoords) {
            for (neighbor in coord.orthogonalNeighbors()) {
                if (board.isValid(neighbor) && neighbor !in processedCoords && neighbor !in allImpactCoords) {
                    adjacentCandidates.add(neighbor)
                }
            }
        }

        for (coord in adjacentCandidates) {
            val tile = board.getTile(coord)
            if (tile is Tile.Blocker && coord !in processedCoords) {
                processedCoords.add(coord)
                val newDurability = tile.durability - 1
                val isDestroyed = newDurability <= 0

                events.add(
                    EngineEvent.BlockerDamaged(
                        coord = coord,
                        blockerType = tile.blockerType,
                        remainingDurability = maxOf(0, newDurability),
                        isDestroyed = isDestroyed
                    )
                )

                if (isDestroyed) {
                    destroyedList.add(coord to tile)
                    events.add(EngineEvent.BlockerDestroyed(coord, tile.blockerType))

                    when (tile.blockerType) {
                        BlockerType.CHAINED_HEART -> {
                            val replacement = tile.payloadTile ?: tile.color?.let { Tile.Normal(color = it) } ?: Tile.Normal(color = HeartColor.RED)
                            board.setTile(coord, replacement)
                        }
                        BlockerType.ICE_HEART -> {
                            val replacement = tile.payloadTile ?: tile.color?.let { Tile.Normal(color = it) }
                            board.setTile(coord, replacement)
                        }
                        BlockerType.BROKEN_HEART,
                        BlockerType.STITCHED_HEART -> {
                            val replacement = tile.payloadTile ?: Tile.Normal(color = tile.color ?: HeartColor.RED)
                            board.setTile(coord, replacement)
                        }
                        else -> {
                            board.setTile(coord, null)
                        }
                    }
                } else {
                    val updatedBlocker = tile.copy(
                        durability = newDurability,
                        turnsSurvived = if (tile.blockerType == BlockerType.DARK_HEART) 0 else tile.turnsSurvived
                    )
                    board.setTile(coord, updatedBlocker)
                    damagedList.add(coord to updatedBlocker)
                }
            }
        }

        return BlockerResolutionResult(damagedList, destroyedList, events)
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
            } else {
                board.setTile(coord, dh.copy(turnsSurvived = newTurns))
            }
        }

        if (readyToSpreadCandidates.isEmpty()) {
            // If none could spread, still ensure turns are incremented up to threshold
            for ((coord, dh) in darkHearts) {
                val newTurns = dh.turnsSurvived + 1
                board.setTile(coord, dh.copy(turnsSurvived = newTurns))
            }
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
