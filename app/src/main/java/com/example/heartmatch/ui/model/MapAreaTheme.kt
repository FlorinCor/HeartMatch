package com.example.heartmatch.ui.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class MapAreaId(val title: String, val startLevel: Int, val endLevel: Int) {
    HEART_MEADOW("Heart Meadow", 1, 20),
    STONE_VALLEY("Stone Valley", 21, 40),
    BROKEN_FOREST("Broken Hearts Forest", 41, 60),
    SHADOW_GARDEN("Shadow Garden", 61, 80),
    HEART_KINGDOM("Heart Kingdom", 81, 100),
    CRYSTAL_COVE("Crystal Cove", 101, 120),
    THORNWOOD_REACH("Thornwood Reach", 121, 140),
    MOONLIT_REEF("Moonlit Reef", 141, 160),
    EMBER_HEARTLANDS("Ember Heartlands", 161, 180),
    EVERHEART_CITADEL("Everheart Citadel", 181, 200);

    companion object {
        fun fromLevel(level: Int): MapAreaId {
            return when {
                level in 1..20 -> HEART_MEADOW
                level in 21..40 -> STONE_VALLEY
                level in 41..60 -> BROKEN_FOREST
                level in 61..80 -> SHADOW_GARDEN
                level in 81..100 -> HEART_KINGDOM
                level in 101..120 -> CRYSTAL_COVE
                level in 121..140 -> THORNWOOD_REACH
                level in 141..160 -> MOONLIT_REEF
                level in 161..180 -> EMBER_HEARTLANDS
                level in 181..200 -> EVERHEART_CITADEL
                else -> HEART_MEADOW
            }
        }
    }
}

