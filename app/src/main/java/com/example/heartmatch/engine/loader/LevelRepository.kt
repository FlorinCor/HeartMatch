package com.example.heartmatch.engine.loader

import com.example.heartmatch.engine.model.LevelConfig
import java.io.File
import java.io.InputStream

class LevelRepository(
    private val assetsDir: File? = null,
    private val resourceClassLoader: ClassLoader = LevelRepository::class.java.classLoader!!
) {
    private val parser = LevelJsonParser()

    /**
     * Loads level configuration by ID (1 to 100).
     */
    fun getLevel(levelId: Int): LevelConfig {
        val json = getLevelJson(levelId)
        return parser.parse(json)
    }

    /**
     * Retrieves the JSON string for level ID (1 to 100).
     */
    fun getLevelJson(levelId: Int): String {
        require(levelId in 1..100) { "Level ID must be between 1 and 100 (got $levelId)" }

        // 1. Check direct file system assets directory if provided
        if (assetsDir != null && assetsDir.exists()) {
            val fileFormatted = File(assetsDir, String.format("level_%03d.json", levelId))
            if (fileFormatted.exists()) return fileFormatted.readText()

            val fileSimple = File(assetsDir, "level_$levelId.json")
            if (fileSimple.exists()) return fileSimple.readText()
        }

        // 2. Check classpath resources
        val resourceNames = listOf(
            String.format("levels/level_%03d.json", levelId),
            "levels/level_$levelId.json",
            String.format("assets/levels/level_%03d.json", levelId),
            "assets/levels/level_$levelId.json"
        )

        for (resName in resourceNames) {
            val stream: InputStream? = resourceClassLoader.getResourceAsStream(resName)
            if (stream != null) {
                return stream.bufferedReader().use { it.readText() }
            }
        }

        // 3. Fallback to programmatic generator
        return LevelGenerator.generateLevelJson(levelId)
    }

    /**
     * Loads all 100 level configurations.
     */
    fun getAllLevels(): List<LevelConfig> {
        return (1..100).map { getLevel(it) }
    }
}
