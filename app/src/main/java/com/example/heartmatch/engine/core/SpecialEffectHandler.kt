package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.EngineEvent
import com.example.heartmatch.engine.model.FireDirection
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialCombinationType
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.engine.model.Tile

class SpecialEffectHandler(
    private val rng: DeterministicRng = DeterministicRng(),
    val defaultBombRadius: Int = 1
) {

    data class SpecialActivationResult(
        val clearedCoords: Set<Coord>,
        val triggeredEvents: List<EngineEvent.SpecialTriggered>,
        val bonusScore: Int = 0,
        val giftHeartsClearedCount: Int = 0
    )

    fun isSpecialInteraction(tileA: Tile?, tileB: Tile?): Boolean {
        if (tileA == null || tileB == null) return false
        if (tileA is Tile.Special && tileB is Tile.Special) return true
        if (tileA is Tile.Special && tileA.specialType == SpecialHeartType.RAINBOW_HEART) return true
        if (tileB is Tile.Special && tileB.specialType == SpecialHeartType.RAINBOW_HEART) return true
        return false
    }

    fun getCombinationType(tileA: Tile, tileB: Tile): SpecialCombinationType? {
        if (tileA is Tile.Special && tileB is Tile.Special) {
            val typeA = tileA.specialType
            val typeB = tileB.specialType
            return when {
                typeA == SpecialHeartType.RAINBOW_HEART && typeB == SpecialHeartType.RAINBOW_HEART ->
                    SpecialCombinationType.RAINBOW_RAINBOW

                (typeA == SpecialHeartType.RAINBOW_HEART && typeB == SpecialHeartType.FIRE_HEART) ||
                (typeA == SpecialHeartType.FIRE_HEART && typeB == SpecialHeartType.RAINBOW_HEART) ->
                    SpecialCombinationType.RAINBOW_FIRE

                (typeA == SpecialHeartType.RAINBOW_HEART && typeB == SpecialHeartType.BOMB_HEART) ||
                (typeA == SpecialHeartType.BOMB_HEART && typeB == SpecialHeartType.RAINBOW_HEART) ->
                    SpecialCombinationType.RAINBOW_BOMB

                typeA == SpecialHeartType.FIRE_HEART && typeB == SpecialHeartType.FIRE_HEART ->
                    SpecialCombinationType.FIRE_FIRE

                (typeA == SpecialHeartType.FIRE_HEART && typeB == SpecialHeartType.BOMB_HEART) ||
                (typeA == SpecialHeartType.BOMB_HEART && typeB == SpecialHeartType.FIRE_HEART) ->
                    SpecialCombinationType.FIRE_BOMB

                typeA == SpecialHeartType.BOMB_HEART && typeB == SpecialHeartType.BOMB_HEART ->
                    SpecialCombinationType.BOMB_BOMB

                else -> SpecialCombinationType.GENERIC_SPECIAL_SWAP
            }
        } else if ((tileA is Tile.Special && tileA.specialType == SpecialHeartType.RAINBOW_HEART) ||
                   (tileB is Tile.Special && tileB.specialType == SpecialHeartType.RAINBOW_HEART)) {
            return SpecialCombinationType.RAINBOW_COLOR
        }
        return null
    }

    fun handleSpecialSwap(
        board: Board,
        from: Coord,
        to: Coord,
        tileA: Tile,
        tileB: Tile
    ): SpecialActivationResult {
        val comboType = getCombinationType(tileA, tileB)
        return when (comboType) {
            SpecialCombinationType.RAINBOW_COLOR -> {
                val rainbowTile = (if (tileA is Tile.Special && tileA.specialType == SpecialHeartType.RAINBOW_HEART) tileA else tileB) as Tile.Special
                val otherTile = if (tileA === rainbowTile) tileB else tileA
                val rainbowCoord = if (tileA === rainbowTile) from else to
                val otherCoord = if (tileA === rainbowTile) to else from
                resolveRainbowColor(board, rainbowCoord, otherCoord, rainbowTile, otherTile)
            }
            SpecialCombinationType.RAINBOW_FIRE -> {
                val rainbowTile = (if ((tileA as Tile.Special).specialType == SpecialHeartType.RAINBOW_HEART) tileA else tileB) as Tile.Special
                val fireTile = (if (tileA === rainbowTile) tileB else tileA) as Tile.Special
                val rainbowCoord = if (tileA === rainbowTile) from else to
                val fireCoord = if (tileA === rainbowTile) to else from
                resolveRainbowFire(board, rainbowCoord, fireCoord, rainbowTile, fireTile)
            }
            SpecialCombinationType.RAINBOW_BOMB -> {
                val rainbowTile = (if ((tileA as Tile.Special).specialType == SpecialHeartType.RAINBOW_HEART) tileA else tileB) as Tile.Special
                val bombTile = (if (tileA === rainbowTile) tileB else tileA) as Tile.Special
                val rainbowCoord = if (tileA === rainbowTile) from else to
                val bombCoord = if (tileA === rainbowTile) to else from
                resolveRainbowBomb(board, rainbowCoord, bombCoord, rainbowTile, bombTile)
            }
            SpecialCombinationType.RAINBOW_RAINBOW -> {
                resolveRainbowRainbow(board, from, to, tileA as Tile.Special, tileB as Tile.Special)
            }
            SpecialCombinationType.FIRE_FIRE -> {
                resolveFireFire(board, from, to, tileA as Tile.Special, tileB as Tile.Special)
            }
            SpecialCombinationType.FIRE_BOMB -> {
                val fireTile = (if ((tileA as Tile.Special).specialType == SpecialHeartType.FIRE_HEART) tileA else tileB) as Tile.Special
                val bombTile = (if (tileA === fireTile) tileB else tileA) as Tile.Special
                resolveFireBomb(board, from, to, fireTile, bombTile)
            }
            SpecialCombinationType.BOMB_BOMB -> {
                resolveBombBomb(board, from, to, tileA as Tile.Special, tileB as Tile.Special)
            }
            SpecialCombinationType.GENERIC_SPECIAL_SWAP, null -> {
                resolveGenericSpecialSwap(board, from, to, tileA, tileB)
            }
        }
    }

    /**
     * RAINBOW + COLOR: Clears all tiles of that color from the board.
     */
    fun resolveRainbowColor(
        board: Board,
        rainbowCoord: Coord,
        colorCoord: Coord,
        rainbowTile: Tile.Special,
        otherTile: Tile
    ): SpecialActivationResult {
        val targetColor = otherTile.matchColor ?: findTargetColorForRainbow(board)
        val allCleared = mutableSetOf<Coord>()
        val events = mutableListOf<EngineEvent.SpecialTriggered>()
        val pendingSpecials = ArrayDeque<Pair<Coord, Tile.Special>>()

        val targetCoords = collectCoordsOfColor(board, targetColor)
        val affected = targetCoords.toMutableSet()
        affected.add(rainbowCoord)
        affected.add(colorCoord)

        allCleared.addAll(affected)
        events.add(EngineEvent.SpecialTriggered(colorCoord, SpecialHeartType.RAINBOW_HEART, affected))

        // Queue any specials caught among the matched color tiles
        for (coord in affected) {
            val tile = board.getTile(coord)
            if (tile is Tile.Special && coord != rainbowCoord) {
                pendingSpecials.add(coord to tile)
            }
        }

        val chainResult = resolvePendingSpecials(board, pendingSpecials, allCleared)
        allCleared.addAll(chainResult.clearedCoords)
        events.addAll(chainResult.triggeredEvents)

        return SpecialActivationResult(
            clearedCoords = allCleared,
            triggeredEvents = events,
            bonusScore = chainResult.bonusScore,
            giftHeartsClearedCount = chainResult.giftHeartsClearedCount
        )
    }

    /**
     * RAINBOW + FIRE: Converts all hearts of the target color to Fire Hearts and triggers all of them.
     */
    fun resolveRainbowFire(
        board: Board,
        rainbowCoord: Coord,
        fireCoord: Coord,
        rainbowTile: Tile.Special,
        fireTile: Tile.Special
    ): SpecialActivationResult {
        val targetColor = fireTile.baseColor ?: findTargetColorForRainbow(board)
        val allCleared = mutableSetOf<Coord>()
        val events = mutableListOf<EngineEvent.SpecialTriggered>()
        val pendingSpecials = ArrayDeque<Pair<Coord, Tile.Special>>()

        val convertedCoords = convertColorToSpecial(
            board = board,
            color = targetColor,
            specialType = SpecialHeartType.FIRE_HEART,
            defaultFireDirection = fireTile.fireDirection ?: FireDirection.BOTH
        )

        allCleared.add(rainbowCoord)
        allCleared.add(fireCoord)
        events.add(EngineEvent.SpecialTriggered(fireCoord, SpecialHeartType.RAINBOW_HEART, convertedCoords.toSet()))

        convertedCoords.forEach { coord ->
            (board.getTile(coord) as? Tile.Special)?.let { pendingSpecials.add(coord to it) }
        }

        val chainResult = resolvePendingSpecials(board, pendingSpecials, allCleared)
        allCleared.addAll(chainResult.clearedCoords)
        events.addAll(chainResult.triggeredEvents)

        return SpecialActivationResult(
            clearedCoords = allCleared,
            triggeredEvents = events,
            bonusScore = chainResult.bonusScore,
            giftHeartsClearedCount = chainResult.giftHeartsClearedCount
        )
    }

    /**
     * RAINBOW + BOMB: Converts all hearts of the target color to Bomb Hearts and triggers all of them.
     */
    fun resolveRainbowBomb(
        board: Board,
        rainbowCoord: Coord,
        bombCoord: Coord,
        rainbowTile: Tile.Special,
        bombTile: Tile.Special
    ): SpecialActivationResult {
        val targetColor = bombTile.baseColor ?: findTargetColorForRainbow(board)
        val allCleared = mutableSetOf<Coord>()
        val events = mutableListOf<EngineEvent.SpecialTriggered>()
        val pendingSpecials = ArrayDeque<Pair<Coord, Tile.Special>>()

        val convertedCoords = convertColorToSpecial(
            board = board,
            color = targetColor,
            specialType = SpecialHeartType.BOMB_HEART,
            bombRadius = bombTile.bombRadius
        )

        allCleared.add(rainbowCoord)
        allCleared.add(bombCoord)
        events.add(EngineEvent.SpecialTriggered(bombCoord, SpecialHeartType.RAINBOW_HEART, convertedCoords.toSet()))

        convertedCoords.forEach { coord ->
            (board.getTile(coord) as? Tile.Special)?.let { pendingSpecials.add(coord to it) }
        }

        val chainResult = resolvePendingSpecials(board, pendingSpecials, allCleared)
        allCleared.addAll(chainResult.clearedCoords)
        events.addAll(chainResult.triggeredEvents)

        return SpecialActivationResult(
            clearedCoords = allCleared,
            triggeredEvents = events,
            bonusScore = chainResult.bonusScore,
            giftHeartsClearedCount = chainResult.giftHeartsClearedCount
        )
    }

    /**
     * RAINBOW + RAINBOW: Clears the entire playable board.
     */
    fun resolveRainbowRainbow(
        board: Board,
        from: Coord,
        to: Coord,
        rainbowA: Tile.Special,
        rainbowB: Tile.Special
    ): SpecialActivationResult {
        val fullBoard = board.getAllPlayableCoords().toSet()
        val events = listOf(EngineEvent.SpecialTriggered(to, SpecialHeartType.RAINBOW_HEART, fullBoard))
        var giftCount = 0
        for (coord in fullBoard) {
            val tile = board.getTile(coord)
            if (tile is Tile.Special && tile.specialType == SpecialHeartType.GIFT_HEART) {
                giftCount++
            }
        }
        return SpecialActivationResult(
            clearedCoords = fullBoard,
            triggeredEvents = events,
            bonusScore = giftCount * 1000,
            giftHeartsClearedCount = giftCount
        )
    }

    /**
     * FIRE + FIRE: Clears 3 full rows and 3 full columns centered on the target.
     */
    fun resolveFireFire(
        board: Board,
        from: Coord,
        to: Coord,
        fireA: Tile.Special,
        fireB: Tile.Special
    ): SpecialActivationResult {
        val affected = mutableSetOf<Coord>()
        for (r in maxOf(0, to.row - 1)..minOf(board.rows - 1, to.row + 1)) {
            for (c in 0 until board.cols) {
                val crd = Coord(r, c)
                if (board[crd]?.isPlayable == true) affected.add(crd)
            }
        }
        for (c in maxOf(0, to.col - 1)..minOf(board.cols - 1, to.col + 1)) {
            for (r in 0 until board.rows) {
                val crd = Coord(r, c)
                if (board[crd]?.isPlayable == true) affected.add(crd)
            }
        }

        val allCleared = affected.toMutableSet()
        val events = mutableListOf(EngineEvent.SpecialTriggered(to, SpecialHeartType.FIRE_HEART, affected))
        val pendingSpecials = ArrayDeque<Pair<Coord, Tile.Special>>()

        for (coord in affected) {
            if (coord != from && coord != to) {
                val tile = board.getTile(coord)
                if (tile is Tile.Special) {
                    pendingSpecials.add(coord to tile)
                }
            }
        }

        val chainResult = resolvePendingSpecials(board, pendingSpecials, allCleared)
        allCleared.addAll(chainResult.clearedCoords)
        events.addAll(chainResult.triggeredEvents)

        return SpecialActivationResult(
            clearedCoords = allCleared,
            triggeredEvents = events,
            bonusScore = chainResult.bonusScore,
            giftHeartsClearedCount = chainResult.giftHeartsClearedCount
        )
    }

    /**
     * FIRE + BOMB: Clears a 3-wide cross (3 full rows and 3 full columns).
     */
    fun resolveFireBomb(
        board: Board,
        from: Coord,
        to: Coord,
        fireTile: Tile.Special,
        bombTile: Tile.Special
    ): SpecialActivationResult {
        val affected = mutableSetOf<Coord>()
        for (r in maxOf(0, to.row - 1)..minOf(board.rows - 1, to.row + 1)) {
            for (c in 0 until board.cols) {
                val crd = Coord(r, c)
                if (board[crd]?.isPlayable == true) affected.add(crd)
            }
        }
        for (c in maxOf(0, to.col - 1)..minOf(board.cols - 1, to.col + 1)) {
            for (r in 0 until board.rows) {
                val crd = Coord(r, c)
                if (board[crd]?.isPlayable == true) affected.add(crd)
            }
        }

        val allCleared = affected.toMutableSet()
        val events = mutableListOf(EngineEvent.SpecialTriggered(to, SpecialHeartType.BOMB_HEART, affected))
        val pendingSpecials = ArrayDeque<Pair<Coord, Tile.Special>>()

        for (coord in affected) {
            if (coord != from && coord != to) {
                val tile = board.getTile(coord)
                if (tile is Tile.Special) {
                    pendingSpecials.add(coord to tile)
                }
            }
        }

        val chainResult = resolvePendingSpecials(board, pendingSpecials, allCleared)
        allCleared.addAll(chainResult.clearedCoords)
        events.addAll(chainResult.triggeredEvents)

        return SpecialActivationResult(
            clearedCoords = allCleared,
            triggeredEvents = events,
            bonusScore = chainResult.bonusScore,
            giftHeartsClearedCount = chainResult.giftHeartsClearedCount
        )
    }

    /**
     * BOMB + BOMB: Creates a 5x5 explosion centered on the target coordinate.
     */
    fun resolveBombBomb(
        board: Board,
        from: Coord,
        to: Coord,
        bombA: Tile.Special,
        bombB: Tile.Special
    ): SpecialActivationResult {
        val radius = maxOf(2, bombA.bombRadius + 1, bombB.bombRadius + 1)
        val affected = mutableSetOf<Coord>()
        for (r in (to.row - radius)..(to.row + radius)) {
            for (c in (to.col - radius)..(to.col + radius)) {
                val coord = Coord(r, c)
                if (board.isValid(coord) && board[coord]?.isPlayable == true) {
                    affected.add(coord)
                }
            }
        }

        val allCleared = affected.toMutableSet()
        val events = mutableListOf(EngineEvent.SpecialTriggered(to, SpecialHeartType.BOMB_HEART, affected))
        val pendingSpecials = ArrayDeque<Pair<Coord, Tile.Special>>()

        for (coord in affected) {
            if (coord != from && coord != to) {
                val tile = board.getTile(coord)
                if (tile is Tile.Special) {
                    pendingSpecials.add(coord to tile)
                }
            }
        }

        val chainResult = resolvePendingSpecials(board, pendingSpecials, allCleared)
        allCleared.addAll(chainResult.clearedCoords)
        events.addAll(chainResult.triggeredEvents)

        return SpecialActivationResult(
            clearedCoords = allCleared,
            triggeredEvents = events,
            bonusScore = chainResult.bonusScore,
            giftHeartsClearedCount = chainResult.giftHeartsClearedCount
        )
    }

    private fun resolveGenericSpecialSwap(
        board: Board,
        from: Coord,
        to: Coord,
        tileA: Tile,
        tileB: Tile
    ): SpecialActivationResult {
        val pending = ArrayDeque<Pair<Coord, Tile.Special>>()
        if (tileA is Tile.Special) pending.add(from to tileA)
        if (tileB is Tile.Special) pending.add(to to tileB)
        return resolvePendingSpecials(board, pending, mutableSetOf(from, to))
    }

    fun triggerSpecials(
        board: Board,
        initialTriggers: List<Pair<Coord, Tile.Special>>,
        alreadyCleared: Set<Coord> = emptySet()
    ): SpecialActivationResult {
        val pending = ArrayDeque(initialTriggers)
        return resolvePendingSpecials(board, pending, alreadyCleared.toMutableSet())
    }

    private fun resolvePendingSpecials(
        board: Board,
        queue: ArrayDeque<Pair<Coord, Tile.Special>>,
        clearedSoFar: MutableSet<Coord>
    ): SpecialActivationResult {
        val events = mutableListOf<EngineEvent.SpecialTriggered>()
        val processedSpecialCoords = mutableSetOf<Coord>()
        var bonusScore = 0
        var giftHeartsCount = 0

        while (queue.isNotEmpty()) {
            val (coord, special) = queue.removeFirst()
            if (coord in processedSpecialCoords) continue
            processedSpecialCoords.add(coord)
            clearedSoFar.add(coord)

            val affected = when (special.specialType) {
                SpecialHeartType.FIRE_HEART -> {
                    val set = mutableSetOf<Coord>()
                    val direction = special.fireDirection ?: FireDirection.BOTH
                    if (direction == FireDirection.ROW || direction == FireDirection.BOTH) {
                        for (c in 0 until board.cols) {
                            val crd = Coord(coord.row, c)
                            if (board[crd]?.isPlayable == true) set.add(crd)
                        }
                    }
                    if (direction == FireDirection.COLUMN || direction == FireDirection.BOTH) {
                        for (r in 0 until board.rows) {
                            val crd = Coord(r, coord.col)
                            if (board[crd]?.isPlayable == true) set.add(crd)
                        }
                    }
                    set
                }

                SpecialHeartType.BOMB_HEART -> {
                    val radius = if (special.bombRadius > 0) special.bombRadius else defaultBombRadius
                    val set = mutableSetOf<Coord>()
                    for (r in (coord.row - radius)..(coord.row + radius)) {
                        for (c in (coord.col - radius)..(coord.col + radius)) {
                            val crd = Coord(r, c)
                            if (board.isValid(crd) && board[crd]?.isPlayable == true) {
                                set.add(crd)
                            }
                        }
                    }
                    set
                }

                SpecialHeartType.RAINBOW_HEART -> {
                    val color = special.baseColor ?: findTargetColorForRainbow(board)
                    val set = collectCoordsOfColor(board, color).toMutableSet()
                    set.add(coord)
                    set
                }

                SpecialHeartType.GIFT_HEART -> {
                    // Gift Heart: clears a 3x3 diamond / cross plus 2 random bonus tiles and awards bonus points
                    giftHeartsCount++
                    bonusScore += 1000
                    val set = mutableSetOf<Coord>()
                    set.add(coord)
                    set.addAll(coord.orthogonalNeighbors().filter { board.isValid(it) && board[it]?.isPlayable == true })
                    val otherPlayable = board.getAllPlayableCoords().filter { it !in set && it !in clearedSoFar }
                    if (otherPlayable.isNotEmpty()) {
                        val sampleCount = minOf(2, otherPlayable.size)
                        val shuffled = otherPlayable.shuffled()
                        for (i in 0 until sampleCount) {
                            set.add(shuffled[i])
                        }
                    }
                    set
                }

                SpecialHeartType.ROYAL_HEART -> {
                    // Royal Crowned Heart: Clears a 3x3 royal diamond + full cross, granting regal score bonus
                    bonusScore += 1500
                    val set = mutableSetOf<Coord>()
                    for (r in (coord.row - 1)..(coord.row + 1)) {
                        for (c in (coord.col - 1)..(coord.col + 1)) {
                            val crd = Coord(r, c)
                            if (board.isValid(crd) && board[crd]?.isPlayable == true) set.add(crd)
                        }
                    }
                    for (c in 0 until board.cols) {
                        val crd = Coord(coord.row, c)
                        if (board[crd]?.isPlayable == true) set.add(crd)
                    }
                    for (r in 0 until board.rows) {
                        val crd = Coord(r, coord.col)
                        if (board[crd]?.isPlayable == true) set.add(crd)
                    }
                    set
                }

                SpecialHeartType.ANGEL_HEART -> {
                    // Winged Angel Heart: Clears blockers and awards divine blessings across up to 5 tiles
                    bonusScore += 1200
                    val set = mutableSetOf<Coord>()
                    set.add(coord)
                    set.addAll(coord.orthogonalNeighbors().filter { board.isValid(it) && board[it]?.isPlayable == true })
                    // Target blockers on board
                    val blockersOnBoard = mutableListOf<Coord>()
                    board.forEachCell { cell ->
                        if (cell.isPlayable && cell.tile is Tile.Blocker && cell.coord !in set) {
                            blockersOnBoard.add(cell.coord)
                        }
                    }
                    blockersOnBoard.take(5).forEach { set.add(it) }
                    set
                }
            }

            clearedSoFar.addAll(affected)
            events.add(EngineEvent.SpecialTriggered(coord, special.specialType, affected))

            // Check if any other specials were hit in this blast
            for (affCoord in affected) {
                if (affCoord !in processedSpecialCoords) {
                    val hitTile = board.getTile(affCoord)
                    if (hitTile is Tile.Special) {
                        queue.add(affCoord to hitTile)
                    }
                }
            }
        }

        return SpecialActivationResult(
            clearedCoords = clearedSoFar,
            triggeredEvents = events,
            bonusScore = bonusScore,
            giftHeartsClearedCount = giftHeartsCount
        )
    }

    fun findTargetColorForRainbow(board: Board): HeartColor {
        val colorCounts = mutableMapOf<HeartColor, Int>()
        board.forEachCell { cell ->
            val color = cell.tile?.matchColor
            if (color != null) {
                colorCounts[color] = (colorCounts[color] ?: 0) + 1
            }
        }
        return colorCounts.maxByOrNull { it.value }?.key ?: HeartColor.RED
    }

    fun collectCoordsOfColor(board: Board, color: HeartColor): List<Coord> {
        val result = mutableListOf<Coord>()
        board.forEachCell { cell ->
            if (cell.isPlayable && cell.tile?.matchColor == color) {
                result.add(cell.coord)
            }
        }
        return result
    }

    private fun convertColorToSpecial(
        board: Board,
        color: HeartColor,
        specialType: SpecialHeartType,
        defaultFireDirection: FireDirection = FireDirection.BOTH,
        bombRadius: Int = defaultBombRadius
    ): List<Coord> {
        val coords = collectCoordsOfColor(board, color)
        coords.forEach { coord ->
            board.setTile(
                coord,
                Tile.Special(
                    specialType = specialType,
                    baseColor = color,
                    fireDirection = defaultFireDirection,
                    bombRadius = bombRadius
                )
            )
        }
        return coords
    }
}
