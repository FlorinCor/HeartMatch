package com.example.heartmatch.ui.animation

import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.Tile
import kotlin.math.max

/**
 * Timings (in milliseconds) shared by the view model (which paces the turn replay)
 * and the board UI (which animates the tiles between two published states).
 */
object BoardAnimationTimings {
    const val SWAP_MS = 320
    const val VANISH_MS = 360
    const val SETTLE_MS = 80
    const val DROP_BASE_MS = 200
    const val DROP_PER_ROW_MS = 60
    const val LANDING_MS = 220
    const val POP_IN_MS = 260

    fun dropDurationMs(rows: Int): Int = DROP_BASE_MS + DROP_PER_ROW_MS * rows.coerceAtLeast(1)
}

/**
 * What the board should currently show. The [board] is a snapshot that is replaced on every
 * animation step; the remaining fields tell the UI how to get there from the previous snapshot.
 */
data class BoardDisplayState(
    val board: Board? = null,
    val version: Long = 0L,
    /** Tiles at these coords are shrinking/fading out and will be gone in the next snapshot. */
    val vanishingCoords: Set<Coord> = emptySet(),
    /** Coordinates involved in a swap; the tile that now sits at [swapTo] is the one that was moved. */
    val swapFrom: Coord? = null,
    val swapTo: Coord? = null,
    /** Tile id -> (virtual, possibly negative) row the freshly spawned tile starts falling from. */
    val fallOrigins: Map<String, Int> = emptyMap(),
    /** Tile ids that appear in place with a pop-in (created specials, revealed payloads, mid-column spawns). */
    val popInIds: Set<String> = emptySet()
)

/** One paced step of a turn replay. */
sealed interface TurnStep {
    val board: Board
    val events: List<EngineEvent>
    val durationMs: Long

    /** Two tiles glide into each other's cells. */
    data class Swap(
        override val board: Board,
        val from: Coord,
        val to: Coord,
        val isRollback: Boolean,
        override val events: List<EngineEvent>
    ) : TurnStep {
        override val durationMs: Long = BoardAnimationTimings.SWAP_MS.toLong()
    }

    /** Matched / blasted tiles shrink and fade while still occupying their cells. */
    data class Clear(
        override val board: Board,
        val vanishing: Set<Coord>,
        override val events: List<EngineEvent>
    ) : TurnStep {
        override val durationMs: Long = BoardAnimationTimings.VANISH_MS.toLong()
    }

    /** Cleared cells are now empty; created specials / revealed payloads pop in. */
    data class Settle(
        override val board: Board,
        val popInIds: Set<String>
    ) : TurnStep {
        override val events: List<EngineEvent> = emptyList()
        override val durationMs: Long = BoardAnimationTimings.SETTLE_MS.toLong()
    }

    /** Gravity: existing tiles fall down, new tiles fall in from above the board. */
    data class Drop(
        override val board: Board,
        val fallOrigins: Map<String, Int>,
        val popInIds: Set<String>,
        val maxDistance: Int,
        override val events: List<EngineEvent>
    ) : TurnStep {
        override val durationMs: Long =
            (BoardAnimationTimings.dropDurationMs(maxDistance) + BoardAnimationTimings.LANDING_MS).toLong()
    }

    /** Final sync with the authoritative engine board plus the bookkeeping events. */
    data class Finish(
        override val board: Board,
        override val events: List<EngineEvent>
    ) : TurnStep {
        override val durationMs: Long = 0L
    }
}

/**
 * Turns the flat list of events produced by one engine turn into a sequence of board snapshots
 * that can be shown one after another, so the player can follow what happened: which two hearts
 * were exchanged, which ones disappeared, and where every heart falls from and lands.
 *
 * The replay is reconstructed forward from the pre-turn board. It is a faithful approximation of
 * the engine's intermediate states; the last step always snaps to the real engine board.
 */
class TurnChoreographer {

