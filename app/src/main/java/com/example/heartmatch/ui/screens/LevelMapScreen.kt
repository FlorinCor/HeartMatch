package com.example.heartmatch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.data.LevelRecord
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.engine.model.LevelConfig
import com.example.heartmatch.ui.components.LevelNode
import com.example.heartmatch.ui.components.ObjectiveIndicator
import com.example.heartmatch.ui.model.MapAreaId
import com.example.heartmatch.ui.model.MapAreaTheme
import kotlinx.coroutines.launch
import kotlin.math.sin

sealed class MapListItem {
    data class AreaBanner(val theme: MapAreaTheme, val areaStars: Int, val isUnlocked: Boolean) : MapListItem()
    data class LevelRow(
        val levelId: Int,
        val theme: MapAreaTheme,
        val record: LevelRecord,
        val isUnlocked: Boolean,
        val isCurrent: Boolean
    ) : MapListItem()
    data class AreaGateway(val nextAreaTheme: MapAreaTheme) : MapListItem()
}

@Composable
fun LevelMapScreen(
    profile: PlayerProfile,
    previewLevelId: Int?,
    getLevelRecord: (Int) -> LevelRecord,
    getLevelConfig: (Int) -> LevelConfig,
    onLevelClick: (Int) -> Unit,
    onStartLevel: (Int) -> Unit,
    onDismissPreview: () -> Unit,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Build the list of map elements from Level 100 down to Level 1 (top of kingdom to meadow at bottom)
    val mapItems = remember(profile.highestUnlockedLevel, profile.totalStarsEarned) {
        buildMapItems(profile, getLevelRecord)
    }

    // Auto-scroll to player's current unlocked level on initial display
    LaunchedEffect(profile.highestUnlockedLevel) {
        val targetItemIndex = mapItems.indexOfFirst {
            it is MapListItem.LevelRow && it.levelId == profile.highestUnlockedLevel
        }
        if (targetItemIndex >= 0) {
            val scrollIndex = (targetItemIndex - 1).coerceAtLeast(0)
            listState.scrollToItem(scrollIndex)
        }
    }

    // Determine current visible area for quick navigator highlight
    val currentArea = remember(profile.highestUnlockedLevel) {
        MapAreaTheme.getThemeForLevel(profile.highestUnlockedLevel)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2A0845), // Heart Kingdom
                        Color(0xFF1A002C), // Shadow Garden
                        Color(0xFF311B92), // Broken Hearts Forest
                        Color(0xFF4E342E), // Stone Valley
                        Color(0xFF0F381E)  // Heart Meadow
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            MapTopBar(
                totalStars = profile.totalStarsEarned,
                currentArea = currentArea,
                onBackClick = onBackClick
            )

            // Area Quick-Jump Bar
            AreaQuickJumpBar(
                activeAreaId = MapAreaId.fromLevel(profile.highestUnlockedLevel),
                onAreaSelected = { areaId ->
                    val targetIndex = mapItems.indexOfFirst {
                        (it is MapListItem.AreaBanner && it.theme.id == areaId) ||
                        (it is MapListItem.LevelRow && it.levelId == areaId.endLevel)
                    }
                    if (targetIndex >= 0) {
                        scope.launch {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                }
            )

            // Main Map Scroll View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(
                        count = mapItems.size,
                        key = { index ->
                            when (val item = mapItems[index]) {
                                is MapListItem.AreaBanner -> "banner_${item.theme.id.name}"
                                is MapListItem.LevelRow -> "level_${item.levelId}"
                                is MapListItem.AreaGateway -> "gateway_${item.nextAreaTheme.id.name}"
                            }
                        }
                    ) { index ->
                        when (val item = mapItems[index]) {
                            is MapListItem.AreaBanner -> {
                                AreaBannerView(
                                    theme = item.theme,
                                    areaStars = item.areaStars,
                                    isUnlocked = item.isUnlocked
                                )
                            }
                            is MapListItem.AreaGateway -> {
                                AreaGatewayDivider(nextTheme = item.nextAreaTheme)
                            }
                            is MapListItem.LevelRow -> {
                                LevelRowView(
                                    item = item,
                                    onClick = {
                                        if (item.isUnlocked) {
                                            onLevelClick(item.levelId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating "Jump to Current Level" Button
                FloatingCurrentLevelButton(
                    currentLevel = profile.highestUnlockedLevel,
                    onClick = {
                        val targetItemIndex = mapItems.indexOfFirst {
                            it is MapListItem.LevelRow && it.levelId == profile.highestUnlockedLevel
                        }
                        if (targetItemIndex >= 0) {
                            scope.launch {
                                listState.animateScrollToItem((targetItemIndex - 1).coerceAtLeast(0))
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }

        // Level Preview Modal Dialog
        if (previewLevelId != null) {
            val levelConfig = getLevelConfig(previewLevelId)
            val record = getLevelRecord(previewLevelId)

            LevelPreviewModal(
                config = levelConfig,
                record = record,
                onPlayClick = { onStartLevel(previewLevelId) },
                onDismiss = onDismissPreview
            )
        }
    }
}

private fun buildMapItems(
    profile: PlayerProfile,
    getLevelRecord: (Int) -> LevelRecord
): List<MapListItem> {
    val items = mutableListOf<MapListItem>()
    val areasDescending = listOf(
        MapAreaId.HEART_KINGDOM,
        MapAreaId.SHADOW_GARDEN,
        MapAreaId.BROKEN_FOREST,
        MapAreaId.STONE_VALLEY,
        MapAreaId.HEART_MEADOW
    )

    for (areaId in areasDescending) {
        val theme = MapAreaTheme.getThemeForArea(areaId)
        val isAreaUnlocked = profile.highestUnlockedLevel >= areaId.startLevel

        // Calculate stars earned within this area
        var areaStars = 0
        for (lvl in areaId.startLevel..areaId.endLevel) {
            areaStars += getLevelRecord(lvl).stars
        }

        // Add Area Header Banner
        items.add(MapListItem.AreaBanner(theme, areaStars, isAreaUnlocked))

        // Add Levels in descending order for that area (e.g. 100 down to 81)
        for (lvl in areaId.endLevel downTo areaId.startLevel) {
            val record = getLevelRecord(lvl)
            val isUnlocked = lvl <= profile.highestUnlockedLevel
            val isCurrent = lvl == profile.highestUnlockedLevel

            items.add(
                MapListItem.LevelRow(
                    levelId = lvl,
                    theme = theme,
                    record = record,
                    isUnlocked = isUnlocked,
                    isCurrent = isCurrent
                )
            )
        }

        // Gateway divider between areas
        if (areaId != MapAreaId.HEART_MEADOW) {
            items.add(MapListItem.AreaGateway(theme))
        }
    }

    return items
}

@Composable
fun MapTopBar(
    totalStars: Int,
    currentArea: MapAreaTheme,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E0A30).copy(alpha = 0.95f), Color(0xFF10041C).copy(alpha = 0.9f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(6.dp, CircleShape)
                .background(Color(0xFF2E0854), CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "◀", color = Color.White, fontSize = 18.sp)
        }

        // Title with Current Realm Badge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MAP OF HEARTS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = currentArea.iconEmoji, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currentArea.name,
                    color = currentArea.primaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Total Stars Badge
        Row(
            modifier = Modifier
                .background(Color(0xFF2E0854), RoundedCornerShape(16.dp))
                .border(1.2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "★", color = Color(0xFFFFD700), fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$totalStars / 300",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AreaQuickJumpBar(
    activeAreaId: MapAreaId,
    onAreaSelected: (MapAreaId) -> Unit
) {
    val scrollState = rememberScrollState()
    val areas = listOf(
        MapAreaId.HEART_MEADOW,
        MapAreaId.STONE_VALLEY,
        MapAreaId.BROKEN_FOREST,
        MapAreaId.SHADOW_GARDEN,
        MapAreaId.HEART_KINGDOM
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF140524).copy(alpha = 0.85f))
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        areas.forEach { areaId ->
            val theme = MapAreaTheme.getThemeForArea(areaId)
            val isCurrentArea = areaId == activeAreaId

            Row(
                modifier = Modifier
                    .shadow(if (isCurrentArea) 6.dp else 2.dp, RoundedCornerShape(14.dp))
                    .background(
                        brush = if (isCurrentArea) {
                            Brush.horizontalGradient(theme.bannerGradientColors)
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF240B3B), Color(0xFF170626)))
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = if (isCurrentArea) 1.5.dp else 1.dp,
                        color = if (isCurrentArea) theme.primaryAccent else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onAreaSelected(areaId) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = theme.iconEmoji, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = theme.name,
                    color = if (isCurrentArea) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = if (isCurrentArea) FontWeight.Black else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AreaBannerView(
    theme: MapAreaTheme,
    areaStars: Int,
    isUnlocked: Boolean
) {
    val maxAreaStars = 60 // 20 levels * 3 stars

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        theme.bannerGradientColors[0].copy(alpha = 0.95f),
                        theme.bannerGradientColors.getOrElse(1) { theme.bannerGradientColors[0] }.copy(alpha = 0.98f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    listOf(theme.primaryAccent, theme.secondaryAccent, theme.primaryAccent)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = theme.iconEmoji, fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = theme.name.uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = theme.iconEmoji, fontSize = 26.sp)
            }

            Text(
                text = theme.subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level Range Badge
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = theme.levelRangeText,
                        color = theme.primaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Star Counter for Area
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "★", color = Color(0xFFFFD700), fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$areaStars / $maxAreaStars",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LevelRowView(
    item: MapListItem.LevelRow,
    onClick: () -> Unit
) {
    // Smooth Sine Wave Winding Offset
    val wavePhase = item.levelId * 0.85
    val xOffsetDp = (sin(wavePhase) * 75.0).toFloat().dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Connector Pathway Line
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            val centerX = size.width / 2f + xOffsetDp.toPx()
            val centerY = size.height / 2f
            
            // Subtle glowing step dots
            drawCircle(
                color = item.theme.pathColor.copy(alpha = if (item.isUnlocked) 0.35f else 0.12f),
                radius = 3.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }

        LevelNode(
            levelId = item.levelId,
            isUnlocked = item.isUnlocked,
            isCurrent = item.isCurrent,
            stars = item.record.stars,
            highScore = item.record.highScore,
            theme = item.theme,
            onClick = onClick,
            modifier = Modifier.offset(x = xOffsetDp)
        )
    }
}

@Composable
fun AreaGatewayDivider(nextTheme: MapAreaTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, nextTheme.pathColor.copy(alpha = 0.6f))
                    )
                )
        )
        Text(
            text = " ✨ ⚜️ ✨ ",
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(nextTheme.pathColor.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun FloatingCurrentLevelButton(
    currentLevel: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFFF4081), Color(0xFFFF8F00))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(2.dp, Color.White, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🎯", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Level $currentLevel",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun LevelPreviewModal(
    config: LevelConfig,
    record: LevelRecord,
    onPlayClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = MapAreaTheme.getThemeForLevel(config.id)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3F145B),
                            Color(0xFF1E0A30)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(2.5.dp, theme.primaryAccent, RoundedCornerShape(28.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Area Badge Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(text = theme.iconEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = theme.name.uppercase(),
                        color = theme.primaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Level Number & Name
                Text(
                    text = "LEVEL ${config.id}",
                    color = Color(0xFFFFD54F),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = config.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Difficulty Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (config.difficulty.name) {
                                "EASY" -> Color(0xFF4CAF50)
                                "MEDIUM", "INTERMEDIATE" -> Color(0xFFFFA000)
                                "HARD" -> Color(0xFFFF5722)
                                else -> Color(0xFFD50000)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = config.difficulty.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Target Objectives Header
                Text(
                    text = "TARGET OBJECTIVES",
                    color = Color(0xFFFF80AB),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    config.objectives.forEach { objConfig ->
                        ObjectiveIndicator(objective = com.example.heartmatch.engine.model.Objective(objConfig))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Moves & High Score Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Moves Limit", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            text = "${config.moveLimit ?: 30}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Earned Stars Display
                    Row {
                        for (i in 1..3) {
                            val earned = i <= record.stars
                            Text(
                                text = "★",
                                color = if (earned) Color(0xFFFFD700) else Color(0x33FFFFFF),
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "High Score", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            text = "${record.highScore}",
                            color = Color(0xFFFFD700),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Play Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF4081), Color(0xFFFF8F00))
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .border(2.dp, Color.White, RoundedCornerShape(22.dp))
                        .clickable { onPlayClick() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "START PUZZLE ▶",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