data class MapAreaTheme(
    val id: MapAreaId,
    val name: String,
    val subtitle: String,
    val levelRangeText: String,
    val iconEmoji: String,
    val backgroundColors: List<Color>,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val nodeGradientColors: List<Color>,
    val completedNodeGradientColors: List<Color>,
    val pathColor: Color,
    val bannerGradientColors: List<Color>
) {
    companion object {
        val HEART_MEADOW = MapAreaTheme(
            id = MapAreaId.HEART_MEADOW,
            name = "Heart Meadow",
            subtitle = "Lush green hills blooming with candy hearts & morning dew",
            levelRangeText = "Levels 1 - 20",
            iconEmoji = "🌸",
            backgroundColors = listOf(
                Color(0xFF0F381E),
                Color(0xFF1B5E20),
                Color(0xFF2E7D32),
                Color(0xFF388E3C)
            ),
            primaryAccent = Color(0xFF66BB6A),
            secondaryAccent = Color(0xFFFF80AB),
            nodeGradientColors = listOf(Color(0xFF43A047), Color(0xFF1B5E20)),
            completedNodeGradientColors = listOf(Color(0xFF00E676), Color(0xFF2E7D32)),
            pathColor = Color(0xFF81C784),
            bannerGradientColors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
        )

        val STONE_VALLEY = MapAreaTheme(
            id = MapAreaId.STONE_VALLEY,
            name = "Stone Valley",
            subtitle = "Ancient gemstone canyons and rugged stone monoliths",
            levelRangeText = "Levels 21 - 40",
            iconEmoji = "⛰️",
            backgroundColors = listOf(
                Color(0xFF3E2723),
                Color(0xFF4E342E),
                Color(0xFF5D4037),
                Color(0xFF6D4C41)
            ),
            primaryAccent = Color(0xFFFFB74D),
            secondaryAccent = Color(0xFFFF7043),
            nodeGradientColors = listOf(Color(0xFFFB8C00), Color(0xFFE65100)),
            completedNodeGradientColors = listOf(Color(0xFFFFB300), Color(0xFFE65100)),
            pathColor = Color(0xFFFFCC80),
            bannerGradientColors = listOf(Color(0xFF4E342E), Color(0xFFE65100))
        )

        val BROKEN_FOREST = MapAreaTheme(
            id = MapAreaId.BROKEN_FOREST,
            name = "Broken Hearts Forest",
            subtitle = "Enchanted twilight woods shrouded in mystical crystal mist",
            levelRangeText = "Levels 41 - 60",
            iconEmoji = "🔮",
            backgroundColors = listOf(
                Color(0xFF1A002C),
                Color(0xFF311B92),
                Color(0xFF4A148C),
                Color(0xFF6A1B9A)
            ),
            primaryAccent = Color(0xFFBA68C8),
            secondaryAccent = Color(0xFF00E5FF),
            nodeGradientColors = listOf(Color(0xFF8E24AA), Color(0xFF4A148C)),
            completedNodeGradientColors = listOf(Color(0xFFAB47BC), Color(0xFF311B92)),
            pathColor = Color(0xFFCE93D8),
            bannerGradientColors = listOf(Color(0xFF311B92), Color(0xFF7B1FA2))
        )

        val SHADOW_GARDEN = MapAreaTheme(
            id = MapAreaId.SHADOW_GARDEN,
            name = "Shadow Garden",
            subtitle = "Midnight rose labyrinth guarded by glowing dark thorns",
            levelRangeText = "Levels 61 - 80",
            iconEmoji = "🥀",
            backgroundColors = listOf(
                Color(0xFF0A0E1A),
                Color(0xFF1B0024),
                Color(0xFF4A0033),
                Color(0xFF880E4F)
            ),
            primaryAccent = Color(0xFFFF4081),
            secondaryAccent = Color(0xFFFF1744),
            nodeGradientColors = listOf(Color(0xFFC2185B), Color(0xFF880E4F)),
            completedNodeGradientColors = listOf(Color(0xFFFF4081), Color(0xFFAD1457)),
            pathColor = Color(0xFFFF80AB),
            bannerGradientColors = listOf(Color(0xFF4A0033), Color(0xFF880E4F))
        )

        val HEART_KINGDOM = MapAreaTheme(
            id = MapAreaId.HEART_KINGDOM,
            name = "Heart Kingdom",
            subtitle = "The celestial royal palace of golden spires and ruby crowns",
            levelRangeText = "Levels 81 - 100",
            iconEmoji = "👑",
            backgroundColors = listOf(
                Color(0xFF2A0845),
                Color(0xFF4A148C),
                Color(0xFF6A1B9A),
                Color(0xFF8E24AA)
            ),
            primaryAccent = Color(0xFFFFD700),
            secondaryAccent = Color(0xFFFFAB00),
            nodeGradientColors = listOf(Color(0xFFFFD54F), Color(0xFFFF8F00)),
            completedNodeGradientColors = listOf(Color(0xFFFFEE58), Color(0xFFFF6F00)),
            pathColor = Color(0xFFFFE082),
            bannerGradientColors = listOf(Color(0xFF4A148C), Color(0xFFFF8F00))
        )

        private fun chapterTheme(
            id: MapAreaId,
            subtitle: String,
            emoji: String,
            night: Color,
            deep: Color,
            mid: Color,
            light: Color,
            accent: Color,
            secondary: Color,
            node: Color,
            completed: Color
        ) = MapAreaTheme(
            id = id,
            name = id.title,
            subtitle = subtitle,
            levelRangeText = "Levels ${id.startLevel} - ${id.endLevel}",
            iconEmoji = emoji,
            backgroundColors = listOf(night, deep, mid, light),
            primaryAccent = accent,
            secondaryAccent = secondary,
            nodeGradientColors = listOf(node, deep),
            completedNodeGradientColors = listOf(completed, node),
            pathColor = accent.copy(alpha = 0.8f),
            bannerGradientColors = listOf(deep, node)
        )

        val CRYSTAL_COVE = chapterTheme(
            MapAreaId.CRYSTAL_COVE, "Sea glass cliffs glitter above a calm turquoise bay", "🪸",
            Color(0xFF061B2B), Color(0xFF0C3D59), Color(0xFF087E8B), Color(0xFF36B7B2),
            Color(0xFF83F0E2), Color(0xFFFFA7C4), Color(0xFF00ACC1), Color(0xFF4DD0E1)
        )
        val THORNWOOD_REACH = chapterTheme(
            MapAreaId.THORNWOOD_REACH, "Wild rose thickets wind through an ancient greenwood", "🌿",
            Color(0xFF102319), Color(0xFF1B472B), Color(0xFF356B38), Color(0xFF5E8D45),
            Color(0xFFA5D66D), Color(0xFFFF9BAA), Color(0xFF689F38), Color(0xFFAED581)
        )
        val MOONLIT_REEF = chapterTheme(
            MapAreaId.MOONLIT_REEF, "Silver moonlight shimmers across a hidden reef", "🌙",
            Color(0xFF10162D), Color(0xFF242B58), Color(0xFF49417E), Color(0xFF7263A2),
            Color(0xFFC4B8FF), Color(0xFF75E5F0), Color(0xFF5C6BC0), Color(0xFF9FA8DA)
        )
        val EMBER_HEARTLANDS = chapterTheme(
            MapAreaId.EMBER_HEARTLANDS, "Warm volcanic gardens glow with living embers", "🔥",
            Color(0xFF2C1115), Color(0xFF64251C), Color(0xFF9C3D20), Color(0xFFC45C28),
            Color(0xFFFFC46B), Color(0xFFFF8A80), Color(0xFFE64A19), Color(0xFFFF8A65)
        )
        val EVERHEART_CITADEL = chapterTheme(
            MapAreaId.EVERHEART_CITADEL, "A rose-gold citadel crowns the farthest reaches", "🏰",
            Color(0xFF281326), Color(0xFF54203D), Color(0xFF873957), Color(0xFFB45A62),
            Color(0xFFFFD18B), Color(0xFFFFA6B7), Color(0xFFAD4057), Color(0xFFEF798D)
        )

        fun getThemeForLevel(level: Int): MapAreaTheme {
            return when (MapAreaId.fromLevel(level)) {
                MapAreaId.HEART_MEADOW -> HEART_MEADOW
                MapAreaId.STONE_VALLEY -> STONE_VALLEY
                MapAreaId.BROKEN_FOREST -> BROKEN_FOREST
                MapAreaId.SHADOW_GARDEN -> SHADOW_GARDEN
                MapAreaId.HEART_KINGDOM -> HEART_KINGDOM
                MapAreaId.CRYSTAL_COVE -> CRYSTAL_COVE
                MapAreaId.THORNWOOD_REACH -> THORNWOOD_REACH
                MapAreaId.MOONLIT_REEF -> MOONLIT_REEF
                MapAreaId.EMBER_HEARTLANDS -> EMBER_HEARTLANDS
                MapAreaId.EVERHEART_CITADEL -> EVERHEART_CITADEL
            }
        }

        fun getThemeForArea(areaId: MapAreaId): MapAreaTheme {
            return when (areaId) {
                MapAreaId.HEART_MEADOW -> HEART_MEADOW
                MapAreaId.STONE_VALLEY -> STONE_VALLEY
                MapAreaId.BROKEN_FOREST -> BROKEN_FOREST
                MapAreaId.SHADOW_GARDEN -> SHADOW_GARDEN
                MapAreaId.HEART_KINGDOM -> HEART_KINGDOM
                MapAreaId.CRYSTAL_COVE -> CRYSTAL_COVE
                MapAreaId.THORNWOOD_REACH -> THORNWOOD_REACH
                MapAreaId.MOONLIT_REEF -> MOONLIT_REEF
                MapAreaId.EMBER_HEARTLANDS -> EMBER_HEARTLANDS
                MapAreaId.EVERHEART_CITADEL -> EVERHEART_CITADEL
            }
        }
    }
}
