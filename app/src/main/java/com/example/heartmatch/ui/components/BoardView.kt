package com.example.heartmatch.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BoardView(
    board: Board,
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
    val rows = board.rows
    val cols = board.cols

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rows.toFloat())
            .padding(12.dp)
            .shadow(18.dp, RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3B2652),
                        Color(0xFF261538)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 3.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8B6BAE),
                        Color(0xFF4A3166)
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
                        val cellTop = if (isEven) Color(0xFF5C4675) else Color(0xFF55406E)
                        val cellBottom = if (isEven) Color(0xFF3F2C57) else Color(0xFF3A2852)

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
                                            Color.White.copy(alpha = 0.22f),
                                            Color.Black.copy(alpha = 0.30f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(7.dp)
                                )
                        )
                    }
                }
            }

            // 2. Active Tiles
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val coord = Coord(r, c)
                    val cell = board[coord]
                    val tile = cell?.tile
                    if (cell != null && cell.isPlayable && tile != null) {
                        val isSelected = selectedCoord == coord
                        val isHinted = hintedCoords.contains(coord)

                        Box(
                            modifier = Modifier
                                .offset { IntOffset((c * cellSizePx).roundToInt(), (r * cellSizePx).roundToInt()) }
                                .size(cellSizeDp)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TileView(
                                tile = tile,
                                isSelected = isSelected,
                                isHinted = isHinted
                            )
                        }
                    }
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
