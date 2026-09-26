package com.example.heartmatch.engine.loader

import com.example.heartmatch.engine.model.*

class LevelJsonParser {

    fun parse(jsonString: String): LevelConfig {
        val root = JsonParser.parse(jsonString).asObject()

        val id = root.getInt("id") ?: 1
        val name = root.getString("name") ?: "Level $id"

        // Board parsing
        val boardObj = root.getObject("board")
        val rows = boardObj?.getInt("rows") ?: root.getInt("rows") ?: 8
        val cols = boardObj?.getInt("columns") ?: boardObj?.getInt("cols") ?: root.getInt("columns") ?: root.getInt("cols") ?: 8

        // Layout parsing if provided
        var cellStates: Array<Array<CellState>>? = null
        var initialTiles: Array<Array<Tile?>>? = null

        val layoutArray = boardObj?.getArray("layout") ?: boardObj?.getArray("grid") ?: root.getArray("layout") ?: root.getArray("grid")
        if (layoutArray != null) {
            cellStates = Array(rows) { Array(cols) { CellState.PLAYABLE } }
            initialTiles = Array(rows) { Array(cols) { null } }

            for (r in 0 until minOf(rows, layoutArray.size)) {
                val rowElem = layoutArray[r]
                if (rowElem is JsonArray) {
                    for (c in 0 until minOf(cols, rowElem.size)) {
                        val cellCode = rowElem[c].asString()
                        parseCellCode(cellCode, r, c, cellStates, initialTiles)
                    }
                }
            }
        }

        // Available colors
        val colorsArray = root.getArray("availableColors") ?: root.getArray("allowedColors")
        val allowedColors = if (colorsArray != null && colorsArray.size > 0) {
            colorsArray.mapNotNull { parseColor(it.asString()) }
        } else {
            listOf(
                HeartColor.RED,
                HeartColor.PINK,
                HeartColor.BLUE,
                HeartColor.GREEN,
                HeartColor.YELLOW
            )
        }

        // Moves and Time
        val moves = root.getInt("moves") ?: root.getInt("moveLimit") ?: 30
        val timeLimitSeconds = root.getInt("timeLimitSeconds") ?: root.getInt("timeLimit")

        // Difficulty
        val difficultyStr = root.getString("difficulty")
        val difficulty = LevelDifficulty.fromString(difficultyStr)

        // Star thresholds
        val starArray = root.getArray("starThresholds")
        val starThresholds = if (starArray != null && starArray.size >= 3) {
            Triple(starArray[0].asInt(), starArray[1].asInt(), starArray[2].asInt())
        } else {
            Triple(5000, 10000, 15000)
        }

        // Blocker config
        val blockerObj = root.getObject("blockerConfig") ?: root.getObject("blockers")
        val blockerConfig = if (blockerObj != null) {
            BlockerConfig(
                stoneDurability = blockerObj.getInt("stoneDurability") ?: 2,
                iceHitPoints = blockerObj.getInt("iceHitPoints") ?: 1,
                woodenLayers = blockerObj.getInt("woodenLayers") ?: 2,
                barbedDurability = blockerObj.getInt("barbedDurability") ?: 1,
                brokenRepairsRequired = blockerObj.getInt("brokenRepairsRequired") ?: 2,
                chainedChainCount = blockerObj.getInt("chainedChainCount") ?: 1,
                darkHeartSpreadTurns = blockerObj.getInt("darkHeartSpreadTurns") ?: 1,
                darkHeartDurability = blockerObj.getInt("darkHeartDurability") ?: 1
            )
        } else {
            BlockerConfig()
        }

        // Objectives
        val objectivesArray = root.getArray("objectives")
        val objectives = mutableListOf<ObjectiveConfig>()
        if (objectivesArray != null) {
            for (objElem in objectivesArray) {
                if (objElem is JsonObject) {
                    val objConfig = parseObjective(objElem)
                    if (objConfig != null) {
                        objectives.add(objConfig)
                    }
                }
            }
        }

        val randomSeed = root.getLong("randomSeed") ?: root.getLong("seed") ?: id.toLong()

        return LevelConfig(
            id = id,
            name = name,
            rows = rows,
            cols = cols,
            cellStates = cellStates,
            initialTiles = initialTiles,
            allowedColors = allowedColors,
            moveLimit = moves,
            timeLimitSeconds = timeLimitSeconds,
            starThresholds = starThresholds,
            difficulty = difficulty,
            objectives = objectives,
            randomSeed = randomSeed,
            blockerConfig = blockerConfig
        )
    }

