package com.example.heartmatch.backend.repository

import com.example.heartmatch.backend.model.GameResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class GameResultRepository {
    private val results = CopyOnWriteArrayList<GameResult>()
    private val userResults = ConcurrentHashMap<String, MutableList<GameResult>>()

    fun save(result: GameResult): GameResult {
        results.add(result)
        result.userId?.let { uid ->
            userResults.computeIfAbsent(uid) { CopyOnWriteArrayList() }.add(result)
        }
        return result
    }

    fun findByUserId(userId: String): List<GameResult> {
        return userResults[userId] ?: emptyList()
    }

    fun getAllResults(): List<GameResult> = results.toList()

    fun clear() {
        results.clear()
        userResults.clear()
    }
}
