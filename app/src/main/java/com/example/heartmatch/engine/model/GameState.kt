package com.example.heartmatch.engine.model

enum class GameStatus {
    READY_FOR_INPUT,
    RESOLVING,
    OBJECTIVE_COMPLETED,
    GAME_OVER
}

data class GameState(
    val levelId: Int,
    var status: GameStatus,
    var movesRemaining: Int,
    var timeRemainingSeconds: Int? = null,
    var score: Int = 0,
    var comboCount: Int = 0,
    var earnedStars: Int = 0,
    val board: Board,
    val objectives: List<Objective>,
    val starThresholds: Triple<Int, Int, Int> = Triple(5000, 10000, 15000)
) {
    val isWon: Boolean
        get() = objectives.isNotEmpty() && objectives.all { it.isFulfilled }

    val isLost: Boolean
        get() = !isWon && (movesRemaining <= 0 || (timeRemainingSeconds != null && timeRemainingSeconds!! <= 0))
}
