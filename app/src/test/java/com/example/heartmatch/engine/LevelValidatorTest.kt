package com.example.heartmatch.engine

import com.example.heartmatch.engine.loader.LevelValidator
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class LevelValidatorTest {

    private val validator = LevelValidator()

    @Test
    fun testValidLevelPasses() {
        val config = LevelConfig(
            id = 1,
            name = "Valid Level",
            rows = 8,
            cols = 8,
            allowedColors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN),
            moveLimit = 25,
            starThresholds = Triple(1000, 2000, 3000),
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.COLLECT_COLOR, targetColor = HeartColor.RED, targetCount = 10),
                ObjectiveConfig(type = ObjectiveType.DESTROY_BLOCKERS, targetBlocker = BlockerType.STONE_HEART, targetCount = 4)
            )
        )

        val result = validator.validate(config)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        result.assertValid() // Should not throw
    }

    @Test
    fun testInvalidDimensions() {
        val configSmall = LevelConfig(id = 1, name = "Too Small", rows = 2, cols = 8)
        val resultSmall = validator.validate(configSmall)
        assertFalse(resultSmall.isValid)
        assertTrue(resultSmall.errors.any { it.contains("rows must be between") })

        val configLarge = LevelConfig(id = 1, name = "Too Large", rows = 8, cols = 35)
        val resultLarge = validator.validate(configLarge)
        assertFalse(resultLarge.isValid)
        assertTrue(resultLarge.errors.any { it.contains("columns must be between") })
    }

    @Test
    fun testMissingMoveAndTimeLimit() {
        val config = LevelConfig(
            id = 1,
            name = "No limits",
            rows = 8,
            cols = 8,
            moveLimit = 0,
            timeLimitSeconds = null
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("positive moveLimit or positive timeLimitSeconds") })
    }

    @Test
    fun testInsufficientColors() {
        val config = LevelConfig(
            id = 1,
            name = "2 Colors",
            rows = 8,
            cols = 8,
            allowedColors = listOf(HeartColor.RED, HeartColor.BLUE)
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("at least 3 distinct allowed colors") })
    }

    @Test
    fun testInvalidStarThresholds() {
        val configDescending = LevelConfig(
            id = 1,
            name = "Bad Stars",
            rows = 8,
            cols = 8,
            starThresholds = Triple(5000, 4000, 8000)
        )
        val result = validator.validate(configDescending)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("starThresholds must be positive and strictly ascending") })
    }

    @Test
    fun testObjectiveColorNotInAllowedColors() {
        val config = LevelConfig(
            id = 1,
            name = "Unobtainable Color",
            rows = 8,
            cols = 8,
            allowedColors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE),
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.COLLECT_COLOR, targetColor = HeartColor.PURPLE, targetCount = 10)
            )
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("is not in level's allowedColors") })
    }

    @Test
    fun testNegativeTargetCount() {
        val config = LevelConfig(
            id = 1,
            name = "Negative Target",
            rows = 8,
            cols = 8,
            objectives = listOf(
                ObjectiveConfig(type = ObjectiveType.DESTROY_BLOCKERS, targetCount = -5)
            )
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("targetCount must be > 0") })
    }

    @Test
    fun testOutOfBoundsSpecificCells() {
        val config = LevelConfig(
            id = 1,
            name = "Bad Coords",
            rows = 5,
            cols = 5,
            objectives = listOf(
                ObjectiveConfig(
                    type = ObjectiveType.CLEAR_SPECIFIC_CELLS,
                    targetCells = setOf(Coord(2, 2), Coord(8, 8))
                )
            )
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("out of board bounds") })
    }
}
