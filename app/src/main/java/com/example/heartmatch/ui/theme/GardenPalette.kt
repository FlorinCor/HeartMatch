package com.example.heartmatch.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

object GardenPalette {
    val Background = Color(0xFF10251F)
    val Panel = Color(0xFF203C32)
    val PanelLight = Color(0xFF345347)
    val Ivory = Color(0xFFF8F1E3)
    val Rose = Color(0xFFAD5267)
    val RoseDark = Color(0xFF853B50)
    val RoseLight = Color(0xFFE9B5BD)
    val Gold = Color(0xFFE4CA91)
    val Rim = Color(0xFF73877B)
    val CellLight = Color(0xFF30483F)
    val CellDark = Color(0xFF283D36)
}

val LocalReducedMotion = staticCompositionLocalOf { false }
