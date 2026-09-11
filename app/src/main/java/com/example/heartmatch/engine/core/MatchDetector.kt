package com.example.heartmatch.engine.core

import com.example.heartmatch.engine.model.Board
import com.example.heartmatch.engine.model.Coord
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.MatchGroup
import com.example.heartmatch.engine.model.MatchShape
import com.example.heartmatch.engine.model.SpecialHeartType

class MatchDetector {

    private data class Span(
        val isHorizontal: Boolean,
        val color: HeartColor,
        val coords: List<Coord>
    ) {
        val size: Int get() = coords.size
        val startCoord: Coord get() = coords.first()
        val endCoord: Coord get() = coords.last()
    }

    fun detectMatches(
        board: Board,
        playerSwappedCoords: Pair<Coord, Coord>? = null
    ): List<MatchGroup> {
        val spans = mutableListOf<Span>()

        // 1. Horizontal scan
        for (r in 0 until board.rows) {
            var currentRun = mutableListOf<Coord>()
            var currentColor: HeartColor? = null

            for (c in 0 until board.cols) {
                val coord = Coord(r, c)
                val tile = board.getTile(coord)
                val color = if (tile != null && tile.isMatchable) tile.matchColor else null

                if (color != null && color == currentColor) {
                    currentRun.add(coord)
                } else {
                    if (currentRun.size >= 3 && currentColor != null) {
                        spans.add(Span(isHorizontal = true, color = currentColor, coords = currentRun.toList()))
                    }
                    currentRun = if (color != null) mutableListOf(coord) else mutableListOf()
                    currentColor = color
                }
            }
            if (currentRun.size >= 3 && currentColor != null) {
                spans.add(Span(isHorizontal = true, color = currentColor, coords = currentRun.toList()))
            }
        }

        // 2. Vertical scan
        for (c in 0 until board.cols) {
            var currentRun = mutableListOf<Coord>()
            var currentColor: HeartColor? = null

            for (r in 0 until board.rows) {
                val coord = Coord(r, c)
                val tile = board.getTile(coord)
                val color = if (tile != null && tile.isMatchable) tile.matchColor else null

                if (color != null && color == currentColor) {
                    currentRun.add(coord)
                } else {
                    if (currentRun.size >= 3 && currentColor != null) {
                        spans.add(Span(isHorizontal = false, color = currentColor, coords = currentRun.toList()))
                    }
                    currentRun = if (color != null) mutableListOf(coord) else mutableListOf()
                    currentColor = color
                }
            }
            if (currentRun.size >= 3 && currentColor != null) {
                spans.add(Span(isHorizontal = false, color = currentColor, coords = currentRun.toList()))
            }
        }

        if (spans.isEmpty()) return emptyList()

        // 3. Cluster overlapping spans of same color
        val clusters = clusterSpans(spans)

        // 4. Convert each cluster to MatchGroup
        return clusters.map { cluster ->
            buildMatchGroup(cluster, playerSwappedCoords)
        }
    }

    private fun clusterSpans(spans: List<Span>): List<List<Span>> {
        val clusters = mutableListOf<MutableList<Span>>()
        val visited = BooleanArray(spans.size)

        for (i in spans.indices) {
            if (visited[i]) continue
            val cluster = mutableListOf<Span>()
            val queue = ArrayDeque<Int>()
            queue.add(i)
            visited[i] = true

            while (queue.isNotEmpty()) {
                val currIdx = queue.removeFirst()
                val currSpan = spans[currIdx]
                cluster.add(currSpan)

                for (j in spans.indices) {
                    if (!visited[j] && spans[j].color == currSpan.color) {
                        // Check if spans share any coordinate
                        if (spans[j].coords.any { it in currSpan.coords }) {
                            visited[j] = true
                            queue.add(j)
                        }
                    }
                }
            }
            clusters.add(cluster)
        }
        return clusters
    }

