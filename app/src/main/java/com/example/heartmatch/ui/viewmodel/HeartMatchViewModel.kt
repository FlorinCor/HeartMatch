package com.example.heartmatch.ui.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.heartmatch.audio.HapticEffect
import com.example.heartmatch.audio.SoundEffect
import com.example.heartmatch.audio.SoundManager
import com.example.heartmatch.data.LevelRecord
import com.example.heartmatch.data.PlayerProfile
import com.example.heartmatch.data.PlayerRepository
import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.core.MatchDetector
import com.example.heartmatch.engine.core.MoveValidator
import com.example.heartmatch.engine.loader.LevelRepository
import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.CellState
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
import com.example.heartmatch.ui.animation.BoardDisplayState
import com.example.heartmatch.ui.animation.TurnChoreographer
import com.example.heartmatch.ui.animation.TurnStep
import com.example.heartmatch.ui.components.BlastWaveData
import com.example.heartmatch.ui.components.ComboBannerData
import com.example.heartmatch.ui.components.DebrisMaterial
import com.example.heartmatch.ui.components.HeartColors
import com.example.heartmatch.ui.components.LaserBeamData
import com.example.heartmatch.ui.components.ParticleBurst
import com.example.heartmatch.ui.components.ScorePopupData
import com.example.heartmatch.ui.components.createBubblePopParticles
import com.example.heartmatch.ui.components.createBurstParticles
import com.example.heartmatch.ui.components.createDebrisParticles
import com.example.heartmatch.ui.navigation.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class VictoryData(
    val levelId: Int,
    val score: Int,
    val stars: Int,
    val coinsEarned: Int,
    val breakdown: com.example.heartmatch.engine.model.ScoreBreakdown,
    val milestone: Boolean
)

data class DefeatData(
    val levelId: Int,
    val reason: String
)

class HeartMatchViewModel(application: Application) : AndroidViewModel(application) {

