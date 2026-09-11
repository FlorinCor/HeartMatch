package com.example.heartmatch.engine

import com.example.heartmatch.engine.loader.LevelJsonParser
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class LevelJsonParserTest {

    private val parser = LevelJsonParser()

    @Test
    fun testParseSamplePromptLevelJson() {
        val json = """
        {
          "id": 12,
          "name": "Stone Valley",
          "board": {
            "rows": 8,
            "columns": 8
          },
          "moves": 24,
          "availableColors": [
            "RED",
            "PINK",
            "BLUE",
            "GREEN",
            "YELLOW",
            "PURPLE"
          ],
          "objectives": [
            {
              "type": "COLLECT",
              "heartType": "RED",
              "amount": 20
            },
            {
              "type": "DESTROY",
              "heartType": "STONE",
              "amount": 8
            },
            {
              "type": "COLLECT",
              "heartType": "GIFT",
              "amount": 2
            }
          ],
          "difficulty": "INTERMEDIATE",
          "starThresholds": [
            15000,
            30000,
            50000
          ]
        }
        """.trimIndent()

        val config = parser.parse(json)
        assertEquals(12, config.id)
        assertEquals("Stone Valley", config.name)
        assertEquals(8, config.rows)
        assertEquals(8, config.cols)
        assertEquals(24, config.moveLimit)
        assertEquals(LevelDifficulty.INTERMEDIATE, config.difficulty)
        assertEquals(Triple(15000, 30000, 50000), config.starThresholds)
        assertEquals(6, config.allowedColors.size)
        assertTrue(config.allowedColors.contains(HeartColor.RED))
        assertTrue(config.allowedColors.contains(HeartColor.PURPLE))

        assertEquals(3, config.objectives.size)

        // Objective 1: COLLECT RED 20
        assertEquals(ObjectiveType.COLLECT_COLOR, config.objectives[0].type)
        assertEquals(HeartColor.RED, config.objectives[0].targetColor)
        assertEquals(20, config.objectives[0].targetCount)

        // Objective 2: DESTROY STONE 8
        assertEquals(ObjectiveType.DESTROY_BLOCKERS, config.objectives[1].type)
        assertEquals(BlockerType.STONE_HEART, config.objectives[1].targetBlocker)
        assertEquals(8, config.objectives[1].targetCount)

        // Objective 3: COLLECT GIFT 2
        assertEquals(ObjectiveType.COLLECT_SPECIAL, config.objectives[2].type)
        assertEquals(SpecialHeartType.GIFT_HEART, config.objectives[2].targetSpecial)
        assertEquals(2, config.objectives[2].targetCount)
    }

    @Test
    fun testParseAllEightObjectiveTypes() {
        val json = """
        {
          "id": 99,
          "name": "Objective Showcase",
          "rows": 7,
          "cols": 7,
          "moveLimit": 35,
          "objectives": [
            { "type": "COLLECT_COLOR", "heartType": "PINK", "amount": 15 },
            { "type": "DESTROY_BLOCKERS", "heartType": "ICE", "amount": 6 },
            { "type": "REPAIR_BROKEN", "amount": 4 },
            { "type": "CLEAR_DARK_HEARTS", "amount": 3 },
            { "type": "COLLECT_SPECIAL", "heartType": "BOMB", "amount": 5 },
            { "type": "SCORE", "amount": 25000 },
            { "type": "CLEAR_BOARD", "amount": 30 },
            { 
              "type": "CLEAR_SPECIFIC_CELLS", 
              "targetCells": [
                { "row": 0, "col": 0 },
                { "row": 0, "col": 6 },
                { "row": 6, "col": 0 },
                { "row": 6, "col": 6 }
              ] 
            }
          ]
        }
        """.trimIndent()

        val config = parser.parse(json)
        assertEquals(8, config.objectives.size)

        assertEquals(ObjectiveType.COLLECT_COLOR, config.objectives[0].type)
        assertEquals(HeartColor.PINK, config.objectives[0].targetColor)
        assertEquals(15, config.objectives[0].targetCount)

        assertEquals(ObjectiveType.DESTROY_BLOCKERS, config.objectives[1].type)
        assertEquals(BlockerType.ICE_HEART, config.objectives[1].targetBlocker)

        assertEquals(ObjectiveType.REPAIR_BROKEN, config.objectives[2].type)
        assertEquals(4, config.objectives[2].targetCount)

        assertEquals(ObjectiveType.CLEAR_DARK_HEARTS, config.objectives[3].type)
        assertEquals(3, config.objectives[3].targetCount)

        assertEquals(ObjectiveType.COLLECT_SPECIAL, config.objectives[4].type)
        assertEquals(SpecialHeartType.BOMB_HEART, config.objectives[4].targetSpecial)

        assertEquals(ObjectiveType.SCORE, config.objectives[5].type)
        assertEquals(25000, config.objectives[5].targetCount)

        assertEquals(ObjectiveType.CLEAR_BOARD, config.objectives[6].type)
        assertEquals(30, config.objectives[6].targetCount)

        assertEquals(ObjectiveType.CLEAR_SPECIFIC_CELLS, config.objectives[7].type)
        assertEquals(4, config.objectives[7].targetCells.size)
        assertTrue(config.objectives[7].targetCells.contains(Coord(0, 0)))
        assertTrue(config.objectives[7].targetCells.contains(Coord(6, 6)))
        assertEquals(4, config.objectives[7].targetCount)
    }

    @Test
    fun testParseBoardLayoutWithBlockersAndHoles() {
        val json = """
        {
          "id": 5,
          "name": "Ruined Temple",
          "board": {
            "rows": 4,
            "columns": 4,
            "grid": [
              ["O", "X", "S3", "I(BLUE)"],
              ["W2", "O", "BR(RED)", "C(YELLOW)"],
              ["D", "GIFT", "FIRE_H", "BOMB"],
              ["RAINBOW", "B", "O", "O"]
            ]
          },
          "moves": 20
        }
        """.trimIndent()

        val config = parser.parse(json)
        assertNotNull(config.cellStates)
        assertNotNull(config.initialTiles)

        // (0, 0) is Open/Playable
        assertEquals(CellState.PLAYABLE, config.cellStates!![0][0])
        assertNull(config.initialTiles!![0][0])

        // (0, 1) is Unavailable / Hole
        assertEquals(CellState.UNAVAILABLE, config.cellStates!![0][1])

        // (0, 2) is Stone (durability 3)
        val stone = config.initialTiles!![0][2] as Tile.Blocker
        assertEquals(BlockerType.STONE_HEART, stone.blockerType)
        assertEquals(3, stone.durability)

        // (0, 3) is Ice with BLUE payload
        val ice = config.initialTiles!![0][3] as Tile.Blocker
        assertEquals(BlockerType.ICE_HEART, ice.blockerType)
        assertEquals(HeartColor.BLUE, ice.matchColor)

        // (1, 0) is Wooden with 2 layers
        val wood = config.initialTiles!![1][0] as Tile.Blocker
        assertEquals(BlockerType.WOODEN_HEART, wood.blockerType)
        assertEquals(2, wood.layers)

        // (1, 2) is Broken (RED)
        val broken = config.initialTiles!![1][2] as Tile.Blocker
        assertEquals(BlockerType.BROKEN_HEART, broken.blockerType)
        assertEquals(HeartColor.RED, broken.color)

        // (1, 3) is Chained (YELLOW)
        val chained = config.initialTiles!![1][3] as Tile.Blocker
        assertEquals(BlockerType.CHAINED_HEART, chained.blockerType)
        assertEquals(HeartColor.YELLOW, chained.color)

        // (2, 0) is Dark Heart
        val dark = config.initialTiles!![2][0] as Tile.Blocker
        assertEquals(BlockerType.DARK_HEART, dark.blockerType)

        // (2, 1) is Gift Heart
        val gift = config.initialTiles!![2][1] as Tile.Special
        assertEquals(SpecialHeartType.GIFT_HEART, gift.specialType)

        // (2, 2) is Fire Heart ROW
        val fire = config.initialTiles!![2][2] as Tile.Special
        assertEquals(SpecialHeartType.FIRE_HEART, fire.specialType)
        assertEquals(FireDirection.ROW, fire.fireDirection)

        // (2, 3) is Bomb Heart
        val bomb = config.initialTiles!![2][3] as Tile.Special
        assertEquals(SpecialHeartType.BOMB_HEART, bomb.specialType)

        // (3, 0) is Rainbow Heart
        val rainbow = config.initialTiles!![3][0] as Tile.Special
        assertEquals(SpecialHeartType.RAINBOW_HEART, rainbow.specialType)

        // (3, 1) is Barbed Heart
        val barbed = config.initialTiles!![3][1] as Tile.Blocker
        assertEquals(BlockerType.BARBED_HEART, barbed.blockerType)
    }

    @Test
    fun testParseCustomBlockerConfig() {
        val json = """
        {
          "id": 8,
          "name": "Tough Blockers",
          "moves": 20,
          "blockerConfig": {
            "stoneDurability": 4,
            "iceHitPoints": 3,
            "woodenLayers": 5,
            "darkHeartSpreadTurns": 2
          }
        }
        """.trimIndent()

        val config = parser.parse(json)
        assertEquals(4, config.blockerConfig.stoneDurability)
        assertEquals(3, config.blockerConfig.iceHitPoints)
        assertEquals(5, config.blockerConfig.woodenLayers)
        assertEquals(2, config.blockerConfig.darkHeartSpreadTurns)
    }
}
