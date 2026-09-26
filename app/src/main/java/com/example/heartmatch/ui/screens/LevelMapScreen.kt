package com.example.heartmatch.ui.screens

import com.example.heartmatch.ui.theme.GardenPalette
import androidx.compose.material3.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import com.example.heartmatch.ui.components.GardenIcon
import com.example.heartmatch.ui.components.GardenIconButton
import com.example.heartmatch.ui.components.GardenAtmosphere
import com.example.heartmatch.ui.theme.LocalReducedMotion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.heartmatch.R
import com.example.heartmatch.data.LevelRecord
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.engine.model.LevelConfig
import com.example.heartmatch.ui.components.LevelNode
import com.example.heartmatch.ui.components.ObjectiveIndicator
import com.example.heartmatch.ui.model.MapAreaId
import com.example.heartmatch.ui.model.MapAreaTheme
import kotlinx.coroutines.launch

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
    onSettingsClick: () -> Unit,
    onDailyRewardClick: () -> Unit,
    onShopClick: () -> Unit = {},
    totalAvailableStars: Int
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val reducedMotion = LocalReducedMotion.current
    val mapItems = remember(profile.highestUnlockedLevel, profile.totalStarsEarned) { buildMapItems(profile, getLevelRecord) }
    val currentIndex = mapItems.indexOfFirst { it is MapListItem.LevelRow && it.levelId == profile.highestUnlockedLevel }.coerceAtLeast(0)
    LaunchedEffect(profile.highestUnlockedLevel) { listState.scrollToItem((currentIndex - 1).coerceAtLeast(0)) }
    val visibleArea by remember(mapItems) {
        derivedStateOf {
            when(val item = mapItems.getOrNull(listState.firstVisibleItemIndex)) {
                is MapListItem.LevelRow -> item.theme
                is MapListItem.AreaBanner -> item.theme
                is MapListItem.AreaGateway -> item.nextAreaTheme
                else -> MapAreaTheme.getThemeForLevel(profile.highestUnlockedLevel)
            }
        }
    }
    fun jump(index: Int) {
        scope.launch {
            if (reducedMotion) listState.scrollToItem(index.coerceAtLeast(0))
            else listState.animateScrollToItem(index.coerceAtLeast(0))
        }
    }
    Box(Modifier.fillMaxSize().background(GardenPalette.Background)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start=16.dp, end=8.dp, top=4.dp), verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if(profile.gardenDecoration == "ROSE_GARDEN") "❀ Heart Match ❀" else if(profile.gardenDecoration == "MOON_GARDEN") "☾ Heart Match ✦" else "Heart Match", color=GardenPalette.Ivory, fontSize=23.sp, fontWeight=FontWeight.Bold)
                    Text("Your garden journey", color=GardenPalette.Gold, fontSize=12.sp)
                }
                GardenIconButton("gift", "Daily gift", onDailyRewardClick)
                GardenIconButton("settings", "Settings and help", onSettingsClick)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=8.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                GardenIcon("coin", Modifier.size(17.dp), GardenPalette.Gold)
                Text("${profile.coins} · Shop", modifier=Modifier.clickable(onClick=onShopClick).padding(8.dp), color=GardenPalette.Ivory, fontSize=13.sp)
                Spacer(Modifier.width(8.dp))
                Text("★ ${profile.totalStarsEarned} / $totalAvailableStars", color=GardenPalette.Gold, fontSize=13.sp)
            }
            AreaQuickJumpBar(visibleArea.id) { area ->
                jump(mapItems.indexOfFirst { it is MapListItem.AreaBanner && it.theme.id == area })
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val gardenPainter = painterResource(R.drawable.rose_garden_backdrop)
                val imageAspectRatio = gardenPainter.intrinsicSize.width / gardenPainter.intrinsicSize.height
                Image(gardenPainter, null, contentScale=ContentScale.Crop, modifier=Modifier.fillMaxSize())
                // Realm lighting keeps the original artwork crisp and the UI visually connected.
                Box(Modifier.fillMaxSize().background(visibleArea.backgroundColors.first().copy(alpha=0.14f)))
                GardenAtmosphere()
                LazyColumn(state=listState, modifier=Modifier.fillMaxSize(), contentPadding=PaddingValues(top=16.dp, bottom=28.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                    items(mapItems.size, key={ index -> when(val item=mapItems[index]) {
                        is MapListItem.AreaBanner -> "banner_${item.theme.id}"
                        is MapListItem.LevelRow -> "level_${item.levelId}"
                        is MapListItem.AreaGateway -> "gateway_${item.nextAreaTheme.id}"
                    } }) { index ->
                        when(val item=mapItems[index]) {
                            is MapListItem.AreaBanner -> AreaBannerView(item.theme,item.areaStars,item.isUnlocked)
                            is MapListItem.AreaGateway -> AreaGatewayDivider(item.nextAreaTheme)
                            is MapListItem.LevelRow -> LevelRowView(item,listState,{ if(item.isUnlocked) onLevelClick(item.levelId) },imageAspectRatio)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                GardenIconButton("locate", "Find my level", { jump(currentIndex - 1) })
                Button(onClick={ onStartLevel(profile.highestUnlockedLevel) },modifier=Modifier.weight(1f).heightIn(min=52.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=GardenPalette.Rose,contentColor=GardenPalette.Ivory)) {
                    Text("Play level ${profile.highestUnlockedLevel}",fontSize=17.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
        if(previewLevelId != null) {
            LevelPreviewModal(getLevelConfig(previewLevelId),getLevelRecord(previewLevelId),{ onStartLevel(previewLevelId) },onDismissPreview)
        }
    }
}

private fun buildMapItems(
    profile: PlayerProfile,
    getLevelRecord: (Int) -> LevelRecord
): List<MapListItem> {
    val items = mutableListOf<MapListItem>()
    val areasDescending = listOf(
        MapAreaId.EVERHEART_CITADEL,
        MapAreaId.EMBER_HEARTLANDS,
        MapAreaId.MOONLIT_REEF,
        MapAreaId.THORNWOOD_REACH,
        MapAreaId.CRYSTAL_COVE,
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

        // Add each chapter's levels in descending order.
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
            val nextTheme = MapAreaTheme.getThemeForArea(areasDescending[areasDescending.indexOf(areaId) + 1])
            items.add(MapListItem.AreaGateway(nextTheme))
        }
    }

    return items
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
        MapAreaId.HEART_KINGDOM,
        MapAreaId.CRYSTAL_COVE,
        MapAreaId.THORNWOOD_REACH,
        MapAreaId.MOONLIT_REEF,
        MapAreaId.EMBER_HEARTLANDS,
        MapAreaId.EVERHEART_CITADEL
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xDD1C4A35), Color(0xD5082019))))
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
                            Brush.verticalGradient(listOf(Color(0xFF82C98D), Color(0xFF286544)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFF315641), Color(0xFF132D22)))
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isCurrentArea) 1.5.dp else 1.dp,
                        color = if (isCurrentArea) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onAreaSelected(areaId) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = theme.name,
                    color = if (isCurrentArea) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = if (isCurrentArea) FontWeight.Bold else FontWeight.Medium
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
    val maxAreaStars = (theme.id.endLevel - theme.id.startLevel + 1) * 3

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(listOf(Color(0xFF2D6544), Color(0xFF123525))),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.9f), theme.primaryAccent, theme.secondaryAccent.copy(alpha = 0.85f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent))
                )
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = theme.name.uppercase(),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isUnlocked) "OPEN" else "LOCKED",
                        color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Text(
                text = theme.subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level Range Badge
                Box(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.24f))), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
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
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.24f))), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "★", color = GardenPalette.Gold, fontSize = 13.sp)
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
    listState: LazyListState,
    onClick: () -> Unit,
    imageAspectRatio: Float
) {
    val density = LocalDensity.current
    val rowYFraction by remember(item.levelId, listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleRow = layoutInfo.visibleItemsInfo.firstOrNull {
                it.key == "level_${item.levelId}"
            }
            val viewportHeight = layoutInfo.viewportSize.height
            if (visibleRow == null || viewportHeight <= 0) {
                0.5f
            } else {
                val centerY = visibleRow.offset + visibleRow.size / 2f
                (centerY / viewportHeight).coerceIn(0f, 1f)
            }
        }
    }
    val fadeProgress = (rowYFraction * 2f).coerceIn(0f, 1f)
    val easedFadeProgress = fadeProgress * fadeProgress * (3f - 2f * fadeProgress)
    val sizeProgress = rowYFraction * rowYFraction * (3f - 2f * rowYFraction)
    val bubbleAlpha = 0.85f + 0.15f * easedFadeProgress
    val bubbleScale = 0.90f + 0.10f * sizeProgress
    val viewportWidth = listState.layoutInfo.viewportSize.width.toFloat()
    val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
    // Match ContentScale.Crop's centered transform so nodes stay on the painted path.
    val renderedWidth = maxOf(viewportWidth, viewportHeight * imageAspectRatio)
    val renderedHeight = renderedWidth / imageAspectRatio
    val imageY = if (renderedHeight > 0f) (rowYFraction * viewportHeight + (renderedHeight - viewportHeight) / 2f) / renderedHeight else rowYFraction
    val pathX = gardenPathCenterX(imageY) * renderedWidth - (renderedWidth - viewportWidth) / 2f
    val xOffsetDp = with(density) { (pathX - viewportWidth / 2f).toDp() }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        contentAlignment = Alignment.Center
    ) {
        LevelNode(
            levelId = item.levelId,
            isUnlocked = item.isUnlocked,
            isCurrent = item.isCurrent,
            stars = item.record.stars,
            highScore = item.record.highScore,
            theme = item.theme,
            onClick = onClick,
            modifier = Modifier
                .offset(x = xOffsetDp)
                .graphicsLayer {
                    alpha = bubbleAlpha
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                }
        )
    }
}

