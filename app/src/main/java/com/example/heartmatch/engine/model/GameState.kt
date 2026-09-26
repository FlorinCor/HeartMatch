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
    var extraMovesGranted: Int = 0,
    var scoreBreakdown: ScoreBreakdown = ScoreBreakdown(),
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


data class ScoreBreakdown(
    val hearts: Int = 0,
    val creations: Int = 0,
    val blockers: Int = 0,
    val specials: Int = 0,
    val cascades: Int = 0,
    val remainingMoves: Int = 0
) {
    val total: Int get() = hearts + creations + blockers + specials + cascades + remainingMoves
    fun plus(other: ScoreBreakdown) = ScoreBreakdown(hearts + other.hearts, creations + other.creations,
        blockers + other.blockers, specials + other.specials, cascades + other.cascades, remainingMoves + other.remainingMoves)
}
