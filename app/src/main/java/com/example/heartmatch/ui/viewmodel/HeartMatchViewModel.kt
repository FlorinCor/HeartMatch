package com.example.heartmatch.ui.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.heartmatch.audio.SoundEffect
import com.example.heartmatch.audio.SoundManager
import com.example.heartmatch.data.LevelRecord
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.data.PlayerRepository
import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.loader.LevelRepository
import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.FireDirection
import com.example.heartmatch.engine.model.GameState
import com.example.heartmatch.engine.model.GameStatus
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.LevelConfig
import com.example.heartmatch.engine.model.MatchShape
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile
import com.example.heartmatch.ui.components.BlastWaveData
import com.example.heartmatch.ui.components.ComboBannerData
import com.example.heartmatch.ui.components.HeartColors
import com.example.heartmatch.ui.components.LaserBeamData
import com.example.heartmatch.ui.components.ParticleBurst
import com.example.heartmatch.ui.components.ScorePopupData
import com.example.heartmatch.ui.components.createBurstParticles
import com.example.heartmatch.ui.navigation.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VictoryData(
    val levelId: Int,
    val score: Int,
    val stars: Int,
    val coinsEarned: Int
)

data class DefeatData(
    val levelId: Int,
    val reason: String
)

class HeartMatchViewModel(application: Application) : AndroidViewModel(application) {

    private val playerRepository = PlayerRepository(application)
    private val levelRepository = LevelRepository(resourceClassLoader = application.classLoader)
    private val soundManager = SoundManager(application)
    private var engine = HeartMatchEngine()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _playerProfile = MutableStateFlow(playerRepository.getProfile())
    val playerProfile: StateFlow<PlayerProfile> = _playerProfile.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _selectedCoord = MutableStateFlow<Coord?>(null)
    val selectedCoord: StateFlow<Coord?> = _selectedCoord.asStateFlow()

    private val _hintedCoords = MutableStateFlow<List<Coord>>(emptyList())
    val hintedCoords: StateFlow<List<Coord>> = _hintedCoords.asStateFlow()

    private val _activeBooster = MutableStateFlow<String?>(null)
    val activeBooster: StateFlow<String?> = _activeBooster.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _victoryData = MutableStateFlow<VictoryData?>(null)
    val victoryData: StateFlow<VictoryData?> = _victoryData.asStateFlow()

    private val _defeatData = MutableStateFlow<DefeatData?>(null)
    val defeatData: StateFlow<DefeatData?> = _defeatData.asStateFlow()

    private val _previewLevelId = MutableStateFlow<Int?>(null)
    val previewLevelId: StateFlow<Int?> = _previewLevelId.asStateFlow()

    // Visual Effects States
    private val _particleBursts = MutableStateFlow<List<ParticleBurst>>(emptyList())
    val particleBursts: StateFlow<List<ParticleBurst>> = _particleBursts.asStateFlow()

    private val _scorePopups = MutableStateFlow<List<ScorePopupData>>(emptyList())
    val scorePopups: StateFlow<List<ScorePopupData>> = _scorePopups.asStateFlow()

    private val _blastWaves = MutableStateFlow<List<BlastWaveData>>(emptyList())
    val blastWaves: StateFlow<List<BlastWaveData>> = _blastWaves.asStateFlow()

    private val _laserBeams = MutableStateFlow<List<LaserBeamData>>(emptyList())
    val laserBeams: StateFlow<List<LaserBeamData>> = _laserBeams.asStateFlow()

    private val _comboBanner = MutableStateFlow<ComboBannerData?>(null)
    val comboBanner: StateFlow<ComboBannerData?> = _comboBanner.asStateFlow()

    private var hintJob: Job? = null

    init {
        refreshProfile()
    }

    fun navigateTo(screen: Screen) {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        _currentScreen.value = screen
    }

    fun refreshProfile() {
        val profile = playerRepository.getProfile()
        _playerProfile.value = profile
        soundManager.updateSettings(profile.sfxEnabled, profile.hapticsEnabled)
    }

    fun getLevelRecord(levelId: Int): LevelRecord {
        return playerRepository.getLevelRecord(levelId)
    }

    fun getLevelConfig(levelId: Int): LevelConfig {
        return levelRepository.getLevel(levelId)
    }

    fun showLevelPreview(levelId: Int) {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        _previewLevelId.value = levelId
    }

    fun hideLevelPreview() {
        _previewLevelId.value = null
    }

