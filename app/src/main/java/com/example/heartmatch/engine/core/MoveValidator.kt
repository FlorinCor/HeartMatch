package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord

class MoveValidator(
    private val matchDetector: MatchDetector = MatchDetector(),
    private val specialEffectHandler: SpecialEffectHandler = SpecialEffectHandler()
) {

    fun isValidMove(board: Board, from: Coord, to: Coord): Boolean {
        if (!board.isValid(from) || !board.isValid(to)) return false
        if (!from.isAdjacentTo(to)) return false

        val cellFrom = board[from] ?: return false
        val cellTo = board[to] ?: return false

        if (!cellFrom.isPlayable || !cellTo.isPlayable) return false

        val tileFrom = cellFrom.tile ?: return false
        val tileTo = cellTo.tile ?: return false

        if (!tileFrom.isMovable || !tileTo.isMovable) return false

        // Check if this is a special heart activation swap
        if (specialEffectHandler.isSpecialInteraction(tileFrom, tileTo)) {
            return true
        }

        // Simulate swap on cloned board
        val clonedBoard = board.clone()
        clonedBoard.swap(from, to)

        val matches = matchDetector.detectMatches(clonedBoard, from to to)
        return matches.isNotEmpty()
    }

    fun findPossibleMoves(board: Board): List<Pair<Coord, Coord>> {
        val possibleMoves = mutableListOf<Pair<Coord, Coord>>()

        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val from = Coord(r, c)
                // Right swap
                val right = Coord(r, c + 1)
                if (board.isValid(right) && isValidMove(board, from, right)) {
                    possibleMoves.add(from to right)
                }
                // Down swap
                val down = Coord(r + 1, c)
                if (board.isValid(down) && isValidMove(board, from, down)) {
                    possibleMoves.add(from to down)
                }
            }
        }

        return possibleMoves
    }
}
