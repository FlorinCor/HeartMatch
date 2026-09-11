package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.MatchDetector
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class MatchDetectorTest {

    private val detector = MatchDetector()

    @Test
    fun testHorizontal3() {
        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(1, 1), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(1, 3), Tile.Normal(color = HeartColor.RED))

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(HeartColor.RED, match.color)
        assertEquals(MatchShape.HORIZONTAL_3, match.shape)
        assertEquals(3, match.matchedCoords.size)
        assertNull(match.createdSpecial)
    }

    @Test
    fun testVertical3() {
        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.BLUE))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.BLUE))
        board.setTile(Coord(3, 2), Tile.Normal(color = HeartColor.BLUE))

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(HeartColor.BLUE, match.color)
        assertEquals(MatchShape.VERTICAL_3, match.shape)
        assertEquals(3, match.matchedCoords.size)
        assertNull(match.createdSpecial)
    }

    @Test
    fun testHorizontal4CreatesFireHeart() {
        val board = Board.createEmpty(6, 6)
        board.setTile(Coord(2, 1), Tile.Normal(color = HeartColor.PINK))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.PINK))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.PINK))
        board.setTile(Coord(2, 4), Tile.Normal(color = HeartColor.PINK))

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(MatchShape.HORIZONTAL_4, match.shape)
        assertEquals(SpecialHeartType.FIRE_HEART, match.createdSpecial)
        assertNotNull(match.specialSpawnCoord)
    }

    @Test
    fun testVertical4CreatesFireHeart() {
        val board = Board.createEmpty(6, 6)
        board.setTile(Coord(1, 3), Tile.Normal(color = HeartColor.GREEN))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.GREEN))
        board.setTile(Coord(3, 3), Tile.Normal(color = HeartColor.GREEN))
        board.setTile(Coord(4, 3), Tile.Normal(color = HeartColor.GREEN))

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(MatchShape.VERTICAL_4, match.shape)
        assertEquals(SpecialHeartType.FIRE_HEART, match.createdSpecial)
        assertNotNull(match.specialSpawnCoord)
    }

    @Test
    fun testFiveInARowCreatesRainbowHeart() {
        val board = Board.createEmpty(7, 7)
        for (c in 1..5) {
            board.setTile(Coord(3, c), Tile.Normal(color = HeartColor.PURPLE))
        }

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(MatchShape.FIVE_IN_A_ROW, match.shape)
        assertEquals(SpecialHeartType.RAINBOW_HEART, match.createdSpecial)
    }

    @Test
    fun testTShapeCreatesBombHeart() {
        val board = Board.createEmpty(7, 7)
        // Horizontal: (2, 1), (2, 2), (2, 3)
        // Vertical stem going down from middle: (2, 2), (3, 2), (4, 2)
        board.setTile(Coord(2, 1), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(3, 2), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(4, 2), Tile.Normal(color = HeartColor.YELLOW))

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(MatchShape.T_SHAPE, match.shape)
        assertEquals(SpecialHeartType.BOMB_HEART, match.createdSpecial)
        assertEquals(Coord(2, 2), match.specialSpawnCoord)
    }

    @Test
    fun testCrossShapeCreatesBombHeart() {
        val board = Board.createEmpty(7, 7)
        // Horizontal: (2, 1), (2, 2), (2, 3)
        // Vertical: (1, 2), (2, 2), (3, 2)
        board.setTile(Coord(2, 1), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(3, 2), Tile.Normal(color = HeartColor.YELLOW))

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(MatchShape.CROSS_OR_MULTI, match.shape)
        assertEquals(SpecialHeartType.BOMB_HEART, match.createdSpecial)
        assertEquals(Coord(2, 2), match.specialSpawnCoord)
    }

    @Test
    fun testLShapeCreatesBombHeart() {
        val board = Board.createEmpty(7, 7)
        // Horizontal: (2, 2), (2, 3), (2, 4)
        // Vertical: (2, 2), (3, 2), (4, 2)
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.ORANGE))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.ORANGE))
        board.setTile(Coord(2, 4), Tile.Normal(color = HeartColor.ORANGE))
        board.setTile(Coord(3, 2), Tile.Normal(color = HeartColor.ORANGE))
        board.setTile(Coord(4, 2), Tile.Normal(color = HeartColor.ORANGE))

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(MatchShape.L_SHAPE, match.shape)
        assertEquals(SpecialHeartType.BOMB_HEART, match.createdSpecial)
        assertEquals(Coord(2, 2), match.specialSpawnCoord)
    }

    @Test
    fun testImmovableBlockerDoesNotMatch() {
        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(2, 1), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(2, 2), Tile.Blocker(blockerType = BlockerType.STONE_HEART))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.RED))

        val matches = detector.detectMatches(board)
        assertTrue(matches.isEmpty())
    }
}