    fun startLevel(levelId: Int) {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        _previewLevelId.value = null
        _victoryData.value = null
        _defeatData.value = null
        _selectedCoord.value = null
        _hintedCoords.value = emptyList()
        _activeBooster.value = null
        _isPaused.value = false

        val config = levelRepository.getLevel(levelId)
        engine = HeartMatchEngine(config)
        engine.addEventListener { event -> handleEngineEvent(event) }
        _gameState.value = engine.getState()
        _currentScreen.value = Screen.Gameplay

        startHintTimer()
    }

    fun onCellClick(coord: Coord) {
        val state = _gameState.value ?: return
        if (state.status != GameStatus.READY_FOR_INPUT) return

        val selected = _selectedCoord.value
        if (selected == null) {
            _selectedCoord.value = coord
            soundManager.playSound(SoundEffect.SWAP)
            soundManager.vibrate(15)
        } else if (selected == coord) {
            _selectedCoord.value = null
        } else if (selected.isAdjacentTo(coord)) {
            _selectedCoord.value = null
            executeSwap(selected, coord)
        } else {
            _selectedCoord.value = coord
            soundManager.playSound(SoundEffect.SWAP)
            soundManager.vibrate(15)
        }
    }

    fun onSwap(from: Coord, to: Coord) {
        _selectedCoord.value = null
        executeSwap(from, to)
    }

    private fun executeSwap(from: Coord, to: Coord) {
        val state = _gameState.value ?: return
        if (state.status != GameStatus.READY_FOR_INPUT) return

        _hintedCoords.value = emptyList()
        hintJob?.cancel()

        viewModelScope.launch {
            val result = engine.swap(from, to)
            _gameState.value = engine.getState()

            if (!result.isSuccessfulMove) {
                soundManager.playSound(SoundEffect.SWAP)
                soundManager.vibrate(25)
            }
            startHintTimer()
        }
    }

    fun activateBooster(boosterType: String) {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        val profile = _playerProfile.value
        when (boosterType) {
            "SHUFFLE" -> {
                if (playerRepository.consumeBooster("SHUFFLE")) {
                    refreshProfile()
                    soundManager.playSound(SoundEffect.BOOSTER_USE)
                    soundManager.vibrate(50)
                    val state = _gameState.value
                    if (state != null) {
                        // Reshuffle board
                        engine.loadLevel(levelRepository.getLevel(state.levelId))
                        _gameState.value = engine.getState()
                    }
                }
            }
            "EXTRA_MOVES" -> {
                if (playerRepository.consumeBooster("EXTRA_MOVES")) {
                    refreshProfile()
                    soundManager.playSound(SoundEffect.BOOSTER_USE)
                    soundManager.vibrate(50)
                    val state = _gameState.value
                    if (state != null) {
                        state.movesRemaining += 5
                        _gameState.value = state.copy(movesRemaining = state.movesRemaining)
                    }
                }
            }
            else -> {
                // Targeted booster (Hammer, Bomb, Rainbow)
                if (_activeBooster.value == boosterType) {
                    _activeBooster.value = null
                } else {
                    _activeBooster.value = boosterType
                }
            }
        }
    }

    fun cancelBooster() {
        _activeBooster.value = null
    }

