package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.MoveValidator
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class MoveValidatorTest {

    private val validator = MoveValidator()

    @Test
    fun testValidSwapCreatesMatch() {
        val board = Board.createEmpty(5, 5)
        // Set up horizontal match if swapped:
        // (1, 1)=RED, (1, 2)=BLUE, (1, 3)=RED
        // (2, 2)=RED
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.BLUE))
        board.setTile(Coord(1, 3), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.RED))

        // Swap (1, 2) with (2, 2)
        assertTrue(validator.isValidMove(board, Coord(1, 2), Coord(2, 2)))
        assertTrue(validator.isValidMove(board, Coord(2, 2), Coord(1, 2)))
    }

    @Test
    fun testInvalidSwapWithoutMatch() {
        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.BLUE))
        board.setTile(Coord(1, 3), Tile.Normal(color = HeartColor.GREEN))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.YELLOW))

        assertFalse(validator.isValidMove(board, Coord(1, 2), Coord(2, 2)))
    }

    @Test
    fun testNonAdjacentOrDiagonalSwapRejected() {
        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.BLUE))
        board.setTile(Coord(1, 3), Tile.Normal(color = HeartColor.GREEN))

        // Diagonal
        assertFalse(validator.isValidMove(board, Coord(1, 1), Coord(2, 2)))
        // Non-adjacent
        assertFalse(validator.isValidMove(board, Coord(1, 1), Coord(1, 3)))
    }

    @Test
    fun testImmovableTileCannotSwap() {
        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(1, 1), Tile.Blocker(blockerType = BlockerType.STONE_HEART))
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.RED))

        assertFalse(validator.isValidMove(board, Coord(1, 1), Coord(1, 2)))
    }

    @Test
    fun testRainbowHeartSwapIsValidWithAnyAdjacent() {
        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(2, 2), Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.RED))

        assertTrue(validator.isValidMove(board, Coord(2, 2), Coord(2, 3)))
    }

    @Test
    fun testSpecialSpecialSwapIsValid() {
        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(2, 2), Tile.Special(specialType = SpecialHeartType.FIRE_HEART, baseColor = HeartColor.RED))
        board.setTile(Coord(2, 3), Tile.Special(specialType = SpecialHeartType.BOMB_HEART, baseColor = HeartColor.BLUE))

        assertTrue(validator.isValidMove(board, Coord(2, 2), Coord(2, 3)))
    }

    @Test
    fun testFindPossibleMoves() {
        val board = Board.createEmpty(4, 4)
        // Setup a single valid move: swap (1, 2) and (2, 2)
        // (1, 1)=RED, (1, 2)=BLUE, (1, 3)=RED
        // (2, 2)=RED
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.BLUE))
        board.setTile(Coord(1, 3), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.RED))

        val moves = validator.findPossibleMoves(board)
        assertTrue(moves.isNotEmpty())
        assertTrue(moves.contains(Coord(1, 2) to Coord(2, 2)) || moves.contains(Coord(2, 2) to Coord(1, 2)))
    }
}
