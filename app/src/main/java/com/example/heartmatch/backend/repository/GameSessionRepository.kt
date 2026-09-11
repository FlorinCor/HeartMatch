package com.example.heartmatch.backend.repository

import com.example.heartmatch.backend.model.GameSession
import java.util.concurrent.ConcurrentHashMap

class GameSessionRepository {
    private val sessions = ConcurrentHashMap<String, GameSession>()

    fun save(session: GameSession): GameSession {
        sessions[session.sessionId] = session
        return session
    }

    fun get(sessionId: String): GameSession? = sessions[sessionId]

    fun remove(sessionId: String): GameSession? = sessions.remove(sessionId)

    fun getActiveSessionsForUser(userId: String): List<GameSession> {
        return sessions.values.filter { it.userId == userId && !it.isCompleted }
    }

    fun clear() {
        sessions.clear()
    }
}
