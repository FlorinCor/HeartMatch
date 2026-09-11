package com.example.heartmatch.engine.model

import java.util.UUID

sealed interface Tile {
    val id: String
    val isMovable: Boolean
    val isMatchable: Boolean
    val canFall: Boolean
    val durability: Int
    val maxDurability: Int

    val matchColor: HeartColor?

    data class Normal(
        override val id: String = UUID.randomUUID().toString(),
        val color: HeartColor
    ) : Tile {
        override val isMovable: Boolean = true
        override val isMatchable: Boolean = true
        override val canFall: Boolean = true
        override val durability: Int = 1
        override val maxDurability: Int = 1
        override val matchColor: HeartColor = color
    }

    data class Special(
        override val id: String = UUID.randomUUID().toString(),
        val specialType: SpecialHeartType,
        val baseColor: HeartColor? = null,
        val fireDirection: FireDirection? = null,
        val bombRadius: Int = 1
    ) : Tile {
        override val isMovable: Boolean = true
        override val isMatchable: Boolean = true
        override val canFall: Boolean = true
        override val durability: Int = 1
        override val maxDurability: Int = 1
        override val matchColor: HeartColor? = baseColor
    }

    data class Blocker(
        override val id: String = UUID.randomUUID().toString(),
        val blockerType: BlockerType,
        override val durability: Int = defaultDurability(blockerType),
        override val maxDurability: Int = durability,
        val payloadTile: Tile? = null,
        val color: HeartColor? = null,
        val turnsSurvived: Int = 0,
        val spreadIntervalTurns: Int = 1
    ) : Tile {
        override val isMovable: Boolean
            get() = when (blockerType) {
                BlockerType.BROKEN_HEART,
                BlockerType.STITCHED_HEART -> true
                else -> false
            }

        override val isMatchable: Boolean
            get() = when (blockerType) {
                BlockerType.ICE_HEART,
                BlockerType.CHAINED_HEART -> true
                else -> false
            }

        override val canFall: Boolean
            get() = when (blockerType) {
                BlockerType.CHAINED_HEART -> false
                else -> true
            }

        override val matchColor: HeartColor?
            get() = color ?: payloadTile?.matchColor

        val hitPoints: Int get() = durability
        val layers: Int get() = durability
        val chainCount: Int get() = durability
        val repairsRemaining: Int get() = durability

        companion object {
            fun defaultDurability(type: BlockerType): Int = when (type) {
                BlockerType.STONE_HEART -> 2
                BlockerType.ICE_HEART -> 1
                BlockerType.WOODEN_HEART -> 2
                BlockerType.BARBED_HEART -> 1
                BlockerType.BROKEN_HEART -> 2
                BlockerType.STITCHED_HEART -> 2
                BlockerType.CHAINED_HEART -> 1
                BlockerType.DARK_HEART -> 1
            }

            fun createStone(durability: Int = 2): Blocker =
                Blocker(blockerType = BlockerType.STONE_HEART, durability = durability, maxDurability = durability)

            fun createIce(hitPoints: Int = 1, payload: Tile? = null, color: HeartColor? = null): Blocker =
                Blocker(blockerType = BlockerType.ICE_HEART, durability = hitPoints, maxDurability = hitPoints, payloadTile = payload, color = color ?: payload?.matchColor)

            fun createWooden(layers: Int = 2): Blocker =
                Blocker(blockerType = BlockerType.WOODEN_HEART, durability = layers, maxDurability = layers)

            fun createBarbed(durability: Int = 1): Blocker =
                Blocker(blockerType = BlockerType.BARBED_HEART, durability = durability, maxDurability = durability)

            fun createBroken(repairsRequired: Int = 2, color: HeartColor = HeartColor.RED): Blocker =
                Blocker(blockerType = BlockerType.BROKEN_HEART, durability = repairsRequired, maxDurability = repairsRequired, color = color)

            fun createStitched(repairsRequired: Int = 2, color: HeartColor = HeartColor.RED): Blocker =
                Blocker(blockerType = BlockerType.STITCHED_HEART, durability = repairsRequired, maxDurability = repairsRequired, color = color)

            fun createChained(chainCount: Int = 1, payload: Tile? = null, color: HeartColor? = null): Blocker =
                Blocker(blockerType = BlockerType.CHAINED_HEART, durability = chainCount, maxDurability = chainCount, payloadTile = payload, color = color ?: payload?.matchColor)

            fun createDark(spreadIntervalTurns: Int = 1, durability: Int = 1): Blocker =
                Blocker(blockerType = BlockerType.DARK_HEART, durability = durability, maxDurability = durability, spreadIntervalTurns = spreadIntervalTurns)
        }
    }
}
