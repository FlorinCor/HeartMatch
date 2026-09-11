package com.example.heartmatch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.GameState
import com.example.heartmatch.ui.components.BlastWaveData
import com.example.heartmatch.ui.components.BoardView
import com.example.heartmatch.ui.components.BoostersView
import com.example.heartmatch.ui.components.ComboBannerData
import com.example.heartmatch.ui.components.LaserBeamData
import com.example.heartmatch.ui.components.ParticleBurst
import com.example.heartmatch.ui.components.ScorePopupData
import com.example.heartmatch.ui.components.TopHudView
import com.example.heartmatch.ui.viewmodel.DefeatData
import com.example.heartmatch.ui.viewmodel.VictoryData

@Composable
fun GameScreen(
    gameState: GameState?,
    playerProfile: PlayerProfile,
    selectedCoord: Coord?,
    hintedCoords: List<Coord>,
    activeBooster: String?,
    isPaused: Boolean,
    victoryData: VictoryData?,
    defeatData: DefeatData?,
    particleBursts: List<ParticleBurst>,
    scorePopups: List<ScorePopupData>,
    blastWaves: List<BlastWaveData>,
    laserBeams: List<LaserBeamData>,
    comboBanner: ComboBannerData?,
    onSwap: (Coord, Coord) -> Unit,
    onCellClick: (Coord) -> Unit,
    onBoosterClick: (String) -> Unit,
    onCancelBooster: () -> Unit,
    onBoosterTarget: (Coord) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onQuitToMapClick: () -> Unit,
    onParticleFinished: (Long) -> Unit,
    onScorePopupFinished: (Long) -> Unit,
    onBlastWaveFinished: (Long) -> Unit,
    onLaserFinished: (Long) -> Unit
) {
    if (gameState == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E0A30),
                        Color(0xFF3F145B),
                        Color(0xFF140824)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top HUD
            TopHudView(gameState = gameState)

            // 2. Center Game Board
            BoardView(
                board = gameState.board,
                selectedCoord = selectedCoord,
                hintedCoords = hintedCoords,
                activeBooster = activeBooster,
                particleBursts = particleBursts,
                scorePopups = scorePopups,
                blastWaves = blastWaves,
                laserBeams = laserBeams,
                comboBanner = comboBanner,
                onSwap = onSwap,
                onCellClick = onCellClick,
                onBoosterTarget = onBoosterTarget,
                onParticleFinished = onParticleFinished,
                onScorePopupFinished = onScorePopupFinished,
                onBlastWaveFinished = onBlastWaveFinished,
                onLaserFinished = onLaserFinished,
                modifier = Modifier.weight(1f, fill = false)
            )

            // 3. Bottom Boosters & Pause Bar
            BoostersView(
                profile = playerProfile,
                activeBooster = activeBooster,
                onBoosterClick = onBoosterClick,
                onCancelBooster = onCancelBooster,
                onPauseClick = onPauseClick
            )
        }

        // Pause Modal Overlay
        if (isPaused) {
            PauseModal(
                onResume = onResumeClick,
                onRestart = onRestartClick,
                onQuit = onQuitToMapClick
            )
        }

        // Victory Modal Overlay
        if (victoryData != null) {
            VictoryModal(
                data = victoryData,
                onNextLevel = onNextLevelClick,
                onReplay = onRestartClick,
                onQuit = onQuitToMapClick
            )
        }

        // Defeat Modal Overlay
        if (defeatData != null) {
            DefeatModal(
                data = defeatData,
                extraMovesCount = playerProfile.extraMovesCount,
                onUseExtraMoves = { onBoosterClick("EXTRA_MOVES") },
                onRetry = onRestartClick,
                onQuit = onQuitToMapClick
            )
        }
    }
}

@Composable
fun PauseModal(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(listOf(Color(0xFF3F145B), Color(0xFF1E0A30))),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(2.5.dp, Color(0xFFFF80AB), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "GAME PAUSED",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                // Resume Button
                ModalButton(text = "RESUME ▶", color1 = Color(0xFFFF4081), color2 = Color(0xFFFF8F00), onClick = onResume)

                // Restart Button
                ModalButton(text = "RESTART ↻", color1 = Color(0xFF7B1FA2), color2 = Color(0xFF512DA8), onClick = onRestart)

                // Quit to Map Button
                ModalButton(text = "QUIT TO MAP 🗺", color1 = Color(0xFF424242), color2 = Color(0xFF212121), onClick = onQuit)
            }
        }
    }
}

@Composable
fun VictoryModal(
    data: VictoryData,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(32.dp, RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF1A002C))),
                    shape = RoundedCornerShape(32.dp)
                )
                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LEVEL COMPLETED!",
                    color = Color(0xFFFFD54F),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Stars Celebration Row
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (i in 1..3) {
                        val isEarned = i <= data.stars
                        Text(
                            text = "★",
                            color = if (isEarned) Color(0xFFFFD700) else Color(0x55FFFFFF),
                            fontSize = if (i == 2) 48.sp else 38.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Final Score: ${data.score}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "+${data.coinsEarned} Coins Earned 💰",
                    color = Color(0xFFFFD700),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Next Level Button
                ModalButton(
                    text = "NEXT LEVEL ▶",
                    color1 = Color(0xFFFF4081),
                    color2 = Color(0xFFFF8F00),
                    onClick = onNextLevel
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Replay & Quit row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .background(Color(0xFF2E0854), RoundedCornerShape(20.dp))
                            .border(1.5.dp, Color(0xFFFF80AB), RoundedCornerShape(20.dp))
                            .clickable { onReplay() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Replay ↻", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .background(Color(0xFF2E0854), RoundedCornerShape(20.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .clickable { onQuit() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Level Map 🗺", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DefeatModal(
    data: DefeatData,
    extraMovesCount: Int,
    onUseExtraMoves: () -> Unit,
    onRetry: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(32.dp, RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(listOf(Color(0xFF3E121D), Color(0xFF1E0A14))),
                    shape = RoundedCornerShape(32.dp)
                )
                .border(3.dp, Color(0xFFFF5252), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OUT OF MOVES!",
                    color = Color(0xFFFF5252),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Don't give up! Keep your romance alive.",
                    color = Color(0xFFFFCDD2),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(22.dp))

                // +5 Extra Moves option
                if (extraMovesCount > 0) {
                    ModalButton(
                        text = "USE +5 MOVES ($extraMovesCount LEFT)",
                        color1 = Color(0xFF4CAF50),
                        color2 = Color(0xFF2E7D32),
                        onClick = onUseExtraMoves
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Retry Button
                ModalButton(
                    text = "RETRY LEVEL ↻",
                    color1 = Color(0xFFFF4081),
                    color2 = Color(0xFFE91E63),
                    onClick = onRetry
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quit to Map
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { onQuit() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Quit to Map 🗺", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModalButton(
    text: String,
    color1: Color,
    color2: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .background(
                brush = Brush.horizontalGradient(listOf(color1, color2)),
                shape = RoundedCornerShape(22.dp)
            )
            .border(2.dp, Color.White, RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
    }
}
