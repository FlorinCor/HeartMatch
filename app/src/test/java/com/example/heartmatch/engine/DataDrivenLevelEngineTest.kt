package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class DataDrivenLevelEngineTest {

    @Test
    fun testLoadAndPlayJsonLevel() {
        val json = """
        {
          "id": 101,
          "name": "Crimson Valley",
          "board": {
            "rows": 6,
            "columns": 6
          },
          "moves": 20,
          "availableColors": ["RED", "BLUE", "GREEN", "YELLOW"],
          "objectives": [
            {
              "type": "COLLECT",
              "heartType": "RED",
              "amount": 3
            }
          ],
          "difficulty": "EASY",
          "starThresholds": [1000, 2000, 3000]
        }
        """.trimIndent()

        val engine = HeartMatchEngine()
        engine.loadLevelFromJson(json)

        val state = engine.getState()
        assertEquals(101, state.levelId)
        assertEquals(20, state.movesRemaining)
        assertEquals(1, state.objectives.size)
        assertEquals(ObjectiveType.COLLECT_COLOR, state.objectives[0].config.type)
        assertEquals(HeartColor.RED, state.objectives[0].config.targetColor)
        assertEquals(3, state.objectives[0].config.targetCount)

        val moves = engine.getPossibleMoves()
        assertTrue(moves.isNotEmpty())
    }

    @Test
    fun testRepairBrokenHeartsObjective() {
        val json = """
        {
          "id": 102,
          "name": "Broken Repair",
          "board": {
            "rows": 5,
            "columns": 5,
            "grid": [
              ["O", "O", "O", "O", "O"],
              ["O", "RED", "BR(RED)", "RED", "O"],
              ["O", "O", "RED", "O", "O"],
              ["O", "O", "O", "O", "O"],
              ["O", "O", "O", "O", "O"]
            ]
          },
          "moves": 10,
          "objectives": [
            { "type": "REPAIR_BROKEN", "amount": 1 }
          ]
        }
        """.trimIndent()

        val engine = HeartMatchEngine()
        engine.loadLevelFromJson(json)

        val obj = engine.getState().objectives.first()
        assertFalse(obj.isFulfilled)

        // Swap (2, 2) RED into (1, 2) to complete match with (1, 1) and (1, 3) adjacent/hitting BR(RED)
        val result = engine.swap(Coord(2, 2), Coord(1, 2))
        assertTrue(result.isSuccessfulMove)
        assertFalse("A damage hit is not a complete repair", obj.isFulfilled)
        val broken = engine.getState().board.getAllPlayableCoords().first { (engine.getState().board.getTile(it) as? Tile.Blocker)?.blockerType == BlockerType.BROKEN_HEART }
        engine.applyHammer(broken)
        assertTrue("Removing the last layer counts a repair", obj.isFulfilled)
    }

    @Test
    fun testClearDarkHeartsObjective() {
        val json = """
        {
          "id": 103,
          "name": "Dark Corruption",
          "board": {
            "rows": 5,
            "columns": 5,
            "grid": [
              ["RED", "YELLOW", "D", "GREEN", "RED"],
              ["YELLOW", "BLUE", "YELLOW", "BLUE", "GREEN"],
              ["GREEN", "RED", "BLUE", "RED", "YELLOW"],
              ["RED", "YELLOW", "GREEN", "YELLOW", "RED"],
              ["GREEN", "RED", "YELLOW", "RED", "GREEN"]
            ]
          },
          "moves": 10,
          "objectives": [
            { "type": "CLEAR_DARK_HEARTS", "amount": 1 }
          ]
        }
        """.trimIndent()

        val engine = HeartMatchEngine()
        engine.loadLevelFromJson(json)

        val obj = engine.getState().objectives.first()
        assertFalse(obj.isFulfilled)

        val result = engine.swap(Coord(2, 2), Coord(1, 2))
        assertTrue(result.isSuccessfulMove)
        assertTrue("Dark heart objective fulfilled", obj.isFulfilled)
        assertTrue(engine.getState().isWon)
    }

    @Test
    fun testClearSpecificCellsObjective() {
        val json = """
        {
          "id": 104,
          "name": "Cell Targeting",
          "board": {
            "rows": 5,
            "columns": 5,
            "grid": [
              ["O", "O", "O", "O", "O"],
              ["O", "GREEN", "GREEN", "YELLOW", "O"],
              ["O", "O", "O", "GREEN", "O"],
              ["O", "O", "O", "O", "O"],
              ["O", "O", "O", "O", "O"]
            ]
          },
          "moves": 10,
          "objectives": [
            { 
              "type": "CLEAR_SPECIFIC_CELLS", 
              "targetCells": [
                { "row": 1, "col": 1 },
                { "row": 1, "col": 2 }
              ] 
            }
          ]
        }
        """.trimIndent()

        val engine = HeartMatchEngine()
        engine.loadLevelFromJson(json)

        val obj = engine.getState().objectives.first()
        assertEquals(2, obj.config.targetCells.size)
        assertFalse(obj.isFulfilled)

        // Swap (2, 3) GREEN with (1, 3) YELLOW -> forms match at (1, 1), (1, 2), (1, 3)
        val result = engine.swap(Coord(2, 3), Coord(1, 3))
        assertTrue(result.isSuccessfulMove)
        assertTrue("Specific cells cleared objective fulfilled", obj.isFulfilled)
        assertTrue(engine.getState().isWon)
    }

    @Test
    fun testScoreObjective() {
        val json = """
        {
          "id": 105,
          "name": "High Score",
          "board": {
            "rows": 6,
            "columns": 6
          },
          "moves": 15,
          "availableColors": ["RED", "BLUE", "GREEN", "YELLOW"],
          "objectives": [
            { "type": "SCORE", "amount": 300 }
          ],
          "starThresholds": [300, 600, 1000]
        }
        """.trimIndent()

        val engine = HeartMatchEngine()
        engine.loadLevelFromJson(json)

        val moves = engine.getPossibleMoves()
        assertTrue(moves.isNotEmpty())

        val result = engine.swap(moves.first().first, moves.first().second)
        assertTrue(result.isSuccessfulMove)
        assertTrue(engine.getState().score >= 300)
        assertTrue(engine.getState().isWon)
    }

    @Test
    fun testDestroyBlockersAndCollectSpecialsObjectives() {
        val json = """
        {
          "id": 106,
          "name": "Wood and Fire",
          "board": {
            "rows": 5,
            "columns": 5,
            "grid": [
              ["RED", "RED", "YELLOW", "RED", "YELLOW"],
              ["GREEN", "W1", "GREEN", "BLUE", "YELLOW"],
              ["YELLOW", "BLUE", "YELLOW", "BLUE", "GREEN"],
              ["RED", "YELLOW", "GREEN", "YELLOW", "RED"],
              ["GREEN", "RED", "YELLOW", "RED", "GREEN"]
            ]
          },
          "moves": 15,
          "availableColors": ["RED", "BLUE", "GREEN", "YELLOW"],
          "objectives": [
            { "type": "DESTROY_BLOCKERS", "heartType": "WOODEN", "amount": 1 }
          ]
        }
        """.trimIndent()

        val engine = HeartMatchEngine()
        engine.loadLevelFromJson(json)

        val woodObj = engine.getState().objectives[0]
        assertFalse(woodObj.isFulfilled)

        // Swap (0, 2) YELLOW with (0, 1) RED: (0, 0)=RED, (0, 1)=YELLOW, (0, 2)=RED, (0, 3)=RED
        // If we swap (0, 2) YELLOW with (1, 2) GREEN:
        // Let's swap (1, 0) GREEN with (1, 2) GREEN? Adjacent is (1, 1) W1 which is immovable.
        // Let's swap (1, 2) GREEN with (0, 2) YELLOW -> (0, 2) becomes GREEN, (1, 2) becomes YELLOW.
        // Wait, (1, 0) is GREEN, (1, 1) is W1, (1, 2) is GREEN.
        // If (2, 0) is RED, (0, 0)=RED, (0, 1)=RED -> swap (0, 2) YELLOW with (0, 3) RED:
        // (0, 0)=RED, (0, 1)=RED, (0, 2)=RED (match of 3 at row 0).
        // (0, 1) is adjacent to (1, 1) W1!
        val result = engine.swap(Coord(0, 2), Coord(0, 3))
        assertTrue(result.isSuccessfulMove)
        assertTrue("Wooden blocker destroyed objective fulfilled", woodObj.isFulfilled)
        assertTrue(engine.getState().isWon)
    }

    @Test
    fun testClearBoardObjective() {
        val json = """
        {
          "id": 107,
          "name": "Clear Board Mini",
          "board": {
            "rows": 4,
            "columns": 4,
            "grid": [
              ["RED", "RED", "YELLOW", "BLUE"],
              ["GREEN", "BLUE", "RED", "YELLOW"],
              ["YELLOW", "GREEN", "BLUE", "RED"],
              ["BLUE", "YELLOW", "GREEN", "GREEN"]
            ]
          },
          "moves": 10,
          "objectives": [
            { "type": "CLEAR_BOARD", "amount": 3 }
          ]
        }
        """.trimIndent()

        val engine = HeartMatchEngine()
        engine.loadLevelFromJson(json)

        val obj = engine.getState().objectives[0]
        assertFalse(obj.isFulfilled)

        // Swap (0, 2) YELLOW with (1, 2) RED -> row 0 has (0, 0)=RED, (0, 1)=RED, (0, 2)=RED
        val result = engine.swap(Coord(0, 2), Coord(1, 2))
        assertTrue(result.isSuccessfulMove)
        assertTrue(obj.currentCount >= 3)
        assertTrue(obj.isFulfilled)
    }

    @Test
    fun testEngineRejectsInvalidLevelJson() {
        val invalidJson = """
        {
          "id": 999,
          "name": "Broken Level",
          "board": {
            "rows": 1,
            "columns": 1
          },
          "moves": 0,
          "availableColors": ["RED"]
        }
        """.trimIndent()

        val engine = HeartMatchEngine()
        val validation = engine.validateLevelJson(invalidJson)
        assertFalse(validation.isValid)
        assertTrue(validation.errors.size >= 3)

        try {
            engine.loadLevelFromJson(invalidJson)
            fail("Expected IllegalArgumentException on invalid level load")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid level configuration"))
        }
    }
}
