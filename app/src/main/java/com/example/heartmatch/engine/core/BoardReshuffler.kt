package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.Tile

class BoardReshuffler(
    private val matchDetector: MatchDetector = MatchDetector(),
    private val moveValidator: MoveValidator = MoveValidator()
) {

    fun hasValidMoves(board: Board): Boolean {
        return moveValidator.findPossibleMoves(board).isNotEmpty()
    }

    fun reshuffle(
        board: Board,
        allowedColors: List<HeartColor>,
        rng: DeterministicRng = DeterministicRng()
    ): Boolean {
        val movableCoords = mutableListOf<Coord>()
        board.forEachCell { cell ->
            if (cell.isPlayable && cell.tile != null && cell.tile!!.isMovable) {
                movableCoords.add(cell.coord)
            }
        }

        if (movableCoords.size < 3) {
            // Not enough movable tiles to shuffle
            return false
        }

        val originalTiles = movableCoords.map { board.getTile(it)!! }

        // Attempt 1: Permutation-based shuffle
        for (attempt in 0 until 100) {
            val shuffledTiles = originalTiles.toMutableList()
            // Fisher-Yates shuffle with deterministic RNG
            for (i in shuffledTiles.size - 1 downTo 1) {
                val j = rng.nextInt(i + 1)
                val tmp = shuffledTiles[i]
                shuffledTiles[i] = shuffledTiles[j]
                shuffledTiles[j] = tmp
            }

            for (i in movableCoords.indices) {
                board.setTile(movableCoords[i], shuffledTiles[i])
            }

            // Must have NO immediate matches and AT LEAST ONE valid move
            val initialMatches = matchDetector.detectMatches(board)
            if (initialMatches.isEmpty() && hasValidMoves(board)) {
                return true
            }
        }

        // Regeneration must never erase gifts, specials, or movable repair objectives.
        movableCoords.forEachIndexed { index, coord -> board.setTile(coord, originalTiles[index]) }
        val normalCoords = movableCoords.filter { board.getTile(it) is Tile.Normal }
        // Attempt 2: Regeneration with non-matching color assignment + guaranteed valid move injection
        for (attempt in 0 until 100) {
            // Reassign random colors avoiding immediate 3-matches
            for (coord in normalCoords) {
                val forbidden = mutableSetOf<HeartColor>()
                val r = coord.row
                val c = coord.col

                // Check left 2
                val left1 = board.getTile(Coord(r, c - 1))?.matchColor
                val left2 = board.getTile(Coord(r, c - 2))?.matchColor
                if (left1 != null && left1 == left2) forbidden.add(left1)

                // Check top 2
                val top1 = board.getTile(Coord(r - 1, c))?.matchColor
                val top2 = board.getTile(Coord(r - 2, c))?.matchColor
                if (top1 != null && top1 == top2) forbidden.add(top1)

                val candidates = allowedColors.filter { it !in forbidden }
                val chosenColor = if (candidates.isNotEmpty()) rng.pickRandom(candidates) else rng.pickRandom(allowedColors)
                board.setTile(coord, Tile.Normal(color = chosenColor))
            }

            if (matchDetector.detectMatches(board).isEmpty()) {
                if (hasValidMoves(board)) {
                    return true
                }
                // Try to inject a guaranteed match-3 move by creating an almost-match
                if (injectGuaranteedMove(board, normalCoords, allowedColors, rng)) {
                    if (matchDetector.detectMatches(board).isEmpty() && hasValidMoves(board)) {
                        return true
                    }
                }
            }
        }

        movableCoords.forEachIndexed { index, coord -> board.setTile(coord, originalTiles[index]) }
        return false
    }

    private fun injectGuaranteedMove(
        board: Board,
        movableCoords: List<Coord>,
        allowedColors: List<HeartColor>,
        rng: DeterministicRng
    ): Boolean {
        // Find a horizontal triplet of movable coords (r, c), (r, c+1), (r, c+2)
        for (coord in movableCoords) {
            val c1 = coord
            val c2 = Coord(c1.row, c1.col + 1)
            val c3 = Coord(c1.row, c1.col + 2)
            val swapCandidate = Coord(c1.row + 1, c1.col + 2)

            if (c2 in movableCoords && c3 in movableCoords && swapCandidate in movableCoords) {
                val color = rng.pickRandom(allowedColors)
                val otherColors = allowedColors.filter { it != color }
                if (otherColors.isNotEmpty()) {
                    val decoyColor = rng.pickRandom(otherColors)
                    board.setTile(c1, Tile.Normal(color = color))
                    board.setTile(c2, Tile.Normal(color = color))
                    board.setTile(c3, Tile.Normal(color = decoyColor))
                    board.setTile(swapCandidate, Tile.Normal(color = color))
                    return true
                }
            }
        }
        return false
    }
}
