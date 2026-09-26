package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class EngineAdvancedScenariosTest {

    @Test
    fun testIrregularBoardWithGravityAroundHoles() {
        val engine = HeartMatchEngine()
        // 5x5 board with center hole at (2, 2)
        val cellStates = Array(5) { r ->
            Array(5) { c ->
                if (r == 2 && c == 2) CellState.UNAVAILABLE else CellState.PLAYABLE
            }
        }
        val config = LevelConfig(
            id = 10,
            name = "Hole in Middle",
            rows = 5,
            cols = 5,
            cellStates = cellStates,
            randomSeed = 1234L
        )

        engine.loadLevel(config)
        val board = engine.getState().board

        assertTrue(board[Coord(2, 2)]!!.isUnavailable)
        assertNull(board.getTile(Coord(2, 2)))

        // Other cells should have tiles
        board.forEachCell { cell ->
            if (cell.coord != Coord(2, 2)) {
                assertNotNull(cell.tile)
            }
        }

        // Possible moves should not involve (2, 2)
        val possibleMoves = engine.getPossibleMoves()
        assertTrue(possibleMoves.none { it.first == Coord(2, 2) || it.second == Coord(2, 2) })
    }

    @Test
    fun testBlockerClearObjectiveFulfillment() {
        val engine = HeartMatchEngine()
        val initialTiles = Array(5) { r ->
            Array(5) { c ->
                Tile.Normal(color = HeartColor.BLUE) as Tile?
            }
        }

        // Place 1 Wooden Heart with durability 1 at (2, 2)
        initialTiles[2][2] = Tile.Blocker(blockerType = BlockerType.WOODEN_HEART, durability = 1)
        // Setup a match adjacent to it: (1, 1)=RED, (1, 2)=GREEN, (1, 3)=RED, (0, 2)=RED
        initialTiles[1][1] = Tile.Normal(color = HeartColor.RED)
        initialTiles[1][2] = Tile.Normal(color = HeartColor.GREEN)
        initialTiles[1][3] = Tile.Normal(color = HeartColor.RED)
        initialTiles[0][2] = Tile.Normal(color = HeartColor.RED)

        val config = LevelConfig(
            id = 20,
            name = "Clear Wood",
            rows = 5,
            cols = 5,
            initialTiles = initialTiles,
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.CLEAR_BLOCKER, targetCount = 1, targetBlocker = BlockerType.WOODEN_HEART)
            )
        )

        engine.loadLevel(config)
        // Swap (0, 2) and (1, 2) to complete match at row 1, adjacent to (2, 2)
        val result = engine.swap(Coord(0, 2), Coord(1, 2))

        assertTrue(result.isSuccessfulMove)
        val woodObj = engine.getState().objectives.first()
        assertEquals(1, woodObj.currentCount)
        assertTrue(woodObj.isFulfilled)
        assertTrue(engine.getState().isWon)
    }

    @Test
    fun testSpecialCreationObjectiveFulfillment() {
        val engine = HeartMatchEngine()
        val initialTiles = Array(6) { r ->
            Array(6) { c ->
                Tile.Normal(color = HeartColor.YELLOW) as Tile?
            }
        }

        // Setup 4 in a line for FIRE_HEART:
        // (1, 1)=RED, (1, 2)=BLUE, (1, 3)=RED, (1, 4)=RED, (2, 2)=RED
        initialTiles[1][1] = Tile.Normal(color = HeartColor.RED)
        initialTiles[1][2] = Tile.Normal(color = HeartColor.BLUE)
        initialTiles[1][3] = Tile.Normal(color = HeartColor.RED)
        initialTiles[1][4] = Tile.Normal(color = HeartColor.RED)
        initialTiles[2][2] = Tile.Normal(color = HeartColor.RED)

        val config = LevelConfig(
            id = 30,
            name = "Create Fire",
            rows = 6,
            cols = 6,
            initialTiles = initialTiles,
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.CREATE_SPECIALS, targetCount = 1, targetSpecial = SpecialHeartType.FIRE_HEART)
            )
        )

        engine.loadLevel(config)
        val result = engine.swap(Coord(1, 2), Coord(2, 2))

        assertTrue(result.isSuccessfulMove)
        val obj = engine.getState().objectives.first()
        assertEquals(1, obj.currentCount)
        assertTrue(obj.isFulfilled)
        assertTrue(engine.getState().isWon)
    }

    @Test
    fun testReachScoreObjectiveFulfillment() {
        val engine = HeartMatchEngine()
        val config = LevelConfig(
            id = 40,
            name = "High Score",
            rows = 8,
            cols = 8,
            randomSeed = 555L,
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.REACH_SCORE, targetCount = 300)
            )
        )

        engine.loadLevel(config)
        val moves = engine.getPossibleMoves()
        assertTrue(moves.isNotEmpty())

        val result = engine.swap(moves.first().first, moves.first().second)
        assertTrue(result.isSuccessfulMove)

        val scoreObj = engine.getState().objectives.first()
        assertTrue(scoreObj.currentCount > 0)
        assertEquals(300, scoreObj.currentCount)
        assertTrue(engine.getState().score >= scoreObj.currentCount)
    }
}
