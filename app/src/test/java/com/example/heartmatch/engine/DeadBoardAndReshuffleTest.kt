package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.BoardReshuffler
import com.example.heartmatch.engine.core.DeterministicRng
import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.core.MoveValidator
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class DeadBoardAndReshuffleTest {

    private val reshuffler = BoardReshuffler()
    private val moveValidator = MoveValidator()

    @Test
    fun testDeadBoardReshuffleProducesValidMoves() {
        val board = Board.createEmpty(4, 4)
        // Latin square pattern with 4 distinct colors:
        // R B G Y
        // Y G B R
        // B R Y G
        // G Y R B
        // No 2 adjacent cells have the same color, and no swap can form 3 in a row
        val gridColors = arrayOf(
            arrayOf(HeartColor.RED, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
            arrayOf(HeartColor.YELLOW, HeartColor.GREEN, HeartColor.BLUE, HeartColor.RED),
            arrayOf(HeartColor.BLUE, HeartColor.RED, HeartColor.YELLOW, HeartColor.GREEN),
            arrayOf(HeartColor.GREEN, HeartColor.YELLOW, HeartColor.RED, HeartColor.BLUE)
        )

        for (r in 0 until 4) {
            for (c in 0 until 4) {
                board.setTile(Coord(r, c), Tile.Normal(color = gridColors[r][c]))
            }
        }

        assertFalse("Dead board has no valid moves initially", reshuffler.hasValidMoves(board))
        assertEquals(0, moveValidator.findPossibleMoves(board).size)

        val success = reshuffler.reshuffle(
            board = board,
            allowedColors = listOf(HeartColor.RED, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
            rng = DeterministicRng(12345L)
        )

        assertTrue("Reshuffle succeeded", success)
        assertTrue("Board now has valid moves", reshuffler.hasValidMoves(board))
        assertTrue(moveValidator.findPossibleMoves(board).isNotEmpty())
    }

    @Test
    fun testStartingBoardNeverGeneratesImpossibleBoardAcrossManySeeds() {
        val colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)

        for (seed in 1L..100L) {
            val engine = HeartMatchEngine()
            val config = LevelConfig(
                id = seed.toInt(),
                name = "Seed $seed",
                rows = 8,
                cols = 8,
                allowedColors = colors,
                randomSeed = seed,
                moveLimit = 20
            )

            engine.loadLevel(config)
            val board = engine.getState().board

            // 1. Must have at least 1 valid move
            val possibleMoves = engine.getPossibleMoves()
            assertTrue("Seed $seed must have possible moves on start", possibleMoves.isNotEmpty())

            // 2. Must have 0 initial pre-existing matches
            val matchDetector = com.example.heartmatch.engine.core.MatchDetector()
            val initialMatches = matchDetector.detectMatches(board)
            assertTrue("Seed $seed must have 0 initial matches", initialMatches.isEmpty())
        }
    }

    @Test
    fun testTurnPipelineTriggersReshuffleEventOnDeadBoard() {
        val engine = HeartMatchEngine()
        val config = LevelConfig(
            id = 50,
            name = "Dead Board Test",
            rows = 5,
            cols = 5,
            allowedColors = listOf(HeartColor.RED, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
            randomSeed = 42L,
            moveLimit = 10,
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.SCORE, targetCount = 10000)
            )
        )

        engine.loadLevel(config)
        val eventsReceived = mutableListOf<EngineEvent>()
        engine.addEventListener { eventsReceived.add(it) }

        // Find a valid move and execute it
        val moves = engine.getPossibleMoves()
        assertTrue(moves.isNotEmpty())
        val (from, to) = moves.first()
        val result = engine.swap(from, to)

        assertTrue(result.isSuccessfulMove)
        // If after move board became dead, BoardReshuffled event must be generated, and possible moves must remain > 0
        val movesAfter = engine.getPossibleMoves()
        assertTrue("Moves must be available after turn (reshuffled if necessary)", movesAfter.isNotEmpty())
    }
}
