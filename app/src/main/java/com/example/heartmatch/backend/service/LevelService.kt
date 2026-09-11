package com.example.heartmatch.backend.service

import com.example.heartmatch.backend.model.LevelDetail
import com.example.heartmatch.backend.model.LevelSummary
import com.example.heartmatch.engine.loader.LevelRepository
import com.example.heartmatch.engine.model.LevelConfig

class LevelService(
    private val levelRepository: LevelRepository = LevelRepository()
) {
    fun getAllLevels(): List<LevelSummary> {
        return (1..100).map { levelId ->
            val config = levelRepository.getLevel(levelId)
            LevelSummary.fromConfig(config)
        }
    }

    fun getLevel(levelId: Int): LevelDetail {
        require(levelId in 1..100) { "Level ID must be between 1 and 100" }
        val config = levelRepository.getLevel(levelId)
        val rawJson = try {
            levelRepository.getLevelJson(levelId)
        } catch (e: Exception) {
            null
        }
        return LevelDetail.fromConfig(config, rawJson)
    }

    fun getLevelConfig(levelId: Int): LevelConfig {
        return levelRepository.getLevel(levelId)
    }
}
