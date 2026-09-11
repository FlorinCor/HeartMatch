package com.example.heartmatch.engine

import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class BoardTest {

    @Test
    fun testBoardDimensions() {
        val board7x7 = Board.createEmpty(7, 7)
        assertEquals(7, board7x7.rows)
        assertEquals(7, board7x7.cols)

        val board8x8 = Board.createEmpty(8, 8)
        assertEquals(8, board8x8.rows)
        assertEquals(8, board8x8.cols)

        val board9x9 = Board.createEmpty(9, 9)
        assertEquals(9, board9x9.rows)
        assertEquals(9, board9x9.cols)
    }

    @Test
    fun testIrregularLayout() {
        val cellStates = Array(7) { r ->
            Array(7) { c ->
                when {
                    (r == 0 && c == 0) || (r == 0 && c == 6) || (r == 6 && c == 0) || (r == 6 && c == 6) ->
                        CellState.UNAVAILABLE
                    r == 3 && c == 3 -> CellState.LOCKED
                    else -> CellState.PLAYABLE
                }
            }
        }

        val board = Board.createWithLayout(7, 7, cellStates = cellStates)

        assertTrue(board[Coord(0, 0)]!!.isUnavailable)
        assertTrue(board[Coord(0, 6)]!!.isUnavailable)
        assertTrue(board[Coord(6, 0)]!!.isUnavailable)
        assertTrue(board[Coord(6, 6)]!!.isUnavailable)
        assertTrue(board[Coord(3, 3)]!!.isLocked)
        assertTrue(board[Coord(1, 1)]!!.isPlayable)

        // Ensure cannot set tile in unavailable or locked cells
        board.setTile(Coord(0, 0), Tile.Normal(color = HeartColor.RED))
        assertNull(board.getTile(Coord(0, 0)))

        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.PINK))
        assertNotNull(board.getTile(Coord(1, 1)))
        assertEquals(HeartColor.PINK, (board.getTile(Coord(1, 1)) as Tile.Normal).color)
    }

    @Test
    fun testCellContents() {
        val board = Board.createEmpty(5, 5)

        // Normal heart
        board.setTile(Coord(0, 0), Tile.Normal(color = HeartColor.RED))
        assertTrue(board.getTile(Coord(0, 0)) is Tile.Normal)

        // Special heart
        board.setTile(Coord(0, 1), Tile.Special(specialType = SpecialHeartType.FIRE_HEART, baseColor = HeartColor.BLUE))
        assertTrue(board.getTile(Coord(0, 1)) is Tile.Special)

        // Blocker
        board.setTile(Coord(0, 2), Tile.Blocker(blockerType = BlockerType.STONE_HEART))
        assertTrue(board.getTile(Coord(0, 2)) is Tile.Blocker)

        // Empty
        board.setTile(Coord(0, 3), null)
        assertNull(board.getTile(Coord(0, 3)))
        assertTrue(board[Coord(0, 3)]!!.isEmpty)
    }

    @Test
    fun testClone() {
        val board = Board.createEmpty(3, 3)
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.GREEN))

        val cloned = board.clone()
        assertEquals(board.rows, cloned.rows)
        assertEquals(board.cols, cloned.cols)
        assertEquals(HeartColor.GREEN, (cloned.getTile(Coord(1, 1)) as Tile.Normal).color)

        // Modify original should not affect clone
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.RED))
        assertEquals(HeartColor.RED, (board.getTile(Coord(1, 1)) as Tile.Normal).color)
        assertEquals(HeartColor.GREEN, (cloned.getTile(Coord(1, 1)) as Tile.Normal).color)
    }
}
