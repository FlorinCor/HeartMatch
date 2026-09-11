package com.example.heartmatch.backend.router

import com.example.heartmatch.backend.model.BackendJsonSerializer
import com.example.heartmatch.backend.model.CompleteGameRequest
import com.example.heartmatch.backend.model.CompleteGameResponse
import com.example.heartmatch.backend.model.GameResult
import com.example.heartmatch.backend.model.LevelDetail
import com.example.heartmatch.backend.model.LevelSummary
import com.example.heartmatch.backend.model.MakeMoveRequest
import com.example.heartmatch.backend.model.MakeMoveResponse
import com.example.heartmatch.backend.model.PlayerProgress
import com.example.heartmatch.backend.model.StartGameRequest
import com.example.heartmatch.backend.model.StartGameResponse
import com.example.heartmatch.backend.model.UpdateProgressRequest
import com.example.heartmatch.engine.loader.JsonObject
import com.example.heartmatch.engine.loader.JsonParser
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class HeartMatchApiClient(
    private val baseUrl: String? = null,
    private val inMemoryRouter: HeartMatchRouter? = null
) {
    init {
        require(baseUrl != null || inMemoryRouter != null) {
            "Either baseUrl or inMemoryRouter must be provided."
        }
    }

    fun startGame(userId: String, levelId: Int): StartGameResponse {
        val req = StartGameRequest(userId, levelId)
        val body = BackendJsonSerializer.toJsonString(mapOf("userId" to userId, "levelId" to levelId))
        val responseJson = post("/api/game/start", body)
        val data = responseJson.getObject("data") ?: error("No data in response: $responseJson")

        val sessionId = data.getString("sessionId") ?: ""
        val status = data.getString("status") ?: "READY_FOR_INPUT"
        val movesRemaining = data.getInt("movesRemaining") ?: 30
        val lives = data.getInt("lives") ?: 5
        val levelObj = data.getObject("level") ?: error("No level summary in start game response")
        val levelSummary = LevelSummary(
            levelId = levelObj.getInt("levelId") ?: levelId,
            name = levelObj.getString("name") ?: "",
            difficulty = levelObj.getString("difficulty") ?: "EASY",
            moves = levelObj.getInt("moves") ?: 30,
            starThresholds = levelObj.getArray("starThresholds")?.elements?.mapNotNull { it.asPrimitive().content.toIntOrNull() } ?: emptyList(),
            objectives = emptyList()
        )

        return StartGameResponse(
            sessionId = sessionId,
            levelId = levelId,
            status = status,
            movesRemaining = movesRemaining,
            lives = lives,
            level = levelSummary
        )
    }

    fun makeMove(sessionId: String, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): MakeMoveResponse {
        val body = BackendJsonSerializer.toJsonString(
            mapOf(
                "sessionId" to sessionId,
                "fromRow" to fromRow,
                "fromCol" to fromCol,
                "toRow" to toRow,
                "toCol" to toCol
            )
        )
        val responseJson = post("/api/game/move", body)
        val data = responseJson.getObject("data") ?: error("No data in response: $responseJson")

        return MakeMoveResponse(
            sessionId = data.getString("sessionId") ?: sessionId,
            isLegal = data.getBoolean("isLegal") ?: false,
            score = data.getInt("score") ?: 0,
            movesRemaining = data.getInt("movesRemaining") ?: 0,
            earnedStars = data.getInt("earnedStars") ?: 0,
            status = data.getString("status") ?: "READY_FOR_INPUT",
            isWon = data.getBoolean("isWon") ?: false,
            isLost = data.getBoolean("isLost") ?: false,
            eventCount = data.getInt("eventCount") ?: 0
        )
    }

    fun completeGame(sessionId: String, clientScore: Int? = null, clientStars: Int? = null, clientMovesUsed: Int? = null): CompleteGameResponse {
        val bodyMap = mutableMapOf<String, Any>("sessionId" to sessionId)
        if (clientScore != null) bodyMap["clientScore"] = clientScore
        if (clientStars != null) bodyMap["clientStars"] = clientStars
        if (clientMovesUsed != null) bodyMap["clientMovesUsed"] = clientMovesUsed

        val body = BackendJsonSerializer.toJsonString(bodyMap)
        val responseJson = post("/api/game/complete", body)
        val data = responseJson.getObject("data") ?: error("No data in response: $responseJson")

        val resultObj = data.getObject("result") ?: error("No result in complete response")
        val progressObj = data.getObject("progress") ?: error("No progress in complete response")

        return CompleteGameResponse(
            success = data.getBoolean("success") ?: true,
            result = GameResult.fromJsonObject(resultObj),
            progress = PlayerProgress.fromJsonObject(progressObj),
            message = data.getString("message")
        )
    }

    fun getLevels(): List<LevelSummary> {
        val responseJson = get("/api/levels")
        val dataArr = responseJson.getArray("data") ?: error("No data array in levels response")
        return dataArr.elements.mapNotNull { elem ->
            val obj = elem.asObject()
            val levelId = obj.getInt("levelId") ?: return@mapNotNull null
            val name = obj.getString("name") ?: ""
            val difficulty = obj.getString("difficulty") ?: "EASY"
            val moves = obj.getInt("moves") ?: 30
            val thresholds = obj.getArray("starThresholds")?.elements?.mapNotNull { it.asPrimitive().content.toIntOrNull() } ?: emptyList()
            LevelSummary(levelId, name, difficulty, moves, thresholds, emptyList())
        }
    }

    fun getLevel(id: Int): JsonObject {
        val responseJson = get("/api/levels/$id")
        return responseJson.getObject("data") ?: error("No data in level response")
    }

    fun getPlayerProgress(userId: String): PlayerProgress {
        val responseJson = get("/api/player/progress?userId=$userId")
        val data = responseJson.getObject("data") ?: error("No data in player progress response")
        return PlayerProgress.fromJsonObject(data)
    }

    fun updatePlayerProgress(request: UpdateProgressRequest): PlayerProgress {
        val bodyMap = mutableMapOf<String, Any>("userId" to request.userId)
        if (request.username != null) bodyMap["username"] = request.username
        if (request.boosters != null) bodyMap["boosters"] = request.boosters
        if (request.addCoins != null) bodyMap["addCoins"] = request.addCoins

        val body = BackendJsonSerializer.toJsonString(bodyMap)
        val responseJson = post("/api/player/progress", body)
        val data = responseJson.getObject("data") ?: error("No data in update progress response")
        return PlayerProgress.fromJsonObject(data)
    }

    fun getPlayerBoosters(userId: String): Map<String, Int> {
        val responseJson = get("/api/player/boosters?userId=$userId")
        val data = responseJson.getObject("data") ?: error("No data in player boosters response")
        val boostersObj = data.getObject("boosters")
        val map = mutableMapOf<String, Int>()
        boostersObj?.members?.forEach { (k, v) ->
            val count = v.asPrimitive().content.toIntOrNull()
            if (count != null) map[k] = count
        }
        return map
    }

    private fun get(path: String): JsonObject {
        val rawJson = if (inMemoryRouter != null) {
            val queryStr = path.substringAfter("?", "")
            val cleanPath = path.substringBefore("?")
            val queryParams = parseQuery(queryStr)
            val req = HttpRequest(method = "GET", path = cleanPath, queryParams = queryParams)
            val res = inMemoryRouter.handleRequest(req)
            if (res.statusCode !in 200..299) {
                error("API GET $path failed with status ${res.statusCode}: ${res.body}")
            }
            res.body
        } else {
            val url = URL("$baseUrl$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (code !in 200..299) {
                error("API GET $path failed with status $code: $text")
            }
            text
        }
        return JsonParser.parse(rawJson).asObject()
    }

    private fun post(path: String, body: String): JsonObject {
        val rawJson = if (inMemoryRouter != null) {
            val queryStr = path.substringAfter("?", "")
            val cleanPath = path.substringBefore("?")
            val queryParams = parseQuery(queryStr)
            val req = HttpRequest(method = "POST", path = cleanPath, queryParams = queryParams, body = body)
            val res = inMemoryRouter.handleRequest(req)
            if (res.statusCode !in 200..299) {
                error("API POST $path failed with status ${res.statusCode}: ${res.body}")
            }
            res.body
        } else {
            val url = URL("$baseUrl$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (code !in 200..299) {
                error("API POST $path failed with status $code: $text")
            }
            text
        }
        return JsonParser.parse(rawJson).asObject()
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        query.split("&").forEach { pair ->
            val idx = pair.indexOf("=")
            if (idx > 0) {
                map[pair.substring(0, idx)] = pair.substring(idx + 1)
            }
        }
        return map
    }
}
