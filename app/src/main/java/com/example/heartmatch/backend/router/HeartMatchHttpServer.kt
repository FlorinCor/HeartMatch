package com.example.heartmatch.backend.router

import com.example.heartmatch.backend.repository.GameResultRepository
import com.example.heartmatch.backend.repository.GameSessionRepository
import com.example.heartmatch.backend.repository.PlayerProgressRepository
import com.example.heartmatch.backend.repository.UserRepository
import com.example.heartmatch.backend.service.GameService
import com.example.heartmatch.backend.service.LevelService
import com.example.heartmatch.backend.service.PlayerService
import com.example.heartmatch.backend.service.ValidationService
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class HeartMatchHttpServer(
    val router: HeartMatchRouter,
    val port: Int = 0
) {
    private var server: HttpServer? = null
    var actualPort: Int = port
        private set

    fun start() {
        if (server != null) return
        val inetSocketAddress = InetSocketAddress(port)
        val s = HttpServer.create(inetSocketAddress, 0)
        s.executor = Executors.newFixedThreadPool(4)
        s.createContext("/api", ApiHttpHandler(router))
        s.start()
        server = s
        actualPort = s.address.port
    }

    fun stop(delaySeconds: Int = 0) {
        server?.stop(delaySeconds)
        server = null
    }

    class ApiHttpHandler(private val router: HeartMatchRouter) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                val method = exchange.requestMethod
                val uri = exchange.requestURI
                val path = uri.path
                val queryParams = parseQuery(uri.query)

                val body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

                val req = HttpRequest(
                    method = method,
                    path = path,
                    queryParams = queryParams,
                    body = body
                )

                val res = router.handleRequest(req)
                val responseBytes = res.body.toByteArray(StandardCharsets.UTF_8)

                res.headers.forEach { (k, v) ->
                    exchange.responseHeaders.set(k, v)
                }

                exchange.sendResponseHeaders(res.statusCode, responseBytes.size.toLong())
                exchange.responseBody.use { os ->
                    os.write(responseBytes)
                }
            } catch (e: Exception) {
                val errorBody = "{\"success\":false,\"error\":\"${e.message}\"}".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(500, errorBody.size.toLong())
                exchange.responseBody.use { it.write(errorBody) }
            }
        }

        private fun parseQuery(query: String?): Map<String, String> {
            if (query.isNullOrBlank()) return emptyMap()
            val map = mutableMapOf<String, String>()
            query.split("&").forEach { pair ->
                val idx = pair.indexOf("=")
                if (idx > 0) {
                    val key = pair.substring(0, idx)
                    val value = pair.substring(idx + 1)
                    map[key] = value
                }
            }
            return map
        }
    }

    companion object {
        fun createDefault(port: Int = 0): HeartMatchHttpServer {
            val userRepo = UserRepository()
            val progressRepo = PlayerProgressRepository()
            val sessionRepo = GameSessionRepository()
            val resultRepo = GameResultRepository()
            val levelService = LevelService()
            val validationService = ValidationService()
            val playerService = PlayerService(userRepo, progressRepo)
            val gameService = GameService(
                userRepository = userRepo,
                progressRepository = progressRepo,
                sessionRepository = sessionRepo,
                resultRepository = resultRepo,
                levelService = levelService,
                validationService = validationService
            )
            val router = HeartMatchRouter(gameService, levelService, playerService)
            return HeartMatchHttpServer(router, port)
        }
    }
}
