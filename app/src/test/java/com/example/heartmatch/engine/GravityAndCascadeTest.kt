package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.DeterministicRng
import com.example.heartmatch.engine.core.GravityManager
import com.example.heartmatch.engine.core.TileSpawner
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class GravityAndCascadeTest {

    @Test
    fun testTilesDropIntoEmptySpaces() {
        val board = Board.createEmpty(4, 4)
        // Set tile at (0, 1) and empty at (1, 1), (2, 1), (3, 1)
        board.setTile(Coord(0, 1), Tile.Normal(color = HeartColor.RED))

        val gravityManager = GravityManager()
        val drops = gravityManager.applyGravity(board)

        // Tile should have fallen to the bottom (3, 1)
        assertNull(board.getTile(Coord(0, 1)))
        val bottomTile = board.getTile(Coord(3, 1))
        assertNotNull(bottomTile)
        assertTrue(bottomTile is Tile.Normal)
        assertEquals(HeartColor.RED, (bottomTile as Tile.Normal).color)

        assertEquals(1, drops.size)
        assertEquals(Coord(0, 1), drops.first().from)
        assertEquals(Coord(3, 1), drops.first().to)
        assertEquals(3, drops.first().distance)
    }

    @Test
    fun testImmovableTileStopsDropThrough() {
        val board = Board.createEmpty(5, 1)
        // (0, 0)=RED
        // (1, 0)=empty
        // (2, 0)=CHAINED_HEART (canFall = false)
        // (3, 0)=empty
        // (4, 0)=empty
        board.setTile(Coord(0, 0), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(2, 0), Tile.Blocker(blockerType = BlockerType.CHAINED_HEART))

        val gravityManager = GravityManager()
        gravityManager.applyGravity(board)

        // (0, 0) should drop only to (1, 0) because chained heart at (2, 0) blocks downward path
        assertEquals(Coord(2, 0), board[Coord(2, 0)]!!.coord)
        assertTrue(board.getTile(Coord(2, 0)) is Tile.Blocker)
        assertNotNull(board.getTile(Coord(1, 0)))
        assertEquals(HeartColor.RED, (board.getTile(Coord(1, 0)) as Tile.Normal).color)
    }

    @Test
    fun testTileSpawnerFillsAllEmptyCells() {
        val board = Board.createEmpty(4, 4)
        val rng = DeterministicRng(12345L)
        val spawner = TileSpawner(
            allowedColors = listOf(HeartColor.RED, HeartColor.BLUE, HeartColor.GREEN),
            rng = rng
        )

        val spawnEvents = spawner.spawnNewTiles(board)
        assertEquals(16, spawnEvents.size)

        board.forEachCell { cell ->
            assertNotNull(cell.tile)
        }
    }
}
