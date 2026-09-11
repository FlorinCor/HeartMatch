package com.example.heartmatch.graphics.model

import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType

enum class AssetCategory {
    HEARTS_NORMAL,
    HEARTS_BLOCKER,
    HEARTS_SPECIAL,
    EFFECTS,
    UI,
    BACKGROUNDS
}

enum class AssetFormat {
    SVG,
    WEBP,
    PNG
}

/**
 * Visual states supported by all hearts and blockers.
 */
sealed interface HeartVisualState {
    object Idle : HeartVisualState
    object Selected : HeartVisualState
    object Matched : HeartVisualState
    data class Damaged(val damageLevel: Int = 1, val healthRatio: Float = 0.5f) : HeartVisualState
    data class Destroyed(val progress: Float = 1.0f) : HeartVisualState

    val stateName: String
        get() = when (this) {
            is Idle -> "idle"
            is Selected -> "selected"
            is Matched -> "matched"
            is Damaged -> "damaged"
            is Destroyed -> "destroyed"
        }
}

/**
 * Strongly typed identifiers for all game graphics.
 */
sealed interface AssetKey {
    val identifier: String
    val category: AssetCategory
    val defaultRelativePath: String
}

sealed class HeartAssetKey : AssetKey {
    data class Normal(val color: HeartColor) : HeartAssetKey() {
        override val identifier: String = "${color.name.lowercase()}-heart"
        override val category: AssetCategory = AssetCategory.HEARTS_NORMAL
        override val defaultRelativePath: String = "assets/hearts/normal/$identifier.svg"
    }

    data class Blocker(val blockerType: BlockerType) : HeartAssetKey() {
        override val identifier: String = when (blockerType) {
            BlockerType.STONE_HEART -> "stone-heart"
            BlockerType.ICE_HEART -> "ice-heart"
            BlockerType.WOODEN_HEART -> "wooden-heart"
            BlockerType.BARBED_HEART -> "barbed-heart"
            BlockerType.BROKEN_HEART -> "broken-heart"
            BlockerType.STITCHED_HEART -> "stitched-heart"
            BlockerType.CHAINED_HEART -> "chained-heart"
            BlockerType.DARK_HEART -> "dark-heart"
        }
        override val category: AssetCategory = AssetCategory.HEARTS_BLOCKER
        override val defaultRelativePath: String = "assets/hearts/blockers/$identifier.svg"
    }

    data class Special(val specialType: SpecialHeartType) : HeartAssetKey() {
        override val identifier: String = when (specialType) {
            SpecialHeartType.RAINBOW_HEART -> "rainbow-heart"
            SpecialHeartType.FIRE_HEART -> "fire-heart"
            SpecialHeartType.BOMB_HEART -> "bomb-heart"
            SpecialHeartType.GIFT_HEART -> "gift-heart"
            SpecialHeartType.ROYAL_HEART -> "royal-heart"
            SpecialHeartType.ANGEL_HEART -> "angel-heart"
        }
        override val category: AssetCategory = AssetCategory.HEARTS_SPECIAL
        override val defaultRelativePath: String = "assets/hearts/special/$identifier.svg"
    }
}

enum class EffectAssetKey(val effectName: String) : AssetKey {
    NORMAL_MATCH("normal-match"),
    MATCH_4("match-4"),
    MATCH_5("match-5"),
    CASCADE("cascade"),
    FIRE_HEART_ACTIVATION("fire-heart-activation"),
    BOMB_HEART_EXPLOSION("bomb-heart-explosion"),
    RAINBOW_HEART_ACTIVATION("rainbow-heart-activation"),
    SPECIAL_HEART_COMBINATION("special-combination"),
    BLOCKER_DAMAGE("blocker-damage"),
    BLOCKER_DESTRUCTION("blocker-destruction"),
    LEVEL_COMPLETION("level-completion"),
    LEVEL_FAILURE("level-failure"),
    STAR_AWARD("star-award");

    override val identifier: String = effectName
    override val category: AssetCategory = AssetCategory.EFFECTS
    override val defaultRelativePath: String = "assets/effects/$effectName.svg"
}

enum class UiAssetKey(val uiName: String) : AssetKey {
    BOOSTER_HAMMER("booster-hammer"),
    BOOSTER_BOMB("booster-bomb"),
    BOOSTER_RAINBOW("booster-rainbow"),
    BOOSTER_SHUFFLE("booster-shuffle"),
    BOOSTER_EXTRA_MOVES("booster-extra-moves"),
    PLAY_BUTTON("play-button"),
    PAUSE_BUTTON("pause-button"),
    STAR_FULL("star-full"),
    STAR_EMPTY("star-empty"),
    LOCK_ICON("lock-icon"),
    COIN_ICON("coin-icon"),
    HEART_LIFE_ICON("heart-life-icon");

    override val identifier: String = uiName
    override val category: AssetCategory = AssetCategory.UI
    override val defaultRelativePath: String = "assets/ui/$uiName.svg"
}

enum class BackgroundAssetKey(val bgName: String) : AssetKey {
    HEART_MEADOW("heart-meadow"),
    STONE_VALLEY("stone-valley"),
    BROKEN_FOREST("broken-forest"),
    SHADOW_GARDEN("shadow-garden"),
    HEART_KINGDOM("heart-kingdom");

    override val identifier: String = bgName
    override val category: AssetCategory = AssetCategory.BACKGROUNDS
    override val defaultRelativePath: String = "assets/backgrounds/$bgName.svg"
}

/**
 * Metadata descriptor for an asset.
 */
data class AssetDescriptor(
    val key: AssetKey,
    val relativePath: String,
    val width: Int = 512,
    val height: Int = 512,
    val format: AssetFormat = AssetFormat.SVG,
    val state: HeartVisualState = HeartVisualState.Idle,
    val tags: Set<String> = emptySet()
)
