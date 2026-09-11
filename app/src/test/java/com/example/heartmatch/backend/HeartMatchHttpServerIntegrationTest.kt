package com.example.heartmatch.backend

import com.example.heartmatch.backend.model.UpdateProgressRequest
import com.example.heartmatch.backend.router.HeartMatchApiClient
import com.example.heartmatch.backend.router.HeartMatchHttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeartMatchHttpServerIntegrationTest {

    private lateinit var server: HeartMatchHttpServer
    private lateinit var client: HeartMatchApiClient

    @Before
    fun setUp() {
        server = HeartMatchHttpServer.createDefault(port = 0)
        server.start()
        val port = server.actualPort
        client = HeartMatchApiClient(baseUrl = "http://127.0.0.1:$port")
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun testLiveHttpEndpointCalls() {
        // 1. GET /api/levels
        val levels = client.getLevels()
        assertEquals(100, levels.size)

        // 2. GET /api/levels/1
        val lvl1 = client.getLevel(1)
        assertEquals(1, lvl1.getInt("levelId"))

        // 3. GET /api/player/progress
        val progress = client.getPlayerProgress("network_user")
        assertEquals("network_user", progress.userId)
        assertEquals(5, progress.lives)

        // 4. POST /api/player/progress
        val updatedProgress = client.updatePlayerProgress(
            UpdateProgressRequest(
                userId = "network_user",
                username = "NetRunner",
                boosters = mapOf("BOMB" to 9)
            )
        )
        assertEquals(9, updatedProgress.boosters["BOMB"])

        // 5. GET /api/player/boosters
        val boosters = client.getPlayerBoosters("network_user")
        assertEquals(9, boosters["BOMB"])

        // 6. POST /api/game/start
        val startRes = client.startGame("network_user", 1)
        assertNotNull(startRes.sessionId)
        assertEquals(4, startRes.lives)

        // 7. POST /api/game/move
        val moveRes = client.makeMove(
            sessionId = startRes.sessionId,
            fromRow = 0,
            fromCol = 0,
            toRow = 0,
            toCol = 1
        )
        assertNotNull(moveRes.sessionId)

        // 8. POST /api/game/complete
        val completeRes = client.completeGame(
            sessionId = startRes.sessionId,
            clientScore = 100,
            clientStars = 1
        )
        assertTrue(completeRes.success)
    }
}
