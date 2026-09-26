package com.example.heartmatch.engine.loader

import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.LevelConfig
import com.example.heartmatch.engine.model.LevelDifficulty

object LevelGenerator {

    /**
     * Generates the JSON string for level [levelId] (1 to 200).
     */
    fun generateLevelJson(levelId: Int): String {
        require(levelId in 1..200) { "Level id must be between 1 and 200 (got $levelId)" }

        val spec = getLevelSpec(levelId)
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"id\": ${spec.id},\n")
        sb.append("  \"name\": \"${spec.name}\",\n")
        sb.append("  \"difficulty\": \"${spec.difficulty.name}\",\n")
        sb.append("  \"board\": {\n")
        sb.append("    \"rows\": ${spec.rows},\n")
        sb.append("    \"columns\": ${spec.cols},\n")
        sb.append("    \"grid\": [\n")
        for (r in 0 until spec.rows) {
            sb.append("      [")
            for (c in 0 until spec.cols) {
                sb.append("\"${spec.grid[r][c]}\"")
                if (c < spec.cols - 1) sb.append(", ")
            }
            sb.append("]")
            if (r < spec.rows - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("    ]\n")
        sb.append("  },\n")
        sb.append("  \"moves\": ${spec.moves},\n")
        sb.append("  \"availableColors\": [")
        sb.append(spec.colors.joinToString(", ") { "\"${it.name}\"" })
        sb.append("],\n")
        sb.append("  \"starThresholds\": [${spec.starThresholds.first}, ${spec.starThresholds.second}, ${spec.starThresholds.third}],\n")
        sb.append("  \"randomSeed\": ${spec.id},\n")
        sb.append("  \"objectives\": [\n")
        for (i in spec.objectives.indices) {
            val obj = spec.objectives[i]
            sb.append("    {\n")
            sb.append("      \"type\": \"${obj.type}\",\n")
            if (obj.heartType != null) {
                sb.append("      \"heartType\": \"${obj.heartType}\",\n")
            }
            sb.append("      \"amount\": ${obj.amount}\n")
            sb.append("    }")
            if (i < spec.objectives.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    /**
     * Generates all 200 level JSONs mapped by level ID.
     */
    fun generateAllLevels(): Map<Int, String> {
        return (1..200).associateWith { generateLevelJson(it) }
    }

    /**
     * Parses the generated level directly into a [LevelConfig].
     */
    fun getLevelConfig(levelId: Int): LevelConfig {
        val json = generateLevelJson(levelId)
        return LevelJsonParser().parse(json)
    }

    data class ObjSpec(
        val type: String,
        val heartType: String?,
        val amount: Int
    )

    data class LevelSpec(
        val id: Int,
        val name: String,
        val difficulty: LevelDifficulty,
        val rows: Int,
        val cols: Int,
        val grid: Array<Array<String>>,
        val moves: Int,
        val colors: List<HeartColor>,
        val starThresholds: Triple<Int, Int, Int>,
        val objectives: List<ObjSpec>
    )

    private fun getLevelSpec(id: Int): LevelSpec {
        return when (id) {
            in 1..10 -> createTier1Spec(id)
            in 11..20 -> createTier2Spec(id)
            in 21..40 -> createTier3Spec(id)
            in 41..60 -> createTier4Spec(id)
            in 61..80 -> createTier5Spec(id)
            in 81..100 -> createTier6Spec(id)
            in 101..200 -> createTier7Spec(id)
            else -> error("Invalid level id $id")
        }
    }

    // =========================================================================
    // TIERS 7-11: LEVELS 101-200 (Advanced remix of every blocker mechanic)
    // =========================================================================
    private fun createTier7Spec(id: Int): LevelSpec {
        val tier = (id - 101) / 20
        val local = (id - 101) % 20
        val chapter = listOf(
            "Crystal Cove", "Thornwood Reach", "Moonlit Reef", "Ember Heartlands", "Everheart Citadel"
        )[tier]
        val epithets = listOf(
            "Shimmering Strand", "Pearl Caverns", "Glass Garden", "Tidal Crossing", "Lighthouse Keep",
            "Coral Switchback", "Silver Shoals", "The Hidden Grotto", "Prism Passage", "Seahorse Steps",
            "The Sunken Arcade", "Brightwater Basin", "Seashell Spire", "Dancing Kelp", "The Quiet Lagoon",
            "Moonstone Shelf", "Starfish Promenade", "The Blue Beyond", "Heartlight Harbor", "The Last Tide"
        )
        val colors5 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)
        val colors6 = colors5 + HeartColor.PURPLE
        val colors = if (tier < 2) colors5 else colors6

        val mechanicSets = listOf(
            listOf("I(BLUE)", "S2", "BR(PINK)", "W2", "B"),
            listOf("W2", "B", "S2", "BR(GREEN)", "I(YELLOW)"),
            listOf("C(RED)", "D", "I(BLUE)", "B", "BR(PINK)"),
            listOf("D", "C(GREEN)", "W2", "B", "S2", "I(PURPLE)"),
            listOf("BR(RED)", "C(BLUE)", "D", "W2", "B", "I(YELLOW)", "S2")
        )[tier]
        val formations = listOf(
            listOf(2 to 2, 2 to 5, 3 to 3, 3 to 6, 4 to 2, 4 to 5, 5 to 3, 5 to 6, 6 to 2, 6 to 5, 3 to 1, 5 to 1),
            listOf(2 to 2, 2 to 6, 3 to 3, 3 to 5, 4 to 2, 4 to 6, 5 to 3, 5 to 5, 6 to 2, 6 to 6, 3 to 1, 5 to 7),
            listOf(2 to 2, 2 to 6, 3 to 3, 3 to 5, 4 to 2, 4 to 6, 5 to 3, 5 to 5, 6 to 2, 6 to 6, 4 to 4, 3 to 1),
            listOf(2 to 2, 4 to 2, 6 to 2, 2 to 4, 4 to 4, 6 to 4, 2 to 6, 4 to 6, 6 to 6, 3 to 3, 5 to 5, 3 to 5, 5 to 3),
            listOf(2 to 3, 3 to 2, 3 to 6, 4 to 4, 5 to 2, 5 to 6, 6 to 3, 2 to 5, 4 to 2, 4 to 6, 6 to 5, 2 to 2)
        )[local % 5]
        val grid = Array(9) { Array(9) { "O" } }
        val primaryType = mechanicSets[local % mechanicSets.size]
        val secondaryType = mechanicSets[(local + 1 + tier) % mechanicSets.size]
        val primaryCount = 3 + (local % 3)
        val secondaryCount = 2 + (local % 2)
        var nextSlot = 0

        fun place(type: String, count: Int) {
            repeat(count) {
                val (row, col) = formations[nextSlot++]
                grid[row][col] = type
            }
        }
        place(primaryType, primaryCount)
        place(secondaryType, secondaryCount)

        val objectives = mutableListOf(
            objectiveFor(primaryType, primaryCount),
        )
        when {
            local % 5 == 4 -> {
                val giftCount = 2
                repeat(giftCount) {
                    val (row, col) = formations[nextSlot++]
                    grid[row][col] = "GIFT"
                }
                objectives += ObjSpec("COLLECT", "GIFT", giftCount)
            }
            local % 4 == 3 -> objectives += ObjSpec("COLLECT", colors[local % colors.size].name, 16 + (local % 4) * 2)
            else -> objectives += objectiveFor(secondaryType, secondaryCount)
        }

        val baseScore = 28000 + tier * 3500 + local * 300
        return LevelSpec(
            id = id,
            name = "$chapter: ${epithets[local]}",
            difficulty = LevelDifficulty.EXPERT,
            rows = 9,
            cols = 9,
            grid = grid,
            moves = 26 - tier,
            colors = colors,
            starThresholds = Triple(baseScore, baseScore + 18000 + tier * 1500, baseScore + 42000 + tier * 3000),
            objectives = objectives
        )
    }

    private fun objectiveFor(cellCode: String, count: Int): ObjSpec = when {
        cellCode.startsWith("BR") -> ObjSpec("REPAIR", null, count)
        cellCode.startsWith("D") -> ObjSpec("CLEAR_DARK", null, count)
        else -> {
            val type = when {
                cellCode.startsWith("S") -> "STONE"
                cellCode.startsWith("I") -> "ICE"
                cellCode.startsWith("W") -> "WOODEN"
                cellCode.startsWith("B") -> "BARBED"
                cellCode.startsWith("C") -> "CHAINED"
                else -> error("Unsupported generated blocker $cellCode")
            }
            ObjSpec("DESTROY", type, count)
        }
    }

    // =========================================================================
    // TIER 1: LEVELS 1-10 (Tutorial and Basic Matching)
    // =========================================================================
    private fun createTier1Spec(id: Int): LevelSpec {
        return when (id) {
            1 -> LevelSpec(
                id = 1,
                name = "First Spark",
                difficulty = LevelDifficulty.EASY,
                rows = 6,
                cols = 6,
                grid = Array(6) { Array(6) { "O" } },
                moves = 20,
                colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE),
                starThresholds = Triple(2000, 4000, 7000),
                objectives = listOf(ObjSpec("COLLECT", "RED", 15))
            )
            2 -> LevelSpec(
                id = 2,
                name = "Pink Blossom",
                difficulty = LevelDifficulty.EASY,
                rows = 6,
                cols = 6,
                grid = Array(6) { Array(6) { "O" } },
                moves = 22,
                colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.GREEN),
                starThresholds = Triple(3000, 6000, 9000),
                objectives = listOf(
                    ObjSpec("COLLECT", "PINK", 15),
                    ObjSpec("COLLECT", "GREEN", 15)
                )
            )
            3 -> LevelSpec(
                id = 3,
                name = "High Hopes",
                difficulty = LevelDifficulty.EASY,
                rows = 7,
                cols = 7,
                grid = Array(7) { Array(7) { "O" } },
                moves = 25,
                colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN),
                starThresholds = Triple(8000, 12000, 18000),
                objectives = listOf(ObjSpec("SCORE", null, 8000))
            )
            4 -> LevelSpec(
                id = 4,
                name = "Fire & Flame",
                difficulty = LevelDifficulty.EASY,
                rows = 7,
                cols = 7,
                grid = Array(7) { Array(7) { "O" } },
                moves = 24,
                colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.YELLOW),
                starThresholds = Triple(4000, 8000, 12000),
                objectives = listOf(
                    ObjSpec("COLLECT", "FIRE", 2),
                    ObjSpec("COLLECT", "RED", 20)
                )
            )
            5 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if ((r == 0 || r == 6) && (c == 0 || c == 6)) "X" else "O"
                    }
                }
                LevelSpec(
                    id = 5,
                    name = "Bombastic",
                    difficulty = LevelDifficulty.EASY,
                    rows = 7,
                    cols = 7,
                    grid = grid,
                    moves = 25,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN),
                    starThresholds = Triple(5000, 10000, 15000),
                    objectives = listOf(
                        ObjSpec("COLLECT", "BOMB", 2),
                        ObjSpec("COLLECT", "BLUE", 20)
                    )
                )
            }
            6 -> LevelSpec(
                id = 6,
                name = "Four Colors",
                difficulty = LevelDifficulty.EASY,
                rows = 8,
                cols = 8,
                grid = Array(8) { Array(8) { "O" } },
                moves = 26,
                colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.YELLOW),
                starThresholds = Triple(6000, 12000, 18000),
                objectives = listOf(
                    ObjSpec("COLLECT", "PINK", 20),
                    ObjSpec("COLLECT", "YELLOW", 20)
                )
            )
            7 -> LevelSpec(
                id = 7,
                name = "Rainbow Wonder",
                difficulty = LevelDifficulty.EASY,
                rows = 8,
                cols = 8,
                grid = Array(8) { Array(8) { "O" } },
                moves = 28,
                colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN),
                starThresholds = Triple(10000, 15000, 22000),
                objectives = listOf(
                    ObjSpec("COLLECT", "RAINBOW", 1),
                    ObjSpec("SCORE", null, 10000)
                )
            )
            8 -> {
                // Cross cutout
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        val isCorner = (r < 2 && c < 2) || (r < 2 && c > 4) || (r > 4 && c < 2) || (r > 4 && c > 4)
                        if (isCorner) "X" else "O"
                    }
                }
                LevelSpec(
                    id = 8,
                    name = "Crossroads",
                    difficulty = LevelDifficulty.EASY,
                    rows = 7,
                    cols = 7,
                    grid = grid,
                    moves = 24,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.GREEN, HeartColor.YELLOW),
                    starThresholds = Triple(6000, 11000, 17000),
                    objectives = listOf(
                        ObjSpec("COLLECT", "GREEN", 15),
                        ObjSpec("COLLECT", "YELLOW", 15),
                        ObjSpec("COLLECT", "RED", 15)
                    )
                )
            }
            9 -> LevelSpec(
                id = 9,
                name = "Special Harmony",
                difficulty = LevelDifficulty.EASY,
                rows = 8,
                cols = 8,
                grid = Array(8) { Array(8) { "O" } },
                moves = 25,
                colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.YELLOW),
                starThresholds = Triple(8000, 14000, 20000),
                objectives = listOf(
                    ObjSpec("COLLECT", "FIRE", 2),
                    ObjSpec("COLLECT", "BOMB", 2),
                    ObjSpec("COLLECT", "RAINBOW", 1)
                )
            )
            10 -> {
                // Diamond shape
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        val dist = kotlin.math.abs(r - 3.5) + kotlin.math.abs(c - 3.5)
                        if (dist > 4.5) "X" else "O"
                    }
                }
                LevelSpec(
                    id = 10,
                    name = "Apprentice Graduation",
                    difficulty = LevelDifficulty.EASY,
                    rows = 8,
                    cols = 8,
                    grid = grid,
                    moves = 26,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
                    starThresholds = Triple(15000, 22000, 30000),
                    objectives = listOf(
                        ObjSpec("COLLECT", "RED", 25),
                        ObjSpec("COLLECT", "BLUE", 25),
                        ObjSpec("SCORE", null, 15000)
                    )
                )
            }
            else -> error("Invalid tier 1 id $id")
        }
    }

    // =========================================================================
    // TIER 2: LEVELS 11-20 (Introduce Stone Hearts & Gift Hearts)
    // =========================================================================
    private fun createTier2Spec(id: Int): LevelSpec {
        return when (id) {
            11 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if (r == 3 && c in 2..4) "S1" else "O"
                    }
                }
                LevelSpec(
                    id = 11,
                    name = "Stone Introduction",
                    difficulty = LevelDifficulty.EASY,
                    rows = 7,
                    cols = 7,
                    grid = grid,
                    moves = 25,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN),
                    starThresholds = Triple(6000, 12000, 18000),
                    objectives = listOf(
                        ObjSpec("DESTROY", "STONE", 3),
                        ObjSpec("COLLECT", "RED", 20)
                    )
                )
            }
            12 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((r == 3 || r == 4) && (c in 1..2 || c in 5..6)) "S1" else "O"
                    }
                }
                LevelSpec(
                    id = 12,
                    name = "Stone Valley",
                    difficulty = LevelDifficulty.INTERMEDIATE,
                    rows = 8,
                    cols = 8,
                    grid = grid,
                    moves = 24,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.YELLOW),
                    starThresholds = Triple(7000, 14000, 21000),
                    objectives = listOf(
                        ObjSpec("DESTROY", "STONE", 8),
                        ObjSpec("COLLECT", "PINK", 25)
                    )
                )
            }
            13 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if ((r == 2 && c == 2) || (r == 4 && c == 4)) "GIFT" else "O"
                    }
                }
                LevelSpec(
                    id = 13,
                    name = "Gift of Love",
                    difficulty = LevelDifficulty.INTERMEDIATE,
                    rows = 7,
                    cols = 7,
                    grid = grid,
                    moves = 22,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN),
                    starThresholds = Triple(8000, 15000, 22000),
                    objectives = listOf(
                        ObjSpec("COLLECT", "GIFT", 2),
                        ObjSpec("COLLECT", "BLUE", 20)
                    )
                )
            }
            14 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 3 && c == 3) || (r == 4 && c == 4) -> "GIFT"
                            (r in 2..5 && c in 2..5 && (r == 2 || r == 5 || c == 2 || c == 5)) -> "S1"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(
                    id = 14,
                    name = "Rock & Present",
                    difficulty = LevelDifficulty.INTERMEDIATE,
                    rows = 8,
                    cols = 8,
                    grid = grid,
                    moves = 24,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.YELLOW),
                    starThresholds = Triple(9000, 16000, 24000),
                    objectives = listOf(
                        ObjSpec("DESTROY", "STONE", 8),
                        ObjSpec("COLLECT", "GIFT", 2)
                    )
                )
            }
            15 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((c == 2 || c == 5) && r in 2..5) "S2" else "O"
                    }
                }
                LevelSpec(
                    id = 15,
                    name = "Granite Pillars",
                    difficulty = LevelDifficulty.MEDIUM,
                    rows = 8,
                    cols = 8,
                    grid = grid,
                    moves = 26,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
                    starThresholds = Triple(10000, 18000, 26000),
                    objectives = listOf(
                        ObjSpec("DESTROY", "STONE", 8),
                        ObjSpec("COLLECT", "GREEN", 20)
                    )
                )
            }
            16 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 3 && c == 3) || (r == 4 && c == 4) -> "GIFT"
                            (r in 3..4 && c in 2..5) -> "S1"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(
                    id = 16,
                    name = "Treasure Chest",
                    difficulty = LevelDifficulty.MEDIUM,
                    rows = 8,
                    cols = 8,
                    grid = grid,
                    moves = 25,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.YELLOW),
                    starThresholds = Triple(11000, 19000, 28000),
                    objectives = listOf(
                        ObjSpec("DESTROY", "STONE", 6),
                        ObjSpec("COLLECT", "GIFT", 2),
                        ObjSpec("COLLECT", "RED", 20)
                    )
                )
            }
            17 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        val isCorner = (r == 0 && c == 0) || (r == 0 && c == 7) || (r == 7 && c == 0) || (r == 7 && c == 7)
                        val isStone = (r == 1 && c == 1) || (r == 1 && c == 6) || (r == 6 && c == 1) || (r == 6 && c == 6) ||
                                (r == 3 && c == 3) || (r == 3 && c == 4) || (r == 4 && c == 3) || (r == 4 && c == 4)
                        if (isCorner) "X" else if (isStone) "S1" else "O"
                    }
                }
                LevelSpec(
                    id = 17,
                    name = "Stone Enclosure",
                    difficulty = LevelDifficulty.MEDIUM,
                    rows = 8,
                    cols = 8,
                    grid = grid,
                    moves = 24,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
                    starThresholds = Triple(10000, 17000, 25000),
                    objectives = listOf(
                        ObjSpec("DESTROY", "STONE", 8),
                        ObjSpec("COLLECT", "BOMB", 2)
                    )
                )
            }
            18 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((r == 1 && c == 1) || (r == 1 && c == 6) || (r == 4 && c == 3)) "GIFT" else "O"
                    }
                }
                LevelSpec(
                    id = 18,
                    name = "Gift Cascade",
                    difficulty = LevelDifficulty.MEDIUM,
                    rows = 8,
                    cols = 8,
                    grid = grid,
                    moves = 23,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
                    starThresholds = Triple(18000, 26000, 36000),
                    objectives = listOf(
                        ObjSpec("COLLECT", "GIFT", 3),
                        ObjSpec("SCORE", null, 18000)
                    )
                )
            }
            19 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if (r in 3..4 && c in 1..6 && c != 3 && c != 4) "S2"
                        else if ((r == 2 || r == 5) && (c == 2 || c == 5)) "S2"
                        else "O"
                    }
                }
                LevelSpec(
                    id = 19,
                    name = "Heavy Stones",
                    difficulty = LevelDifficulty.MEDIUM,
                    rows = 8,
                    cols = 8,
                    grid = grid,
                    moves = 25,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
                    starThresholds = Triple(12000, 20000, 30000),
                    objectives = listOf(
                        ObjSpec("DESTROY", "STONE", 8),
                        ObjSpec("COLLECT", "BLUE", 25)
                    )
                )
            }
            20 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        val isVaultWall = (r == 3 || r == 5) && c in 2..6 || (c == 2 || c == 6) && r in 3..5
                        val isGift = r == 4 && (c == 3 || c == 5)
                        if (isGift) "GIFT" else if (isVaultWall) "S1" else "O"
                    }
                }
                LevelSpec(
                    id = 20,
                    name = "The Vault",
                    difficulty = LevelDifficulty.MEDIUM,
                    rows = 9,
                    cols = 9,
                    grid = grid,
                    moves = 26,
                    colors = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW),
                    starThresholds = Triple(25000, 35000, 50000),
                    objectives = listOf(
                        ObjSpec("DESTROY", "STONE", 10),
                        ObjSpec("COLLECT", "GIFT", 2),
                        ObjSpec("SCORE", null, 25000)
                    )
                )
            }
            else -> error("Invalid tier 2 id $id")
        }
    }

    // =========================================================================
    // TIER 3: LEVELS 21-40 (Broken Hearts, Ice Hearts & Wooden Hearts)
    // =========================================================================
    private fun createTier3Spec(id: Int): LevelSpec {
        val colors4 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN)
        val colors5 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)
        val colors6 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW, HeartColor.PURPLE)

        return when (id) {
            // 21-25: Broken Hearts
            21 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if ((r == 2 || r == 4) && (c == 2 || c == 4)) if ((r + c) % 2 == 0) "BR(RED)" else "BR(BLUE)" else "O"
                    }
                }
                LevelSpec(21, "Cracked Hearts", LevelDifficulty.MEDIUM, 7, 7, grid, 24, colors4, Triple(7000, 14000, 21000),
                    listOf(ObjSpec("REPAIR", null, 4), ObjSpec("COLLECT", "RED", 20)))
            }
            22 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((r == 3 && c in 2..4) || (r == 4 && c in 3..5)) {
                            if ((r + c) % 2 == 0) "BR(PINK)" else "BR(BLUE)"
                        } else "O"
                    }
                }
                LevelSpec(22, "Healing Touch", LevelDifficulty.MEDIUM, 8, 8, grid, 23, colors4, Triple(8000, 15000, 23000),
                    listOf(ObjSpec("REPAIR", null, 6), ObjSpec("COLLECT", "PINK", 20)))
            }
            23 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if (r in 3..4 && c in 2..5) {
                            if ((r + c) % 2 == 0) "BR(BLUE)" else "BR(RED)"
                        } else "O"
                    }
                }
                LevelSpec(23, "Mended Bridges", LevelDifficulty.MEDIUM, 8, 8, grid, 25, colors5, Triple(16000, 24000, 34000),
                    listOf(ObjSpec("REPAIR", null, 8), ObjSpec("SCORE", null, 16000)))
            }
            24 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 1 || r == 6) && (c == 3 || c == 4) -> "S1"
                            r in 3..4 && c in 1..4 -> if ((r + c) % 2 == 0) "BR(RED)" else "BR(PINK)"
                            r in 3..4 && c in 5..6 -> if ((r + c) % 2 == 0) "BR(BLUE)" else "BR(GREEN)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(24, "Shattered Pathway", LevelDifficulty.MEDIUM, 8, 8, grid, 24, colors5, Triple(10000, 18000, 26000),
                    listOf(ObjSpec("REPAIR", null, 8), ObjSpec("DESTROY", "STONE", 4)))
            }
            25 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((r in 2..5 && (c == 2 || c == 5)) || (r == 3 && c in 3..4)) {
                            if ((r + c) % 2 == 0) "BR(PINK)" else "BR(BLUE)"
                        } else "O"
                    }
                }
                LevelSpec(25, "Heart Surgeon", LevelDifficulty.MEDIUM, 8, 8, grid, 24, colors5, Triple(12000, 20000, 30000),
                    listOf(ObjSpec("REPAIR", null, 10), ObjSpec("COLLECT", "FIRE", 2)))
            }

            // 26-30: Ice Hearts
            26 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if (r in 2..4 && (c == 2 || c == 4)) {
                            if ((r + c) % 2 == 0) "I(BLUE)" else "I(RED)"
                        } else "O"
                    }
                }
                LevelSpec(26, "First Frost", LevelDifficulty.MEDIUM, 7, 7, grid, 24, colors4, Triple(8000, 15000, 22000),
                    listOf(ObjSpec("DESTROY", "ICE", 6), ObjSpec("COLLECT", "BLUE", 20)))
            }
            27 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((r == 2 || r == 5) && c in 2..5) {
                            if ((r + c) % 2 == 0) "I(PINK)" else "I(BLUE)"
                        } else "O"
                    }
                }
                LevelSpec(27, "Glacier Ridge", LevelDifficulty.MEDIUM, 8, 8, grid, 23, colors4, Triple(9000, 16000, 24000),
                    listOf(ObjSpec("DESTROY", "ICE", 8), ObjSpec("COLLECT", "PINK", 25)))
            }
            28 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 3 && c == 3) || (r == 4 && c == 4) -> "GIFT"
                            (r in 2..5 && (c == 2 || c == 5)) -> if ((r + c) % 2 == 0) "I(GREEN)" else "I(YELLOW)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(28, "Frozen Treasures", LevelDifficulty.MEDIUM, 8, 8, grid, 24, colors5, Triple(10000, 18000, 26000),
                    listOf(ObjSpec("DESTROY", "ICE", 8), ObjSpec("COLLECT", "GIFT", 2)))
            }
            29 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 2..3 && c in 2..4 -> if ((r + c) % 2 == 0) "I(RED)" else "I(BLUE)"
                            r in 4..5 && c in 3..5 -> "S1"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(29, "Ice & Stone", LevelDifficulty.MEDIUM, 8, 8, grid, 25, colors5, Triple(11000, 19000, 28000),
                    listOf(ObjSpec("DESTROY", "ICE", 6), ObjSpec("DESTROY", "STONE", 6)))
            }
            30 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        val isHole = (r == 0 || r == 7) && (c == 0 || c == 7)
                        val isIce = r in 2..5 && (c == 1 || c == 6 || (r in 3..4 && (c == 2 || c == 5)))
                        if (isHole) "X" else if (isIce) (if ((r + c) % 2 == 0) "I(RED)" else "I(PINK)") else "O"
                    }
                }
                LevelSpec(30, "Frostbite Cavern", LevelDifficulty.MEDIUM, 8, 8, grid, 25, colors5, Triple(12000, 22000, 32000),
                    listOf(ObjSpec("DESTROY", "ICE", 12), ObjSpec("COLLECT", "RED", 30)))
            }

            // 31-35: Wooden Hearts
            31 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if (r == 3 && c in 1..6 && c != 3) "W1" else "O"
                    }
                }
                LevelSpec(31, "Timber Trail", LevelDifficulty.MEDIUM, 7, 7, grid, 24, colors4, Triple(8000, 15000, 23000),
                    listOf(ObjSpec("DESTROY", "WOODEN", 5), ObjSpec("COLLECT", "GREEN", 20)))
            }
            32 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((r == 2 || r == 5) && c in 2..5) "W2" else "O"
                    }
                }
                LevelSpec(32, "Wooden Barricade", LevelDifficulty.MEDIUM, 8, 8, grid, 23, colors5, Triple(9000, 17000, 25000),
                    listOf(ObjSpec("DESTROY", "WOODEN", 8), ObjSpec("COLLECT", "YELLOW", 20)))
            }
            33 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if (r in 3..4 && c in 1..6 && c != 3 && c != 4 || (r == 2 && c in 3..4) || (r == 5 && c in 3..4)) "W2" else "O"
                    }
                }
                LevelSpec(33, "Dense Lumber", LevelDifficulty.MEDIUM, 8, 8, grid, 24, colors5, Triple(11000, 19000, 28000),
                    listOf(ObjSpec("DESTROY", "WOODEN", 8), ObjSpec("COLLECT", "BOMB", 2)))
            }
            34 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 3 && c == 3) || (r == 4 && c == 4) -> "GIFT"
                            (r in 2..5 && (c == 2 || c == 5)) || ((r == 2 || r == 5) && c in 3..4) -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(34, "Crate Storage", LevelDifficulty.MEDIUM, 8, 8, grid, 25, colors5, Triple(12000, 20000, 30000),
                    listOf(ObjSpec("DESTROY", "WOODEN", 10), ObjSpec("COLLECT", "GIFT", 2)))
            }
            35 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        if ((r == 2 || r == 6) && c in 2..6 || (c == 2 || c == 6) && r in 3..5) "W2" else "O"
                    }
                }
                LevelSpec(35, "Lumberjack's Maze", LevelDifficulty.MEDIUM, 9, 9, grid, 26, colors5, Triple(22000, 32000, 45000),
                    listOf(ObjSpec("DESTROY", "WOODEN", 12), ObjSpec("SCORE", null, 22000)))
            }

            // 36-40: Combined Triad
            36 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 2..3 && c in 1..2 -> if ((r + c) % 2 == 0) "BR(RED)" else "BR(BLUE)"
                            r in 2..3 && c in 5..6 -> if ((r + c) % 2 == 0) "I(BLUE)" else "I(PINK)"
                            r in 4..5 && c in 3..4 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(36, "Trio Trouble", LevelDifficulty.MEDIUM, 8, 8, grid, 25, colors5, Triple(12000, 21000, 30000),
                    listOf(ObjSpec("REPAIR", null, 4), ObjSpec("DESTROY", "ICE", 4), ObjSpec("DESTROY", "WOODEN", 4)))
            }
            37 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 2 || r == 5) && c in 2..4 -> if ((r + c) % 2 == 0) "I(PINK)" else "I(BLUE)"
                            (r in 3..4) && c in 1..4 -> "W2"
                            (r in 3..4) && c in 5..6 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(37, "Frozen Forest", LevelDifficulty.MEDIUM, 8, 8, grid, 24, colors5, Triple(13000, 22000, 32000),
                    listOf(ObjSpec("DESTROY", "ICE", 6), ObjSpec("DESTROY", "WOODEN", 8)))
            }
            38 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 2..3 && c in 2..4 -> if ((r + c) % 2 == 0) "BR(GREEN)" else "BR(YELLOW)"
                            r in 4..5 && c in 3..5 -> "W2"
                            (r == 1 || r == 6) && (c == 3 || c == 4) -> "S1"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(38, "Mending the Glade", LevelDifficulty.MEDIUM, 8, 8, grid, 23, colors5, Triple(14000, 23000, 34000),
                    listOf(ObjSpec("REPAIR", null, 6), ObjSpec("DESTROY", "WOODEN", 6), ObjSpec("DESTROY", "STONE", 4)))
            }
            39 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 1 && c == 1) || (r == 6 && c == 6) -> "GIFT"
                            (r in 2..3 && c in 2..5) -> if ((r + c) % 2 == 0) "I(BLUE)" else "I(RED)"
                            (r in 4..5 && c in 2..5) -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(39, "Crystal & Wood", LevelDifficulty.MEDIUM, 8, 8, grid, 25, colors5, Triple(15000, 25000, 36000),
                    listOf(ObjSpec("DESTROY", "ICE", 8), ObjSpec("DESTROY", "WOODEN", 8), ObjSpec("COLLECT", "GIFT", 2)))
            }
            40 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        val isCorner = (r == 0 || r == 8) && (c == 0 || c == 8)
                        val tile = when {
                            r in 2..3 && c in 3..5 -> if ((r + c) % 2 == 0) "BR(RED)" else "BR(BLUE)"
                            r in 4..5 && c in 2..4 -> if ((r + c) % 2 == 0) "I(PINK)" else "I(GREEN)"
                            r in 4..5 && c in 5..7 -> "W2"
                            (r == 1 || r == 7) && (c == 4) -> "S2"
                            else -> "O"
                        }
                        if (isCorner) "X" else tile
                    }
                }
                LevelSpec(40, "The Triad Citadel", LevelDifficulty.HARD, 9, 9, grid, 26, colors6, Triple(25000, 38000, 55000),
                    listOf(ObjSpec("REPAIR", null, 6), ObjSpec("DESTROY", "ICE", 6), ObjSpec("DESTROY", "WOODEN", 6)))
            }
            else -> error("Invalid tier 3 id $id")
        }
    }

    // =========================================================================
    // TIER 4: LEVELS 41-60 (Introduce Dark Hearts)
    // =========================================================================
    private fun createTier4Spec(id: Int): LevelSpec {
        val colors5 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)
        val colors6 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW, HeartColor.PURPLE)

        return when (id) {
            41 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if (r == 5 && (c == 1 || c == 5)) "D" else "O"
                    }
                }
                LevelSpec(41, "Shadow Awakens", LevelDifficulty.MEDIUM, 7, 7, grid, 22, colors5, Triple(8000, 15000, 22000),
                    listOf(ObjSpec("CLEAR_DARK", null, 2), ObjSpec("COLLECT", "RED", 20)))
            }
            42 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if (r == 4 && (c == 1 || c == 3 || c == 5)) "D" else "O"
                    }
                }
                LevelSpec(42, "Creeping Shadow", LevelDifficulty.MEDIUM, 7, 7, grid, 22, colors5, Triple(9000, 16000, 24000),
                    listOf(ObjSpec("CLEAR_DARK", null, 3), ObjSpec("COLLECT", "PINK", 20)))
            }
            43 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if (r == 4 && c in 2..5) "D" else "O"
                    }
                }
                LevelSpec(43, "Dark Roots", LevelDifficulty.MEDIUM, 8, 8, grid, 23, colors5, Triple(10000, 18000, 26000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("COLLECT", "FIRE", 2)))
            }
            44 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 3 || r == 4) && (c == 3 || c == 4) -> "D"
                            (r == 2 || r == 5) && c in 2..4 -> "S1"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(44, "Shadow & Stone", LevelDifficulty.MEDIUM, 8, 8, grid, 24, colors5, Triple(11000, 19000, 28000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "STONE", 6)))
            }
            45 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 5 && c in 2..5 -> "D"
                            r in 2..3 && c in 2..4 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(45, "Corrupted Glade", LevelDifficulty.MEDIUM, 8, 8, grid, 23, colors5, Triple(12000, 20000, 30000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "WOODEN", 6)))
            }

            46 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((r == 2 && c == 4) || (r == 4 && c == 2) || (r == 4 && c == 6) || (r == 6 && c == 4)) "D" else "O"
                    }
                }
                LevelSpec(46, "Dark Eclipse", LevelDifficulty.MEDIUM, 8, 8, grid, 22, colors5, Triple(11000, 19000, 28000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("COLLECT", "BLUE", 25)))
            }
            47 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r in 4..5 && (c in 1..2 || c in 5..6)) -> "D"
                            (r in 2..3 && (c in 2..3 || c in 4..5)) -> if ((r + c) % 2 == 0) "I(RED)" else "I(BLUE)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(47, "Shadow Twins", LevelDifficulty.MEDIUM, 8, 8, grid, 23, colors5, Triple(12000, 21000, 31000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "ICE", 4)))
            }
            48 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 5 && c in 1..5) -> "D"
                            (r in 2..3 && (c == 2 || c == 5)) -> if ((r + c) % 2 == 0) "BR(PINK)" else "BR(BLUE)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(48, "Corruption Spread", LevelDifficulty.MEDIUM, 8, 8, grid, 23, colors5, Triple(13000, 22000, 33000),
                    listOf(ObjSpec("CLEAR_DARK", null, 5), ObjSpec("REPAIR", null, 4)))
            }
            49 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 2 && c == 2) || (r == 5 && c == 5) -> "GIFT"
                            (r == 3 || r == 4) && c in 2..4 -> "D"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(49, "Abyssal Gate", LevelDifficulty.MEDIUM, 8, 8, grid, 24, colors5, Triple(14000, 23000, 35000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("COLLECT", "GIFT", 2)))
            }
            50 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 5 && c in 1..6 -> "D"
                            r == 3 && c in 1..6 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(50, "Dark Nexus", LevelDifficulty.HARD, 8, 8, grid, 24, colors5, Triple(24000, 36000, 50000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "WOODEN", 6), ObjSpec("SCORE", null, 24000)))
            }

            51 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 5 && c in 1..5 -> "D"
                            r in 2..3 && c in 2..5 -> if ((r + c) % 2 == 0) "I(BLUE)" else "I(GREEN)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(51, "Frozen Abyss", LevelDifficulty.HARD, 8, 8, grid, 22, colors5, Triple(13000, 22000, 33000),
                    listOf(ObjSpec("CLEAR_DARK", null, 5), ObjSpec("DESTROY", "ICE", 8)))
            }
            52 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 4..5 && c in 2..4 -> "D"
                            r in 2..3 && c in 3..5 -> if ((r + c) % 2 == 0) "BR(RED)" else "BR(PINK)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(52, "Broken Twilight", LevelDifficulty.HARD, 8, 8, grid, 23, colors5, Triple(14000, 24000, 36000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("REPAIR", null, 6)))
            }
            53 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 5 && c in 1..6 -> "D"
                            (r == 2 || r == 3) && c in 1..3 -> "S2"
                            (r == 2 || r == 3) && c in 4..6 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(53, "Shadow Fortress", LevelDifficulty.HARD, 8, 8, grid, 24, colors5, Triple(15000, 25000, 38000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "STONE", 6)))
            }
            54 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        val isCut = (r in 0..1 || r in 6..7) && (c == 0 || c == 7)
                        val tile = if (r in 4..5 && c in 2..4) "D" else "O"
                        if (isCut) "X" else tile
                    }
                }
                LevelSpec(54, "Dark Spores", LevelDifficulty.HARD, 8, 8, grid, 22, colors5, Triple(14000, 23000, 35000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("COLLECT", "YELLOW", 30)))
            }
            55 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 1 && c == 1) || (r == 1 && c == 6) -> "GIFT"
                            r in 4..5 && c in 1..4 -> "D"
                            r == 6 && c in 2..4 -> "D"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(55, "Shadow Bloom", LevelDifficulty.HARD, 8, 8, grid, 23, colors5, Triple(16000, 26000, 40000),
                    listOf(ObjSpec("CLEAR_DARK", null, 7), ObjSpec("COLLECT", "GIFT", 2)))
            }

            56 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 5..6 && c in 2..5 -> "D"
                            r == 4 && c in 1..6 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(56, "Quarantine Zone", LevelDifficulty.HARD, 8, 8, grid, 24, colors5, Triple(16000, 27000, 42000),
                    listOf(ObjSpec("CLEAR_DARK", null, 8), ObjSpec("DESTROY", "WOODEN", 6)))
            }
            57 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 5..6 && c in 1..4 -> "D"
                            r in 2..3 && c in 3..6 -> if ((r + c) % 2 == 0) "I(PINK)" else "I(YELLOW)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(57, "Eclipse of Hope", LevelDifficulty.HARD, 8, 8, grid, 23, colors5, Triple(17000, 28000, 44000),
                    listOf(ObjSpec("CLEAR_DARK", null, 8), ObjSpec("DESTROY", "ICE", 8)))
            }
            58 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        val isCorner = (r == 0 || r == 8) && (c == 0 || c == 8)
                        val tile = when {
                            r in 5..6 && c in 2..5 -> "D"
                            r in 2..3 && c in 3..5 -> if ((r + c) % 2 == 0) "BR(BLUE)" else "BR(RED)"
                            else -> "O"
                        }
                        if (isCorner) "X" else tile
                    }
                }
                LevelSpec(58, "Void Labyrinth", LevelDifficulty.HARD, 9, 9, grid, 24, colors5, Triple(18000, 30000, 46000),
                    listOf(ObjSpec("CLEAR_DARK", null, 8), ObjSpec("REPAIR", null, 6)))
            }
            59 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 5..6 && c in 2..5 -> "D"
                            (r == 2 || r == 3) && c in 2..5 -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(59, "Cursed Monoliths", LevelDifficulty.HARD, 8, 8, grid, 23, colors5, Triple(28000, 42000, 60000),
                    listOf(ObjSpec("CLEAR_DARK", null, 8), ObjSpec("DESTROY", "STONE", 8), ObjSpec("SCORE", null, 28000)))
            }
            60 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r in 6..7 && c in 2..6 -> "D"
                            r == 4 && c in 1..6 -> "W2"
                            r in 2..3 && c in 3..4 -> if ((r + c) % 2 == 0) "I(RED)" else "I(PINK)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(60, "Lord of the Void", LevelDifficulty.HARD, 9, 9, grid, 24, colors6, Triple(28000, 44000, 65000),
                    listOf(ObjSpec("CLEAR_DARK", null, 10), ObjSpec("DESTROY", "WOODEN", 6), ObjSpec("DESTROY", "ICE", 4)))
            }
            else -> error("Invalid tier 4 id $id")
        }
    }

    // =========================================================================
    // TIER 5: LEVELS 61-80 (Chained Hearts & Complex Layouts)
    // =========================================================================
    private fun createTier5Spec(id: Int): LevelSpec {
        val colors5 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)
        val colors6 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW, HeartColor.PURPLE)

        return when (id) {
            61 -> {
                val grid = Array(7) { r ->
                    Array(7) { c ->
                        if (r == 3 && c in 1..6 && c != 3) {
                            if (c % 2 == 0) "C(RED)" else "C(BLUE)"
                        } else "O"
                    }
                }
                LevelSpec(61, "Chained Bonds", LevelDifficulty.HARD, 7, 7, grid, 22, colors5, Triple(10000, 18000, 27000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 5), ObjSpec("COLLECT", "RED", 20)))
            }
            62 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        if ((r == 2 || r == 5) && c in 2..5) {
                            if ((r + c) % 2 == 0) "C(PINK)" else "C(YELLOW)"
                        } else "O"
                    }
                }
                LevelSpec(62, "Iron Links", LevelDifficulty.HARD, 8, 8, grid, 21, colors5, Triple(11000, 19000, 29000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("COLLECT", "PINK", 25)))
            }
            63 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 3 && c in 1..6 && c != 3) -> if (c % 2 == 0) "C(BLUE)" else "C(GREEN)"
                            (r == 5 && c in 1..6 && c != 4) -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(63, "Chains & Stones", LevelDifficulty.HARD, 8, 8, grid, 22, colors5, Triple(12000, 21000, 32000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 5), ObjSpec("DESTROY", "STONE", 5)))
            }
            64 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r in 2..3 && c in 2..4) -> "B"
                            (r in 4..5 && c in 3..5) -> if ((r + c) % 2 == 0) "C(GREEN)" else "C(RED)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(64, "Barbed Introduction", LevelDifficulty.HARD, 8, 8, grid, 22, colors5, Triple(13000, 22000, 34000),
                    listOf(ObjSpec("DESTROY", "BARBED", 6), ObjSpec("DESTROY", "CHAINED", 6)))
            }
            65 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 3 && c == 3) || (r == 4 && c == 4) -> "GIFT"
                            (r in 2..5 && (c == 2 || c == 5)) || ((r == 2 || r == 5) && c in 3..4) -> {
                                if ((r + c) % 2 == 0) "C(RED)" else "C(BLUE)"
                            }
                            else -> "O"
                        }
                    }
                }
                LevelSpec(65, "Locked Vault", LevelDifficulty.HARD, 8, 8, grid, 23, colors5, Triple(14000, 24000, 36000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("COLLECT", "GIFT", 2)))
            }

            66 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        val isBridge = c == 3 || c == 4
                        val isChamber = c in 0..2 || c in 5..7
                        val tile = if (r in 3..4 && c in 1..6 && c != 3 && c != 4) {
                            if ((r + c) % 2 == 0) "C(BLUE)" else "C(PINK)"
                        } else "O"
                        if (!isBridge && !isChamber) "X" else tile
                    }
                }
                LevelSpec(66, "Dual Chambers", LevelDifficulty.HARD, 8, 8, grid, 22, colors5, Triple(14000, 23000, 35000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 4), ObjSpec("COLLECT", "BLUE", 30)))
            }
            67 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r in 2..3 && c in 2..5) -> if ((r + c) % 2 == 0) "C(PINK)" else "C(BLUE)"
                            (r in 4..5 && c in 2..5) -> if ((r + c) % 2 == 0) "I(YELLOW)" else "I(GREEN)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(67, "Frozen Shackles", LevelDifficulty.HARD, 8, 8, grid, 22, colors5, Triple(15000, 25000, 38000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "ICE", 8)))
            }
            68 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r in 2..3 && c in 2..5) -> if ((r + c) % 2 == 0) "C(RED)" else "C(BLUE)"
                            (r in 4..5 && c in 2..4) -> "B"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(68, "Chains & Thorns", LevelDifficulty.HARD, 8, 8, grid, 21, colors5, Triple(15000, 26000, 40000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "BARBED", 6)))
            }
            69 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r in 2..3 && c in 2..5) -> if ((r + c) % 2 == 0) "C(GREEN)" else "C(YELLOW)"
                            (r in 4..5 && c in 2..5) -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(69, "Wooden Stockade", LevelDifficulty.HARD, 8, 8, grid, 22, colors5, Triple(16000, 27000, 42000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "WOODEN", 8)))
            }
            70 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r in 2..3 && c in 1..5) -> if ((r + c) % 2 == 0) "C(BLUE)" else "C(PINK)"
                            (r in 4..5 && c in 2..4) -> if ((r + c) % 2 == 0) "BR(RED)" else "BR(GREEN)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(70, "Prison Break", LevelDifficulty.HARD, 8, 8, grid, 23, colors6, Triple(17000, 28000, 44000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 10), ObjSpec("REPAIR", null, 6)))
            }

            71 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 5 && c in 1..6 -> "D"
                            r in 2..3 && c in 2..4 -> if ((r + c) % 2 == 0) "C(YELLOW)" else "C(RED)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(71, "Dark Fetters", LevelDifficulty.HARD, 8, 8, grid, 21, colors6, Triple(16000, 27000, 42000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "CHAINED", 6)))
            }
            72 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 6 && c in 1..6 -> "D"
                            r in 3..4 && c in 2..5 -> if ((r + c) % 2 == 0) "C(PINK)" else "C(BLUE)"
                            (r == 1 && (c == 2 || c == 5)) || (r == 2 && (c == 2 || c == 5)) -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(72, "Abyssal Chains", LevelDifficulty.HARD, 8, 8, grid, 22, colors6, Triple(17000, 29000, 45000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "CHAINED", 8)))
            }
            73 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 6 && c in 1..6 -> "D"
                            r in 3..4 && c in 2..4 -> "B"
                            r in 1..2 && c in 3..5 -> if ((r + c) % 2 == 0) "C(RED)" else "C(YELLOW)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(73, "Thorned Shadows", LevelDifficulty.HARD, 8, 8, grid, 22, colors6, Triple(18000, 30000, 48000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "BARBED", 6), ObjSpec("DESTROY", "CHAINED", 6)))
            }
            74 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 6 && c in 1..6 -> "D"
                            r in 3..4 && c in 2..4 -> if ((r + c) % 2 == 0) "C(BLUE)" else "C(PINK)"
                            r in 1..2 && c in 2..4 -> if ((r + c) % 2 == 0) "I(GREEN)" else "I(RED)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(74, "Glacial Bonds", LevelDifficulty.HARD, 8, 8, grid, 21, colors6, Triple(18000, 31000, 49000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("DESTROY", "ICE", 6)))
            }
            75 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r == 7 && c in 2..5 -> "D"
                            r in 4..5 && c in 2..5 -> if ((r + c) % 2 == 0) "C(YELLOW)" else "C(BLUE)"
                            r in 2..3 && c in 2..5 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(75, "Labyrinth of Iron", LevelDifficulty.HARD, 9, 9, grid, 23, colors6, Triple(22000, 36000, 56000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "WOODEN", 8), ObjSpec("CLEAR_DARK", null, 4)))
            }

            76 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 1..2 && c in 2..5 -> if ((r + c) % 2 == 0) "C(RED)" else "C(BLUE)"
                            r in 3..4 && c in 2..4 -> if ((r + c) % 2 == 0) "BR(BLUE)" else "BR(PINK)"
                            (r == 5 || r == 6) && c in 3..5 -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(76, "Shattered Fortress", LevelDifficulty.HARD, 8, 8, grid, 22, colors6, Triple(19000, 32000, 50000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("REPAIR", null, 6), ObjSpec("DESTROY", "STONE", 6)))
            }
            77 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 1 && c == 1) || (r == 6 && c == 6) -> "GIFT"
                            r in 2..3 && c in 2..5 -> if ((r + c) % 2 == 0) "C(PINK)" else "C(GREEN)"
                            r in 4..5 && c in 2..5 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(77, "Ironclad Orchard", LevelDifficulty.HARD, 8, 8, grid, 23, colors6, Triple(20000, 34000, 52000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "WOODEN", 8), ObjSpec("COLLECT", "GIFT", 2)))
            }
            78 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 1..2 && c in 2..5 -> "B"
                            r in 3..4 && c in 2..5 -> if ((r + c) % 2 == 0) "C(GREEN)" else "C(BLUE)"
                            r in 5..6 && c in 2..5 -> if ((r + c) % 2 == 0) "I(YELLOW)" else "I(PINK)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(78, "Thorny Citadel", LevelDifficulty.HARD, 8, 8, grid, 22, colors6, Triple(21000, 35000, 54000),
                    listOf(ObjSpec("DESTROY", "BARBED", 8), ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "ICE", 8)))
            }
            79 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 6 && c in 0..7 -> "D"
                            r in 3..4 && c in 2..5 -> if ((r + c) % 2 == 0) "C(RED)" else "C(PINK)"
                            (r in 1..2 && c in 2..4) -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(79, "The Black Gate", LevelDifficulty.HARD, 8, 8, grid, 22, colors6, Triple(22000, 36000, 56000),
                    listOf(ObjSpec("CLEAR_DARK", null, 8), ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "STONE", 6)))
            }
            80 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r in 6..7 && c in 3..5 -> "D"
                            r in 4..5 && c in 2..5 -> if ((r + c) % 2 == 0) "C(BLUE)" else "C(YELLOW)"
                            r in 2..3 && c in 3..5 -> "W2"
                            (r == 1 && (c == 2 || c == 6)) || (r == 2 && (c == 2 || c == 6)) -> {
                                if ((r + c) % 2 == 0) "BR(PINK)" else "BR(RED)"
                            }
                            else -> "O"
                        }
                    }
                }
                LevelSpec(80, "Fortress of Despair", LevelDifficulty.HARD, 9, 9, grid, 23, colors6, Triple(28000, 46000, 70000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "WOODEN", 6)))
            }
            else -> error("Invalid tier 5 id $id")
        }
    }

    // =========================================================================
    // TIER 6: LEVELS 81-100 (Expert Levels Combining Multiple Mechanics)
    // =========================================================================
    private fun createTier6Spec(id: Int): LevelSpec {
        val colors6 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW, HeartColor.PURPLE)
        val colors7 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW, HeartColor.PURPLE, HeartColor.ORANGE)

        return when (id) {
            81 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 6 && c in 1..6 -> "D"
                            r in 3..4 && c in 2..4 -> "S2"
                            r in 1..2 && c in 3..5 -> if ((r + c) % 2 == 0) "I(RED)" else "I(BLUE)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(81, "Corrupted Citadel", LevelDifficulty.EXPERT, 8, 8, grid, 21, colors6, Triple(20000, 35000, 55000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "STONE", 6), ObjSpec("DESTROY", "ICE", 6)))
            }
            82 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 1..2 && c in 2..5 -> if ((r + c) % 2 == 0) "I(PINK)" else "I(YELLOW)"
                            r in 3..4 && c in 2..5 -> "W2"
                            r in 5..6 && c in 2..4 -> if ((r + c) % 2 == 0) "C(BLUE)" else "C(GREEN)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(82, "Frozen Labyrinth", LevelDifficulty.EXPERT, 8, 8, grid, 21, colors6, Triple(21000, 36000, 56000),
                    listOf(ObjSpec("DESTROY", "ICE", 8), ObjSpec("DESTROY", "WOODEN", 8), ObjSpec("DESTROY", "CHAINED", 6)))
            }
            83 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r == 6 && c in 1..6 -> "D"
                            r in 3..4 && c in 2..5 -> "B"
                            r in 1..2 && c in 2..4 -> if ((r + c) % 2 == 0) "BR(YELLOW)" else "BR(PINK)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(83, "Thorn & Shadow", LevelDifficulty.EXPERT, 8, 8, grid, 20, colors6, Triple(22000, 37000, 58000),
                    listOf(ObjSpec("CLEAR_DARK", null, 6), ObjSpec("DESTROY", "BARBED", 8), ObjSpec("REPAIR", null, 6)))
            }
            84 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 1..2 && c in 2..5 -> if ((r + c) % 2 == 0) "C(RED)" else "C(BLUE)"
                            r in 3..4 && c in 2..5 -> if ((r + c) % 2 == 0) "I(GREEN)" else "I(YELLOW)"
                            (r in 5..6 && c in 2..4) -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(84, "The Triple Lock", LevelDifficulty.EXPERT, 8, 8, grid, 21, colors6, Triple(23000, 38000, 60000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "ICE", 8), ObjSpec("DESTROY", "STONE", 6)))
            }
            85 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            (r == 1 && c == 1) || (r == 6 && c == 6) -> "GIFT"
                            r == 6 && c in 1..6 && c != 6 -> "D"
                            r in 3..4 && c in 2..4 -> if ((r + c) % 2 == 0) "C(PINK)" else "C(BLUE)"
                            r in 1..2 && c in 3..5 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(85, "Prismatic Fortress", LevelDifficulty.EXPERT, 8, 8, grid, 21, colors6, Triple(24000, 40000, 62000),
                    listOf(ObjSpec("CLEAR_DARK", null, 5), ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("COLLECT", "GIFT", 2)))
            }

            86 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 1..2 && c in 2..5 -> "B"
                            r in 3..4 && c in 2..4 -> if ((r + c) % 2 == 0) "C(BLUE)" else "C(GREEN)"
                            r in 5..6 && c in 3..5 -> if ((r + c) % 2 == 0) "I(YELLOW)" else "I(RED)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(86, "Barbed Gauntlet", LevelDifficulty.EXPERT, 8, 8, grid, 20, colors6, Triple(24000, 41000, 64000),
                    listOf(ObjSpec("DESTROY", "BARBED", 8), ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("DESTROY", "ICE", 6)))
            }
            87 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 1..2 && c in 2..5 -> if ((r + c) % 2 == 0) "BR(RED)" else "BR(BLUE)"
                            r == 6 && c in 2..5 -> "D"
                            r in 3..4 && c in 2..5 -> "W2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(87, "Mending the Abyss", LevelDifficulty.EXPERT, 8, 8, grid, 24, colors6, Triple(25000, 42000, 66000),
                    listOf(ObjSpec("REPAIR", null, 6), ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "WOODEN", 6)))
            }
            88 -> {
                val grid = Array(8) { r ->
                    Array(8) { c ->
                        when {
                            r in 1..2 && c in 2..5 -> if ((r + c) % 2 == 0) "I(PINK)" else "I(BLUE)"
                            r == 6 && c in 2..5 -> "D"
                            r in 3..4 && c in 2..5 -> if ((r + c) % 2 == 0) "C(GREEN)" else "C(YELLOW)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(88, "Glacial Nightmare", LevelDifficulty.EXPERT, 8, 8, grid, 24, colors6, Triple(26000, 44000, 68000),
                    listOf(ObjSpec("DESTROY", "ICE", 8), ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "CHAINED", 6)))
            }
            89 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r in 2..3 && c in 2..5 -> if ((r + c) % 2 == 0) "C(BLUE)" else "C(RED)"
                            r in 4..5 && c in 2..5 -> "B"
                            r in 6..7 && c in 3..5 -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(89, "Iron Labyrinth", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(27000, 45000, 70000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 8), ObjSpec("DESTROY", "BARBED", 6), ObjSpec("DESTROY", "STONE", 6)))
            }
            90 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            (r in 1..2 && c in 2..4) -> "S2"
                            (r in 1..2 && c in 5..7) -> if ((r + c) % 2 == 0) "I(RED)" else "I(BLUE)"
                            (r in 4..5 && c in 3..5) -> "W2"
                            (r == 7 && c in 3..6) -> "D"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(90, "The Four Elements", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(30000, 50000, 75000),
                    listOf(ObjSpec("DESTROY", "STONE", 6), ObjSpec("DESTROY", "ICE", 6), ObjSpec("CLEAR_DARK", null, 4)))
            }

            91 -> {
                // Crown layout
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        val isHole = (r == 0 && (c == 2 || c == 6)) || (r == 8 && (c == 0 || c == 8))
                        val tile = when {
                            r in 2..3 && c in 2..5 -> "B"
                            r in 4..5 && c in 2..5 -> if ((r + c) % 2 == 0) "C(PURPLE)" else "C(PINK)"
                            r in 6..7 && c in 3..5 -> if ((r + c) % 2 == 0) "BR(RED)" else "BR(BLUE)"
                            else -> "O"
                        }
                        if (isHole) "X" else tile
                    }
                }
                LevelSpec(91, "Crown of Thorns", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(28000, 46000, 72000),
                    listOf(ObjSpec("DESTROY", "BARBED", 6), ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("REPAIR", null, 6)))
            }
            92 -> {
                // Twin hearts layout
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        val isCut = (r == 0 && (c == 0 || c == 4 || c == 8)) || (r == 8 && (c < 2 || c > 6))
                        val tile = when {
                            r == 6 && (c in 2..3 || c in 5..6) -> "D"
                            r in 2..3 && c in 2..5 -> if ((r + c) % 2 == 0) "I(PINK)" else "I(BLUE)"
                            r in 4..5 && c in 3..5 -> "W2"
                            else -> "O"
                        }
                        if (isCut) "X" else tile
                    }
                }
                LevelSpec(92, "Twin Hearts of Darkness", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(29000, 48000, 74000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "ICE", 6), ObjSpec("DESTROY", "WOODEN", 6)))
            }
            93 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            (r == 1 && c == 1) || (r == 7 && c == 7) -> "GIFT"
                            r == 7 && c in 3..5 -> "D"
                            r in 3..4 && c in 3..5 -> if ((r + c) % 2 == 0) "C(YELLOW)" else "C(BLUE)"
                            r in 1..2 && (c == 2 || c == 6) -> "S1"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(93, "Cataclysmic Core", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(30000, 50000, 76000),
                    listOf(ObjSpec("CLEAR_DARK", null, 3), ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("COLLECT", "GIFT", 2)))
            }
            94 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r == 7 && c in 3..6 -> "D"
                            r in 3..4 && c in 2..5 -> "B"
                            r in 1..2 && c in 2..5 -> if ((r + c) % 2 == 0) "I(BLUE)" else "I(GREEN)"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(94, "Chamber of Shadows", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(31000, 52000, 80000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "BARBED", 6), ObjSpec("DESTROY", "ICE", 6)))
            }
            95 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r in 1..2 && c in 2..5 -> if ((r + c) % 2 == 0) "C(RED)" else "C(BLUE)"
                            r in 3..4 && c in 3..5 -> "W2"
                            r in 5..6 && c in 3..5 -> if ((r + c) % 2 == 0) "BR(GREEN)" else "BR(YELLOW)"
                            (r == 7 && c in 3..5) -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(95, "The Hexagonal Siege", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(32000, 54000, 82000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("DESTROY", "WOODEN", 6), ObjSpec("REPAIR", null, 6)))
            }

            96 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r == 7 && c in 3..6 -> "D"
                            r in 3..4 && c in 2..5 -> if ((r + c) % 2 == 0) "C(PINK)" else "C(BLUE)"
                            r in 1..2 && c in 2..5 -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(96, "Heart of Darkness", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(34000, 56000, 85000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("DESTROY", "STONE", 6)))
            }
            97 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r == 7 && c in 3..6 -> "D"
                            r in 4..5 && c in 2..5 -> if ((r + c) % 2 == 0) "I(BLUE)" else "I(PINK)"
                            r in 2..3 && c in 2..5 -> "W2"
                            r in 0..1 && c in 3..5 -> "B"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(97, "Prismatic Siege", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(36000, 60000, 90000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "ICE", 6), ObjSpec("DESTROY", "WOODEN", 6)))
            }
            98 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r in 2..3 && c in 2..5 -> if ((r + c) % 2 == 0) "C(YELLOW)" else "C(BLUE)"
                            r in 4..5 && c in 3..5 -> "B"
                            r in 0..1 && c in 3..5 -> if ((r + c) % 2 == 0) "BR(RED)" else "BR(PINK)"
                            r == 7 && c in 3..6 -> "D"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(98, "Eternal Chains", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(38000, 62000, 95000),
                    listOf(ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("CLEAR_DARK", null, 4), ObjSpec("REPAIR", null, 6)))
            }
            99 -> {
                val grid = Array(9) { r ->
                    Array(9) { c ->
                        when {
                            r == 7 && c in 3..6 -> "D"
                            r in 3..4 && c in 2..5 -> if ((r + c) % 2 == 0) "C(RED)" else "C(PINK)"
                            r in 1..2 && c in 2..5 -> "W2"
                            r == 0 && c in 3..5 -> "S2"
                            else -> "O"
                        }
                    }
                }
                LevelSpec(99, "The Grand Crucible", LevelDifficulty.EXPERT, 9, 9, grid, 24, colors6, Triple(40000, 68000, 100000),
                    listOf(ObjSpec("CLEAR_DARK", null, 4), ObjSpec("DESTROY", "CHAINED", 6), ObjSpec("DESTROY", "WOODEN", 6)))
            }
            100 -> {
                // Heart-shaped Master Layout
                val grid = Array(9) { r ->
                    val isRowOutside = r !in 0..8
                    Array(9) { c ->
                        val isOutsideHeart = (r == 0 && (c == 0 || c == 4 || c == 8)) ||
                                (r == 1 && (c == 0 || c == 8)) ||
                                (r == 7 && (c < 2 || c > 6)) ||
                                (r == 8 && (c < 3 || c > 5))
                        val tile = when {
                            (r == 3 && c == 1) || (r == 3 && c == 7) -> "GIFT"
                            r == 5 && c in 2..5 -> "D"
                            r in 3..4 && c in 2..5 -> if ((r + c) % 2 == 0) "C(PINK)" else "C(BLUE)"
                            r in 1..2 && (c == 3 || c == 5) -> if ((r + c) % 2 == 0) "I(GREEN)" else "I(RED)"
                            (r == 6 && c in 3..5) -> "W2"
                            else -> "O"
                        }
                        if (isOutsideHeart) "X" else tile
                    }
                }
                val colors5 = listOf(HeartColor.RED, HeartColor.PINK, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)
                LevelSpec(100, "Heart Match Master Champion", LevelDifficulty.EXPERT, 9, 9, grid, 26, colors5, Triple(35000, 55000, 80000),
                    listOf(
                        ObjSpec("CLEAR_DARK", null, 4),
                        ObjSpec("DESTROY", "CHAINED", 6),
                        ObjSpec("DESTROY", "ICE", 4),
                        ObjSpec("COLLECT", "GIFT", 2),
                        ObjSpec("SCORE", null, 35000)
                    ))
            }
            else -> error("Invalid tier 6 id $id")
        }
    }
}
