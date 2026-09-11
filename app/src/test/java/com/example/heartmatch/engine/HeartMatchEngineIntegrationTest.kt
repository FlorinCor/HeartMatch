package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class HeartMatchEngineIntegrationTest {

    @Test
    fun testLoadLevelAndInitialState() {
        val engine = HeartMatchEngine()
        val config = LevelConfig(
            id = 101,
            name = "Rose Garden",
            rows = 8,
            cols = 8,
            moveLimit = 20,
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.COLLECT_HEARTS, targetCount = 10, targetColor = HeartColor.RED)
            )
        )

        engine.loadLevel(config)
        val state = engine.getState()

        assertEquals(101, state.levelId)
        assertEquals(8, state.board.rows)
        assertEquals(8, state.board.cols)
        assertEquals(20, state.movesRemaining)
        assertEquals(GameStatus.READY_FOR_INPUT, state.status)
        assertEquals(1, state.objectives.size)
        assertEquals(0, state.objectives.first().currentCount)
        assertFalse(state.isWon)
        assertFalse(state.isLost)
    }

    @Test
    fun testSwapAndMatchResolution() {
        val engine = HeartMatchEngine()
        // Create custom 5x5 board layout
        val initialTiles = Array(5) { r ->
            Array(5) { c ->
                Tile.Normal(color = HeartColor.GREEN) as Tile?
            }
        }
        // Place a set of tiles where swapping (1, 2) and (2, 2) creates a match of 3 RED
        // (1, 1)=RED, (1, 2)=BLUE, (1, 3)=RED
        // (2, 2)=RED
        initialTiles[1][1] = Tile.Normal(color = HeartColor.RED)
        initialTiles[1][2] = Tile.Normal(color = HeartColor.BLUE)
        initialTiles[1][3] = Tile.Normal(color = HeartColor.RED)
        initialTiles[2][2] = Tile.Normal(color = HeartColor.RED)

        val config = LevelConfig(
            id = 1,
            name = "Test",
            rows = 5,
            cols = 5,
            initialTiles = initialTiles,
            moveLimit = 15,
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.COLLECT_HEARTS, targetCount = 3, targetColor = HeartColor.RED)
            )
        )

        val eventsReceived = mutableListOf<EngineEvent>()
        engine.addEventListener { event -> eventsReceived.add(event) }

        engine.loadLevel(config)
        val result = engine.swap(Coord(1, 2), Coord(2, 2))

        assertTrue(result.isSuccessfulMove)
        assertTrue(result.scoreDelta > 0)
        assertEquals(14, engine.getState().movesRemaining)

        // Objective for collecting 3 red hearts should now be fulfilled
        val redObj = engine.getState().objectives.first()
        assertTrue(redObj.currentCount >= 3)
        assertTrue(redObj.isFulfilled)
        assertTrue(engine.getState().isWon)
        assertEquals(GameStatus.OBJECTIVE_COMPLETED, engine.getState().status)

        // Verify events were dispatched
        assertTrue(eventsReceived.any { it is EngineEvent.Swap && !it.isRollback })
        assertTrue(eventsReceived.any { it is EngineEvent.Match && it.color == HeartColor.RED })
        assertTrue(eventsReceived.any { it is EngineEvent.GameWon })
    }

    @Test
    fun testInvalidSwapRollsBack() {
        val engine = HeartMatchEngine()
        val initialTiles = Array(4) { r ->
            Array(4) { c ->
                Tile.Normal(color = if ((r + c) % 2 == 0) HeartColor.RED else HeartColor.BLUE) as Tile?
            }
        }

        val config = LevelConfig(
            id = 2,
            name = "Checkerboard",
            rows = 4,
            cols = 4,
            initialTiles = initialTiles,
            moveLimit = 10
        )

        engine.loadLevel(config)
        val initialMoves = engine.getState().movesRemaining
        val result = engine.swap(Coord(0, 0), Coord(0, 1))

        assertFalse(result.isSuccessfulMove)
        assertEquals(initialMoves, engine.getState().movesRemaining)
        assertTrue(result.events.any { it is EngineEvent.Swap && it.isRollback })
    }

    @Test
    fun testGameOverWhenOutOfMoves() {
        val engine = HeartMatchEngine()
        val initialTiles = Array(5) { r ->
            Array(5) { c ->
                Tile.Normal(color = HeartColor.YELLOW) as Tile?
            }
        }
        // Matchable setup
        initialTiles[1][1] = Tile.Normal(color = HeartColor.RED)
        initialTiles[1][2] = Tile.Normal(color = HeartColor.BLUE)
        initialTiles[1][3] = Tile.Normal(color = HeartColor.RED)
        initialTiles[2][2] = Tile.Normal(color = HeartColor.RED)

        val config = LevelConfig(
            id = 3,
            name = "Last Move",
            rows = 5,
            cols = 5,
            initialTiles = initialTiles,
            moveLimit = 1, // Only 1 move allowed!
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.COLLECT_HEARTS, targetCount = 100, targetColor = HeartColor.RED)
            )
        )

        engine.loadLevel(config)
        engine.swap(Coord(1, 2), Coord(2, 2))

        assertEquals(0, engine.getState().movesRemaining)
        assertFalse(engine.getState().isWon)
        assertTrue(engine.getState().isLost)
        assertEquals(GameStatus.GAME_OVER, engine.getState().status)
    }

    @Test
    fun testCascadeResolutionChain() {
        val engine = HeartMatchEngine()
        val config = LevelConfig(
            id = 4,
            name = "Cascade Test",
            rows = 9,
            cols = 9,
            randomSeed = 100L
        )

        engine.loadLevel(config)
        val possibleMoves = engine.getPossibleMoves()
        assertTrue(possibleMoves.isNotEmpty())

        val (from, to) = possibleMoves.first()
        val result = engine.swap(from, to)

        assertTrue(result.isSuccessfulMove)
        assertTrue(result.combosAchieved >= 1)
        assertTrue(engine.getState().score > 0)
    }
}
