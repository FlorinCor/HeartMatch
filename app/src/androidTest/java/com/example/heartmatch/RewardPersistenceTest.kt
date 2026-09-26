package com.example.heartmatch

import androidx.test.platform.app.InstrumentationRegistry
import com.example.heartmatch.data.PlayerRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RewardPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repo: PlayerRepository
    @Before fun setUp() {
        check(context.packageName.endsWith(".qa")) { "Use -Pqa; reward tests must never reset a real player's save." }
        repo=PlayerRepository(context); repo.resetAllData()
    }
    @Test fun shopSpendsCoinsOnlyOnceAndDecorationsCanBeReused() {
        repo.addCoins(500)
        assertTrue(repo.purchase("ROSE_GARDEN")); assertEquals(400,repo.getProfile().coins)
        assertTrue(repo.purchase("CLASSIC")); assertTrue(repo.purchase("ROSE_GARDEN"))
        assertEquals(400,repo.getProfile().coins)
        val before=repo.getProfile().hammerCount
        assertTrue(repo.purchase("HAMMER")); assertEquals(before+1,repo.getProfile().hammerCount)
        assertFalse(repo.purchase("MOON_GARDEN")); assertEquals(280,repo.getProfile().coins)
    }
    @Test fun replayKeepsBestStarsAndMilestoneIsFirstClearOnly() {
        assertEquals(150 to true,repo.completeLevel(10,3,5000))
        assertEquals(15 to false,repo.completeLevel(10,1,1000))
        assertEquals(3,repo.getLevelRecord(10).stars)
        assertEquals(5000,repo.getLevelRecord(10).highScore)
        assertEquals(4,repo.getProfile().hammerCount)
    }
    @Test fun missedDaysKeepTrackAndDaySevenGrantsEveryBooster() {
        val profile=repo.getProfile()
        repo.saveProfile(profile.copy(dailyRewardStreak=6,lastDailyClaimDay=1))
        assertTrue(repo.claimDailyReward(7,1000,"RAINBOW"))
        val after=repo.getProfile()
        assertEquals(profile.hammerCount+1,after.hammerCount)
        assertEquals(profile.bombBoosterCount+1,after.bombBoosterCount)
        assertEquals(profile.rainbowBoosterCount+1,after.rainbowBoosterCount)
        assertEquals(profile.shuffleCount+1,after.shuffleCount)
        assertEquals(profile.extraMovesCount+1,after.extraMovesCount)
        assertFalse(repo.claimDailyReward(7,1000,"RAINBOW"))
    }
}
