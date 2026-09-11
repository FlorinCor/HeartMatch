package com.example.heartmatch.graphics.registry

import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.graphics.model.AssetDescriptor
import com.example.heartmatch.graphics.model.AssetFormat
import com.example.heartmatch.graphics.model.AssetKey
import com.example.heartmatch.graphics.model.BackgroundAssetKey
import com.example.heartmatch.graphics.model.EffectAssetKey
import com.example.heartmatch.graphics.model.HeartAssetKey
import com.example.heartmatch.graphics.model.HeartVisualState
import com.example.heartmatch.graphics.model.UiAssetKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry that decouples game logic from graphical asset file paths.
 * Allows runtime skinning and asset replacement without changing any engine code.
 */
class AssetRegistry {

    private val defaultDescriptors = ConcurrentHashMap<String, AssetDescriptor>()
    private val customOverrides = ConcurrentHashMap<String, AssetDescriptor>()
    private val memorySvgStore = ConcurrentHashMap<String, String>()

    init {
        initializeDefaultRegistry()
    }

    private fun initializeDefaultRegistry() {
        // 1. Normal Hearts
        HeartColor.values().forEach { color ->
            val key = HeartAssetKey.Normal(color)
            registerDefault(key, key.defaultRelativePath)
        }

        // 2. Blocker Hearts
        BlockerType.values().forEach { type ->
            val key = HeartAssetKey.Blocker(type)
            registerDefault(key, key.defaultRelativePath)
        }

        // 3. Special Hearts
        SpecialHeartType.values().forEach { type ->
            val key = HeartAssetKey.Special(type)
            registerDefault(key, key.defaultRelativePath)
        }

        // 4. Effects
        EffectAssetKey.values().forEach { effect ->
            registerDefault(effect, effect.defaultRelativePath)
        }

        // 5. UI
        UiAssetKey.values().forEach { ui ->
            registerDefault(ui, ui.defaultRelativePath)
        }

        // 6. Backgrounds
        BackgroundAssetKey.values().forEach { bg ->
            registerDefault(bg, bg.defaultRelativePath)
        }
    }

    private fun registerDefault(key: AssetKey, relativePath: String) {
        val descriptor = AssetDescriptor(
            key = key,
            relativePath = relativePath,
            width = 512,
            height = 512,
            format = if (relativePath.endsWith(".webp")) AssetFormat.WEBP else if (relativePath.endsWith(".png")) AssetFormat.PNG else AssetFormat.SVG
        )
        defaultDescriptors[key.identifier] = descriptor
    }

    /**
     * Replaces or overrides an asset at runtime without modifying game logic.
     */
    fun registerCustomAsset(key: AssetKey, customPath: String, width: Int = 512, height: Int = 512) {
        val descriptor = AssetDescriptor(
            key = key,
            relativePath = customPath,
            width = width,
            height = height,
            format = if (customPath.endsWith(".webp")) AssetFormat.WEBP else if (customPath.endsWith(".png")) AssetFormat.PNG else AssetFormat.SVG
        )
        customOverrides[key.identifier] = descriptor
    }

    /**
     * Stores in-memory custom SVG content for a given asset key.
     */
    fun setMemorySvg(key: AssetKey, state: HeartVisualState = HeartVisualState.Idle, svgContent: String) {
        val storageKey = "${key.identifier}_${state.stateName}"
        memorySvgStore[storageKey] = svgContent
    }

    fun getMemorySvg(key: AssetKey, state: HeartVisualState = HeartVisualState.Idle): String? {
        val storageKey = "${key.identifier}_${state.stateName}"
        return memorySvgStore[storageKey] ?: memorySvgStore["${key.identifier}_idle"]
    }

    /**
     * Retrieves the active descriptor for the given key.
     */
    fun getDescriptor(key: AssetKey): AssetDescriptor {
        return customOverrides[key.identifier]
            ?: defaultDescriptors[key.identifier]
            ?: AssetDescriptor(key, key.defaultRelativePath)
    }

    /**
     * Returns the relative path for a key and specific visual state.
     */
    fun getPathForState(key: AssetKey, state: HeartVisualState = HeartVisualState.Idle): String {
        val baseDescriptor = getDescriptor(key)
        if (state == HeartVisualState.Idle) {
            return baseDescriptor.relativePath
        }

        val basePath = baseDescriptor.relativePath
        val dotIndex = basePath.lastIndexOf('.')
        return if (dotIndex > 0) {
            "${basePath.substring(0, dotIndex)}-${state.stateName}${basePath.substring(dotIndex)}"
        } else {
            "${basePath}-${state.stateName}"
        }
    }

    // Convenience lookups by domain enums
    fun getNormalHeartPath(color: HeartColor, state: HeartVisualState = HeartVisualState.Idle): String =
        getPathForState(HeartAssetKey.Normal(color), state)

    fun getBlockerHeartPath(type: BlockerType, state: HeartVisualState = HeartVisualState.Idle): String =
        getPathForState(HeartAssetKey.Blocker(type), state)

    fun getSpecialHeartPath(type: SpecialHeartType, state: HeartVisualState = HeartVisualState.Idle): String =
        getPathForState(HeartAssetKey.Special(type), state)

    fun getEffectPath(effect: EffectAssetKey): String =
        getDescriptor(effect).relativePath

    fun getUiPath(ui: UiAssetKey): String =
        getDescriptor(ui).relativePath

    fun getBackgroundPath(bg: BackgroundAssetKey): String =
        getDescriptor(bg).relativePath

    /**
     * Clears all custom overrides and restores defaults.
     */
    fun resetToDefaults() {
        customOverrides.clear()
        memorySvgStore.clear()
    }
}
