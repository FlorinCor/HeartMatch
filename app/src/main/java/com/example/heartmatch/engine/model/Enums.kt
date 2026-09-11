package com.example.heartmatch.engine.model

enum class HeartColor {
    RED,
    PINK,
    BLUE,
    GREEN,
    YELLOW,
    PURPLE,
    ORANGE
}

enum class SpecialHeartType {
    RAINBOW_HEART, // Clears all hearts of a color (5 in a row)
    FIRE_HEART,    // Clears row or column (4 in a row)
    BOMB_HEART,    // Area explosion with configurable radius (T or L shape)
    GIFT_HEART,    // Bonus points and reward effects
    ROYAL_HEART,   // Crowned jewel heart with cross/diamond area blast
    ANGEL_HEART    // Winged celestial heart with holy blast / objective blessing
}

enum class FireDirection {
    ROW,       // Clears entire horizontal row
    COLUMN,    // Clears entire vertical column
    BOTH       // Clears both row and column (cross)
}

enum class SpecialCombinationType {
    RAINBOW_COLOR,
    RAINBOW_FIRE,
    RAINBOW_BOMB,
    RAINBOW_RAINBOW,
    FIRE_FIRE,
    FIRE_BOMB,
    BOMB_BOMB,
    GENERIC_SPECIAL_SWAP
}

enum class BlockerType {
    STONE_HEART,   // Immovable solid barrier; damaged by adjacent matches/specials
    ICE_HEART,     // Encapsulates normal heart; cracked by matching inner color or adjacent hit
    WOODEN_HEART,  // Multi-hit crate (1-3 durability)
    BARBED_HEART,  // Thorns locking a heart; cleared via adjacent match or special explosion
    BROKEN_HEART,  // Cracked piece requiring 2 repair matches/hits; movable and matchable
    STITCHED_HEART,// Stitched leather/cloth heart requiring repair hits; movable and matchable
    CHAINED_HEART, // Locks heart in place (immovable); matchable; breaking chain frees heart
    DARK_HEART     // Corruption tile; spreads to adjacent cell if not damaged in a turn
}
