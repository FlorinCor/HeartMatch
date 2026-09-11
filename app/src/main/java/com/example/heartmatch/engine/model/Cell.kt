package com.example.heartmatch.engine.model

enum class CellState {
    PLAYABLE,
    UNAVAILABLE, // Irregular layout holes / out of bounds
    LOCKED       // Locked cell
}

data class Cell(
    val coord: Coord,
    val state: CellState = CellState.PLAYABLE,
    var tile: Tile? = null
) {
    val isPlayable: Boolean get() = state == CellState.PLAYABLE
    val isUnavailable: Boolean get() = state == CellState.UNAVAILABLE
    val isLocked: Boolean get() = state == CellState.LOCKED
    val isEmpty: Boolean get() = isPlayable && tile == null
}
