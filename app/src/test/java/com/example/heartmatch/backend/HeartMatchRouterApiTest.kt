package com.example.heartmatch.backend

import com.example.heartmatch.backend.model.BackendJsonSerializer
import com.example.heartmatch.backend.model.UpdateProgressRequest
import com.example.heartmatch.backend.router.HeartMatchApiClient
import com.example.heartmatch.backend.router.HeartMatchHttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeartMatchRouterApiTest {

    private lateinit var client: HeartMatchApiClient

    @Before
    fun setUp() {
        val server = HeartMatchHttpServer.createDefault(port = 0)
        client = HeartMatchApiClient(inMemoryRouter = server.router)
    }

    @Test
    fun testEndpointGetLevels() {
        // GET /api/levels
        val levels = client.getLevels()
        assertEquals(100, levels.size)
        assertEquals(1, levels[0].levelId)
        assertEquals("First Spark", levels[0].name)
        assertEquals("EASY", levels[0].difficulty)
        assertEquals(100, levels[99].levelId)
    }

    @Test
    fun testEndpointGetLevelById() {
        // GET /api/levels/{id}
        val levelObj = client.getLevel(1)
        assertEquals(1, levelObj.getInt("levelId"))
        assertEquals("EASY", levelObj.getString("difficulty"))
        assertNotNull(levelObj.getObject("configuration"))
        assertNotNull(levelObj.getArray("objectives"))
    }

    @Test
    fun testEndpointPlayerProgress() {
        val userId = "test_player_api"

        // GET /api/player/progress
        val initialProgress = client.getPlayerProgress(userId)
        assertEquals(userId, initialProgress.userId)
        assertEquals(1, initialProgress.currentLevel)
        assertEquals(5, initialProgress.lives)
        assertEquals(3, initialProgress.boosters["HAMMER"])

        // POST /api/player/progress
        val updated = client.updatePlayerProgress(
            UpdateProgressRequest(
                userId = userId,
                username = "HeartHero",
                boosters = mapOf("HAMMER" to 10, "BOMB" to 7)
            )
        )
        assertEquals(10, updated.boosters["HAMMER"])
        assertEquals(7, updated.boosters["BOMB"])

        // GET /api/player/boosters
        val boosters = client.getPlayerBoosters(userId)
        assertEquals(10, boosters["HAMMER"])
        assertEquals(7, boosters["BOMB"])
    }

    @Test
    fun testEndpointGameLifecycle() {
        val userId = "game_flow_user"

        // 1. POST /api/game/start
        val startRes = client.startGame(userId = userId, levelId = 1)
        assertNotNull(startRes.sessionId)
        assertEquals(1, startRes.levelId)
        assertEquals("READY_FOR_INPUT", startRes.status)
        assertEquals(4, startRes.lives)

        // 2. POST /api/game/move
        // Execute a move
        val moveRes = client.makeMove(
            sessionId = startRes.sessionId,
            fromRow = 0,
            fromCol = 0,
            toRow = 0,
            toCol = 1
        )
        assertNotNull(moveRes.sessionId)

        // 3. POST /api/game/complete
        val completeRes = client.completeGame(
            sessionId = startRes.sessionId,
            clientScore = 500,
            clientStars = 1
        )
        assertTrue(completeRes.success)
        assertEquals(1, completeRes.result.levelId)
    }
}
