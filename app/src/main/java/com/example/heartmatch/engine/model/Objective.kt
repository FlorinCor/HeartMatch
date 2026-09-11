package com.example.heartmatch.engine.model

enum class ObjectiveType {
    COLLECT_COLOR,
    DESTROY_BLOCKERS,
    REPAIR_BROKEN,
    CLEAR_DARK_HEARTS,
    COLLECT_SPECIAL,
    SCORE,
    CLEAR_BOARD,
    CLEAR_SPECIFIC_CELLS,

    // Backward-compatible aliases
    COLLECT_HEARTS,
    CLEAR_BLOCKER,
    CREATE_SPECIALS,
    REACH_SCORE,
    COLLECT_GIFT
}

data class ObjectiveConfig(
    val type: ObjectiveType,
    val targetCount: Int = 0,
    val targetColor: HeartColor? = null,
    val targetBlocker: BlockerType? = null,
    val targetSpecial: SpecialHeartType? = null,
    val targetCells: Set<Coord> = emptySet()
)

data class Objective(
    val config: ObjectiveConfig,
    var currentCount: Int = 0,
    val clearedCells: MutableSet<Coord> = mutableSetOf()
) {
    val isFulfilled: Boolean
        get() = when (config.type) {
            ObjectiveType.CLEAR_SPECIFIC_CELLS -> {
                if (config.targetCells.isNotEmpty()) {
                    clearedCells.containsAll(config.targetCells)
                } else {
                    currentCount >= config.targetCount
                }
            }
            else -> currentCount >= config.targetCount
        }

    val remainingCount: Int
        get() = when (config.type) {
            ObjectiveType.CLEAR_SPECIFIC_CELLS -> {
                if (config.targetCells.isNotEmpty()) {
                    (config.targetCells - clearedCells).size
                } else {
                    maxOf(0, config.targetCount - currentCount)
                }
            }
            else -> maxOf(0, config.targetCount - currentCount)
        }
}
