package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.DeterministicRng
import com.example.heartmatch.engine.core.MatchDetector
import com.example.heartmatch.engine.core.MoveValidator
import com.example.heartmatch.engine.core.SpecialEffectHandler
import com.example.heartmatch.engine.core.TileSpawner
import com.example.heartmatch.engine.core.TurnPipeline
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class SpecialEffectTest {

    private val specialHandler = SpecialEffectHandler()

    @Test
    fun testFireHeartClearsRowOnly() {
        val board = Board.createEmpty(5, 5)
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.RED))
            }
        }
        val fireHeart = Tile.Special(
            specialType = SpecialHeartType.FIRE_HEART,
            fireDirection = FireDirection.ROW
        )
        board.setTile(Coord(2, 2), fireHeart)

        val result = specialHandler.triggerSpecials(board, listOf(Coord(2, 2) to fireHeart))

        assertEquals(5, result.clearedCoords.size)
        for (c in 0 until 5) {
            assertTrue(result.clearedCoords.contains(Coord(2, c)))
        }
        assertFalse(result.clearedCoords.contains(Coord(0, 2)))
        assertFalse(result.clearedCoords.contains(Coord(4, 2)))
    }

    @Test
    fun testFireHeartClearsColumnOnly() {
        val board = Board.createEmpty(5, 5)
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.BLUE))
            }
        }
        val fireHeart = Tile.Special(
            specialType = SpecialHeartType.FIRE_HEART,
            fireDirection = FireDirection.COLUMN
        )
        board.setTile(Coord(2, 2), fireHeart)

        val result = specialHandler.triggerSpecials(board, listOf(Coord(2, 2) to fireHeart))

        assertEquals(5, result.clearedCoords.size)
        for (r in 0 until 5) {
            assertTrue(result.clearedCoords.contains(Coord(r, 2)))
        }
        assertFalse(result.clearedCoords.contains(Coord(2, 0)))
        assertFalse(result.clearedCoords.contains(Coord(2, 4)))
    }

    @Test
    fun testFireHeartClearsRowAndColumnWhenBoth() {
        val board = Board.createEmpty(5, 5)
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.RED))
            }
        }
        val fireHeart = Tile.Special(
            specialType = SpecialHeartType.FIRE_HEART,
            fireDirection = FireDirection.BOTH
        )
        board.setTile(Coord(2, 2), fireHeart)

        val result = specialHandler.triggerSpecials(board, listOf(Coord(2, 2) to fireHeart))

        // Entire row 2 and col 2 should be in clearedCoords (9 unique cells)
        assertEquals(9, result.clearedCoords.size)
        for (c in 0 until 5) {
            assertTrue(result.clearedCoords.contains(Coord(2, c)))
        }
        for (r in 0 until 5) {
            assertTrue(result.clearedCoords.contains(Coord(r, 2)))
        }
    }

    @Test
    fun testBombHeartClears3x3DefaultRadius() {
        val board = Board.createEmpty(7, 7)
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.BLUE))
            }
        }
        val bombHeart = Tile.Special(specialType = SpecialHeartType.BOMB_HEART, bombRadius = 1)
        board.setTile(Coord(3, 3), bombHeart)

        val result = specialHandler.triggerSpecials(board, listOf(Coord(3, 3) to bombHeart))

        assertEquals(9, result.clearedCoords.size)
        for (r in 2..4) {
            for (c in 2..4) {
                assertTrue(result.clearedCoords.contains(Coord(r, c)))
            }
        }
    }

    @Test
    fun testBombHeartConfigurableRadius() {
        val board = Board.createEmpty(7, 7)
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.BLUE))
            }
        }
        val bombHeart = Tile.Special(specialType = SpecialHeartType.BOMB_HEART, bombRadius = 2)
        board.setTile(Coord(3, 3), bombHeart)

        val result = specialHandler.triggerSpecials(board, listOf(Coord(3, 3) to bombHeart))

        // Radius 2 -> 5x5 centered on (3,3) -> 25 coords
        assertEquals(25, result.clearedCoords.size)
        for (r in 1..5) {
            for (c in 1..5) {
                assertTrue(result.clearedCoords.contains(Coord(r, c)))
            }
        }
    }

    @Test
    fun testCombinationRainbowPlusColor() {
        val board = Board.createEmpty(5, 5)
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.GREEN))
            }
        }
        board.setTile(Coord(0, 0), Tile.Normal(color = HeartColor.PINK))
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.PINK))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.PINK))
        board.setTile(Coord(4, 4), Tile.Normal(color = HeartColor.PINK))

        val rainbow = Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART)
        val pinkTarget = Tile.Normal(color = HeartColor.PINK)
        board.setTile(Coord(2, 2), rainbow)
        board.setTile(Coord(2, 3), pinkTarget)

        assertEquals(SpecialCombinationType.RAINBOW_COLOR, specialHandler.getCombinationType(rainbow, pinkTarget))

        val result = specialHandler.handleSpecialSwap(
            board = board,
            from = Coord(2, 2),
            to = Coord(2, 3),
            tileA = rainbow,
            tileB = pinkTarget
        )

        // All 4 pink coords + rainbow origin should be cleared
        assertTrue(result.clearedCoords.contains(Coord(0, 0)))
        assertTrue(result.clearedCoords.contains(Coord(1, 1)))
        assertTrue(result.clearedCoords.contains(Coord(2, 3)))
        assertTrue(result.clearedCoords.contains(Coord(4, 4)))
        assertTrue(result.clearedCoords.contains(Coord(2, 2)))
        assertEquals(5, result.clearedCoords.size)
    }

    @Test
    fun testCombinationRainbowPlusFire() {
        val board = Board.createEmpty(6, 6)
        for (r in 0 until 6) {
            for (c in 0 until 6) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.YELLOW))
            }
        }
        // Place a few RED hearts
        board.setTile(Coord(0, 1), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(5, 5), Tile.Normal(color = HeartColor.RED))

        val rainbow = Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART)
        val fire = Tile.Special(specialType = SpecialHeartType.FIRE_HEART, baseColor = HeartColor.RED)
        board.setTile(Coord(2, 2), rainbow)
        board.setTile(Coord(2, 3), fire)

        assertEquals(SpecialCombinationType.RAINBOW_FIRE, specialHandler.getCombinationType(rainbow, fire))

        val result = specialHandler.handleSpecialSwap(
            board = board,
            from = Coord(2, 2),
            to = Coord(2, 3),
            tileA = rainbow,
            tileB = fire
        )

        // All converted red hearts should trigger fire lines, plus rainbow and fire tiles
        assertTrue(result.clearedCoords.contains(Coord(2, 2)))
        assertTrue(result.clearedCoords.contains(Coord(2, 3)))
        assertTrue(result.clearedCoords.contains(Coord(0, 1)))
        assertTrue(result.clearedCoords.contains(Coord(5, 5)))
        // Since (0,1) and (5,5) triggered fire hearts, row 0 and row 5 must be cleared
        for (c in 0 until 6) {
            assertTrue(result.clearedCoords.contains(Coord(0, c)))
            assertTrue(result.clearedCoords.contains(Coord(5, c)))
        }
    }

    @Test
    fun testCombinationRainbowPlusBomb() {
        val board = Board.createEmpty(7, 7)
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.BLUE))
            }
        }
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.PURPLE))
        board.setTile(Coord(5, 5), Tile.Normal(color = HeartColor.PURPLE))

        val rainbow = Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART)
        val bomb = Tile.Special(specialType = SpecialHeartType.BOMB_HEART, baseColor = HeartColor.PURPLE)
        board.setTile(Coord(3, 3), rainbow)
        board.setTile(Coord(3, 4), bomb)

        assertEquals(SpecialCombinationType.RAINBOW_BOMB, specialHandler.getCombinationType(rainbow, bomb))

        val result = specialHandler.handleSpecialSwap(
            board = board,
            from = Coord(3, 3),
            to = Coord(3, 4),
            tileA = rainbow,
            tileB = bomb
        )

        // Converted purple tiles (1,1) and (5,5) explode 3x3
        assertTrue(result.clearedCoords.contains(Coord(0, 0)))
        assertTrue(result.clearedCoords.contains(Coord(2, 2)))
        assertTrue(result.clearedCoords.contains(Coord(6, 6)))
        assertTrue(result.clearedCoords.contains(Coord(3, 3)))
        assertTrue(result.clearedCoords.contains(Coord(3, 4)))
    }

    @Test
    fun testCombinationRainbowPlusRainbow() {
        val board = Board.createEmpty(6, 6)
        val rainbowA = Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART)
        val rainbowB = Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART)
        board.setTile(Coord(2, 2), rainbowA)
        board.setTile(Coord(2, 3), rainbowB)

        assertEquals(SpecialCombinationType.RAINBOW_RAINBOW, specialHandler.getCombinationType(rainbowA, rainbowB))

        val result = specialHandler.handleSpecialSwap(
            board = board,
            from = Coord(2, 2),
            to = Coord(2, 3),
            tileA = rainbowA,
            tileB = rainbowB
        )

        assertEquals(36, result.clearedCoords.size)
    }

    @Test
    fun testCombinationFirePlusFire() {
        val board = Board.createEmpty(7, 7)
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.ORANGE))
            }
        }
        val fireA = Tile.Special(specialType = SpecialHeartType.FIRE_HEART)
        val fireB = Tile.Special(specialType = SpecialHeartType.FIRE_HEART)
        board.setTile(Coord(3, 3), fireA)
        board.setTile(Coord(3, 4), fireB)

        assertEquals(SpecialCombinationType.FIRE_FIRE, specialHandler.getCombinationType(fireA, fireB))

        val result = specialHandler.handleSpecialSwap(
            board = board,
            from = Coord(3, 3),
            to = Coord(3, 4),
            tileA = fireA,
            tileB = fireB
        )

        // 3 rows (2..4) and 3 columns (3..5) centered on target (3, 4)
        for (r in 2..4) {
            for (c in 0 until 7) {
                assertTrue(result.clearedCoords.contains(Coord(r, c)))
            }
        }
        for (c in 3..5) {
            for (r in 0 until 7) {
                assertTrue(result.clearedCoords.contains(Coord(r, c)))
            }
        }
    }

    @Test
    fun testCombinationFirePlusBomb() {
        val board = Board.createEmpty(7, 7)
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.PINK))
            }
        }
        val fire = Tile.Special(specialType = SpecialHeartType.FIRE_HEART)
        val bomb = Tile.Special(specialType = SpecialHeartType.BOMB_HEART)
        board.setTile(Coord(3, 3), fire)
        board.setTile(Coord(3, 4), bomb)

        assertEquals(SpecialCombinationType.FIRE_BOMB, specialHandler.getCombinationType(fire, bomb))

        val result = specialHandler.handleSpecialSwap(
            board = board,
            from = Coord(3, 3),
            to = Coord(3, 4),
            tileA = fire,
            tileB = bomb
        )

        // 3-wide cross centered on (3, 4): rows 2..4 and cols 3..5
        for (r in 2..4) {
            for (c in 0 until 7) {
                assertTrue(result.clearedCoords.contains(Coord(r, c)))
            }
        }
        for (c in 3..5) {
            for (r in 0 until 7) {
                assertTrue(result.clearedCoords.contains(Coord(r, c)))
            }
        }
    }

    @Test
    fun testCombinationBombPlusBomb() {
        val board = Board.createEmpty(7, 7)
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.RED))
            }
        }
        val bombA = Tile.Special(specialType = SpecialHeartType.BOMB_HEART)
        val bombB = Tile.Special(specialType = SpecialHeartType.BOMB_HEART)
        board.setTile(Coord(3, 3), bombA)
        board.setTile(Coord(3, 4), bombB)

        assertEquals(SpecialCombinationType.BOMB_BOMB, specialHandler.getCombinationType(bombA, bombB))

        val result = specialHandler.handleSpecialSwap(
            board = board,
            from = Coord(3, 3),
            to = Coord(3, 4),
            tileA = bombA,
            tileB = bombB
        )

        // 5x5 centered on to (3, 4) -> rows 1..5, cols 2..6 -> 25 coords
        assertEquals(25, result.clearedCoords.size)
        for (r in 1..5) {
            for (c in 2..6) {
                assertTrue(result.clearedCoords.contains(Coord(r, c)))
            }
        }
    }

    @Test
    fun testGiftHeartBonusRewardAndObjective() {
        val board = Board.createEmpty(5, 5)
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.BLUE))
            }
        }
        val giftHeart = Tile.Special(specialType = SpecialHeartType.GIFT_HEART)
        board.setTile(Coord(2, 2), giftHeart)

        val result = specialHandler.triggerSpecials(board, listOf(Coord(2, 2) to giftHeart))

        assertTrue(result.clearedCoords.contains(Coord(2, 2)))
        assertTrue(result.clearedCoords.contains(Coord(1, 2)))
        assertTrue(result.clearedCoords.contains(Coord(3, 2)))
        assertTrue(result.clearedCoords.contains(Coord(2, 1)))
        assertTrue(result.clearedCoords.contains(Coord(2, 3)))
        assertTrue(result.bonusScore >= 1000)
        assertEquals(1, result.giftHeartsClearedCount)
    }

    @Test
    fun testGiftHeartTurnPipelineIntegration() {
        val board = Board.createEmpty(5, 5)
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val color = if ((r + c) % 2 == 0) HeartColor.RED else HeartColor.BLUE
                board.setTile(Coord(r, c), Tile.Normal(color = color))
            }
        }
        val giftHeart = Tile.Special(specialType = SpecialHeartType.GIFT_HEART)
        board.setTile(Coord(2, 2), giftHeart)
        board.setTile(Coord(2, 3), Tile.Special(specialType = SpecialHeartType.BOMB_HEART))

        val giftObjective = Objective(ObjectiveConfig(ObjectiveType.COLLECT_GIFT, targetCount = 1))
        val gameState = GameState(
            levelId = 1,
            status = GameStatus.READY_FOR_INPUT,
            movesRemaining = 10,
            board = board,
            objectives = listOf(giftObjective)
        )

        val tileSpawner = TileSpawner(
            allowedColors = listOf(HeartColor.RED, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
            rng = DeterministicRng(42)
        )
        val pipeline = TurnPipeline(tileSpawner = tileSpawner)

        val turnResult = pipeline.executeSwap(gameState, Coord(2, 2), Coord(2, 3))

        assertTrue(turnResult.isSuccessfulMove)
        assertTrue(giftObjective.isFulfilled)
        assertTrue(gameState.score > 1000)
    }

    @Test
    fun testMatchDetectorAssignsDirectionToFireHeart() {
        val detector = MatchDetector()

        // Horizontal 4
        val boardH = Board.createEmpty(5, 5)
        for (c in 0 until 4) {
            boardH.setTile(Coord(2, c), Tile.Normal(color = HeartColor.RED))
        }
        val matchesH = detector.detectMatches(boardH)
        assertEquals(1, matchesH.size)
        assertEquals(SpecialHeartType.FIRE_HEART, matchesH[0].createdSpecial)
        assertEquals(FireDirection.ROW, matchesH[0].createdSpecialDirection)

        // Vertical 4
        val boardV = Board.createEmpty(5, 5)
        for (r in 0 until 4) {
            boardV.setTile(Coord(r, 2), Tile.Normal(color = HeartColor.RED))
        }
        val matchesV = detector.detectMatches(boardV)
        assertEquals(1, matchesV.size)
        assertEquals(SpecialHeartType.FIRE_HEART, matchesV[0].createdSpecial)
        assertEquals(FireDirection.COLUMN, matchesV[0].createdSpecialDirection)
    }
}
