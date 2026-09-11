package com.example.heartmatch.graphics

import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.graphics.animation.EasingType
import com.example.heartmatch.graphics.animation.StandardHeartAnimations
import com.example.heartmatch.graphics.animation.interpolate
import com.example.heartmatch.graphics.generator.SvgAssetGenerator
import com.example.heartmatch.graphics.model.BackgroundAssetKey
import com.example.heartmatch.graphics.model.EffectAssetKey
import com.example.heartmatch.graphics.model.HeartAssetKey
import com.example.heartmatch.graphics.model.HeartVisualState
import com.example.heartmatch.graphics.model.UiAssetKey
import com.example.heartmatch.graphics.registry.AssetManager
import com.example.heartmatch.graphics.registry.AssetRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class AssetSystemTest {

    private lateinit var registry: AssetRegistry
    private lateinit var assetManager: AssetManager

    @Before
    fun setUp() {
        registry = AssetRegistry()
        assetManager = AssetManager(registry)
    }

    @Test
    fun testExportAllAssetsToDirectories() {
        val rootDir = File(".").canonicalFile
        val assetsMainDir = if (File(rootDir, "app/src/main/assets").exists()) {
            File(rootDir, "app/src/main/assets")
        } else {
            File(rootDir, "src/main/assets")
        }
        val resourcesMainDir = if (File(rootDir, "app/src/main/resources").exists()) {
            File(rootDir, "app/src/main/resources")
        } else {
            File(rootDir, "src/main/resources")
        }

        SvgAssetGenerator.exportAllAssets(assetsMainDir)
        SvgAssetGenerator.exportAllAssets(resourcesMainDir)

        // Verify normal hearts exist
        HeartColor.values().forEach { color ->
            val key = HeartAssetKey.Normal(color)
            val file = File(assetsMainDir, key.defaultRelativePath)
            assertTrue("File must exist: ${file.path}", file.exists())
            assertTrue("File size must be > 100 bytes: ${file.path}", file.length() > 100)
            val svgContent = file.readText()
            assertTrue(svgContent.contains("viewBox=\"0 0 512 512\""))
            assertTrue(svgContent.contains("<svg"))
            assertTrue(svgContent.contains("</svg>"))
        }

        // Verify blockers exist
        BlockerType.values().forEach { type ->
            val key = HeartAssetKey.Blocker(type)
            val file = File(assetsMainDir, key.defaultRelativePath)
            assertTrue("Blocker asset must exist: ${file.path}", file.exists())
            val svgContent = file.readText()
            assertTrue(svgContent.contains("viewBox=\"0 0 512 512\""))
        }

        // Verify specials exist
        SpecialHeartType.values().forEach { type ->
            val key = HeartAssetKey.Special(type)
            val file = File(assetsMainDir, key.defaultRelativePath)
            assertTrue("Special asset must exist: ${file.path}", file.exists())
            val svgContent = file.readText()
            assertTrue(svgContent.contains("viewBox=\"0 0 512 512\""))
        }

        // Verify all 13 effects exist
        EffectAssetKey.values().forEach { effect ->
            val file = File(assetsMainDir, effect.defaultRelativePath)
            assertTrue("Effect asset must exist: ${file.path}", file.exists())
        }

        // Verify all 12 UI icons exist
        UiAssetKey.values().forEach { ui ->
            val file = File(assetsMainDir, ui.defaultRelativePath)
            assertTrue("UI asset must exist: ${file.path}", file.exists())
        }

        // Verify all 5 backgrounds exist
        BackgroundAssetKey.values().forEach { bg ->
            val file = File(assetsMainDir, bg.defaultRelativePath)
            assertTrue("Background asset must exist: ${file.path}", file.exists())
        }
    }

    @Test
    fun testAssetRegistryMapping() {
        assertEquals("assets/hearts/normal/red-heart.svg", registry.getNormalHeartPath(HeartColor.RED))
        assertEquals("assets/hearts/normal/blue-heart.svg", registry.getNormalHeartPath(HeartColor.BLUE))
        assertEquals("assets/hearts/blockers/stone-heart.svg", registry.getBlockerHeartPath(BlockerType.STONE_HEART))
        assertEquals("assets/hearts/blockers/dark-heart.svg", registry.getBlockerHeartPath(BlockerType.DARK_HEART))
        assertEquals("assets/hearts/blockers/stitched-heart.svg", registry.getBlockerHeartPath(BlockerType.STITCHED_HEART))
        assertEquals("assets/hearts/special/rainbow-heart.svg", registry.getSpecialHeartPath(SpecialHeartType.RAINBOW_HEART))
        assertEquals("assets/hearts/special/royal-heart.svg", registry.getSpecialHeartPath(SpecialHeartType.ROYAL_HEART))
        assertEquals("assets/hearts/special/angel-heart.svg", registry.getSpecialHeartPath(SpecialHeartType.ANGEL_HEART))

        assertEquals("assets/effects/bomb-heart-explosion.svg", registry.getEffectPath(EffectAssetKey.BOMB_HEART_EXPLOSION))
        assertEquals("assets/ui/booster-hammer.svg", registry.getUiPath(UiAssetKey.BOOSTER_HAMMER))
        assertEquals("assets/backgrounds/heart-meadow.svg", registry.getBackgroundPath(BackgroundAssetKey.HEART_MEADOW))
    }

    @Test
    fun testVisualStateVariants() {
        val normalRed = HeartAssetKey.Normal(HeartColor.RED)
        assertEquals("assets/hearts/normal/red-heart.svg", registry.getPathForState(normalRed, HeartVisualState.Idle))
        assertEquals("assets/hearts/normal/red-heart-selected.svg", registry.getPathForState(normalRed, HeartVisualState.Selected))
        assertEquals("assets/hearts/normal/red-heart-matched.svg", registry.getPathForState(normalRed, HeartVisualState.Matched))
        assertEquals("assets/hearts/normal/red-heart-damaged.svg", registry.getPathForState(normalRed, HeartVisualState.Damaged()))
        assertEquals("assets/hearts/normal/red-heart-destroyed.svg", registry.getPathForState(normalRed, HeartVisualState.Destroyed()))

        val stoneBlocker = HeartAssetKey.Blocker(BlockerType.STONE_HEART)
        assertEquals("assets/hearts/blockers/stone-heart-damaged.svg", registry.getPathForState(stoneBlocker, HeartVisualState.Damaged()))
    }

    @Test
    fun testRuntimeSkinningAndAssetReplacement() {
        val redKey = HeartAssetKey.Normal(HeartColor.RED)
        val originalPath = registry.getDescriptor(redKey).relativePath

        // Replace asset dynamically without modifying game logic
        registry.registerCustomAsset(redKey, "assets/custom_skins/cyber_red.webp")
        assertEquals("assets/custom_skins/cyber_red.webp", registry.getDescriptor(redKey).relativePath)
        assertEquals("assets/custom_skins/cyber_red-selected.webp", registry.getPathForState(redKey, HeartVisualState.Selected))

        // Reset to default
        registry.resetToDefaults()
        assertEquals(originalPath, registry.getDescriptor(redKey).relativePath)
    }

    @Test
    fun testAssetManagerLoadingAndCache() {
        val redSvg = assetManager.getNormalHeartSvg(HeartColor.RED)
        assertNotNull(redSvg)
        assertTrue(redSvg.contains("<svg"))

        val cachedRedSvg = assetManager.getNormalHeartSvg(HeartColor.RED)
        assertEquals(redSvg, cachedRedSvg)

        val royalSvg = assetManager.getSpecialHeartSvg(SpecialHeartType.ROYAL_HEART)
        assertTrue(royalSvg.contains("<svg"))

        val stitchedSvg = assetManager.getBlockerHeartSvg(BlockerType.STITCHED_HEART)
        assertTrue(stitchedSvg.contains("<svg"))

        assetManager.warmUpCache()
    }

    @Test
    fun testDecoupledAnimationDefinitions() {
        val idleAnim = StandardHeartAnimations.IDLE_BREATHE
        assertEquals(2000L, idleAnim.durationMs)
        assertTrue(idleAnim.isLooping)

        val kf0 = idleAnim.sample(0.0f)
        assertEquals(1.0f, kf0.scaleX, 0.001f)

        val kfMid = idleAnim.sample(0.5f)
        assertTrue(kfMid.scaleX > 1.0f)

        val matchAnim = StandardHeartAnimations.MATCH_VANISH
        assertFalse(matchAnim.isLooping)
        val kfEnd = matchAnim.sample(1.0f)
        assertEquals(0.0f, kfEnd.scaleX, 0.001f)
        assertEquals(0.0f, kfEnd.alpha, 0.001f)

        val damageShake = StandardHeartAnimations.BLOCKER_DAMAGE_SHAKE
        val kfShake = damageShake.sample(0.2f)
        assertTrue(kfShake.shakeIntensity > 0f)

        // Test easing interpolation
        assertEquals(0f, EasingType.LINEAR.interpolate(0f), 0.001f)
        assertEquals(1f, EasingType.LINEAR.interpolate(1f), 0.001f)
        assertEquals(0.25f, EasingType.EASE_IN.interpolate(0.5f), 0.001f)
    }

    @Test
    fun testSvgIntegrityNoTextInHeartAssets() {
        HeartColor.values().forEach { color ->
            val svg = SvgAssetGenerator.generateHeartSvg(HeartAssetKey.Normal(color))
            assertFalse("Heart assets must not contain text tags: $color", svg.contains("<text"))
        }

        BlockerType.values().forEach { blocker ->
            val svg = SvgAssetGenerator.generateHeartSvg(HeartAssetKey.Blocker(blocker))
            assertFalse("Blocker assets must not contain text tags: $blocker", svg.contains("<text"))
        }

        SpecialHeartType.values().forEach { special ->
            val svg = SvgAssetGenerator.generateHeartSvg(HeartAssetKey.Special(special))
            assertFalse("Special assets must not contain text tags: $special", svg.contains("<text"))
        }
    }
}
