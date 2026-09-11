package com.example.heartmatch.backend.service

import com.example.heartmatch.backend.model.PlayerProgress
import com.example.heartmatch.backend.model.UpdateProgressRequest
import com.example.heartmatch.backend.repository.PlayerProgressRepository
import com.example.heartmatch.backend.repository.UserRepository

class PlayerService(
    private val userRepository: UserRepository,
    private val progressRepository: PlayerProgressRepository
) {
    fun getProgress(userId: String): PlayerProgress {
        userRepository.getOrCreateUser(userId)
        return progressRepository.getProgress(userId)
    }

    fun updateProgress(request: UpdateProgressRequest): PlayerProgress {
        val user = userRepository.getOrCreateUser(
            userId = request.userId,
            username = request.username ?: "Player"
        )
        if (request.username != null && request.username != user.username) {
            userRepository.save(user.copy(username = request.username))
        }

        var currentProgress = progressRepository.getProgress(request.userId)

        if (request.boosters != null) {
            val updatedBoosters = currentProgress.boosters.toMutableMap()
            request.boosters.forEach { (k, v) ->
                updatedBoosters[k] = v
            }
            currentProgress = currentProgress.copy(boosters = updatedBoosters)
            progressRepository.saveProgress(currentProgress)
        }

        return progressRepository.getProgress(request.userId)
    }

    fun getBoosters(userId: String): Map<String, Int> {
        userRepository.getOrCreateUser(userId)
        return progressRepository.getProgress(userId).boosters
    }

    fun consumeBooster(userId: String, boosterType: String): Boolean {
        return progressRepository.consumeBooster(userId, boosterType)
    }

    fun addBooster(userId: String, boosterType: String, count: Int = 1): PlayerProgress {
        return progressRepository.addBooster(userId, boosterType, count)
    }

    fun deductLife(userId: String): Boolean {
        return progressRepository.deductLife(userId)
    }

    fun addLives(userId: String, count: Int): PlayerProgress {
        return progressRepository.addLives(userId, count)
    }
}
