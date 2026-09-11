package com.example.heartmatch.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.heartmatch.data.LevelRecord
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.ui.components.HeartColors
import com.example.heartmatch.ui.components.createBurstParticles
import com.example.heartmatch.ui.components.getGlossColor
import com.example.heartmatch.ui.components.getGradients
import com.example.heartmatch.ui.navigation.Screen
import com.example.heartmatch.ui.screens.dailyRewardsList
import org.junit.Assert.*
import org.junit.Test

class FrontendSystemTest {

    @Test
    fun testNavigationScreens() {
        val screens: List<Screen> = listOf(
            Screen.Splash,
            Screen.MainMenu,
            Screen.LevelMap,
            Screen.Gameplay,
            Screen.Settings,
            Screen.Tutorial,
            Screen.DailyReward
        )
        assertEquals(7, screens.size)
        assertTrue(screens.contains(Screen.Gameplay))
        assertTrue(screens.contains(Screen.DailyReward))
    }

    @Test
    fun testHeartColorGradientsAndGloss() {
        for (color in HeartColor.values()) {
            val (light, main, dark) = color.getGradients()
            val gloss = color.getGlossColor()

            assertNotNull(light)
            assertNotNull(main)
            assertNotNull(dark)
            assertNotNull(gloss)

            assertNotEquals(light, dark)
        }

        val redGradients = HeartColor.RED.getGradients()
        assertEquals(HeartColors.RedMain, redGradients.second)
    }

    @Test
    fun testParticleBurstCreation() {
        val origin = Offset(150f, 200f)
        val particles = createBurstParticles(origin, Color.Red, count = 20)

        assertEquals(20, particles.size)
        particles.forEach { p ->
            assertEquals(150f, p.x, 0.001f)
            assertEquals(200f, p.y, 0.001f)
            assertTrue(p.size > 0f)
            assertNotNull(p.color)
        }
    }

    @Test
    fun testDailyRewardsSchedule() {
        assertEquals(7, dailyRewardsList.size)

        assertEquals(1, dailyRewardsList[0].day)
        assertEquals(100, dailyRewardsList[0].coins)
        assertNull(dailyRewardsList[0].boosterType)

        assertEquals(2, dailyRewardsList[1].day)
        assertEquals("HAMMER", dailyRewardsList[1].boosterType)

        assertEquals(7, dailyRewardsList[6].day)
        assertEquals(1000, dailyRewardsList[6].coins)
    }

    @Test
    fun testPlayerProfileDefaults() {
        val profile = PlayerProfile()
        assertEquals(500, profile.coins)
        assertEquals(1, profile.highestUnlockedLevel)
        assertEquals(3, profile.hammerCount)
        assertEquals(3, profile.bombBoosterCount)
        assertEquals(2, profile.rainbowBoosterCount)
        assertEquals(3, profile.shuffleCount)
        assertEquals(3, profile.extraMovesCount)
        assertTrue(profile.sfxEnabled)
        assertTrue(profile.musicEnabled)
        assertTrue(profile.hapticsEnabled)
    }

    @Test
    fun testLevelRecordModel() {
        val record = LevelRecord(
            levelId = 15,
            stars = 3,
            highScore = 45000,
            isCompleted = true
        )
        assertEquals(15, record.levelId)
        assertEquals(3, record.stars)
        assertEquals(45000, record.highScore)
        assertTrue(record.isCompleted)
    }
}
