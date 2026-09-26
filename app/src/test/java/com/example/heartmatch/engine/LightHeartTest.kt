package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.MatchDetector
import com.example.heartmatch.engine.core.SpecialEffectHandler
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class LightHeartTest {

    private val detector = MatchDetector()
    private val handler = SpecialEffectHandler()

    /**
     * A large multi-line cluster (a plus/cross of 6+ tiles spanning intersecting
     * horizontal and vertical spans) should create a Light Heart, not a Bomb.
     */
    @Test
    fun testLargeMultiLineClusterCreatesLightHeart() {
        val board = Board.createEmpty(9, 9)
        // Horizontal run of 4: (4,2),(4,3),(4,4),(4,5)
        for (c in 2..5) board.setTile(Coord(4, c), Tile.Normal(color = HeartColor.RED))
        // Vertical run of 4 intersecting at (4,4): (2,4),(3,4),(4,4),(5,4)
        for (r in 2..5) board.setTile(Coord(r, 4), Tile.Normal(color = HeartColor.RED))

        val matches = detector.detectMatches(board)
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(SpecialHeartType.LIGHT_HEART, match.createdSpecial)
        assertNotNull(match.specialSpawnCoord)
        assertTrue(match.matchedCoords.size >= 6)
    }

    /**
     * A small 5-tile T/cross should still create a Bomb Heart (unchanged behavior).
     */
    @Test
    fun testSmallCrossStillCreatesBomb() {
        val board = Board.createEmpty(7, 7)
        board.setTile(Coord(2, 1), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(3, 2), Tile.Normal(color = HeartColor.YELLOW))

        val match = detector.detectMatches(board).first()
        assertEquals(SpecialHeartType.BOMB_HEART, match.createdSpecial)
    }

    /**
     * Activating a Light Heart clears the surrounding 3x3 area.
     */
    @Test
    fun testLightHeartActivationClears3x3() {
        val board = Board.createEmpty(7, 7)
        // Fill a region so the 3x3 around (3,3) has playable tiles
        for (r in 0 until 7) for (c in 0 until 7) {
            board.setTile(Coord(r, c), Tile.Normal(color = HeartColor.BLUE))
        }
        val lightCoord = Coord(3, 3)
        board.setTile(lightCoord, Tile.Special(specialType = SpecialHeartType.LIGHT_HEART, baseColor = HeartColor.BLUE))

        val result = handler.triggerSpecials(
            board,
            listOf(lightCoord to (board.getTile(lightCoord) as Tile.Special))
        )

        // 3x3 block around (3,3) = 9 cells, all should be cleared
        for (r in 2..4) for (c in 2..4) {
            assertTrue("Expected ($r,$c) cleared", Coord(r, c) in result.clearedCoords)
        }
        // A tile well outside the 3x3 must not be cleared by the light burst alone
        assertFalse(Coord(0, 0) in result.clearedCoords)
        assertEquals(400, result.bonusScore)
    }
}
