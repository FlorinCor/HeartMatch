package com.example.heartmatch.ui.screens

import com.example.heartmatch.ui.theme.GardenPalette
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
import com.example.heartmatch.ui.components.GardenBackdrop
import com.example.heartmatch.ui.components.GardenIcon
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
                        GardenPalette.Panel,
                        GardenPalette.PanelLight,
                        GardenPalette.Background
                    )
                )
            )
            .padding(16.dp)
    ) {
        GardenBackdrop(dim = 0.72f)
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
                        .background(GardenPalette.Panel, CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    GardenIcon("back")
                }

                Text(
                    text = "HOW TO PLAY",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(modifier = Modifier.size(42.dp))
            }

            // Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GardenPalette.Panel, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) GardenPalette.Rose else Color.Transparent,
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
        desc = "Complete every objective to earn at least one star. Unused original moves add 100 points each before stars are awarded; extra moves cannot inflate this bonus. First clears and improved stars earn more coins than replays."
    )
}

@Composable
fun SpecialsTab() {
    TutorialCard(title = "Special combinations", desc = "Fire + Fire clears one row and column. Fire + Bomb clears three rows and columns. Bomb + Bomb clears a 5×5 area.")
    TutorialCard(title = "Light and Angel", desc = "Light clears 3×3 and removes an extra blocker layer inside that area. Angel targets up to five blockers, prioritizing unfinished objectives and dark hearts. Tap specials to preview their current targets.")
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
        desc = "Clears a cross plus two objective-priority targets. Tap to preview the exact targets before activating. Gives 1,000 bonus points."
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
        desc = "Layer dots show hits remaining. Wood loses two layers if a match touches it on two sides; stone loses one per wave."
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            TileView(tile = Tile.Blocker(blockerType = BlockerType.WOODEN_HEART, durability = 2))
        }
    }

    TutorialCard(
        title = "Chained Heart & Broken Heart",
        desc = "Chains anchor a heart until unlocked. Broken hearts move but cannot match; repair every layer to count one repair. Stitched hearts also heal adjacent broken or stitched hearts when repaired."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(44.dp)) { TileView(tile = Tile.Blocker(blockerType = BlockerType.CHAINED_HEART, payloadTile = Tile.Normal(color = HeartColor.PINK), durability = 1)) }
            Box(modifier = Modifier.size(44.dp)) { TileView(tile = Tile.Blocker(blockerType = BlockerType.BROKEN_HEART, durability = 1)) }
        }
    }

    TutorialCard(
        title = "Dark Heart",
        desc = "Spreads after paid swaps if no dark heart was hit. Free boosters never advance its spread timer."
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
        desc = "Tap to preview, then tap the same cell to spend one hammer. Removes one blocker layer, clears a normal heart, or activates a special. Cascades resolve without spending a move."
    )

    TutorialCard(
        title = "💣 Bomb Booster",
        desc = "Replace a normal heart with a bomb of the same color. Match it or combine it with another special to activate. Cannot overwrite blockers."
    )

    TutorialCard(
        title = "🌈 Rainbow Booster",
        desc = "Replace a normal heart with a rainbow. Swap it with a colored heart to activate. Preview uses the placed heart color; actual targets use the swap partner."
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
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .background(GardenPalette.Panel.copy(alpha = 0.95f), RoundedCornerShape(20.dp))
            .border(1.5.dp, GardenPalette.RoseLight.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = GardenPalette.Gold,
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
