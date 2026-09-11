package com.example.heartmatch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.data.PlayerProfile

@Composable
fun BoostersView(
    profile: PlayerProfile,
    activeBooster: String?,
    onBoosterClick: (String) -> Unit,
    onCancelBooster: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Booster Instructions Banner
        AnimatedVisibility(
            visible = activeBooster != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color(0xFFFFD54F), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val instructionText = when (activeBooster) {
                    "HAMMER" -> "🔨 Tap any tile to smash it!"
                    "BOMB" -> "💣 Tap any cell to drop a Bomb Heart!"
                    "RAINBOW" -> "🌈 Tap any cell to drop a Rainbow Heart!"
                    else -> "Select a target cell"
                }
                Text(
                    text = instructionText,
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cancel ✕",
                    color = Color(0xFFD50000),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { onCancelBooster() }
                )
            }
        }

        // Bottom Action Bar: Pause + Boosters
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, CircleShape)
                    .background(
                        brush = Brush.radialGradient(listOf(Color(0xFF7B1FA2), Color(0xFF4A148C))),
                        shape = CircleShape
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .clickable { onPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏸",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            // Booster 1: Hammer
            BoosterButton(
                icon = "🔨",
                name = "Hammer",
                count = profile.hammerCount,
                isActive = activeBooster == "HAMMER",
                onClick = { onBoosterClick("HAMMER") }
            )

            // Booster 2: Bomb
            BoosterButton(
                icon = "💣",
                name = "Bomb",
                count = profile.bombBoosterCount,
                isActive = activeBooster == "BOMB",
                onClick = { onBoosterClick("BOMB") }
            )

            // Booster 3: Rainbow
            BoosterButton(
                icon = "🌈",
                name = "Rainbow",
                count = profile.rainbowBoosterCount,
                isActive = activeBooster == "RAINBOW",
                onClick = { onBoosterClick("RAINBOW") }
            )

            // Booster 4: Shuffle
            BoosterButton(
                icon = "🔀",
                name = "Shuffle",
                count = profile.shuffleCount,
                isActive = false,
                onClick = { onBoosterClick("SHUFFLE") }
            )

            // Booster 5: Extra Moves (+5)
            BoosterButton(
                icon = "+5",
                name = "Moves",
                count = profile.extraMovesCount,
                isActive = false,
                onClick = { onBoosterClick("EXTRA_MOVES") }
            )
        }
    }
}

@Composable
fun BoosterButton(
    icon: String,
    name: String,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(if (isActive) 10.dp else 4.dp, CircleShape)
                .background(
                    brush = if (isActive) Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00)))
                    else Brush.radialGradient(listOf(Color(0xFF880E4F), Color(0xFF4A148C))),
                    shape = CircleShape
                )
                .border(
                    width = if (isActive) 2.5.dp else 1.5.dp,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = if (isActive) Color.Black else Color.White,
                fontSize = if (icon.startsWith("+")) 15.sp else 18.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Count Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(20.dp)
                .shadow(4.dp, CircleShape)
                .background(Color(0xFFE91E63), CircleShape)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
