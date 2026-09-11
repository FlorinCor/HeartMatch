package com.example.heartmatch.graphics.registry

import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.graphics.generator.SvgAssetGenerator
import com.example.heartmatch.graphics.model.AssetDescriptor
import com.example.heartmatch.graphics.model.AssetKey
import com.example.heartmatch.graphics.model.BackgroundAssetKey
import com.example.heartmatch.graphics.model.EffectAssetKey
import com.example.heartmatch.graphics.model.HeartAssetKey
import com.example.heartmatch.graphics.model.HeartVisualState
import com.example.heartmatch.graphics.model.UiAssetKey
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * High-level Asset Manager handling asset loading, caching, state variations,
 * and procedural fallback rendering.
 */
class AssetManager(
    val registry: AssetRegistry = AssetRegistry()
) {

    private val svgCache = ConcurrentHashMap<String, String>()

    /**
     * Loads or generates the SVG content for a given heart asset and visual state.
     */
    fun getHeartSvg(key: HeartAssetKey, state: HeartVisualState = HeartVisualState.Idle): String {
        val cacheKey = "${key.identifier}_${state.stateName}"
        return svgCache.computeIfAbsent(cacheKey) {
            // Check memory override first
            registry.getMemorySvg(key, state)
                ?: loadFromResources(registry.getPathForState(key, state))
                ?: SvgAssetGenerator.generateHeartSvg(key, state)
        }
    }

    /**
     * Loads or generates the SVG content for a visual effect.
     */
    fun getEffectSvg(key: EffectAssetKey): String {
        return svgCache.computeIfAbsent(key.identifier) {
            registry.getMemorySvg(key)
                ?: loadFromResources(registry.getEffectPath(key))
                ?: SvgAssetGenerator.generateEffectSvg(key)
        }
    }

    /**
     * Loads or generates the SVG content for a UI icon.
     */
    fun getUiSvg(key: UiAssetKey): String {
        return svgCache.computeIfAbsent(key.identifier) {
            registry.getMemorySvg(key)
                ?: loadFromResources(registry.getUiPath(key))
                ?: SvgAssetGenerator.generateUiSvg(key)
        }
    }

    /**
     * Loads or generates the SVG content for a themed background.
     */
    fun getBackgroundSvg(key: BackgroundAssetKey): String {
        return svgCache.computeIfAbsent(key.identifier) {
            registry.getMemorySvg(key)
                ?: loadFromResources(registry.getBackgroundPath(key))
                ?: SvgAssetGenerator.generateBackgroundSvg(key)
        }
    }

    /**
     * Helper for domain-based normal heart SVG lookup.
     */
    fun getNormalHeartSvg(color: HeartColor, state: HeartVisualState = HeartVisualState.Idle): String =
        getHeartSvg(HeartAssetKey.Normal(color), state)

    /**
     * Helper for domain-based blocker heart SVG lookup.
     */
    fun getBlockerHeartSvg(type: BlockerType, state: HeartVisualState = HeartVisualState.Idle): String =
        getHeartSvg(HeartAssetKey.Blocker(type), state)

    /**
     * Helper for domain-based special heart SVG lookup.
     */
    fun getSpecialHeartSvg(type: SpecialHeartType, state: HeartVisualState = HeartVisualState.Idle): String =
        getHeartSvg(HeartAssetKey.Special(type), state)

    /**
     * Clears cached SVG strings in memory.
     */
    fun clearCache() {
        svgCache.clear()
    }

    /**
     * Preloads all default game graphics into memory cache.
     */
    fun warmUpCache() {
        HeartColor.values().forEach { color ->
            val key = HeartAssetKey.Normal(color)
            getHeartSvg(key, HeartVisualState.Idle)
            getHeartSvg(key, HeartVisualState.Selected)
            getHeartSvg(key, HeartVisualState.Matched)
        }
        BlockerType.values().forEach { type ->
            val key = HeartAssetKey.Blocker(type)
            getHeartSvg(key, HeartVisualState.Idle)
            getHeartSvg(key, HeartVisualState.Damaged())
            getHeartSvg(key, HeartVisualState.Destroyed())
        }
        SpecialHeartType.values().forEach { type ->
            val key = HeartAssetKey.Special(type)
            getHeartSvg(key, HeartVisualState.Idle)
            getHeartSvg(key, HeartVisualState.Selected)
        }
        EffectAssetKey.values().forEach { getEffectSvg(it) }
        UiAssetKey.values().forEach { getUiSvg(it) }
        BackgroundAssetKey.values().forEach { getBackgroundSvg(it) }
    }

    private fun loadFromResources(path: String): String? {
        return try {
            val resourcePath = if (path.startsWith("/")) path else "/$path"
            val stream: InputStream? = javaClass.getResourceAsStream(resourcePath)
                ?: javaClass.classLoader?.getResourceAsStream(path)
            stream?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
}