private val gardenPathProfile = listOf(
    0.700f, 0.680f, 0.642f, 0.577f, 0.461f,
    0.542f, 0.626f, 0.605f, 0.504f, 0.445f,
    0.521f, 0.568f, 0.613f, 0.555f, 0.440f,
    0.418f, 0.436f, 0.479f, 0.521f, 0.476f,
    0.468f
)

private fun gardenPathCenterX(yFraction: Float): Float {
    val scaled = yFraction.coerceIn(0f, 1f) * (gardenPathProfile.size - 1)
    val segment = scaled.toInt().coerceAtMost(gardenPathProfile.lastIndex - 1)
    val t = scaled - segment
    val p0 = gardenPathProfile[(segment - 1).coerceAtLeast(0)]
    val p1 = gardenPathProfile[segment]
    val p2 = gardenPathProfile[segment + 1]
    val p3 = gardenPathProfile[(segment + 2).coerceAtMost(gardenPathProfile.lastIndex)]
    val t2 = t * t
    val t3 = t2 * t
    return (
        0.5f * (
            2f * p1 +
                (-p0 + p2) * t +
                (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                (-p0 + 3f * p1 - 3f * p2 + p3) * t3
            )
        ).coerceIn(0.08f, 0.92f)
}

@Composable
fun AreaGatewayDivider(nextTheme: MapAreaTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 28.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xDD2F6443), Color(0xE90D2A20))))
            .border(1.dp, nextTheme.pathColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
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
        Text(text = "✦", color = nextTheme.primaryAccent, fontSize = 15.sp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(text = "NEXT REALM", color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {

                Spacer(modifier = Modifier.width(5.dp))
                Text(text = nextTheme.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(text = "✦", color = nextTheme.primaryAccent, fontSize = 15.sp)
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
                .shadow(6.dp, RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GardenPalette.Panel,
                            GardenPalette.Background
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(1.dp, theme.primaryAccent, RoundedCornerShape(28.dp))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close", color = GardenPalette.Ivory) }
                // Area Badge Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = theme.name.uppercase(),
                        color = theme.primaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Level Number & Name
                Text(
                    text = "LEVEL ${config.id}",
                    color = GardenPalette.Gold,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
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
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(com.example.heartmatch.ui.screens.levelLearningTip(config.id), color=GardenPalette.Gold, fontSize=13.sp)
                Spacer(modifier=Modifier.height(12.dp))
                // Target Objectives Header
                Text(
                    text = "TARGET OBJECTIVES",
                    color = GardenPalette.RoseLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    config.objectives.forEach { objConfig ->
                        val objective = com.example.heartmatch.engine.model.Objective(objConfig)
                        ObjectiveIndicator(
                            objective = objective,
                            remainingCount = objective.remainingCount,
                            isFulfilled = objective.isFulfilled
                        )
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
                                color = if (earned) GardenPalette.Gold else Color(0x33FFFFFF),
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "High Score", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            text = "${record.highScore}",
                            color = GardenPalette.Gold,
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
                        .shadow(6.dp, RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(GardenPalette.Rose, GardenPalette.RoseDark)
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .border(1.dp, Color.White, RoundedCornerShape(22.dp))
                        .clickable { onPlayClick() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Play level ${config.id}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
