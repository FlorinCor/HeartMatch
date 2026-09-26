package com.example.heartmatch.ui.components

import com.example.heartmatch.ui.theme.GardenPalette
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.VectorConverter
import com.example.heartmatch.ui.animation.BoardAnimationTimings
import com.example.heartmatch.ui.animation.BoardDisplayState
import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BoardView(
    board: Board,
    displayState: BoardDisplayState,
    selectedCoord: Coord?,
    hintedCoords: List<Coord>,
    activeBooster: String?,
    particleBursts: List<ParticleBurst>,
    scorePopups: List<ScorePopupData>,
    blastWaves: List<BlastWaveData>,
    laserBeams: List<LaserBeamData>,
    comboBanner: ComboBannerData?,
    onSwap: (Coord, Coord) -> Unit,
    onCellClick: (Coord) -> Unit,
    onBoosterTarget: (Coord) -> Unit,
    onParticleFinished: (Long) -> Unit,
    onScorePopupFinished: (Long) -> Unit,
    onBlastWaveFinished: (Long) -> Unit,
    onLaserFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val board = displayState.board ?: board
    val rows = board.rows
    val cols = board.cols

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rows.toFloat())
            .padding(12.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GardenPalette.Panel,
                        GardenPalette.Background
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GardenPalette.Rim,
                        GardenPalette.PanelLight
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        val boardWidthPx = constraints.maxWidth.toFloat()
        val boardHeightPx = constraints.maxHeight.toFloat()
        val cellWidthPx = boardWidthPx / cols
        val cellHeightPx = boardHeightPx / rows
        val cellSizePx = minOf(cellWidthPx, cellHeightPx)

        val density = LocalDensity.current
        val cellSizeDp = with(density) { cellSizePx.toDp() }

        var dragStartCoord by remember { mutableStateOf<Coord?>(null) }
        var accumulatedDragX by remember { mutableFloatStateOf(0f) }
        var accumulatedDragY by remember { mutableFloatStateOf(0f) }
        var hasSwappedDuringDrag by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(board, activeBooster) {
                    detectTapGestures { offset ->
                        val col = (offset.x / cellSizePx).toInt().coerceIn(0, cols - 1)
                        val row = (offset.y / cellSizePx).toInt().coerceIn(0, rows - 1)
                        val coord = Coord(row, col)
                        val cell = board[coord]
                        if (cell != null && cell.isPlayable) {
                            if (activeBooster != null) {
                                onBoosterTarget(coord)
                            } else {
                                onCellClick(coord)
                            }
                        }
                    }
                }
                .pointerInput(board, activeBooster) {
                    if (activeBooster == null) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val col = (offset.x / cellSizePx).toInt().coerceIn(0, cols - 1)
                                val row = (offset.y / cellSizePx).toInt().coerceIn(0, rows - 1)
                                val coord = Coord(row, col)
                                val cell = board[coord]
                                if (cell != null && cell.isPlayable && cell.tile?.isMovable == true) {
                                    dragStartCoord = coord
                                    accumulatedDragX = 0f
                                    accumulatedDragY = 0f
                                    hasSwappedDuringDrag = false
                                } else {
                                    dragStartCoord = null
                                }
                            },
                            onDragEnd = {
                                dragStartCoord = null
                                hasSwappedDuringDrag = false
                            },
                            onDragCancel = {
                                dragStartCoord = null
                                hasSwappedDuringDrag = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val start = dragStartCoord
                                if (start != null && !hasSwappedDuringDrag) {
                                    accumulatedDragX += dragAmount.x
                                    accumulatedDragY += dragAmount.y
                                    val threshold = cellSizePx * 0.35f

                                    val targetCoord: Coord? = when {
                                        accumulatedDragX > threshold -> Coord(start.row, start.col + 1)
                                        accumulatedDragX < -threshold -> Coord(start.row, start.col - 1)
                                        accumulatedDragY > threshold -> Coord(start.row + 1, start.col)
                                        accumulatedDragY < -threshold -> Coord(start.row - 1, start.col)
                                        else -> null
                                    }

                                    if (targetCoord != null && board.isValid(targetCoord) && board[targetCoord]?.isPlayable == true) {
                                        hasSwappedDuringDrag = true
                                        onSwap(start, targetCoord)
                                    }
                                }
                            }
                        )
                    }
                }
        ) {
            // 1. Grid Background Tiles
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val coord = Coord(r, c)
                    val cell = board[coord]
                    if (cell != null && cell.isPlayable) {
                        val isEven = (r + c) % 2 == 0
                        val cellTop = if (isEven) GardenPalette.CellLight else GardenPalette.CellLight
                        val cellBottom = if (isEven) GardenPalette.CellDark else GardenPalette.CellDark

                        Box(
                            modifier = Modifier
                                .offset { IntOffset((c * cellSizePx).roundToInt(), (r * cellSizePx).roundToInt()) }
                                .size(cellSizeDp)
                                .padding(1.5.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(cellTop, cellBottom)
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.08f),
                                            Color.Black.copy(alpha = 0.15f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(7.dp)
                                )
                        )
                    }
                }
            }

            // Keep tile identity keyed across the whole board so each heart visibly travels
            // from its old cell to its new one instead of being redrawn in place.
            val positionedTiles = buildList {
                board.forEachCell { cell ->
                    cell.tile?.let { tile -> if (cell.isPlayable) add(cell.coord to tile) }
                }
            }
            for ((coord, tile) in positionedTiles) key(tile.id) {
                val targetOffset = Offset(coord.col * cellSizePx, coord.row * cellSizePx)
                val spawnRow = displayState.fallOrigins[tile.id]
                val animatedOffset = remember(tile.id) {
                    Animatable(
                        Offset(targetOffset.x, (spawnRow ?: coord.row) * cellSizePx),
                        Offset.VectorConverter
                    )
                }
                LaunchedEffect(displayState.version, coord) {
                    animatedOffset.animateTo(
                        targetOffset,
                        spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                    )
                }

                val isVanishing = coord in displayState.vanishingCoords
                val alpha by animateFloatAsState(
                    targetValue = if (isVanishing) 0f else 1f,
                    animationSpec = tween(BoardAnimationTimings.VANISH_MS, easing = FastOutSlowInEasing),
                    label = "tileFade-${tile.id}"
                )
                val vanishScale by animateFloatAsState(
                    targetValue = if (isVanishing) 0.15f else 1f,
                    animationSpec = tween(BoardAnimationTimings.VANISH_MS, easing = FastOutSlowInEasing),
                    label = "tileVanish-${tile.id}"
                )
                val isSwapping = coord == displayState.swapFrom || coord == displayState.swapTo
                val swapScale by animateFloatAsState(
                    targetValue = if (isSwapping) 1.14f else 1f,
                    animationSpec = tween(BoardAnimationTimings.SWAP_MS, easing = FastOutSlowInEasing),
                    label = "tileSwap-${tile.id}"
                )
                // Newly appearing hearts should use the same full cell size as every other
                // heart. Keep the pop-in bookkeeping for transition timing, but don't shrink
                // the tile when it first appears.
                val tileScale = vanishScale * swapScale

                Box(
                    modifier = Modifier
                        .offset { IntOffset(animatedOffset.value.x.roundToInt(), animatedOffset.value.y.roundToInt()) }
                        .size(cellSizeDp)
                        .padding(2.dp)
                        .zIndex(if (isSwapping) 1f else 0f)
                        .graphicsLayer {
                            this.alpha = alpha
                            scaleX = tileScale
                            scaleY = tileScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    TileView(
                        tile = tile,
                        isSelected = selectedCoord == coord,
                        isHinted = hintedCoords.contains(coord),
                        isHealed = coord in displayState.healedCoords
                    )
                }
            }

            // 3. Visual Effects Overlays
            // Laser beams
            laserBeams.forEach { laser ->
                LaserBeamEffect(
                    laser = laser,
                    cellSizePx = cellSizePx,
                    onFinished = { onLaserFinished(laser.id) }
                )
            }

            // Blast waves
            blastWaves.forEach { blast ->
                BlastWaveEffect(
                    blast = blast,
                    onFinished = { onBlastWaveFinished(blast.id) }
                )
            }

            // Particle bursts
            particleBursts.forEach { burst ->
                ParticleBurstEffect(
                    burst = burst,
                    onFinished = { onParticleFinished(burst.id) }
                )
            }

            // Score popups
            scorePopups.forEach { popup ->
                ScorePopupItem(
                    popup = popup,
                    onFinished = { onScorePopupFinished(popup.id) }
                )
            }

            // Combo Banner
            ComboBannerOverlay(
                banner = comboBanner,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