    private fun parseObjective(obj: JsonObject): ObjectiveConfig? {
        val typeStr = obj.getString("type")?.trim()?.uppercase() ?: return null
        val heartTypeStr = (obj.getString("heartType") ?: obj.getString("targetType") ?: obj.getString("target"))?.trim()?.uppercase()
        val amount = obj.getInt("amount") ?: obj.getInt("targetCount") ?: obj.getInt("count") ?: 0

        val targetColor = parseColor(heartTypeStr)
        val targetBlocker = parseBlocker(heartTypeStr)
        val targetSpecial = parseSpecial(heartTypeStr)

        val targetCells = mutableSetOf<Coord>()
        val cellsArray = obj.getArray("targetCells") ?: obj.getArray("cells")
        if (cellsArray != null) {
            for (elem in cellsArray) {
                if (elem is JsonObject) {
                    val r = elem.getInt("row") ?: elem.getInt("r")
                    val c = elem.getInt("col") ?: elem.getInt("c")
                    if (r != null && c != null) {
                        targetCells.add(Coord(r, c))
                    }
                }
            }
        }

        val resolvedType = when (typeStr) {
            "COLLECT", "COLLECT_COLOR", "COLLECT_HEARTS" -> {
                when {
                    targetSpecial != null -> ObjectiveType.COLLECT_SPECIAL
                    targetBlocker != null -> ObjectiveType.DESTROY_BLOCKERS
                    else -> ObjectiveType.COLLECT_COLOR
                }
            }
            "DESTROY", "DESTROY_BLOCKERS", "CLEAR_BLOCKER" -> ObjectiveType.DESTROY_BLOCKERS
            "REPAIR_BROKEN", "REPAIR" -> ObjectiveType.REPAIR_BROKEN
            "CLEAR_DARK_HEARTS", "CLEAR_DARK" -> ObjectiveType.CLEAR_DARK_HEARTS
            "COLLECT_SPECIAL", "CREATE_SPECIALS", "COLLECT_GIFT" -> ObjectiveType.COLLECT_SPECIAL
            "SCORE", "REACH_SCORE" -> ObjectiveType.SCORE
            "CLEAR_BOARD" -> ObjectiveType.CLEAR_BOARD
            "CLEAR_SPECIFIC_CELLS", "CLEAR_CELLS" -> ObjectiveType.CLEAR_SPECIFIC_CELLS
            else -> ObjectiveType.COLLECT_COLOR
        }

        return ObjectiveConfig(
            type = resolvedType,
            targetCount = if (resolvedType == ObjectiveType.CLEAR_SPECIFIC_CELLS && targetCells.isNotEmpty() && amount == 0) {
                targetCells.size
            } else {
                amount
            },
            targetColor = targetColor,
            targetBlocker = targetBlocker,
            targetSpecial = targetSpecial,
            targetCells = targetCells
        )
    }

    private fun parseColor(str: String?): HeartColor? {
        if (str == null) return null
        return try {
            HeartColor.valueOf(str.uppercase())
        } catch (_: Exception) {
            null
        }
    }

    private fun parseBlocker(str: String?): BlockerType? {
        if (str == null) return null
        val upper = str.uppercase()
        return when {
            upper == "STONE" || upper == "STONE_HEART" -> BlockerType.STONE_HEART
            upper == "ICE" || upper == "ICE_HEART" -> BlockerType.ICE_HEART
            upper == "WOODEN" || upper == "WOOD" || upper == "WOODEN_HEART" -> BlockerType.WOODEN_HEART
            upper == "BARBED" || upper == "BARBED_HEART" -> BlockerType.BARBED_HEART
            upper == "BROKEN" || upper == "BROKEN_HEART" -> BlockerType.BROKEN_HEART
            upper == "CHAINED" || upper == "CHAIN" || upper == "CHAINED_HEART" -> BlockerType.CHAINED_HEART
            upper == "DARK" || upper == "DARK_HEART" -> BlockerType.DARK_HEART
            else -> null
        }
    }

    private fun parseSpecial(str: String?): SpecialHeartType? {
        if (str == null) return null
        val upper = str.uppercase()
        return when {
            upper == "GIFT" || upper == "GIFT_HEART" -> SpecialHeartType.GIFT_HEART
            upper == "FIRE" || upper == "FIRE_HEART" -> SpecialHeartType.FIRE_HEART
            upper == "BOMB" || upper == "BOMB_HEART" -> SpecialHeartType.BOMB_HEART
            upper == "RAINBOW" || upper == "RAINBOW_HEART" -> SpecialHeartType.RAINBOW_HEART
            upper == "LIGHT" || upper == "LIGHT_HEART" -> SpecialHeartType.LIGHT_HEART
            else -> null
        }
    }

