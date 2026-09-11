package com.example.heartmatch.backend

import com.example.heartmatch.backend.model.BackendJsonSerializer
import com.example.heartmatch.backend.model.GameResult
import com.example.heartmatch.backend.model.ObjectiveSummary
import com.example.heartmatch.backend.model.PlayerProgress
import com.example.heartmatch.backend.model.User
import com.example.heartmatch.backend.repository.PlayerProgressRepository
import com.example.heartmatch.backend.repository.UserRepository
import com.example.heartmatch.engine.loader.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendModelTest {

    @Test
    fun testUserModel() {
        val user = User(id = "user_42", username = "HeartMaster", createdAt = 1000L, lastLogin = 2000L)
        val map = user.toMap()
        val jsonStr = BackendJsonSerializer.toJsonString(map)
        val parsed = JsonParser.parse(jsonStr).asObject()
        val reconstructed = User.fromJsonObject(parsed)

        assertEquals("user_42", reconstructed.id)
        assertEquals("HeartMaster", reconstructed.username)
        assertEquals(1000L, reconstructed.createdAt)
        assertEquals(2000L, reconstructed.lastLogin)
    }

    @Test
    fun testPlayerProgressModel() {
        val progress = PlayerProgress(
            userId = "user_1",
            currentLevel = 5,
            completedLevels = listOf(1, 2, 3, 4),
            starsPerLevel = mapOf(1 to 3, 2 to 2, 3 to 3, 4 to 1),
            totalScore = 48500,
            boosters = mapOf("HAMMER" to 5, "BOMB" to 2),
            lives = 4,
            maxLives = 5,
            highScoresPerLevel = mapOf(1 to 15000, 2 to 12000, 3 to 14000, 4 to 7500)
        )

        val jsonStr = BackendJsonSerializer.toJsonString(progress.toMap())
        val parsed = JsonParser.parse(jsonStr).asObject()
        val restored = PlayerProgress.fromJsonObject(parsed)

        assertEquals("user_1", restored.userId)
        assertEquals(5, restored.currentLevel)
        assertEquals(listOf(1, 2, 3, 4), restored.completedLevels)
        assertEquals(3, restored.starsPerLevel[1])
        assertEquals(2, restored.starsPerLevel[2])
        assertEquals(48500, restored.totalScore)
        assertEquals(5, restored.boosters["HAMMER"])
        assertEquals(2, restored.boosters["BOMB"])
        assertEquals(4, restored.lives)
        assertEquals(15000, restored.highScoresPerLevel[1])
    }

    @Test
    fun testGameResultModel() {
        val result = GameResult(
            levelId = 12,
            score = 25400,
            movesUsed = 14,
            objectivesCompleted = true,
            stars = 3,
            completedAt = 50000L,
            userId = "u100",
            sessionId = "sess_xyz",
            isVerified = true
        )

        val jsonStr = BackendJsonSerializer.toJsonString(result.toMap())
        val parsed = JsonParser.parse(jsonStr).asObject()
        val restored = GameResult.fromJsonObject(parsed)

        assertEquals(12, restored.levelId)
        assertEquals(25400, restored.score)
        assertEquals(14, restored.movesUsed)
        assertTrue(restored.objectivesCompleted)
        assertEquals(3, restored.stars)
        assertEquals("u100", restored.userId)
        assertEquals("sess_xyz", restored.sessionId)
        assertTrue(restored.isVerified)
    }

    @Test
    fun testLivesRegeneration() {
        val repo = PlayerProgressRepository(lifeRegenIntervalMs = 1000L) // 1 sec per life
        val initial = PlayerProgress(
            userId = "u1",
            lives = 2,
            maxLives = 5,
            lastLifeRefillTime = System.currentTimeMillis() - 2500L // 2.5 seconds ago -> 2 lives regenerated
        )
        repo.saveProgress(initial)

        val updated = repo.getProgress("u1")
        assertEquals(4, updated.lives)
    }

    @Test
    fun testUserRepositoryGetOrCreate() {
        val repo = UserRepository()
        val u1 = repo.getOrCreateUser("userA", "Alice")
        assertEquals("userA", u1.id)
        assertEquals("Alice", u1.username)

        val u2 = repo.getOrCreateUser("userA", "AliceNewName")
        assertEquals("userA", u2.id)
        assertEquals("Alice", u2.username) // name preserved on login
        assertTrue(u2.lastLogin >= u1.lastLogin)
    }
}
