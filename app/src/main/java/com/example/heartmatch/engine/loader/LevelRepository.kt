package com.example.heartmatch.engine.loader

import com.example.heartmatch.engine.model.LevelConfig
import java.io.File
import java.io.InputStream

/**
 * Loads level definitions.
 *
 * The canonical source of truth is `assets/levels/level_%03d.json` (Android assets). Levels are
 * resolved in this order:
 *   1. [assetOpener]  – Android AssetManager on-device (path relative to the assets root).
 *   2. [assetsDir]    – a direct filesystem directory (used by JVM unit tests).
 *   3. classpath      – any bundled resource copy, kept only as a defensive fallback.
 *   4. [LevelGenerator] – deterministic programmatic generation (always succeeds).
 *
 * @param assetOpener opens an asset by its path relative to the assets root, e.g.
 *                    `"levels/level_001.json"`; returns null when the asset is missing.
 * @param assetsDir   filesystem directory that directly contains `level_%03d.json` files.
 */
class LevelRepository(
    private val assetOpener: ((String) -> InputStream?)? = null,
    private val assetsDir: File? = null,
    private val resourceClassLoader: ClassLoader = LevelRepository::class.java.classLoader!!
) {
    private val parser = LevelJsonParser()

    /**
     * Loads level configuration by ID (1 to 200).
     */
    fun getLevel(levelId: Int): LevelConfig {
        val json = getLevelJson(levelId)
        return parser.parse(json)
    }

    /**
     * Retrieves the JSON string for level ID (1 to 200).
     */
    fun getLevelJson(levelId: Int): String {
        require(levelId in 1..200) { "Level ID must be between 1 and 200 (got $levelId)" }

        val canonicalName = String.format("level_%03d.json", levelId)
        val canonicalAssetPath = "levels/$canonicalName"

        // 1. Android assets (canonical on-device path: assets/levels/level_%03d.json)
        assetOpener?.let { opener ->
            opener(canonicalAssetPath)?.let { stream ->
                return stream.bufferedReader().use { it.readText() }
            }
        }

        // 2. Direct filesystem assets directory (unit tests)
        if (assetsDir != null && assetsDir.exists()) {
            val fileFormatted = File(assetsDir, canonicalName)
            if (fileFormatted.exists()) return fileFormatted.readText()
        }

        // 3. Classpath resource fallback
        val resourceNames = listOf(
            "assets/$canonicalAssetPath",
            canonicalAssetPath
        )
        for (resName in resourceNames) {
            val stream: InputStream? = resourceClassLoader.getResourceAsStream(resName)
            if (stream != null) {
                return stream.bufferedReader().use { it.readText() }
            }
        }

        // 4. Fallback to programmatic generator
        return LevelGenerator.generateLevelJson(levelId)
    }

    /**
     * Loads all 200 level configurations.
     */
    fun getAllLevels(): List<LevelConfig> {
        return (1..200).map { getLevel(it) }
    }
}
