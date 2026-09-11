package com.example.heartmatch.backend.router

import com.example.heartmatch.backend.model.ApiResponse
import com.example.heartmatch.backend.model.BackendJsonSerializer
import com.example.heartmatch.backend.model.CompleteGameRequest
import com.example.heartmatch.backend.model.MakeMoveRequest
import com.example.heartmatch.backend.model.StartGameRequest
import com.example.heartmatch.backend.model.UpdateProgressRequest
import com.example.heartmatch.backend.service.GameService
import com.example.heartmatch.backend.service.LevelService
import com.example.heartmatch.backend.service.PlayerService
import com.example.heartmatch.engine.loader.JsonObject
import com.example.heartmatch.engine.loader.JsonParser

data class HttpRequest(
    val method: String,
    val path: String,
    val queryParams: Map<String, String> = emptyMap(),
    val body: String = ""
)

data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = mapOf("Content-Type" to "application/json; charset=utf-8")
)

class HeartMatchRouter(
    val gameService: GameService,
    val levelService: LevelService,
    val playerService: PlayerService
) {

    fun handleRequest(request: HttpRequest): HttpResponse {
        val method = request.method.uppercase()
        val path = normalizePath(request.path)

        return try {
            when {
                // POST /api/game/start
                method == "POST" && path == "/api/game/start" -> {
                    handleStartGame(request)
                }

                // POST /api/game/move
                method == "POST" && path == "/api/game/move" -> {
                    handleMakeMove(request)
                }

                // POST /api/game/complete
                method == "POST" && path == "/api/game/complete" -> {
                    handleCompleteGame(request)
                }

                // GET /api/levels/{id}
                method == "GET" && path.matches(Regex("^/api/levels/\\d+$")) -> {
                    val levelId = path.substringAfterLast("/").toInt()
                    handleGetLevelById(levelId)
                }

                // GET /api/levels
                method == "GET" && path == "/api/levels" -> {
                    handleGetAllLevels()
                }

                // GET /api/player/progress
                method == "GET" && path == "/api/player/progress" -> {
                    handleGetPlayerProgress(request)
                }

                // POST /api/player/progress
                method == "POST" && path == "/api/player/progress" -> {
                    handleUpdatePlayerProgress(request)
                }

                // GET /api/player/boosters
                method == "GET" && path == "/api/player/boosters" -> {
                    handleGetPlayerBoosters(request)
                }

                else -> {
                    jsonResponse(404, ApiResponse.error<Any>("Endpoint not found: $method $path"))
                }
            }
        } catch (e: IllegalArgumentException) {
            jsonResponse(400, ApiResponse.error<Any>(e.message ?: "Bad Request"))
        } catch (e: IllegalStateException) {
            jsonResponse(400, ApiResponse.error<Any>(e.message ?: "Invalid State"))
        } catch (e: Exception) {
            jsonResponse(500, ApiResponse.error<Any>(e.message ?: "Internal Server Error"))
        }
    }

    private fun handleStartGame(request: HttpRequest): HttpResponse {
        val json = parseBodyAsJsonObject(request.body)
        val reqDto = StartGameRequest.fromJsonObject(json)
        val resDto = gameService.startGame(reqDto)
        return jsonResponse(200, ApiResponse.ok(resDto.toMap()))
    }

    private fun handleMakeMove(request: HttpRequest): HttpResponse {
        val json = parseBodyAsJsonObject(request.body)
        val reqDto = MakeMoveRequest.fromJsonObject(json)
        val resDto = gameService.makeMove(reqDto)
        return jsonResponse(200, ApiResponse.ok(resDto.toMap()))
    }

    private fun handleCompleteGame(request: HttpRequest): HttpResponse {
        val json = parseBodyAsJsonObject(request.body)
        val reqDto = CompleteGameRequest.fromJsonObject(json)
        val resDto = gameService.completeGame(reqDto)
        return jsonResponse(200, ApiResponse.ok(resDto.toMap()))
    }

    private fun handleGetAllLevels(): HttpResponse {
        val levels = levelService.getAllLevels().map { it.toMap() }
        return jsonResponse(200, ApiResponse.ok(levels))
    }

    private fun handleGetLevelById(levelId: Int): HttpResponse {
        val detail = levelService.getLevel(levelId)
        return jsonResponse(200, ApiResponse.ok(detail.toMap()))
    }

    private fun handleGetPlayerProgress(request: HttpRequest): HttpResponse {
        val userId = request.queryParams["userId"]
            ?: parseBodyAsJsonObjectOrNull(request.body)?.getString("userId")
            ?: "guest"
        val progress = playerService.getProgress(userId)
        return jsonResponse(200, ApiResponse.ok(progress.toMap()))
    }

    private fun handleUpdatePlayerProgress(request: HttpRequest): HttpResponse {
        val json = parseBodyAsJsonObject(request.body)
        val reqDto = UpdateProgressRequest.fromJsonObject(json)
        val progress = playerService.updateProgress(reqDto)
        return jsonResponse(200, ApiResponse.ok(progress.toMap()))
    }

    private fun handleGetPlayerBoosters(request: HttpRequest): HttpResponse {
        val userId = request.queryParams["userId"]
            ?: parseBodyAsJsonObjectOrNull(request.body)?.getString("userId")
            ?: "guest"
        val boosters = playerService.getBoosters(userId)
        return jsonResponse(200, ApiResponse.ok(mapOf("userId" to userId, "boosters" to boosters)))
    }

    private fun parseBodyAsJsonObject(body: String): JsonObject {
        if (body.isBlank()) return JsonObject(emptyMap())
        return try {
            JsonParser.parse(body).asObject()
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
    }

    private fun parseBodyAsJsonObjectOrNull(body: String): JsonObject? {
        if (body.isBlank()) return null
        return try {
            JsonParser.parse(body).asObject()
        } catch (e: Exception) {
            null
        }
    }

    private fun <T> jsonResponse(statusCode: Int, response: ApiResponse<T>): HttpResponse {
        val jsonString = BackendJsonSerializer.toJsonString(response.toMap())
        return HttpResponse(statusCode, jsonString)
    }

    private fun normalizePath(rawPath: String): String {
        val clean = rawPath.substringBefore("?").trim()
        return if (clean.endsWith("/") && clean.length > 1) {
            clean.dropLast(1)
        } else {
            clean
        }
    }
}
