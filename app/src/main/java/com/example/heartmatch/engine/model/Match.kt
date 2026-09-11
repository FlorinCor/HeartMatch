package com.example.heartmatch.engine.model

enum class MatchShape {
    HORIZONTAL_3,
    VERTICAL_3,
    HORIZONTAL_4,
    VERTICAL_4,
    FIVE_IN_A_ROW,
    T_SHAPE,
    L_SHAPE,
    CROSS_OR_MULTI
}

data class MatchGroup(
    val color: HeartColor,
    val shape: MatchShape,
    val matchedCoords: Set<Coord>,
    val createdSpecial: SpecialHeartType? = null,
    val createdSpecialDirection: FireDirection? = null,
    val specialSpawnCoord: Coord? = null
)
