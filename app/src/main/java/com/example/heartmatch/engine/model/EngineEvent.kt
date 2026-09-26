package com.example.heartmatch.engine.model

sealed interface EngineEvent {
    data class Swap(val from: Coord, val to: Coord, val isRollback: Boolean) : EngineEvent
    data class Match(val coords: Set<Coord>, val color: HeartColor, val shape: MatchShape, val comboIndex: Int) : EngineEvent
    data class SpecialCreated(val coord: Coord, val specialType: SpecialHeartType) : EngineEvent
    data class SpecialTriggered(val coord: Coord, val specialType: SpecialHeartType, val affectedCoords: Set<Coord>) : EngineEvent
    data class LightHeartActivated(val cells: List<Coord>) : EngineEvent
    data class BlockerDamaged(val coord: Coord, val blockerType: BlockerType, val remainingDurability: Int, val isDestroyed: Boolean) : EngineEvent
    data class BlockerDestroyed(val coord: Coord, val blockerType: BlockerType) : EngineEvent
    data class TileDrop(val from: Coord?, val to: Coord, val tile: Tile, val distance: Int) : EngineEvent
    data class DarkHeartSpread(val source: Coord, val target: Coord) : EngineEvent
    data class TilesCleared(val coords: Set<Coord>) : EngineEvent
    data class ScoreStep(val coord: Coord, val points: Int, val multiplier: Double) : EngineEvent
    data class ScoreChanged(val newScore: Int, val delta: Int, val combo: Int) : EngineEvent
    data class ObjectiveUpdated(val objective: Objective) : EngineEvent
    data class BoardReshuffled(val reason: String = "NO_VALID_MOVES") : EngineEvent
    data class GameWon(val finalScore: Int, val stars: Int) : EngineEvent
    data class GameOver(val reason: String) : EngineEvent
}
