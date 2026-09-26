package com.example.heartmatch.ui.screens

import com.example.heartmatch.ui.theme.GardenPalette
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
import com.example.heartmatch.ui.components.GardenBackdrop
import com.example.heartmatch.ui.components.GardenIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.example.heartmatch.ui.animation.BoardDisplayState
import com.example.heartmatch.ui.viewmodel.DefeatData
import com.example.heartmatch.ui.viewmodel.VictoryData

@Composable
fun GameScreen(
    gameState: GameState?,
    boardDisplay: BoardDisplayState,
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
                        GardenPalette.Background,
                        GardenPalette.Panel,
                        GardenPalette.Background
                    )
                )
            )
    ) {
        GardenBackdrop(dim = 0.86f)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top HUD
            TopHudView(gameState = gameState, modifier = Modifier.testTag("puzzle-hud"))

            // Fit the board to the remaining height as well as the screen width.
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val visibleBoard = boardDisplay.board ?: gameState.board
                val boardRatio = visibleBoard.cols.toFloat() / visibleBoard.rows.toFloat()
                val boardWidth = minOf(maxWidth, maxHeight * boardRatio)
            BoardView(
                board = boardDisplay.board ?: gameState.board,
                displayState = boardDisplay,
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
                modifier = Modifier.width(boardWidth).testTag("puzzle-board")
            )
            }

            if (activeBooster == null && selectedCoord != null && gameState.board.getTile(selectedCoord) is com.example.heartmatch.engine.model.Tile.Special)
                Text("Current activation preview · swapping can change targets", color=GardenPalette.Gold, fontSize=11.sp)
            if (activeBooster != null) Text(
                if (activeBooster == "HAMMER") "Tap to preview · tap same cell again to use 1 hammer"
                else "Normal heart only · tap twice to place 1 ${activeBooster.lowercase()}. Highlight previews its effect; rainbow uses swapped color.",
                modifier=Modifier.padding(horizontal=12.dp), color=GardenPalette.Gold, fontSize=12.sp)
            // 3. Bottom Boosters & Pause Bar
            BoostersView(
                profile = playerProfile,
                activeBooster = activeBooster,
                onBoosterClick = onBoosterClick,
                onCancelBooster = onCancelBooster,
                onPauseClick = onPauseClick,
                modifier = Modifier.testTag("puzzle-boosters")
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
private fun GardenModal(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.padding(20.dp).widthIn(max=420.dp).fillMaxWidth()
            .background(GardenPalette.Panel,RoundedCornerShape(24.dp))
            .border(1.dp,GardenPalette.Rim.copy(alpha=0.5f),RoundedCornerShape(24.dp))
            .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
    }
}

@Composable
fun PauseModal(onResume: () -> Unit,onRestart: () -> Unit,onQuit: () -> Unit) {
    GardenModal(onResume) {
        Text("Take a breath",fontSize=26.sp,fontWeight=FontWeight.Bold,color=GardenPalette.Ivory)
        Text("Your puzzle is paused",fontSize=14.sp,color=GardenPalette.Ivory.copy(alpha=0.7f))
        ModalButton("Resume",GardenPalette.Rose,GardenPalette.RoseDark,onResume)
        OutlinedButton(onClick=onRestart,modifier=Modifier.fillMaxWidth()){Text("Restart",color=GardenPalette.Ivory)}
        TextButton(onClick=onQuit){Text("Back to garden",color=GardenPalette.Gold)}
    }
}

@Composable
fun VictoryModal(data: VictoryData,onNextLevel: () -> Unit,onReplay: () -> Unit,onQuit: () -> Unit) {
    GardenModal({}) {
        Text("Beautifully done",fontSize=26.sp,fontWeight=FontWeight.Bold,color=GardenPalette.Ivory)
        Text("Level ${data.levelId} complete",color=GardenPalette.Ivory.copy(alpha=0.7f))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            for(i in 1..3) Text("★",color=if(i<=data.stars) GardenPalette.Gold else GardenPalette.Rim,fontSize=40.sp)
        }
        Text("${data.score} points",color=GardenPalette.Ivory,fontSize=22.sp,fontWeight=FontWeight.Bold)
        with(data.breakdown) {
            Text("Hearts $hearts · creations $creations · blockers $blockers\nSpecials $specials · cascades $cascades · unused moves $remainingMoves",
                color=GardenPalette.Ivory, fontSize=12.sp)
        }
        if (data.milestone) Text("Garden milestone: +1 hammer and +1 shuffle", color=GardenPalette.Gold)
        Text("+${data.coinsEarned} coins",color=GardenPalette.Gold,fontSize=16.sp)
        ModalButton("Continue",GardenPalette.Rose,GardenPalette.RoseDark,onNextLevel)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            TextButton(onClick=onReplay){Text("Replay",color=GardenPalette.Ivory)}
            TextButton(onClick=onQuit){Text("Garden",color=GardenPalette.Ivory)}
        }
    }
}

@Composable
fun DefeatModal(data: DefeatData,extraMovesCount: Int,onUseExtraMoves: () -> Unit,onRetry: () -> Unit,onQuit: () -> Unit) {
    GardenModal({}) {
        Text("Out of moves",fontSize=26.sp,fontWeight=FontWeight.Bold,color=GardenPalette.Ivory)
        Text("Another try, another chance to bloom.",color=GardenPalette.Ivory.copy(alpha=0.7f),fontSize=14.sp)
        if(extraMovesCount>0) ModalButton("Use +5 moves ($extraMovesCount left)",GardenPalette.PanelLight,GardenPalette.Panel,onUseExtraMoves)
        ModalButton("Try again",GardenPalette.Rose,GardenPalette.RoseDark,onRetry)
        TextButton(onClick=onQuit){Text("Back to garden",color=GardenPalette.Gold)}
    }
}

@Composable
fun ModalButton(text: String,color1: Color,color2: Color,onClick: () -> Unit) {
    Button(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(16.dp),
        colors=ButtonDefaults.buttonColors(containerColor=color1,contentColor=GardenPalette.Ivory)) {
        Text(text,fontSize=16.sp,fontWeight=FontWeight.Bold)
    }
}
