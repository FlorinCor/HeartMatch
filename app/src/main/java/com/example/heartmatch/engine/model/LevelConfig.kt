package com.example.heartmatch.engine.model

enum class LevelDifficulty {
    EASY,
    INTERMEDIATE,
    MEDIUM,
    HARD,
    EXPERT;

    companion object {
        fun fromString(str: String?): LevelDifficulty {
            return when (str?.trim()?.uppercase()) {
                "EASY" -> EASY
                "INTERMEDIATE" -> INTERMEDIATE
                "MEDIUM" -> MEDIUM
                "HARD" -> HARD
                "EXPERT" -> EXPERT
                else -> MEDIUM
            }
        }
    }
}

data class BlockerConfig(
    val stoneDurability: Int = 2,
    val iceHitPoints: Int = 1,
    val woodenLayers: Int = 2,
    val barbedDurability: Int = 1,
    val brokenRepairsRequired: Int = 2,
    val chainedChainCount: Int = 1,
    val darkHeartSpreadTurns: Int = 1,
    val darkHeartDurability: Int = 1
)

data class LevelConfig(
    val id: Int,
    val name: String,
    val rows: Int,
    val cols: Int,
    val cellStates: Array<Array<CellState>>? = null,
    val initialTiles: Array<Array<Tile?>>? = null,
    val allowedColors: List<HeartColor> = listOf(
        HeartColor.RED,
        HeartColor.PINK,
        HeartColor.BLUE,
        HeartColor.GREEN,
        HeartColor.YELLOW
    ),
    val moveLimit: Int? = 30,
    val timeLimitSeconds: Int? = null,
    val starThresholds: Triple<Int, Int, Int> = Triple(5000, 10000, 15000),
    val difficulty: LevelDifficulty = LevelDifficulty.MEDIUM,
    val colorWeights: Map<HeartColor, Int>? = null,
    val objectives: List<ObjectiveConfig> = emptyList(),
    val randomSeed: Long = 0L,
    val blockerConfig: BlockerConfig = BlockerConfig()
)