    fun build(boardBefore: Board, events: List<EngineEvent>, finalBoard: Board): List<TurnStep> {
        val display = boardBefore.clone()
        val steps = mutableListOf<TurnStep>()
        val trailingEvents = mutableListOf<EngineEvent>()

        var i = 0
        while (i < events.size) {
            val event = events[i]
            when {
                event is EngineEvent.Swap -> {
                    display.swap(event.from, event.to)
                    steps += TurnStep.Swap(display.clone(), event.from, event.to, event.isRollback, listOf(event))
                    if (event.isRollback) {
                        display.swap(event.from, event.to)
                        steps += TurnStep.Swap(display.clone(), event.to, event.from, isRollback = true, events = emptyList())
                    }
                    i++
                }

                event.isClearPhaseEvent() -> {
                    val phase = mutableListOf<EngineEvent>()
                    while (i < events.size && events[i].isClearPhaseEvent()) {
                        phase += events[i]
                        i++
                    }
                    val upcomingDrops = mutableListOf<EngineEvent.TileDrop>()
                    var j = i
                    while (j < events.size && events[j] is EngineEvent.TileDrop) {
                        upcomingDrops += events[j] as EngineEvent.TileDrop
                        j++
                    }
                    buildClearSteps(display, phase, upcomingDrops, finalBoard, steps)
                }

                event is EngineEvent.TileDrop -> {
                    val drops = mutableListOf<EngineEvent.TileDrop>()
                    while (i < events.size && events[i] is EngineEvent.TileDrop) {
                        drops += events[i] as EngineEvent.TileDrop
                        i++
                    }
                    steps += buildDropStep(display, drops)
                }

                else -> {
                    trailingEvents += event
                    i++
                }
            }
        }

        steps += TurnStep.Finish(finalBoard.clone(), trailingEvents)
        return steps
    }

    private fun EngineEvent.isClearPhaseEvent(): Boolean = when (this) {
        is EngineEvent.Match,
        is EngineEvent.SpecialCreated,
        is EngineEvent.SpecialTriggered,
        is EngineEvent.BlockerDamaged,
        is EngineEvent.BlockerDestroyed -> true
        else -> false
    }

    private fun buildClearSteps(
        display: Board,
        phase: List<EngineEvent>,
        upcomingDrops: List<EngineEvent.TileDrop>,
        finalBoard: Board,
        steps: MutableList<TurnStep>
    ) {
        val destroyedCoords = phase.filterIsInstance<EngineEvent.BlockerDestroyed>().map { it.coord }.toSet()
        val damaged = phase.filterIsInstance<EngineEvent.BlockerDamaged>().filter { !it.isDestroyed }
        val created = phase.filterIsInstance<EngineEvent.SpecialCreated>()
        val matches = phase.filterIsInstance<EngineEvent.Match>()

        val hitCoords = mutableSetOf<Coord>()
        for (event in phase) {
            when (event) {
                is EngineEvent.Match -> hitCoords += event.coords
                is EngineEvent.SpecialTriggered -> {
                    hitCoords += event.coord
                    hitCoords += event.affectedCoords
                }
                is EngineEvent.BlockerDestroyed -> hitCoords += event.coord
                else -> {}
            }
        }

        // Blockers that only lost durability stay on the board; everything else that was hit vanishes.
        val vanishing = hitCoords.filter { coord ->
            val tile = display.getTile(coord)
            tile != null && (tile !is Tile.Blocker || coord in destroyedCoords)
        }.toSet()

        val destroyedBlockers = destroyedCoords.associateWith { display.getTile(it) as? Tile.Blocker }

        steps += TurnStep.Clear(display.clone(), vanishing, phase)

        val popIn = mutableSetOf<String>()
        for (coord in vanishing) {
            display.setTile(coord, null)
        }
        for (event in damaged) {
            val tile = display.getTile(event.coord)
            if (tile is Tile.Blocker) {
                display.setTile(event.coord, tile.copy(durability = event.remainingDurability))
            }
        }
        for (coord in destroyedCoords) {
            val blocker = destroyedBlockers[coord]
            val replacement = resolveTile(coord, upcomingDrops, finalBoard) { it !is Tile.Blocker }
                ?: blocker?.let { defaultBlockerReplacement(it) }
            if (replacement != null) {
                display.setTile(coord, replacement)
                popIn += replacement.id
            }
        }
        for (event in created) {
            val color = matches.firstOrNull { event.coord in it.coords }?.color
            val tile = resolveTile(event.coord, upcomingDrops, finalBoard) {
                it is Tile.Special && it.specialType == event.specialType
            } ?: Tile.Special(specialType = event.specialType, baseColor = color)
            display.setTile(event.coord, tile)
            popIn += tile.id
        }

        if (vanishing.isNotEmpty() || popIn.isNotEmpty()) {
            steps += TurnStep.Settle(display.clone(), popIn)
        }
    }

