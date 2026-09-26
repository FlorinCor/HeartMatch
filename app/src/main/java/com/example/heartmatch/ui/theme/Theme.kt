package com.example.heartmatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GardenColors = darkColorScheme(
    primary = GardenPalette.RoseLight,
    onPrimary = GardenPalette.Background,
    secondary = GardenPalette.Gold,
    background = GardenPalette.Background,
    surface = GardenPalette.Panel,
    onSurface = GardenPalette.Ivory,
    onBackground = GardenPalette.Ivory
)

@Composable
fun HeartMatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GardenColors, typography = Typography, content = content)
}