    fun applyBoosterTarget(coord: Coord) {
        val booster = _activeBooster.value ?: return
        val state = _gameState.value ?: return
        val board = state.board
        val cell = board[coord] ?: return
        if (!cell.isPlayable) return

        if (playerRepository.consumeBooster(booster)) {
            refreshProfile()
            _activeBooster.value = null
            soundManager.playSound(SoundEffect.BOOSTER_USE)
            soundManager.vibrate(60)

            when (booster) {
                "HAMMER" -> {
                    // Destroy target cell tile / blocker
                    val tile = cell.tile
                    if (tile != null) {
                        triggerBurst(coord, Color.White, 20)
                        board.setTile(coord, null)
                        soundManager.playSound(SoundEffect.BLOCKER_DESTROY)
                    }
                }
                "BOMB" -> {
                    board.setTile(coord, Tile.Special(specialType = SpecialHeartType.BOMB_HEART))
                    triggerBlastWave(coord, Color(0xFFFF5252))
                    soundManager.playSound(SoundEffect.SPECIAL_BOMB)
                }
                "RAINBOW" -> {
                    board.setTile(coord, Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART))
                    triggerBurst(coord, Color(0xFFFFD700), 24)
                    soundManager.playSound(SoundEffect.SPECIAL_RAINBOW)
                }
            }
            _gameState.value = engine.getState()
        }
    }

    fun togglePause() {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        _isPaused.value = !_isPaused.value
    }

    fun resumeGame() {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        _isPaused.value = false
    }

    fun claimDailyReward(day: Int, rewardCoins: Int, boosterType: String? = null) {
        soundManager.playSound(SoundEffect.STAR_EARNED)
        soundManager.vibrate(60)
        playerRepository.claimDailyReward(day, rewardCoins, boosterType)
        refreshProfile()
    }

    fun updateSettings(sfx: Boolean, music: Boolean, haptics: Boolean) {
        val current = _playerProfile.value
        val updated = current.copy(sfxEnabled = sfx, musicEnabled = music, hapticsEnabled = haptics)
        playerRepository.saveProfile(updated)
        refreshProfile()
    }

    fun resetAllData() {
        playerRepository.resetAllData()
        refreshProfile()
    }

    private fun handleEngineEvent(event: EngineEvent) {
        when (event) {
            is EngineEvent.Swap -> {
                soundManager.playSound(SoundEffect.SWAP)
                soundManager.vibrate(20)
            }
            is EngineEvent.Match -> {
                val colorTriple = when (event.color) {
                    HeartColor.RED -> HeartColors.RedMain
                    HeartColor.PINK -> HeartColors.PinkMain
                    HeartColor.BLUE -> HeartColors.BlueMain
                    HeartColor.GREEN -> HeartColors.GreenMain
                    HeartColor.YELLOW -> HeartColors.YellowMain
                    HeartColor.PURPLE -> HeartColors.PurpleMain
                    HeartColor.ORANGE -> HeartColors.OrangeMain
                }
                event.coords.forEach { coord ->
                    triggerBurst(coord, colorTriple, 12)
                }
                soundManager.playSound(SoundEffect.MATCH, event.comboIndex)
                soundManager.vibrate((30 + event.comboIndex * 10).toLong())

                val scoreText = "+${event.coords.size * 50 * (event.comboIndex + 1)}"
                val firstCoord = event.coords.firstOrNull() ?: Coord(0, 0)
                addScorePopup(firstCoord, scoreText, Color(0xFFFFD54F))

                if (event.comboIndex >= 2) {
                    val (title, sub) = when (event.comboIndex) {
                        2 -> "Sweet!" to "Combo x2"
                        3 -> "Love Combo!" to "Combo x3"
                        4 -> "Heart Burst!" to "Combo x4"
                        else -> "Incredible!" to "Combo x${event.comboIndex}"
                    }
                    showComboBanner(title, sub, colorTriple)
                }
            }
            is EngineEvent.SpecialCreated -> {
                triggerBurst(event.coord, Color(0xFFFFD700), 20)
                soundManager.playSound(SoundEffect.STAR_EARNED)
                soundManager.vibrate(40)
            }
            is EngineEvent.SpecialTriggered -> {
                when (event.specialType) {
                    SpecialHeartType.FIRE_HEART -> {
                        val isHoriz = event.coord.row % 2 == 0
                        triggerLaserBeam(isHoriz, if (isHoriz) event.coord.row else event.coord.col, Color(0xFFFF3D00))
                        soundManager.playSound(SoundEffect.SPECIAL_FIRE)
                    }
                    SpecialHeartType.BOMB_HEART -> {
                        triggerBlastWave(event.coord, Color(0xFFFF5252))
                        soundManager.playSound(SoundEffect.SPECIAL_BOMB)
                    }
                    SpecialHeartType.RAINBOW_HEART -> {
                        triggerBurst(event.coord, Color(0xFFFFD700), 30)
                        soundManager.playSound(SoundEffect.SPECIAL_RAINBOW)
                    }
                    SpecialHeartType.GIFT_HEART -> {
                        triggerBurst(event.coord, Color(0xFFFF80AB), 24)
                        soundManager.playSound(SoundEffect.STAR_EARNED)
                    }
                    SpecialHeartType.ROYAL_HEART -> {
                        triggerBlastWave(event.coord, Color(0xFFFFD700))
                        triggerBurst(event.coord, Color(0xFFFFF176), 32)
                        soundManager.playSound(SoundEffect.SPECIAL_RAINBOW)
                    }
                    SpecialHeartType.ANGEL_HEART -> {
                        triggerBlastWave(event.coord, Color(0xFFE1BEE7))
                        triggerBurst(event.coord, Color(0xFFFFF9C4), 28)
                        soundManager.playSound(SoundEffect.STAR_EARNED)
                    }
                }
                soundManager.vibrate(70)
            }
            is EngineEvent.BlockerDamaged -> {
                triggerBurst(event.coord, Color(0xFFCFD8DC), 10)
                soundManager.playSound(SoundEffect.BLOCKER_HIT)
                soundManager.vibrate(35)
            }
            is EngineEvent.BlockerDestroyed -> {
                triggerBurst(event.coord, Color(0xFFFF5252), 20)
                soundManager.playSound(SoundEffect.BLOCKER_DESTROY)
                soundManager.vibrate(50)
            }
            is EngineEvent.GameWon -> {
                val coinsAwarded = 50 + event.stars * 25
                playerRepository.saveLevelProgress(engine.getState().levelId, event.stars, event.finalScore)
                playerRepository.addCoins(coinsAwarded)
                refreshProfile()

                viewModelScope.launch {
                    delay(600)
                    soundManager.playSound(SoundEffect.VICTORY)
                    soundManager.vibrate(100)
                    _victoryData.value = VictoryData(
                        levelId = engine.getState().levelId,
                        score = event.finalScore,
                        stars = event.stars,
                        coinsEarned = coinsAwarded
                    )
                }
            }
            is EngineEvent.GameOver -> {
                viewModelScope.launch {
                    delay(500)
                    soundManager.playSound(SoundEffect.GAME_OVER)
                    soundManager.vibrate(80)
                    _defeatData.value = DefeatData(
                        levelId = engine.getState().levelId,
                        reason = event.reason
                    )
                }
            }
            else -> {}
        }
    }

    private fun startHintTimer() {
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(5000)
            val possible = engine.getPossibleMoves()
            if (possible.isNotEmpty()) {
                val (from, to) = possible.first()
                _hintedCoords.value = listOf(from, to)
            }
        }
    }

    private fun triggerBurst(coord: Coord, color: Color, count: Int = 16) {
        val board = _gameState.value?.board ?: return
        val cellSize = 300f / maxOf(board.cols, 1)
        val center = Offset((coord.col + 0.5f) * cellSize, (coord.row + 0.5f) * cellSize)
        val burst = ParticleBurst(
            origin = center,
            color = color,
            particles = createBurstParticles(center, color, count)
        )
        _particleBursts.value = _particleBursts.value + burst
    }

    private fun addScorePopup(coord: Coord, text: String, color: Color) {
        val board = _gameState.value?.board ?: return
        val cellSize = 300f / maxOf(board.cols, 1)
        val pos = Offset((coord.col + 0.5f) * cellSize, (coord.row + 0.5f) * cellSize)
        val popup = ScorePopupData(text = text, position = pos, color = color)
        _scorePopups.value = _scorePopups.value + popup
    }

    private fun triggerBlastWave(coord: Coord, color: Color) {
        val board = _gameState.value?.board ?: return
        val cellSize = 300f / maxOf(board.cols, 1)
        val center = Offset((coord.col + 0.5f) * cellSize, (coord.row + 0.5f) * cellSize)
        val wave = BlastWaveData(center = center, maxRadius = cellSize * 2.5f, color = color)
        _blastWaves.value = _blastWaves.value + wave
    }

    private fun triggerLaserBeam(isHorizontal: Boolean, index: Int, color: Color) {
        val laser = LaserBeamData(isHorizontal = isHorizontal, coordIndex = index, color = color)
        _laserBeams.value = _laserBeams.value + laser
    }

    private fun showComboBanner(title: String, subtitle: String, color: Color) {
        val banner = ComboBannerData(title = title, subtitle = subtitle, color = color)
        _comboBanner.value = banner
        viewModelScope.launch {
            delay(1200)
            if (_comboBanner.value?.id == banner.id) {
                _comboBanner.value = null
            }
        }
    }

    fun onParticleFinished(id: Long) {
        _particleBursts.value = _particleBursts.value.filterNot { it.id == id }
    }

    fun onScorePopupFinished(id: Long) {
        _scorePopups.value = _scorePopups.value.filterNot { it.id == id }
    }

    fun onBlastWaveFinished(id: Long) {
        _blastWaves.value = _blastWaves.value.filterNot { it.id == id }
    }

    fun onLaserFinished(id: Long) {
        _laserBeams.value = _laserBeams.value.filterNot { it.id == id }
    }
}
