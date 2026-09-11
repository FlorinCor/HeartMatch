package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.Tile

class TileSpawner(
    private val allowedColors: List<HeartColor>,
    private val colorWeights: Map<HeartColor, Int>? = null,
    private val rng: DeterministicRng = DeterministicRng(),
    private val boardReshuffler: BoardReshuffler = BoardReshuffler()
) {

    fun spawnNewTiles(board: Board): List<EngineEvent.TileDrop> {
        val spawnEvents = mutableListOf<EngineEvent.TileDrop>()

        for (c in 0 until board.cols) {
            var emptyCountInCol = 0
            for (r in (board.rows - 1) downTo 0) {
                val coord = Coord(r, c)
                val cell = board[coord]
                if (cell != null && cell.isPlayable && cell.isEmpty) {
                    emptyCountInCol++
                    val color = rng.pickWeightedColor(allowedColors, colorWeights)
                    val newTile = Tile.Normal(color = color)
                    board.setTile(coord, newTile)

                    spawnEvents.add(
                        EngineEvent.TileDrop(
                            from = null,
                            to = coord,
                            tile = newTile,
                            distance = emptyCountInCol
                        )
                    )
                }
            }
        }

        return spawnEvents
    }

    fun fillInitialBoardWithoutMatches(board: Board) {
        val rows = board.rows
        val cols = board.cols
        val matchDetector = MatchDetector()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val coord = Coord(r, c)
                val cell = board[coord]
                if (cell != null && cell.isPlayable && cell.isEmpty) {
                    val forbiddenColors = mutableSetOf<HeartColor>()

                    // Horizontal checks
                    val left1 = board.getTile(Coord(r, c - 1))?.matchColor
                    val left2 = board.getTile(Coord(r, c - 2))?.matchColor
                    if (left1 != null && left1 == left2) forbiddenColors.add(left1)

                    val right1 = board.getTile(Coord(r, c + 1))?.matchColor
                    val right2 = board.getTile(Coord(r, c + 2))?.matchColor
                    if (right1 != null && right1 == right2) forbiddenColors.add(right1)
                    if (left1 != null && left1 == right1) forbiddenColors.add(left1)

                    // Vertical checks
                    val top1 = board.getTile(Coord(r - 1, c))?.matchColor
                    val top2 = board.getTile(Coord(r - 2, c))?.matchColor
                    if (top1 != null && top1 == top2) forbiddenColors.add(top1)

                    val bot1 = board.getTile(Coord(r + 1, c))?.matchColor
                    val bot2 = board.getTile(Coord(r + 2, c))?.matchColor
                    if (bot1 != null && bot1 == bot2) forbiddenColors.add(bot1)
                    if (top1 != null && top1 == bot1) forbiddenColors.add(top1)

                    val candidateColors = allowedColors.filter { it !in forbiddenColors }
                    val finalCandidates = if (candidateColors.isNotEmpty()) candidateColors else allowedColors
                    val chosenColor = rng.pickWeightedColor(finalCandidates, colorWeights)

                    board.setTile(coord, Tile.Normal(color = chosenColor))
                }
            }
        }

        // Break any residual matches (e.g. from initial board layout)
        var attempts = 0
        while (attempts < 20) {
            val matches = matchDetector.detectMatches(board)
            if (matches.isEmpty()) break

            for (match in matches) {
                for (crd in match.matchedCoords) {
                    val tile = board.getTile(crd)
                    if (tile is Tile.Normal) {
                        val otherColors = allowedColors.filter { it != match.color }
                        if (otherColors.isNotEmpty()) {
                            board.setTile(crd, Tile.Normal(color = rng.pickRandom(otherColors)))
                            break
                        }
                    }
                }
            }
            attempts++
        }

        // Guarantee at least one valid move exists on the starting board
        if (!boardReshuffler.hasValidMoves(board)) {
            boardReshuffler.reshuffle(board, allowedColors, rng)
        }
    }
}
