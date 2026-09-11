package com.example.heartmatch.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.ui.model.MapAreaTheme

enum class LevelNodeStatus {
    LOCKED,
    UNLOCKED,
    CURRENT,
    COMPLETED
}

@Composable
fun LevelNode(
    levelId: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    theme: MapAreaTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highScore: Int = 0,
    size: Dp = 72.dp
) {
    val status = when {
        isCurrent -> LevelNodeStatus.CURRENT
        !isUnlocked -> LevelNodeStatus.LOCKED
        stars > 0 -> LevelNodeStatus.COMPLETED
        else -> LevelNodeStatus.UNLOCKED
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CurrentNodeAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating "CURRENT" or Crown indicator for active level
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .offset(y = 4.dp)
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFFF6D00))
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(1.dp, Color.White, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PLAY ▶",
                    color = Color(0xFF2E0854),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Box(
            modifier = Modifier.size(size + 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing Glowing Halo for Current Level
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(size + 14.dp)
                        .scale(pulseScale)
                        .alpha(glowAlpha)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.8f),
                                    Color(0xFFFF4081).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Outer Circular Node
            Box(
                modifier = Modifier
                    .size(size)
                    .shadow(
                        elevation = when (status) {
                            LevelNodeStatus.CURRENT -> 16.dp
                            LevelNodeStatus.COMPLETED -> 10.dp
                            LevelNodeStatus.UNLOCKED -> 8.dp
                            LevelNodeStatus.LOCKED -> 4.dp
                        },
                        shape = CircleShape
                    )
                    .background(
                        brush = when (status) {
                            LevelNodeStatus.CURRENT -> Brush.radialGradient(
                                listOf(Color(0xFFFF80AB), Color(0xFFFF1744), Color(0xFFC2185B))
                            )
                            LevelNodeStatus.COMPLETED -> Brush.radialGradient(
                                theme.completedNodeGradientColors
                            )
                            LevelNodeStatus.UNLOCKED -> Brush.radialGradient(
                                theme.nodeGradientColors
                            )
                            LevelNodeStatus.LOCKED -> Brush.radialGradient(
                                listOf(Color(0xFF424242), Color(0xFF262626), Color(0xFF1E1E1E))
                            )
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = when (status) {
                            LevelNodeStatus.CURRENT -> 3.5.dp
                            LevelNodeStatus.COMPLETED -> 2.5.dp
                            LevelNodeStatus.UNLOCKED -> 2.dp
                            LevelNodeStatus.LOCKED -> 1.5.dp
                        },
                        brush = when (status) {
                            LevelNodeStatus.CURRENT -> Brush.sweepGradient(
                                listOf(Color(0xFFFFD700), Color.White, Color(0xFFFF4081), Color(0xFFFFD700))
                            )
                            LevelNodeStatus.COMPLETED -> Brush.verticalGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA000), Color.White)
                            )
                            LevelNodeStatus.UNLOCKED -> Brush.verticalGradient(
                                listOf(theme.primaryAccent, Color.White.copy(alpha = 0.7f))
                            )
                            LevelNodeStatus.LOCKED -> Brush.verticalGradient(
                                listOf(Color(0xFF757575), Color(0xFF424242))
                            )
                        },
                        shape = CircleShape
                    )
                    .clickable(enabled = isUnlocked) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                // Inner Gloss Bevel (Top-Left Highlight)
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isUnlocked) 0.35f else 0.12f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f)
                                )
                            )
                        )
                )

                // Level Content: Number or Lock Icon
                if (isUnlocked) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$levelId",
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.95f),
                            fontSize = if (levelId >= 100) 19.sp else 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                } else {
                    Text(
                        text = "🔒",
                        fontSize = 22.sp,
                        modifier = Modifier.alpha(0.65f)
                    )
                }
            }
        }

        // Star Rating Cluster for Completed Levels (1, 2, or 3 Stars)
        if (isUnlocked) {
            Row(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF2E0854).copy(alpha = 0.9f), Color(0xFF150424).copy(alpha = 0.95f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, if (stars > 0) Color(0xFFFFD700).copy(alpha = 0.8f) else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                for (starIndex in 1..3) {
                    val isEarned = starIndex <= stars
                    Text(
                        text = "★",
                        color = if (isEarned) Color(0xFFFFD700) else Color(0x33FFFFFF),
                        fontSize = if (starIndex == 2) 13.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 0.5.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
