package com.example.heartmatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.heartmatch.ui.navigation.Screen
import com.example.heartmatch.ui.screens.DailyRewardScreen
import com.example.heartmatch.ui.screens.GameScreen
import com.example.heartmatch.ui.screens.LevelMapScreen
import com.example.heartmatch.ui.screens.MainMenuScreen
import com.example.heartmatch.ui.screens.SettingsScreen
import com.example.heartmatch.ui.screens.SplashScreen
import com.example.heartmatch.ui.screens.TutorialScreen
import com.example.heartmatch.ui.theme.HeartMatchTheme
import com.example.heartmatch.ui.viewmodel.HeartMatchViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HeartMatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeartMatchTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HeartMatchApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HeartMatchApp(viewModel: HeartMatchViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val playerProfile by viewModel.playerProfile.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val selectedCoord by viewModel.selectedCoord.collectAsState()
    val hintedCoords by viewModel.hintedCoords.collectAsState()
    val activeBooster by viewModel.activeBooster.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val victoryData by viewModel.victoryData.collectAsState()
    val defeatData by viewModel.defeatData.collectAsState()
    val previewLevelId by viewModel.previewLevelId.collectAsState()

    val particleBursts by viewModel.particleBursts.collectAsState()
    val scorePopups by viewModel.scorePopups.collectAsState()
    val blastWaves by viewModel.blastWaves.collectAsState()
    val laserBeams by viewModel.laserBeams.collectAsState()
    val comboBanner by viewModel.comboBanner.collectAsState()

    // Handle System Back Button
    BackHandler(enabled = currentScreen != Screen.MainMenu && currentScreen != Screen.Splash) {
        when (currentScreen) {
            Screen.Gameplay -> viewModel.togglePause()
            Screen.LevelMap, Screen.Settings, Screen.Tutorial, Screen.DailyReward -> viewModel.navigateTo(Screen.MainMenu)
            else -> {}
        }
    }

    when (currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onStartClick = { viewModel.navigateTo(Screen.MainMenu) }
            )
        }

        Screen.MainMenu -> {
            MainMenuScreen(
                profile = playerProfile,
                onPlayClick = { viewModel.startLevel(playerProfile.highestUnlockedLevel) },
                onLevelMapClick = { viewModel.navigateTo(Screen.LevelMap) },
                onDailyRewardClick = { viewModel.navigateTo(Screen.DailyReward) },
                onTutorialClick = { viewModel.navigateTo(Screen.Tutorial) },
                onSettingsClick = { viewModel.navigateTo(Screen.Settings) }
            )
        }

        Screen.LevelMap -> {
            LevelMapScreen(
                profile = playerProfile,
                previewLevelId = previewLevelId,
                getLevelRecord = { levelId -> viewModel.getLevelRecord(levelId) },
                getLevelConfig = { levelId -> viewModel.getLevelConfig(levelId) },
                onLevelClick = { levelId -> viewModel.showLevelPreview(levelId) },
                onStartLevel = { levelId -> viewModel.startLevel(levelId) },
                onDismissPreview = { viewModel.hideLevelPreview() },
                onBackClick = { viewModel.navigateTo(Screen.MainMenu) }
            )
        }

        Screen.Gameplay -> {
            GameScreen(
                gameState = gameState,
                playerProfile = playerProfile,
                selectedCoord = selectedCoord,
                hintedCoords = hintedCoords,
                activeBooster = activeBooster,
                isPaused = isPaused,
                victoryData = victoryData,
                defeatData = defeatData,
                particleBursts = particleBursts,
                scorePopups = scorePopups,
                blastWaves = blastWaves,
                laserBeams = laserBeams,
                comboBanner = comboBanner,
                onSwap = { from, to -> viewModel.onSwap(from, to) },
                onCellClick = { coord -> viewModel.onCellClick(coord) },
                onBoosterClick = { boosterType -> viewModel.activateBooster(boosterType) },
                onCancelBooster = { viewModel.cancelBooster() },
                onBoosterTarget = { coord -> viewModel.applyBoosterTarget(coord) },
                onPauseClick = { viewModel.togglePause() },
                onResumeClick = { viewModel.resumeGame() },
                onRestartClick = {
                    gameState?.levelId?.let { viewModel.startLevel(it) }
                },
                onNextLevelClick = {
                    val currentId = gameState?.levelId ?: 1
                    val nextId = (currentId + 1).coerceAtMost(100)
                    viewModel.startLevel(nextId)
                },
                onQuitToMapClick = { viewModel.navigateTo(Screen.LevelMap) },
                onParticleFinished = { id -> viewModel.onParticleFinished(id) },
                onScorePopupFinished = { id -> viewModel.onScorePopupFinished(id) },
                onBlastWaveFinished = { id -> viewModel.onBlastWaveFinished(id) },
                onLaserFinished = { id -> viewModel.onLaserFinished(id) }
            )
        }

        Screen.Settings -> {
            SettingsScreen(
                profile = playerProfile,
                onUpdateSettings = { sfx, music, haptics ->
                    viewModel.updateSettings(sfx, music, haptics)
                },
                onResetData = { viewModel.resetAllData() },
                onBackClick = { viewModel.navigateTo(Screen.MainMenu) }
            )
        }

        Screen.Tutorial -> {
            TutorialScreen(
                onBackClick = { viewModel.navigateTo(Screen.MainMenu) }
            )
        }

        Screen.DailyReward -> {
            DailyRewardScreen(
                profile = playerProfile,
                onClaimReward = { day, coins, booster ->
                    viewModel.claimDailyReward(day, coins, booster)
                },
                onBackClick = { viewModel.navigateTo(Screen.MainMenu) }
            )
        }
    }
}
