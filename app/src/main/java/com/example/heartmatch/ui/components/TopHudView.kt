package com.example.heartmatch.ui.components

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

@Composable
fun TopHudView(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    val levelId = gameState.levelId
    val movesRemaining = gameState.movesRemaining
    val score = gameState.score
    val (star1, star2, star3) = gameState.starThresholds
    val maxStarScore = star3.toFloat().coerceAtLeast(1f)
    val scoreProgress = (score.toFloat() / maxStarScore).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = scoreProgress, animationSpec = tween(400), label = "ScoreProgress")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3F145B).copy(alpha = 0.95f),
                        Color(0xFF1E0A30).copy(alpha = 0.98f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF80AB).copy(alpha = 0.5f), Color(0xFFC2185B).copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Level Title, Moves Left, Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level Badge
            Column {
                Text(
                    text = "LEVEL $levelId",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Score: $score",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Moves Badge (Circular Glowing)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (movesRemaining <= 5) listOf(Color(0xFFFF5252), Color(0xFFD50000))
                            else listOf(Color(0xFFFF4081), Color(0xFFC2185B))
                        ),
                        shape = CircleShape
                    )
                    .border(2.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$movesRemaining",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "MOVES",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Earned Stars Display
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..3) {
                    val isEarned = score >= when (i) {
                        1 -> star1
                        2 -> star2
                        else -> star3
                    }
                    val starColor by animateColorAsState(
                        targetValue = if (isEarned) Color(0xFFFFD700) else Color(0xFF424242),
                        animationSpec = tween(300),
                        label = "StarColor$i"
                    )
                    Text(
                        text = "★",
                        color = starColor,
                        fontSize = if (isEarned) 24.sp else 20.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Star Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF212121))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(10.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF4081), Color(0xFFFFD700))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Objectives Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            gameState.objectives.forEach { objective ->
                ObjectiveIndicator(objective = objective)
            }
        }
    }
}

@Composable
fun ObjectiveIndicator(
    objective: Objective,
    modifier: Modifier = Modifier
) {
    val remaining = objective.remainingCount
    val isFulfilled = objective.isFulfilled

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
                    Text(text = "★", color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                ObjectiveType.CLEAR_BOARD -> {
                    Text(text = "♥", color = Color(0xFFFF4081), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                ObjectiveType.CLEAR_SPECIFIC_CELLS -> {
                    Text(text = "⛶", color = Color(0xFF00E5FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        if (isFulfilled) {
            Text(
                text = "✓",
                color = Color(0xFF4CAF50),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        } else {
            Text(
                text = "$remaining",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