    private val playerRepository = PlayerRepository(application)
    private val levelRepository = LevelRepository(
        assetOpener = { path -> runCatching { application.assets.open(path) }.getOrNull() },
        resourceClassLoader = application.classLoader
    )
    val totalAvailableStars = application.assets.list("levels").orEmpty().count { it.startsWith("level_") && it.endsWith(".json") } * 3
    private val soundManager = SoundManager(application)
    private val turnChoreographer = TurnChoreographer()
    private var engine = HeartMatchEngine()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.LevelMap)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _playerProfile = MutableStateFlow(playerRepository.getProfile())
    val playerProfile: StateFlow<PlayerProfile> = _playerProfile.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    /** Board snapshot currently shown, replayed step by step while a turn is being animated. */
    private val _boardDisplay = MutableStateFlow(BoardDisplayState())
    val boardDisplay: StateFlow<BoardDisplayState> = _boardDisplay.asStateFlow()

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

    private var resultJob: Job? = null
    private var rewarded = false
    private var boosterTarget: Coord? = null
    private var hintJob: Job? = null
    private var turnJob: Job? = null
    private val isTurnRunning: Boolean get() = turnJob?.isActive == true

    init {
        refreshProfile()
    }

    fun navigateTo(screen: Screen) {
        playButtonFeedback()
        if (screen != Screen.Gameplay) { resultJob?.cancel(); hintJob?.cancel(); turnJob?.cancel() }
        _currentScreen.value = screen
    }

    private fun playButtonFeedback() {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        soundManager.haptic(HapticEffect.BUTTON)
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
        playButtonFeedback()
        _previewLevelId.value = levelId
    }

    fun hideLevelPreview() {
        _previewLevelId.value = null
    }

    fun startLevel(levelId: Int) {
        playButtonFeedback()
        _previewLevelId.value = null
        _victoryData.value = null
        _defeatData.value = null
        _selectedCoord.value = null
        _hintedCoords.value = emptyList()
        _activeBooster.value = null
        _isPaused.value = false

        turnJob?.cancel()
        resultJob?.cancel()
        rewarded = false
        boosterTarget = null
        val config = randomizeStartingHearts(levelRepository.getLevel(levelId))
        engine = HeartMatchEngine(config)
        _gameState.value = engine.getState()
        publishBoard(engine.getState().board)
        _currentScreen.value = Screen.Gameplay

        startHintTimer()
    }

    private fun randomizeStartingHearts(config: LevelConfig): LevelConfig {
        val seed = kotlin.random.Random.nextLong()
        val initialTiles = config.initialTiles ?: return config.copy(randomSeed = seed)
        val normalCoords = buildList {
            for (row in 0 until config.rows) {
                for (col in 0 until config.cols) {
                    if (initialTiles.getOrNull(row)?.getOrNull(col) is Tile.Normal) add(Coord(row, col))
                }
            }
        }
        if (normalCoords.size < 2) return config.copy(randomSeed = seed)

        val normalHearts = normalCoords.map { initialTiles[it.row][it.col]!! }
        val matchDetector = MatchDetector()
        val moveValidator = MoveValidator(matchDetector)
        val random = kotlin.random.Random(seed)
        val hasEmptyPlayableCells = (0 until config.rows).any { row ->
            (0 until config.cols).any { col ->
                val cellState = config.cellStates?.getOrNull(row)?.getOrNull(col)
                val playable = cellState == null || cellState == CellState.PLAYABLE
                playable && initialTiles.getOrNull(row)?.getOrNull(col) == null
            }
        }

        repeat(250) {
            val candidate = Array(config.rows) { row -> initialTiles[row].copyOf() }
            val shuffledHearts = normalHearts.shuffled(random)
            normalCoords.forEachIndexed { index, coord ->
                candidate[coord.row][coord.col] = shuffledHearts[index]
            }
            val board = Board.createWithLayout(config.rows, config.cols, config.cellStates, candidate)
            if (matchDetector.detectMatches(board).isEmpty() &&
                (hasEmptyPlayableCells || moveValidator.findPossibleMoves(board).isNotEmpty())
            ) {
                return config.copy(initialTiles = candidate, randomSeed = seed)
            }
        }

        // Keep the level's authored layout if no safe heart permutation was found.
        return config.copy(randomSeed = seed)
    }

    fun onCellClick(coord: Coord) {
        val state = _gameState.value ?: return
        if (state.status != GameStatus.READY_FOR_INPUT || isTurnRunning || _isPaused.value) return

        val selected = _selectedCoord.value
        if (selected == null) {
            _selectedCoord.value = coord
            _hintedCoords.value = if (state.board.getTile(coord) is Tile.Special) engine.previewTarget(coord).toList() else emptyList()
            playSelectFeedback()
        } else if (selected == coord) {
            _selectedCoord.value = null
            _hintedCoords.value = emptyList()
            startHintTimer()
            soundManager.haptic(HapticEffect.SELECT)
        } else if (selected.isAdjacentTo(coord)) {
            _selectedCoord.value = null
            executeSwap(selected, coord)
        } else {
            _selectedCoord.value = coord
            _hintedCoords.value = if (state.board.getTile(coord) is Tile.Special) engine.previewTarget(coord).toList() else emptyList()
            playSelectFeedback()
        }
    }

    private fun playSelectFeedback() {
        soundManager.playSound(SoundEffect.SELECT)
        soundManager.haptic(HapticEffect.SELECT)
    }

    fun onSwap(from: Coord, to: Coord) {
        _selectedCoord.value = null
        executeSwap(from, to)
    }

    private fun executeSwap(from: Coord, to: Coord) {
        val state = _gameState.value ?: return
        if (state.status != GameStatus.READY_FOR_INPUT || isTurnRunning || _isPaused.value) return

        _hintedCoords.value = emptyList()
        hintJob?.cancel()

        turnJob = viewModelScope.launch {
            val boardBefore = state.board.clone()
            // Publish a new value before the engine mutates its in-place GameState. This lets
            // StateFlow observe the later move and objective updates as a distinct state too.
            _gameState.value = state.copy(status = GameStatus.RESOLVING)
            val result = engine.swap(from, to)
            val finalState = engine.getState()
            val steps = turnChoreographer.build(boardBefore, result.events, finalState.board)
            playTurn(steps)
            // The replay always ends on the authoritative engine board.
            publishBoard(finalState.board)
            _gameState.value = finalState
            startHintTimer()
        }
    }

    private suspend fun playTurn(steps: List<TurnStep>) {
        for (step in steps) {
            step.events.forEach { handleEngineEvent(it) }
            _boardDisplay.value = when (step) {
                is TurnStep.Swap -> nextDisplay(step.board, swapFrom = step.from, swapTo = step.to)
                is TurnStep.Clear -> nextDisplay(step.board, vanishing = step.vanishing)
                is TurnStep.Settle -> nextDisplay(
                    step.board,
                    popInIds = step.popInIds,
                    healedCoords = step.healedCoords
                )
                is TurnStep.Drop -> nextDisplay(step.board, fallOrigins = step.fallOrigins, popInIds = step.popInIds)
                is TurnStep.Finish -> nextDisplay(step.board)
            }
            if (step.durationMs > 0) delay(step.durationMs)
        }
    }

    private fun nextDisplay(
        board: Board,
        vanishing: Set<Coord> = emptySet(),
        swapFrom: Coord? = null,
        swapTo: Coord? = null,
        fallOrigins: Map<String, Int> = emptyMap(),
        popInIds: Set<String> = emptySet(),
        healedCoords: Set<Coord> = emptySet()
    ): BoardDisplayState = BoardDisplayState(
        board = board,
        version = _boardDisplay.value.version + 1,
        vanishingCoords = vanishing,
        swapFrom = swapFrom,
        swapTo = swapTo,
        fallOrigins = fallOrigins,
        popInIds = popInIds,
        healedCoords = healedCoords
    )

    /** Shows [board] as-is (no transition hints); a fresh clone is published so the UI always sees a new snapshot. */
    private fun publishBoard(board: Board) {
        _boardDisplay.value = nextDisplay(board.clone())
    }

    private fun stock(type: String): Int = with(_playerProfile.value) {
        when (type) { "HAMMER" -> hammerCount; "BOMB" -> bombBoosterCount; "RAINBOW" -> rainbowBoosterCount;
            "SHUFFLE" -> shuffleCount; "EXTRA_MOVES" -> extraMovesCount; else -> 0 }
    }

    fun activateBooster(boosterType: String) {
        if (isTurnRunning || _isPaused.value || stock(boosterType) <= 0) return
        val state = _gameState.value ?: return
        val accepted = when (boosterType) {
            "EXTRA_MOVES" -> engine.addExtraMoves()
            "SHUFFLE" -> state.status == GameStatus.READY_FOR_INPUT && engine.reshuffleCurrentBoard()
            else -> {
                if (state.status != GameStatus.READY_FOR_INPUT) return
                cancelBooster()
                _activeBooster.value = boosterType
                return
            }
        }
        if (accepted) {
            playerRepository.consumeBooster(boosterType)
            resultJob?.cancel()
            _defeatData.value = null
            refreshProfile()
            _gameState.value = engine.getState().copy()
            publishBoard(engine.getState().board)
            startHintTimer()
        }
    }

    fun cancelBooster() {
        _activeBooster.value = null
        boosterTarget = null
        _hintedCoords.value = emptyList()
    }

    fun applyBoosterTarget(coord: Coord) {
        val booster = _activeBooster.value ?: return
        val state = _gameState.value ?: return
        val tile = state.board[coord]?.takeIf { it.isPlayable }?.tile ?: return
        if (isTurnRunning || _isPaused.value || state.status != GameStatus.READY_FOR_INPUT || stock(booster) <= 0) return
        if (booster != "HAMMER" && tile !is Tile.Normal) return
        // First tap previews; a second tap on the same target confirms spending one item.
        if (boosterTarget != coord) {
            hintJob?.cancel()
            boosterTarget = coord
            _selectedCoord.value = coord
            _hintedCoords.value = when (booster) {
                "HAMMER" -> engine.previewTarget(coord).toList()
                "BOMB" -> state.board.getAllPlayableCoords().filter { kotlin.math.abs(it.row - coord.row) <= 1 && kotlin.math.abs(it.col - coord.col) <= 1 }
                else -> state.board.getAllPlayableCoords().filter { state.board.getTile(it)?.matchColor == tile.matchColor }
            }
            return
        }
        val before = state.board.clone()
        val events = when (booster) {
            "HAMMER" -> engine.applyHammer(coord).also { if (it.isEmpty()) return }
            else -> {
                if (!engine.placeBooster(if (booster == "BOMB") SpecialHeartType.BOMB_HEART else SpecialHeartType.RAINBOW_HEART, coord)) return
                emptyList()
            }
        }
        playerRepository.consumeBooster(booster)
        refreshProfile()
        cancelBooster()
        _selectedCoord.value = null
        turnJob = viewModelScope.launch {
            _gameState.value = state.copy(status = GameStatus.RESOLVING)
            playTurn(turnChoreographer.build(before, events, engine.getState().board))
            _gameState.value = engine.getState().copy()
            publishBoard(engine.getState().board)
            startHintTimer()
        }
    }

    fun purchase(item: String) { playerRepository.purchase(item); refreshProfile() }

    fun togglePause() {
        playButtonFeedback()
        _isPaused.value = !_isPaused.value
    }

    fun resumeGame() {
        playButtonFeedback()
        _isPaused.value = false
    }

    fun claimDailyReward(day: Int, rewardCoins: Int, boosterType: String? = null) {
        if (!playerRepository.claimDailyReward(day, rewardCoins, boosterType)) return
        soundManager.playSound(SoundEffect.STAR_EARNED)
        soundManager.haptic(HapticEffect.REWARD)
        refreshProfile()
    }

    fun updateSettings(sfx: Boolean, haptics: Boolean, reducedMotion: Boolean) {
        val current = _playerProfile.value
        val updated = current.copy(sfxEnabled = sfx, hapticsEnabled = haptics, reducedMotion = reducedMotion)
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
                if (event.isRollback) {
                    soundManager.playSound(SoundEffect.INVALID_MOVE)
                    soundManager.haptic(HapticEffect.INVALID_MOVE)
                } else {
                    soundManager.playSound(SoundEffect.SWAP)
                    soundManager.haptic(HapticEffect.SWAP)
                }
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
                if (event.comboIndex == 1) {
                    soundManager.playSound(SoundEffect.MATCH, 0)
                } else {
                    soundManager.playSound(SoundEffect.CASCADE, event.comboIndex - 1)
                }
                soundManager.haptic(HapticEffect.MATCH, event.comboIndex)

            }
            is EngineEvent.ScoreStep -> {
                if (event.points > 0) addScorePopup(event.coord, "+${event.points}", Color(0xFFFFD54F))
                if (event.multiplier > 1.0) showComboBanner("Lovely cascade!", "×${event.multiplier}", Color(0xFFFFD54F))
            }

            is EngineEvent.SpecialCreated -> {
                triggerBurst(event.coord, Color(0xFFFFD700), 20)
                soundManager.playSound(SoundEffect.SPECIAL_CREATED)
                soundManager.haptic(HapticEffect.SPECIAL_CREATED)
            }
            is EngineEvent.SpecialTriggered -> {
                when (event.specialType) {
                    SpecialHeartType.FIRE_HEART -> {
                        val playable = engine.getState().board.getAllPlayableCoords()
                        event.affectedCoords.groupBy { it.row }.filter { (row, cells) ->
                            cells.size == playable.count { it.row == row }
                        }.keys.forEach { triggerLaserBeam(true, it, Color(0xFFFF3D00)) }
                        event.affectedCoords.groupBy { it.col }.filter { (col, cells) ->
                            cells.size == playable.count { it.col == col }
                        }.keys.forEach { triggerLaserBeam(false, it, Color(0xFFFF3D00)) }
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
                    SpecialHeartType.LIGHT_HEART -> {
                        triggerBlastWave(event.coord, Color(0xFFFFFDE7))
                        triggerBurst(event.coord, Color(0xFFFFF3B0), 30)
                        soundManager.playSound(SoundEffect.SPECIAL_RAINBOW)
                    }
                }
                soundManager.haptic(HapticEffect.SPECIAL_TRIGGERED)
            }
            is EngineEvent.BlockerDamaged -> {
                if (!event.isDestroyed) triggerBurst(event.coord, Color(0xFFCFD8DC), 10)
                soundManager.playSound(SoundEffect.BLOCKER_HIT)
                soundManager.haptic(HapticEffect.BLOCKER_HIT)
            }
            is EngineEvent.BlockerDestroyed -> {
                when (event.blockerType) {
                    BlockerType.WOODEN_HEART -> triggerDebris(event.coord, DebrisMaterial.WOOD, 16)
                    BlockerType.STONE_HEART -> triggerDebris(event.coord, DebrisMaterial.STONE, 22)
                    BlockerType.ICE_HEART -> triggerDebris(event.coord, DebrisMaterial.ICE, 20)
                    BlockerType.CHAINED_HEART -> triggerBubblePop(event.coord)
                    else -> triggerBurst(event.coord, Color(0xFFFF5252), 20)
                }
                soundManager.playSound(SoundEffect.BLOCKER_DESTROY)
                soundManager.haptic(HapticEffect.BLOCKER_DESTROY)
            }
            is EngineEvent.GameWon -> {
                if (rewarded) return
                rewarded = true
                val completedState = engine.getState()
                val (coinsAwarded, milestone) = playerRepository.completeLevel(completedState.levelId, event.stars, event.finalScore)
                refreshProfile()

                resultJob = viewModelScope.launch {
                    // Keep the board visible until score popups from the final match/cascade finish.
                    withTimeoutOrNull(1_800) {
                        _scorePopups.first { it.isEmpty() }
                    }
                    delay(200)
                    soundManager.playSound(SoundEffect.VICTORY)
                    soundManager.haptic(HapticEffect.VICTORY)
                    _victoryData.value = VictoryData(
                        levelId = engine.getState().levelId,
                        score = event.finalScore,
                        stars = event.stars,
                        coinsEarned = coinsAwarded,
                        breakdown = completedState.scoreBreakdown,
                        milestone = milestone
                    )
                }
            }
            is EngineEvent.GameOver -> {
                resultJob = viewModelScope.launch {
                    delay(500)
                    soundManager.playSound(SoundEffect.GAME_OVER)
                    soundManager.haptic(HapticEffect.GAME_OVER)
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
            if (_selectedCoord.value != null || _activeBooster.value != null || _isPaused.value || _currentScreen.value != Screen.Gameplay || engine.getState().status != GameStatus.READY_FOR_INPUT) return@launch
            val possible = engine.getRankedMoves()
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

    private fun triggerDebris(coord: Coord, material: DebrisMaterial, count: Int) {
        val board = _gameState.value?.board ?: return
        val cellSize = 300f / maxOf(board.cols, 1)
        val center = Offset((coord.col + 0.5f) * cellSize, (coord.row + 0.5f) * cellSize)
        val baseColor = when (material) {
            DebrisMaterial.WOOD -> Color(0xFFAA682F)
            DebrisMaterial.STONE -> Color(0xFF777781)
            DebrisMaterial.ICE -> Color(0xFF9DEBFF)
        }
        val burst = ParticleBurst(
            origin = center,
            color = baseColor,
            particles = createDebrisParticles(center, material, count)
        )
        _particleBursts.value = _particleBursts.value + burst
    }

    private fun triggerBubblePop(coord: Coord) {
        val board = _gameState.value?.board ?: return
        val cellSize = 300f / maxOf(board.cols, 1)
        val center = Offset((coord.col + 0.5f) * cellSize, (coord.row + 0.5f) * cellSize)
        val color = Color(0xFFBFE6FF)
        val burst = ParticleBurst(
            origin = center,
            color = color,
            particles = createBubblePopParticles(center),
            durationMs = 420
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

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
