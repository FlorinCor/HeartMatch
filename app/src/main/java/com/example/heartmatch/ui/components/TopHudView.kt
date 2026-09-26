package com.example.heartmatch.ui.components

import com.example.heartmatch.ui.theme.GardenPalette
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.GameState
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.Objective
import com.example.heartmatch.engine.model.ObjectiveType
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TopHudView(gameState: GameState, modifier: Modifier = Modifier) {
    val progress = (gameState.score.toFloat() / gameState.starThresholds.third.coerceAtLeast(1)).coerceIn(0f,1f)
    val animatedProgress by animateFloatAsState(progress,tween(300),label="Score progress")
    Column(modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp)
        .background(GardenPalette.Panel,RoundedCornerShape(20.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Level ${gameState.levelId}",color=GardenPalette.Ivory,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("Score ${gameState.score}",color=GardenPalette.Ivory.copy(alpha=0.7f),fontSize=12.sp)
            }
            Row(Modifier.background(if(gameState.movesRemaining<=5) GardenPalette.RoseDark else GardenPalette.Background,RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("${gameState.movesRemaining}",color=GardenPalette.Ivory,fontSize=26.sp,fontWeight=FontWeight.Bold)
                Text("Moves",color=GardenPalette.Ivory,fontSize=12.sp)
            }
        }
        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            gameState.objectives.forEach { ObjectiveIndicator(it,it.remainingCount,it.isFulfilled) }
        }
        Text("Complete objectives: ★ · ★★ ${gameState.starThresholds.second} · ★★★ ${gameState.starThresholds.third}",
            color=GardenPalette.Gold,fontSize=10.sp)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(GardenPalette.Background)) {
                Box(Modifier.fillMaxWidth(animatedProgress).height(5.dp).background(GardenPalette.Gold))
            }
            val thresholds = listOf(gameState.starThresholds.first,gameState.starThresholds.second,gameState.starThresholds.third)
            thresholds.forEach { threshold -> Text("★",color=if(gameState.score>=threshold) GardenPalette.Gold else GardenPalette.Rim,fontSize=15.sp) }
        }
    }
}

@Composable
fun ObjectiveIndicator(
    objective: Objective,
    remainingCount: Int,
    isFulfilled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = if (isFulfilled) Color(0x334CAF50) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isFulfilled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini Icon
        Box(
            modifier = Modifier
                .size(26.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            when (objective.config.type) {
                ObjectiveType.COLLECT_COLOR, ObjectiveType.COLLECT_HEARTS -> {
                    val color = objective.config.targetColor ?: HeartColor.RED
                    TileView(tile = Tile.Normal(color = color))
                }
                ObjectiveType.DESTROY_BLOCKERS, ObjectiveType.CLEAR_BLOCKER -> {
                    val blocker = objective.config.targetBlocker ?: BlockerType.STONE_HEART
                    TileView(tile = Tile.Blocker(blockerType = blocker, durability = 1))
                }
                ObjectiveType.CLEAR_DARK_HEARTS -> {
                    TileView(tile = Tile.Blocker(blockerType = BlockerType.DARK_HEART, durability = 1))
                }
                ObjectiveType.REPAIR_BROKEN -> {
                    TileView(tile = Tile.Blocker(blockerType = BlockerType.BROKEN_HEART, durability = 1))
                }
                ObjectiveType.COLLECT_SPECIAL, ObjectiveType.COLLECT_GIFT, ObjectiveType.CREATE_SPECIALS -> {
                    TileView(tile = Tile.Special(specialType = objective.config.targetSpecial ?: SpecialHeartType.GIFT_HEART))
                }
                ObjectiveType.SCORE, ObjectiveType.REACH_SCORE -> {
                    Text(text = "★", color = GardenPalette.Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                ObjectiveType.CLEAR_BOARD -> {
                    Text(text = "♥", color = GardenPalette.Rose, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                ObjectiveType.CLEAR_SPECIFIC_CELLS -> {
                    Text(text = "⛶", color = GardenPalette.Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        if (isFulfilled) {
            Text(
                text = "✓",
                color = Color(0xFF4CAF50),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "$remainingCount",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
