package com.example.heartmatch.ui

import com.example.heartmatch.data.LevelRecord
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.ui.components.LevelNodeStatus
import com.example.heartmatch.ui.model.MapAreaId
import com.example.heartmatch.ui.model.MapAreaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelMapSystemTest {

    @Test
    fun testAllFiveThemedAreasMapping() {
        // Area 1: Heart Meadow (1-20)
        for (lvl in 1..20) {
            assertEquals(MapAreaId.HEART_MEADOW, MapAreaId.fromLevel(lvl))
            val theme = MapAreaTheme.getThemeForLevel(lvl)
            assertEquals("Heart Meadow", theme.name)
            assertEquals("🌸", theme.iconEmoji)
            assertEquals("Levels 1 - 20", theme.levelRangeText)
        }

        // Area 2: Stone Valley (21-40)
        for (lvl in 21..40) {
            assertEquals(MapAreaId.STONE_VALLEY, MapAreaId.fromLevel(lvl))
            val theme = MapAreaTheme.getThemeForLevel(lvl)
            assertEquals("Stone Valley", theme.name)
            assertEquals("⛰️", theme.iconEmoji)
            assertEquals("Levels 21 - 40", theme.levelRangeText)
        }

        // Area 3: Broken Hearts Forest (41-60)
        for (lvl in 41..60) {
            assertEquals(MapAreaId.BROKEN_FOREST, MapAreaId.fromLevel(lvl))
            val theme = MapAreaTheme.getThemeForLevel(lvl)
            assertEquals("Broken Hearts Forest", theme.name)
            assertEquals("🔮", theme.iconEmoji)
            assertEquals("Levels 41 - 60", theme.levelRangeText)
        }

        // Area 4: Shadow Garden (61-80)
        for (lvl in 61..80) {
            assertEquals(MapAreaId.SHADOW_GARDEN, MapAreaId.fromLevel(lvl))
            val theme = MapAreaTheme.getThemeForLevel(lvl)
            assertEquals("Shadow Garden", theme.name)
            assertEquals("🥀", theme.iconEmoji)
            assertEquals("Levels 61 - 80", theme.levelRangeText)
        }

        // Area 5: Heart Kingdom (81-100)
        for (lvl in 81..100) {
            assertEquals(MapAreaId.HEART_KINGDOM, MapAreaId.fromLevel(lvl))
            val theme = MapAreaTheme.getThemeForLevel(lvl)
            assertEquals("Heart Kingdom", theme.name)
            assertEquals("👑", theme.iconEmoji)
            assertEquals("Levels 81 - 100", theme.levelRangeText)
        }
    }

    @Test
    fun testAreaThemePalettesAndGradients() {
        val allAreas = MapAreaId.values()
        assertEquals(5, allAreas.size)

        allAreas.forEach { areaId ->
            val theme = MapAreaTheme.getThemeForArea(areaId)
            assertEquals(areaId, theme.id)
            assertTrue(theme.name.isNotEmpty())
            assertTrue(theme.subtitle.isNotEmpty())
            assertTrue(theme.levelRangeText.isNotEmpty())
            assertTrue(theme.iconEmoji.isNotEmpty())
            assertTrue(theme.backgroundColors.size >= 2)
            assertTrue(theme.nodeGradientColors.size >= 2)
            assertTrue(theme.completedNodeGradientColors.size >= 2)
            assertTrue(theme.bannerGradientColors.size >= 2)
            assertNotNull(theme.primaryAccent)
            assertNotNull(theme.secondaryAccent)
            assertNotNull(theme.pathColor)
        }
    }

    @Test
    fun testLevelProgressionAndNodeStates() {
        val playerProfile = PlayerProfile(highestUnlockedLevel = 25)

        // Test level 1 (completed with 3 stars)
        val level1Record = LevelRecord(levelId = 1, stars = 3, highScore = 25000, isCompleted = true)
        val isLevel1Unlocked = 1 <= playerProfile.highestUnlockedLevel
        val isLevel1Current = 1 == playerProfile.highestUnlockedLevel
        assertTrue(isLevel1Unlocked)
        assertFalse(isLevel1Current)
        val level1Status = when {
            isLevel1Current -> LevelNodeStatus.CURRENT
            !isLevel1Unlocked -> LevelNodeStatus.LOCKED
            level1Record.stars > 0 -> LevelNodeStatus.COMPLETED
            else -> LevelNodeStatus.UNLOCKED
        }
        assertEquals(LevelNodeStatus.COMPLETED, level1Status)

        // Test level 25 (current active level, 0 stars)
        val level25Record = LevelRecord(levelId = 25, stars = 0, highScore = 0, isCompleted = false)
        val isLevel25Unlocked = 25 <= playerProfile.highestUnlockedLevel
        val isLevel25Current = 25 == playerProfile.highestUnlockedLevel
        assertTrue(isLevel25Unlocked)
        assertTrue(isLevel25Current)
        val level25Status = when {
            isLevel25Current -> LevelNodeStatus.CURRENT
            !isLevel25Unlocked -> LevelNodeStatus.LOCKED
            level25Record.stars > 0 -> LevelNodeStatus.COMPLETED
            else -> LevelNodeStatus.UNLOCKED
        }
        assertEquals(LevelNodeStatus.CURRENT, level25Status)

        // Test level 26 (locked level)
        val level26Record = LevelRecord(levelId = 26, stars = 0, highScore = 0, isCompleted = false)
        val isLevel26Unlocked = 26 <= playerProfile.highestUnlockedLevel
        val isLevel26Current = 26 == playerProfile.highestUnlockedLevel
        assertFalse(isLevel26Unlocked)
        assertFalse(isLevel26Current)
        val level26Status = when {
            isLevel26Current -> LevelNodeStatus.CURRENT
            !isLevel26Unlocked -> LevelNodeStatus.LOCKED
            level26Record.stars > 0 -> LevelNodeStatus.COMPLETED
            else -> LevelNodeStatus.UNLOCKED
        }
        assertEquals(LevelNodeStatus.LOCKED, level26Status)
    }

    @Test
    fun testCompletedLevelsStarCounts() {
        val records = mapOf(
            1 to LevelRecord(1, 1, 1000, true),
            2 to LevelRecord(2, 2, 2000, true),
            3 to LevelRecord(3, 3, 3000, true),
            4 to LevelRecord(4, 0, 0, false)
        )

        assertEquals(1, records[1]?.stars)
        assertEquals(2, records[2]?.stars)
        assertEquals(3, records[3]?.stars)
        assertEquals(0, records[4]?.stars)

        // Verify star summation for zone
        val totalZoneStars = (1..3).sumOf { records[it]?.stars ?: 0 }
        assertEquals(6, totalZoneStars)
    }

    @Test
    fun testMapAreaLevelRanges() {
        assertEquals(1, MapAreaId.HEART_MEADOW.startLevel)
        assertEquals(20, MapAreaId.HEART_MEADOW.endLevel)

        assertEquals(21, MapAreaId.STONE_VALLEY.startLevel)
        assertEquals(40, MapAreaId.STONE_VALLEY.endLevel)

        assertEquals(41, MapAreaId.BROKEN_FOREST.startLevel)
        assertEquals(60, MapAreaId.BROKEN_FOREST.endLevel)

        assertEquals(61, MapAreaId.SHADOW_GARDEN.startLevel)
        assertEquals(80, MapAreaId.SHADOW_GARDEN.endLevel)

        assertEquals(81, MapAreaId.HEART_KINGDOM.startLevel)
        assertEquals(100, MapAreaId.HEART_KINGDOM.endLevel)

        // 20 levels per area * 5 areas = 100 levels total
        val totalLevels = MapAreaId.values().sumOf { it.endLevel - it.startLevel + 1 }
        assertEquals(100, totalLevels)
    }
}