    private fun parseCellCode(
        code: String,
        r: Int,
        c: Int,
        cellStates: Array<Array<CellState>>,
        initialTiles: Array<Array<Tile?>>
    ) {
        val trimmed = code.trim().uppercase()
        val directColor = parseColor(trimmed)
        if (directColor != null) {
            cellStates[r][c] = CellState.PLAYABLE
            initialTiles[r][c] = Tile.Normal(color = directColor)
            return
        }

        when {
            trimmed == "X" || trimmed == "#" || trimmed == "UNAVAILABLE" || trimmed == "HOLE" -> {
                cellStates[r][c] = CellState.UNAVAILABLE
            }
            trimmed == "LOCKED" || trimmed == "LOCK" -> {
                cellStates[r][c] = CellState.LOCKED
            }
            trimmed == "O" || trimmed == "." || trimmed == "OPEN" || trimmed == "PLAYABLE" -> {
                cellStates[r][c] = CellState.PLAYABLE
            }
            trimmed == "S" || trimmed.startsWith("STONE") || (trimmed.startsWith("S") && (trimmed.length == 1 || trimmed.drop(1).all { it.isDigit() })) -> { // Stone
                cellStates[r][c] = CellState.PLAYABLE
                val dur = trimmed.removePrefix("STONE").removePrefix("S").toIntOrNull() ?: 2
                initialTiles[r][c] = Tile.Blocker.createStone(durability = dur)
            }
            trimmed == "I" || trimmed.startsWith("ICE") || trimmed.startsWith("I(") -> { // Ice
                cellStates[r][c] = CellState.PLAYABLE
                val colorStr = extractColor(trimmed)
                val color = parseColor(colorStr) ?: HeartColor.RED
                initialTiles[r][c] = Tile.Blocker.createIce(hitPoints = 1, color = color, payload = Tile.Normal(color = color))
            }
            trimmed == "W" || trimmed.startsWith("WOOD") || (trimmed.startsWith("W") && (trimmed.length == 1 || trimmed.drop(1).all { it.isDigit() })) -> { // Wooden
                cellStates[r][c] = CellState.PLAYABLE
                val layers = trimmed.removePrefix("WOODEN").removePrefix("WOOD").removePrefix("W").toIntOrNull() ?: 2
                initialTiles[r][c] = Tile.Blocker.createWooden(layers = layers)
            }
            trimmed == "BR" || trimmed.startsWith("BROKEN") || trimmed.startsWith("BR(") -> { // Broken
                cellStates[r][c] = CellState.PLAYABLE
                val colorStr = extractColor(trimmed)
                val color = parseColor(colorStr) ?: HeartColor.RED
                initialTiles[r][c] = Tile.Blocker.createBroken(repairsRequired = 2, color = color)
            }
            trimmed == "B" || trimmed.startsWith("BARBED") || trimmed.startsWith("B(") -> { // Barbed
                cellStates[r][c] = CellState.PLAYABLE
                initialTiles[r][c] = Tile.Blocker.createBarbed(durability = 1)
            }
            trimmed == "C" || trimmed.startsWith("CHAIN") || trimmed.startsWith("C(") -> { // Chained
                cellStates[r][c] = CellState.PLAYABLE
                val colorStr = extractColor(trimmed)
                val color = parseColor(colorStr) ?: HeartColor.RED
                initialTiles[r][c] = Tile.Blocker.createChained(chainCount = 1, color = color, payload = Tile.Normal(color = color))
            }
            trimmed == "D" || trimmed == "DARK" || trimmed == "DARK_HEART" -> { // Dark
                cellStates[r][c] = CellState.PLAYABLE
                initialTiles[r][c] = Tile.Blocker.createDark(spreadIntervalTurns = 1, durability = 1)
            }
            trimmed == "GIFT" || trimmed == "GIFT_HEART" -> {
                cellStates[r][c] = CellState.PLAYABLE
                initialTiles[r][c] = Tile.Special(specialType = SpecialHeartType.GIFT_HEART)
            }
            trimmed == "FIRE" || trimmed == "FIRE_H" || trimmed == "FIRE_HEART" -> {
                cellStates[r][c] = CellState.PLAYABLE
                initialTiles[r][c] = Tile.Special(specialType = SpecialHeartType.FIRE_HEART, fireDirection = FireDirection.ROW)
            }
            trimmed == "FIRE_V" -> {
                cellStates[r][c] = CellState.PLAYABLE
                initialTiles[r][c] = Tile.Special(specialType = SpecialHeartType.FIRE_HEART, fireDirection = FireDirection.COLUMN)
            }
            trimmed == "BOMB" || trimmed == "BOMB_HEART" -> {
                cellStates[r][c] = CellState.PLAYABLE
                initialTiles[r][c] = Tile.Special(specialType = SpecialHeartType.BOMB_HEART)
            }
            trimmed == "RAINBOW" || trimmed == "RAINBOW_HEART" -> {
                cellStates[r][c] = CellState.PLAYABLE
                initialTiles[r][c] = Tile.Special(specialType = SpecialHeartType.RAINBOW_HEART)
            }
            else -> {
                cellStates[r][c] = CellState.PLAYABLE
            }
        }
    }

    private fun extractColor(str: String): String? {
        val start = str.indexOf('(')
        val end = str.indexOf(')')
        if (start >= 0 && end > start) {
            return str.substring(start + 1, end).trim()
        }
        return null
    }
}
