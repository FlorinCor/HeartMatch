package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.core.MatchDetector
import com.example.heartmatch.engine.loader.LevelGenerator
import com.example.heartmatch.engine.loader.LevelJsonParser
import com.example.heartmatch.engine.loader.LevelRepository
import com.example.heartmatch.engine.loader.LevelValidator
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class LevelValidationSuiteTest {

    companion object {
        private val allLevels: Map<Int, LevelConfig> = (1..100).associateWith { id ->
            LevelGenerator.getLevelConfig(id)
        }

        @JvmStatic
        @BeforeClass
        fun exportAllLevelJsonFiles() {
            val assetsDir = File("src/main/assets/levels")
            val resourcesDir = File("src/main/resources/levels")
            assetsDir.mkdirs()
            resourcesDir.mkdirs()

            for (id in 1..100) {
                val json = LevelGenerator.generateLevelJson(id)
                val fileNameFormatted = String.format("level_%03d.json", id)
                val fileNameSimple = "level_$id.json"

                File(assetsDir, fileNameFormatted).writeText(json)
                File(assetsDir, fileNameSimple).writeText(json)
                File(resourcesDir, fileNameFormatted).writeText(json)
                File(resourcesDir, fileNameSimple).writeText(json)
            }
        }
    }

    private val validator = LevelValidator()
    private val parser = LevelJsonParser()
    private val matchDetector = MatchDetector()

    @Test
    fun testAll100LevelsAreGeneratedAndSyntacticallyValid() {
        assertEquals(100, allLevels.size)
        for (id in 1..100) {
            val config = allLevels[id]
            assertNotNull("Level $id must exist", config)
            assertEquals("Level id must match $id", id, config!!.id)
            assertTrue("Level name must not be blank", config.name.isNotBlank())
        }
    }

    @Test
    fun testAll100LevelsPassLevelValidator() {
        for (id in 1..100) {
            val config = allLevels[id]!!
            val result = validator.validate(config)
            assertTrue("Level $id failed validation:\n" + result.errors.joinToString("\n- ", prefix = "- "), result.isValid)
        }
    }

    @Test
    fun testValidBoardConfigurations() {
        for (id in 1..100) {
            val config = allLevels[id]!!
            assertTrue("Level $id rows must be between 6 and 12 (got ${config.rows})", config.rows in 6..12)
            assertTrue("Level $id cols must be between 6 and 12 (got ${config.cols})", config.cols in 6..12)

            val board = Board.createWithLayout(config.rows, config.cols, config.cellStates, config.initialTiles)
            val playableCoords = board.getAllPlayableCoords()
            assertTrue("Level $id must have at least 25 playable cells (got ${playableCoords.size})", playableCoords.size >= 25)

            // Check that unavailable cells have null tiles
            board.forEachCell { cell ->
                if (cell.isUnavailable) {
                    assertNull("Unavailable cell at ${cell.coord} in Level $id must have null tile", cell.tile)
                }
            }
        }
    }

    @Test
    fun testAchievableObjectives() {
        for (id in 1..100) {
            val config = allLevels[id]!!
            assertFalse("Level $id must have at least one objective", config.objectives.isEmpty())

            val board = Board.createWithLayout(config.rows, config.cols, config.cellStates, config.initialTiles)
            val initialBlockers = mutableMapOf<BlockerType, Int>()
            var initialGifts = 0

            board.forEachCell { cell ->
                val tile = cell.tile
                if (tile is Tile.Blocker) {
                    initialBlockers[tile.blockerType] = (initialBlockers[tile.blockerType] ?: 0) + 1
                }
                if (tile is Tile.Special && tile.specialType == SpecialHeartType.GIFT_HEART) {
                    initialGifts++
                }
            }

            for (obj in config.objectives) {
                assertTrue("Objective target count must be > 0 in Level $id", obj.targetCount > 0)

                when (obj.type) {
                    ObjectiveType.COLLECT_COLOR, ObjectiveType.COLLECT_HEARTS -> {
                        if (obj.targetColor != null) {
                            assertTrue("Target color ${obj.targetColor} in Level $id must be in allowedColors",
                                obj.targetColor in config.allowedColors)
                        }
                    }
                    ObjectiveType.DESTROY_BLOCKERS, ObjectiveType.CLEAR_BLOCKER -> {
                        if (obj.targetBlocker != null) {
                            val count = initialBlockers[obj.targetBlocker] ?: 0
                            assertTrue("Level $id requires destroying ${obj.targetCount} ${obj.targetBlocker}, but board only has $count",
                                count >= obj.targetCount)
                        }
                    }
                    ObjectiveType.CLEAR_DARK_HEARTS -> {
                        val count = initialBlockers[BlockerType.DARK_HEART] ?: 0
                        assertTrue("Level $id requires clearing ${obj.targetCount} Dark Hearts, but board only has $count",
                            count >= obj.targetCount)
                    }
                    ObjectiveType.REPAIR_BROKEN -> {
                        val count = initialBlockers[BlockerType.BROKEN_HEART] ?: 0
                        assertTrue("Level $id requires repairing ${obj.targetCount} Broken Hearts, but board only has $count",
                            count >= obj.targetCount)
                    }
                    ObjectiveType.COLLECT_SPECIAL, ObjectiveType.COLLECT_GIFT -> {
                        if (obj.targetSpecial == SpecialHeartType.GIFT_HEART) {
                            assertTrue("Level $id requires collecting ${obj.targetCount} Gift Hearts, but board has $initialGifts",
                                initialGifts >= obj.targetCount)
                        }
                    }
                    ObjectiveType.SCORE, ObjectiveType.REACH_SCORE -> {
                        assertTrue("Score objective in Level $id must be <= 1-star threshold",
                            obj.targetCount <= config.starThresholds.first)
                    }
                    else -> {}
                }
            }
        }
    }

    @Test
    fun testValidMoveAvailabilityAndNoImmediateImpossibleState() {
        for (id in 1..100) {
            val config = allLevels[id]!!
            val engine = HeartMatchEngine(config)

            // 1. Initial engine status must be READY_FOR_INPUT
            assertEquals("Level $id must start in READY_FOR_INPUT status",
                GameStatus.READY_FOR_INPUT, engine.getState().status)

            // 2. Starting board must have NO immediate matches
            val initialMatches = matchDetector.detectMatches(engine.getState().board)
            assertTrue("Level $id starting board must not have immediate matches before player moves (found ${initialMatches.size})",
                initialMatches.isEmpty())

            // 3. Starting board must have at least one valid move available
            val possibleMoves = engine.getPossibleMoves()
            assertTrue("Level $id starting board must have valid moves available (found ${possibleMoves.size})",
                possibleMoves.isNotEmpty())
        }
    }

    @Test
    fun testReasonableDifficultyProgression() {
        for (id in 1..100) {
            val config = allLevels[id]!!

            // Move limits between 15 and 35
            assertTrue("Level $id move limit (${config.moveLimit}) must be between 15 and 35",
                config.moveLimit in 15..35)

            // Star thresholds strictly ascending
            val (s1, s2, s3) = config.starThresholds
            assertTrue("Level $id star thresholds must strictly ascend: $s1 < $s2 < $s3",
                s1 < s2 && s2 < s3)

            // Color counts based on tiers
            val colorCount = config.allowedColors.size
            when (id) {
                in 1..10 -> assertTrue("Tier 1 Level $id color count ($colorCount) must be 3 or 4", colorCount in 3..5)
                in 11..20 -> assertTrue("Tier 2 Level $id color count ($colorCount) must be 4 or 5", colorCount in 4..5)
                in 21..40 -> assertTrue("Tier 3 Level $id color count ($colorCount) must be 4 to 6", colorCount in 4..6)
                in 41..60 -> assertTrue("Tier 4 Level $id color count ($colorCount) must be 5 or 6", colorCount in 5..6)
                in 61..80 -> assertTrue("Tier 5 Level $id color count ($colorCount) must be 5 or 6", colorCount in 5..6)
                in 81..100 -> assertTrue("Tier 6 Level $id color count ($colorCount) must be 5 to 7", colorCount in 5..7)
            }
        }
    }

    @Test
    fun testTierProgressionRequirements() {
        // Levels 1-10: Tutorial & basic matching
        for (id in 1..10) {
            val config = allLevels[id]!!
            assertEquals("Levels 1-10 must be EASY", LevelDifficulty.EASY, config.difficulty)
        }

        // Levels 11-20: Introduce Stone & Gift Hearts
        val tier2 = (11..20).map { allLevels[it]!! }
        val hasStone = tier2.any { cfg ->
            cfg.objectives.any { it.targetBlocker == BlockerType.STONE_HEART }
        }
        val hasGift = tier2.any { cfg ->
            cfg.objectives.any { it.targetSpecial == SpecialHeartType.GIFT_HEART }
        }
        assertTrue("Tier 2 (Levels 11-20) must introduce Stone Hearts", hasStone)
        assertTrue("Tier 2 (Levels 11-20) must introduce Gift Hearts", hasGift)

        // Levels 21-40: Introduce Broken, Ice, Wooden Hearts
        val tier3 = (21..40).map { allLevels[it]!! }
        val hasBroken = tier3.any { cfg ->
            cfg.objectives.any { it.type == ObjectiveType.REPAIR_BROKEN }
        }
        val hasIce = tier3.any { cfg ->
            cfg.objectives.any { it.targetBlocker == BlockerType.ICE_HEART }
        }
        val hasWooden = tier3.any { cfg ->
            cfg.objectives.any { it.targetBlocker == BlockerType.WOODEN_HEART }
        }
        assertTrue("Tier 3 (Levels 21-40) must introduce Broken Hearts", hasBroken)
        assertTrue("Tier 3 (Levels 21-40) must introduce Ice Hearts", hasIce)
        assertTrue("Tier 3 (Levels 21-40) must introduce Wooden Hearts", hasWooden)

        // Levels 41-60: Introduce Dark Hearts
        val tier4 = (41..60).map { allLevels[it]!! }
        val hasDark = tier4.any { cfg ->
            cfg.objectives.any { it.type == ObjectiveType.CLEAR_DARK_HEARTS || it.targetBlocker == BlockerType.DARK_HEART }
        }
        assertTrue("Tier 4 (Levels 41-60) must introduce Dark Hearts", hasDark)

        // Levels 61-80: Introduce Chained Hearts & complex layouts
        val tier5 = (61..80).map { allLevels[it]!! }
        val hasChained = tier5.any { cfg ->
            cfg.objectives.any { it.targetBlocker == BlockerType.CHAINED_HEART }
        }
        assertTrue("Tier 5 (Levels 61-80) must introduce Chained Hearts", hasChained)

        // Levels 81-100: Expert levels combining multiple mechanics
        for (id in 81..100) {
            val config = allLevels[id]!!
            assertEquals("Levels 81-100 must be EXPERT difficulty", LevelDifficulty.EXPERT, config.difficulty)
            assertTrue("Expert levels must feature multiple objectives (got ${config.objectives.size} on Level $id)",
                config.objectives.size >= 2)
        }
    }

    @Test
    fun testLevelRepositoryLoadsFromDiskAndGenerator() {
        val repo = LevelRepository(assetsDir = File("src/main/assets/levels"))
        val level1 = repo.getLevel(1)
        assertEquals(1, level1.id)
        assertEquals("First Spark", level1.name)

        val level100 = repo.getLevel(100)
        assertEquals(100, level100.id)
        assertEquals("Heart Match Master Champion", level100.name)

        val all = repo.getAllLevels()
        assertEquals(100, all.size)
    }
}
