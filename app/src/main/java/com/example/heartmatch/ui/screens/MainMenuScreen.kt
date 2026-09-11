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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.Tile
import com.example.heartmatch.ui.components.TileView

@Composable
fun MainMenuScreen(
    profile: PlayerProfile,
    onPlayClick: () -> Unit,
    onLevelMapClick: () -> Unit,
    onDailyRewardClick: () -> Unit,
    onTutorialClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Coins & Stars Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coins Badge
                Row(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(Color(0xFF2E0854), RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💰", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${profile.coins}",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Total Stars Badge
                Row(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(Color(0xFF2E0854), RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0xFFFF4081), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "★", color = Color(0xFFFFD700), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${profile.totalStarsEarned}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Settings Icon Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(6.dp, CircleShape)
                        .background(Color(0xFF4A148C), CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚙", color = Color.White, fontSize = 20.sp)
                }
            }

            // Center: Title & Glowing Animated Heart
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TileView(tile = Tile.Normal(color = HeartColor.PINK))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "HEART MATCH",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Level ${profile.highestUnlockedLevel} / 100",
                    color = Color(0xFFFFD54F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Buttons: Play, Map, Daily Reward, Tutorial
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Play Button (Primary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(16.dp, RoundedCornerShape(26.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF4081), Color(0xFFFF8F00))
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .border(2.5.dp, Color.White, RoundedCornerShape(26.dp))
                        .clickable { onPlayClick() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CONTINUE LEVEL ${profile.highestUnlockedLevel} ▶",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Level Map Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(10.dp, RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF7B1FA2), Color(0xFF512DA8))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(2.dp, Color(0xFFFF80AB), RoundedCornerShape(24.dp))
                        .clickable { onLevelMapClick() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗺 LEVEL MAP (1-100)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Secondary Row: Daily Reward + Tutorial
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Daily Reward Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .background(Color(0xFF2E0854), RoundedCornerShape(20.dp))
                            .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(20.dp))
                            .clickable { onDailyRewardClick() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎁 Daily Gift",
                            color = Color(0xFFFFD700),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Tutorial Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .background(Color(0xFF2E0854), RoundedCornerShape(20.dp))
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp))
                            .clickable { onTutorialClick() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📖 Tutorial",
                            color = Color(0xFF00E5FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
