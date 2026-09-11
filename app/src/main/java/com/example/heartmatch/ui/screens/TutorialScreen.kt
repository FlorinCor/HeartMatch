package com.example.heartmatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.FireDirection
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile
import com.example.heartmatch.ui.components.TileView

@Composable
fun TutorialScreen(
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Basics", "Specials", "Blockers", "Boosters")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3F145B),
                        Color(0xFF6A1B9A),
                        Color(0xFF1A002C)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Text(
                    text = "HOW TO PLAY",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Box(modifier = Modifier.size(42.dp))
            }

            // Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E0854), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) Color(0xFFFF4081) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content per tab
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> BasicsTab()
                    1 -> SpecialsTab()
                    2 -> BlockersTab()
                    3 -> BoostersTab()
                }
            }
        }
    }
}

@Composable
fun BasicsTab() {
    TutorialCard(
        title = "Match 3 of the Same Color",
        desc = "Swipe or tap adjacent hearts horizontally or vertically to line up 3 or more matching colored hearts and clear them from the board."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(44.dp)) { TileView(tile = Tile.Normal(color = HeartColor.RED)) }
            Box(modifier = Modifier.size(44.dp)) { TileView(tile = Tile.Normal(color = HeartColor.RED)) }
            Box(modifier = Modifier.size(44.dp)) { TileView(tile = Tile.Normal(color = HeartColor.RED)) }
        }
    }

    TutorialCard(
        title = "Gravity & Cascades",
        desc = "When matched hearts clear, remaining hearts fall with gravity and new colorful hearts drop from above, triggering cascading combos!"
    )

    TutorialCard(
        title = "Level Objectives & Star Ratings",
        desc = "Fulfill all level objectives before running out of moves to earn up to 3 stars and unlock the next romantic puzzle level."
    )
}

@Composable
fun SpecialsTab() {
    TutorialCard(
        title = "Fire Heart (Match 4)",
        desc = "Created by matching 4 hearts in a line. When matched or triggered, blasts a fiery laser clearing the entire row or column!"
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            TileView(tile = Tile.Special(specialType = SpecialHeartType.FIRE_HEART, fireDirection = FireDirection.ROW, baseColor = HeartColor.RED))
        }
    }

    TutorialCard(
        title = "Bomb Heart (Match 5 T/L)",
        desc = "Created by T-shape or L-shape 5-heart matches. Explodes a 3x3 blast radius clearing all tiles and damaging blockers!"
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            TileView(tile = Tile.Special(specialType = SpecialHeartType.BOMB_HEART, baseColor = HeartColor.PURPLE))
        }
    }

    TutorialCard(
        title = "Rainbow Heart (Match 5 Line)",
        desc = "Created by straight 5-in-a-row match. Swap with any colored heart to remove ALL hearts of that color from the board!"
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            TileView(tile = Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART))
        }
    }

    TutorialCard(
        title = "Gift Heart",
        desc = "Special festive reward tile that gives bonus points and fulfills Gift Collection objectives."
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            TileView(tile = Tile.Special(specialType = SpecialHeartType.GIFT_HEART))
        }
    }
}

@Composable
fun BlockersTab() {
    TutorialCard(
        title = "Stone Heart",
        desc = "Solid slate barrier that cannot be moved. Takes damage and shatters from adjacent matches or explosions."
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            TileView(tile = Tile.Blocker(blockerType = BlockerType.STONE_HEART, durability = 1))
        }
    }

    TutorialCard(
        title = "Ice Heart",
        desc = "Translucent frozen crystal casing. Cracked by matching the enclosed heart color or making adjacent matches."
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            TileView(tile = Tile.Blocker(blockerType = BlockerType.ICE_HEART, payloadTile = Tile.Normal(color = HeartColor.BLUE), durability = 1))
        }
    }

    TutorialCard(
        title = "Wooden Heart",
        desc = "Multi-layer wooden crate. Each adjacent match removes one wooden layer until destroyed."
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            TileView(tile = Tile.Blocker(blockerType = BlockerType.WOODEN_HEART, durability = 2))
        }
    }

    TutorialCard(
        title = "Chained Heart & Broken Heart",
        desc = "Chained hearts cannot move until unlocked. Broken hearts require 2 adjacent matches to repair into normal hearts."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(44.dp)) { TileView(tile = Tile.Blocker(blockerType = BlockerType.CHAINED_HEART, payloadTile = Tile.Normal(color = HeartColor.PINK), durability = 1)) }
            Box(modifier = Modifier.size(44.dp)) { TileView(tile = Tile.Blocker(blockerType = BlockerType.BROKEN_HEART, durability = 1)) }
        }
    }

    TutorialCard(
        title = "Dark Heart",
        desc = "Corrupted void heart! Spreads dark infection to adjacent cells if left undamaged across turns."
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            TileView(tile = Tile.Blocker(blockerType = BlockerType.DARK_HEART, durability = 1))
        }
    }
}

@Composable
fun BoostersTab() {
    TutorialCard(
        title = "🔨 Heart Hammer",
        desc = "Tap any cell on the board to instantly smash and destroy that tile or blocker without consuming a move."
    )

    TutorialCard(
        title = "💣 Bomb Booster",
        desc = "Drop a pre-charged Bomb Heart onto any target cell on the board."
    )

    TutorialCard(
        title = "🌈 Rainbow Booster",
        desc = "Drop a dazzling Rainbow Heart anywhere on the board for instant mass color clearing."
    )

    TutorialCard(
        title = "🔀 Board Reshuffle",
        desc = "Instantly shuffle and rearrange all hearts on the active board."
    )

    TutorialCard(
        title = "+5 Extra Moves",
        desc = "Add 5 additional turns to your remaining moves count when you're close to completing the puzzle."
    )
}

@Composable
fun TutorialCard(
    title: String,
    desc: String,
    previewContent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .background(Color(0xFF2E0854).copy(alpha = 0.95f), RoundedCornerShape(20.dp))
            .border(1.5.dp, Color(0xFFFF80AB).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFFFD54F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            if (previewContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    previewContent()
                }
            }
        }
    }
}