    /**
     * Finds the real engine instance of the tile that ends up at [coord] after this clear phase:
     * either it falls away in the very next gravity pass, or it is still there on the final board.
     */
    private fun resolveTile(
        coord: Coord,
        upcomingDrops: List<EngineEvent.TileDrop>,
        finalBoard: Board,
        accept: (Tile) -> Boolean
    ): Tile? {
        val dropped = upcomingDrops.firstOrNull { it.from == coord }?.tile
        if (dropped != null && accept(dropped)) return dropped
        val onFinal = finalBoard.getTile(coord)
        if (onFinal != null && accept(onFinal)) return onFinal
        return null
    }

    private fun defaultBlockerReplacement(blocker: Tile.Blocker): Tile? = when (blocker.blockerType) {
        BlockerType.CHAINED_HEART ->
            blocker.payloadTile ?: Tile.Normal(color = blocker.color ?: HeartColor.RED)
        BlockerType.ICE_HEART ->
            blocker.payloadTile ?: blocker.color?.let { Tile.Normal(color = it) }
        BlockerType.BROKEN_HEART,
        BlockerType.STITCHED_HEART ->
            blocker.payloadTile ?: Tile.Normal(color = blocker.color ?: HeartColor.RED)
        else -> null
    }

    private fun buildDropStep(display: Board, drops: List<EngineEvent.TileDrop>): TurnStep.Drop {
        val fallOrigins = mutableMapOf<String, Int>()
        val popIn = mutableSetOf<String>()
        var maxDistance = 0

        val spawnRowsByCol: Map<Int, List<Int>> = drops
            .filter { it.from == null }
            .groupBy({ it.to.col }, { it.to.row })
            .mapValues { (_, rows) -> rows.sorted() }

        for (drop in drops) {
            val from = drop.from
            if (from != null) {
                // Keep the instance already shown at `from` so the tile keeps its identity while it falls.
                val moving = display.getTile(from) ?: drop.tile
                display.setTile(from, null)
                display.setTile(drop.to, moving)
                maxDistance = max(maxDistance, drop.distance)
            } else {
                display.setTile(drop.to, drop.tile)
                val run = contiguousRun(spawnRowsByCol[drop.to.col].orEmpty(), drop.to.row)
                if (isOpenToSky(display, drop.to.col, run.first())) {
                    // Stack the new hearts above the board so they rain down in order.
                    val travel = run.size
                    fallOrigins[drop.tile.id] = drop.to.row - travel
                    maxDistance = max(maxDistance, travel)
                } else {
                    popIn += drop.tile.id
                }
            }
        }

        return TurnStep.Drop(display.clone(), fallOrigins, popIn, maxDistance, drops)
    }

    private fun contiguousRun(sortedRows: List<Int>, row: Int): List<Int> {
        val index = sortedRows.indexOf(row)
        if (index < 0) return listOf(row)
        var start = index
        while (start > 0 && sortedRows[start - 1] == sortedRows[start] - 1) start--
        var end = index
        while (end < sortedRows.size - 1 && sortedRows[end + 1] == sortedRows[end] + 1) end++
        return sortedRows.subList(start, end + 1)
    }

    private fun isOpenToSky(board: Board, col: Int, topRow: Int): Boolean {
        for (r in 0 until topRow) {
            val cell = board[Coord(r, col)] ?: return false
            if (cell.isPlayable) return false
        }
        return true
    }
}
