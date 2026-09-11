package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent

class GravityManager {

    fun applyGravity(board: Board): List<EngineEvent.TileDrop> {
        val dropEvents = mutableListOf<EngineEvent.TileDrop>()

        for (c in 0 until board.cols) {
            for (r in (board.rows - 1) downTo 0) {
                val targetCoord = Coord(r, c)
                val targetCell = board[targetCoord]

                if (targetCell != null && targetCell.isPlayable && targetCell.isEmpty) {
                    // Search upwards for the first falling tile
                    var foundSourceCoord: Coord? = null
                    for (aboveR in (r - 1) downTo 0) {
                        val aboveCoord = Coord(aboveR, c)
                        val aboveCell = board[aboveCoord]

                        if (aboveCell != null && aboveCell.isPlayable) {
                            val tile = aboveCell.tile
                            if (tile != null) {
                                if (tile.canFall) {
                                    foundSourceCoord = aboveCoord
                                    break
                                } else {
                                    // Hit an immovable tile like Chained Heart, stop looking higher in this column segment
                                    break
                                }
                            }
                        } else if (aboveCell != null && !aboveCell.isPlayable) {
                            // Hit an unavailable/locked cell, break
                            break
                        }
                    }

                    if (foundSourceCoord != null) {
                        val tile = board.getTile(foundSourceCoord)!!
                        board.setTile(foundSourceCoord, null)
                        board.setTile(targetCoord, tile)

                        val distance = targetCoord.row - foundSourceCoord.row
                        dropEvents.add(EngineEvent.TileDrop(from = foundSourceCoord, to = targetCoord, tile = tile, distance = distance))
                    }
                }
            }
        }

        return dropEvents
    }
}