    private fun buildMatchGroup(
        spans: List<Span>,
        playerSwappedCoords: Pair<Coord, Coord>?
    ): MatchGroup {
        val color = spans.first().color
        val allCoords = spans.flatMap { it.coords }.toSet()

        val hSpans = spans.filter { it.isHorizontal }
        val vSpans = spans.filter { !it.isHorizontal }

        val maxHSize = hSpans.maxOfOrNull { it.size } ?: 0
        val maxVSize = vSpans.maxOfOrNull { it.size } ?: 0
        val maxSpanSize = maxOf(maxHSize, maxVSize)

        // Find intersections between horizontal and vertical spans
        val intersectionCoords = mutableSetOf<Coord>()
        for (h in hSpans) {
            for (v in vSpans) {
                val intersection = h.coords.intersect(v.coords.toSet())
                intersectionCoords.addAll(intersection)
            }
        }

        val shape: MatchShape
        val specialType: SpecialHeartType?
        val specialDirection: com.example.heartmatch.engine.model.FireDirection?

        when {
            maxSpanSize >= 5 -> {
                shape = MatchShape.FIVE_IN_A_ROW
                specialType = SpecialHeartType.RAINBOW_HEART
                specialDirection = null
            }
            intersectionCoords.isNotEmpty() -> {
                // T-shape, L-shape, or cross
                val isTShape = checkIsTShape(hSpans, vSpans, intersectionCoords)
                val isLShape = checkIsLShape(hSpans, vSpans, intersectionCoords)
                shape = when {
                    isTShape -> MatchShape.T_SHAPE
                    isLShape -> MatchShape.L_SHAPE
                    else -> MatchShape.CROSS_OR_MULTI
                }
                specialType = SpecialHeartType.BOMB_HEART
                specialDirection = null
            }
            maxHSize == 4 -> {
                shape = MatchShape.HORIZONTAL_4
                specialType = SpecialHeartType.FIRE_HEART
                specialDirection = com.example.heartmatch.engine.model.FireDirection.ROW
            }
            maxVSize == 4 -> {
                shape = MatchShape.VERTICAL_4
                specialType = SpecialHeartType.FIRE_HEART
                specialDirection = com.example.heartmatch.engine.model.FireDirection.COLUMN
            }
            maxHSize >= 3 && maxVSize == 0 -> {
                shape = MatchShape.HORIZONTAL_3
                specialType = null
                specialDirection = null
            }
            maxVSize >= 3 && maxHSize == 0 -> {
                shape = MatchShape.VERTICAL_3
                specialType = null
                specialDirection = null
            }
            else -> {
                shape = MatchShape.HORIZONTAL_3
                specialType = null
                specialDirection = null
            }
        }

        val spawnCoord = if (specialType != null) {
            determineSpecialSpawnCoord(allCoords, intersectionCoords, spans, playerSwappedCoords)
        } else {
            null
        }

        return MatchGroup(
            color = color,
            shape = shape,
            matchedCoords = allCoords,
            createdSpecial = specialType,
            createdSpecialDirection = specialDirection,
            specialSpawnCoord = spawnCoord
        )
    }

    private fun checkIsLShape(
        hSpans: List<Span>,
        vSpans: List<Span>,
        intersections: Set<Coord>
    ): Boolean {
        for (inter in intersections) {
            val h = hSpans.firstOrNull { inter in it.coords } ?: continue
            val v = vSpans.firstOrNull { inter in it.coords } ?: continue
            val isHEndpoint = inter == h.startCoord || inter == h.endCoord
            val isVEndpoint = inter == v.startCoord || inter == v.endCoord
            if (isHEndpoint && isVEndpoint) {
                return true
            }
        }
        return false
    }

    private fun checkIsTShape(
        hSpans: List<Span>,
        vSpans: List<Span>,
        intersections: Set<Coord>
    ): Boolean {
        for (inter in intersections) {
            val h = hSpans.firstOrNull { inter in it.coords } ?: continue
            val v = vSpans.firstOrNull { inter in it.coords } ?: continue
            val isHEndpoint = inter == h.startCoord || inter == h.endCoord
            val isVEndpoint = inter == v.startCoord || inter == v.endCoord
            if ((isHEndpoint && !isVEndpoint) || (!isHEndpoint && isVEndpoint)) {
                return true
            }
        }
        return false
    }

    private fun determineSpecialSpawnCoord(
        allCoords: Set<Coord>,
        intersectionCoords: Set<Coord>,
        spans: List<Span>,
        playerSwappedCoords: Pair<Coord, Coord>?
    ): Coord {
        // 1. If player swapped, prefer swapTo, then swapFrom if in matched coords
        if (playerSwappedCoords != null) {
            val (from, to) = playerSwappedCoords
            if (to in allCoords) return to
            if (from in allCoords) return from
        }

        // 2. If there are intersections (T, L, cross), prefer the intersection
        if (intersectionCoords.isNotEmpty()) {
            return intersectionCoords.first()
        }

        // 3. Middle of the largest span
        val largestSpan = spans.maxByOrNull { it.size } ?: spans.first()
        return largestSpan.coords[largestSpan.coords.size / 2]
    }
}
