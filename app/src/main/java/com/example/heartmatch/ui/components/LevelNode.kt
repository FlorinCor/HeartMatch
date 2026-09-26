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
import com.example.heartmatch.ui.theme.LocalReducedMotion
import androidx.compose.ui.semantics.*
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

    var pulseScale = 1f
    var glowAlpha = 0.35f
    if (isCurrent && !LocalReducedMotion.current) {
        val transition = rememberInfiniteTransition(label = "Current level")
        val pulse by transition.animateFloat(1f, 1.035f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label="Level pulse")
        val glow by transition.animateFloat(0.25f, 0.45f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label="Level glow")
        pulseScale = pulse
        glowAlpha = glow
    }

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Level $levelId, ${if (!isUnlocked) "locked" else if (isCurrent) "current level" else "$stars stars"}"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating "CURRENT" or Crown indicator for active level
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .offset(y = 4.dp)
                    .shadow(10.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFF2C3B4), Color(0xFFD99483), Color(0xFFB8676B))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 11.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CURRENT",
                    color = Color(0xFF35443A),
                    fontSize = 11.sp,
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
                                    Color(0xFFF0B8B4).copy(alpha = 0.88f),
                                    Color(0xFFD77D82).copy(alpha = 0.38f),
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
                            LevelNodeStatus.CURRENT -> 6.dp
                            LevelNodeStatus.COMPLETED -> 4.dp
                            LevelNodeStatus.UNLOCKED -> 4.dp
                            LevelNodeStatus.LOCKED -> 4.dp
                        },
                        shape = CircleShape
                    )
                    .background(
                        brush = when (status) {
                            LevelNodeStatus.CURRENT -> Brush.radialGradient(
                                listOf(Color(0xFFF1D8CB), Color(0xFFD08C80), Color(0xFF965760))
                            )
                            LevelNodeStatus.COMPLETED -> Brush.radialGradient(
                                listOf(Color(0xFFD8E0BC), Color(0xFF83976C), Color(0xFF4A6348))
                            )
                            LevelNodeStatus.UNLOCKED -> Brush.radialGradient(
                                listOf(Color(0xFFF0E2C8), Color(0xFFD2B88F), Color(0xFF94775A))
                            )
                            LevelNodeStatus.LOCKED -> Brush.radialGradient(
                                listOf(Color(0xFF929A82), Color(0xFF5B6C53), Color(0xFF354A3C))
                            )
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = when (status) {
                            LevelNodeStatus.CURRENT -> 1.5.dp
                            LevelNodeStatus.COMPLETED -> 1.5.dp
                            LevelNodeStatus.UNLOCKED -> 1.dp
                            LevelNodeStatus.LOCKED -> 1.5.dp
                        },
                        brush = when (status) {
                            LevelNodeStatus.CURRENT -> Brush.sweepGradient(
                                listOf(Color(0xFFF8EAD8), Color.White, Color(0xFFD89A8E), Color(0xFFF8EAD8))
                            )
                            LevelNodeStatus.COMPLETED -> Brush.verticalGradient(
                                listOf(Color(0xFFE4E8CC), Color(0xFFA8B890), Color(0xFFF9F5E8))
                            )
                            LevelNodeStatus.UNLOCKED -> Brush.verticalGradient(
                                listOf(Color(0xFFF5E9D6), Color.White.copy(alpha = 0.62f))
                            )
                            LevelNodeStatus.LOCKED -> Brush.verticalGradient(
                                listOf(Color(0xFFB4B89D), Color(0xFF64765A))
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
                                    Color.White.copy(alpha = if (isUnlocked) 0.28f else 0.1f),
                                    Color.Transparent,
                                    Color(0xFF26372C).copy(alpha = 0.18f)
                                )
                            )
                        )
                )

                // A focused specular highlight makes each level gem read as glass.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = size * 0.08f)
                        .size(width = size * 0.62f, height = size * 0.24f)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isUnlocked) 0.18f else 0.08f),
                                    Color.White.copy(alpha = 0.03f)
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
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                } else {
                    GardenIcon("lock", tint = com.example.heartmatch.ui.theme.GardenPalette.Ivory)
                }
            }
        }

        // Star Rating Cluster for Completed Levels (1, 2, or 3 Stars)
        if (isUnlocked && stars > 0) {
            Row(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF52664A).copy(alpha = 0.97f), Color(0xFF263D31).copy(alpha = 0.98f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, if (stars > 0) Color(0xFFE8CE91).copy(alpha = 0.84f) else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                for (starIndex in 1..3) {
                    val isEarned = starIndex <= stars
                    Text(
                        text = "★",
                        color = if (isEarned) Color(0xFFE8CE91) else Color(0x33FFFFFF),
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
