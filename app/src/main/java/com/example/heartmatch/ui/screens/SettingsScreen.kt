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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.heartmatch.data.PlayerProfile

@Composable
fun SettingsScreen(
    profile: PlayerProfile,
    onUpdateSettings: (sfx: Boolean, music: Boolean, haptics: Boolean) -> Unit,
    onResetData: () -> Unit,
    onBackClick: () -> Unit
) {
    var sfx by remember { mutableStateOf(profile.sfxEnabled) }
    var music by remember { mutableStateOf(profile.musicEnabled) }
    var haptics by remember { mutableStateOf(profile.hapticsEnabled) }
    var showResetConfirm by remember { mutableStateOf(false) }

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
                    .padding(top = 16.dp, bottom = 24.dp),
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
                    text = "SETTINGS",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Box(modifier = Modifier.size(42.dp))
            }

            // Settings Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .background(Color(0xFF2E0854).copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFFFF80AB).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    // SFX Toggle
                    SettingsToggleRow(
                        title = "Sound Effects",
                        icon = "🔊",
                        isChecked = sfx,
                        onCheckedChange = {
                            sfx = it
                            onUpdateSettings(sfx, music, haptics)
                        }
                    )

                    // Music Toggle
                    SettingsToggleRow(
                        title = "Background Music",
                        icon = "🎵",
                        isChecked = music,
                        onCheckedChange = {
                            music = it
                            onUpdateSettings(sfx, music, haptics)
                        }
                    )

                    // Haptics Toggle
                    SettingsToggleRow(
                        title = "Haptic Vibration",
                        icon = "📳",
                        isChecked = haptics,
                        onCheckedChange = {
                            haptics = it
                            onUpdateSettings(sfx, music, haptics)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Game Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Heart Match v1.0", color = Color(0xFFFFD54F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Original Match-3 Game Engine & UI", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Reset Progress Button
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color(0x33FF5252), RoundedCornerShape(20.dp))
                    .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(20.dp))
                    .clickable { showResetConfirm = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RESET ALL PROGRESS ⚠️",
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Reset Confirmation Dialog
        if (showResetConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showResetConfirm = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color(0xFF2E0854), RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFF5252), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Reset All Progress?",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This will erase your stars, unlocked levels, and booster inventory.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                    .clickable { showResetConfirm = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Cancel", color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFD50000), RoundedCornerShape(16.dp))
                                    .clickable {
                                        showResetConfirm = false
                                        onResetData()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Reset", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    icon: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF4081),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF212121)
            )
        )
    }
}
