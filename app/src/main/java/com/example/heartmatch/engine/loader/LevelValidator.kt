package com.example.heartmatch.engine.loader

import com.example.heartmatch.engine.model.*

data class LevelValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    fun assertValid() {
        if (!isValid) {
            throw IllegalArgumentException("Invalid level configuration:\n" + errors.joinToString("\n- ", prefix = "- "))
        }
    }
}

class LevelValidator {

    fun validate(config: LevelConfig): LevelValidationResult {
        val errors = mutableListOf<String>()

        // 1. Dimensions
        if (config.rows < 3 || config.rows > 30) {
            errors.add("Level rows must be between 3 and 30 (got ${config.rows})")
        }
        if (config.cols < 3 || config.cols > 30) {
            errors.add("Level columns must be between 3 and 30 (got ${config.cols})")
        }

        // 2. Playable cell count
        var playableCount = 0
        if (config.cellStates != null) {
            if (config.cellStates.size != config.rows || config.cellStates.any { it.size != config.cols }) {
                errors.add("cellStates grid size does not match level dimensions (${config.rows}x${config.cols})")
            } else {
                for (r in 0 until config.rows) {
                    for (c in 0 until config.cols) {
                        if (config.cellStates[r][c] == CellState.PLAYABLE) {
                            playableCount++
                        }
                    }
                }
                if (playableCount < 3) {
                    errors.add("Board must contain at least 3 playable cells (found $playableCount)")
                }
            }
        } else {
            playableCount = config.rows * config.cols
        }

        // 3. Initial tiles bounds
        if (config.initialTiles != null) {
            if (config.initialTiles.size != config.rows || config.initialTiles.any { it.size != config.cols }) {
                errors.add("initialTiles grid size does not match level dimensions (${config.rows}x${config.cols})")
            }
        }

        // 4. Move and time limit
        val hasValidMoves = config.moveLimit != null && config.moveLimit > 0
        val hasValidTime = config.timeLimitSeconds != null && config.timeLimitSeconds > 0
        if (!hasValidMoves && !hasValidTime) {
            errors.add("Level must have either a positive moveLimit or positive timeLimitSeconds")
        }

        // 5. Allowed colors
        val uniqueColors = config.allowedColors.distinct()
        if (uniqueColors.size < 3) {
            errors.add("Level must specify at least 3 distinct allowed colors (got ${uniqueColors.size}: ${uniqueColors.joinToString()})")
        }

        // 6. Star thresholds
        val (s1, s2, s3) = config.starThresholds
        if (s1 <= 0 || s2 <= s1 || s3 <= s2) {
            errors.add("starThresholds must be positive and strictly ascending (got [$s1, $s2, $s3])")
        }

        // 7. Objectives
        config.objectives.forEachIndexed { index, obj ->
            validateObjective(obj, config, index, errors)
        }

        return LevelValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun validateObjective(
        obj: ObjectiveConfig,
        config: LevelConfig,
        index: Int,
        errors: MutableList<String>
    ) {
        val label = "Objective #$index (${obj.type})"

        when (obj.type) {
            ObjectiveType.COLLECT_COLOR, ObjectiveType.COLLECT_HEARTS -> {
                if (obj.targetCount <= 0) {
                    errors.add("$label targetCount must be > 0 (got ${obj.targetCount})")
                }
                if (obj.targetColor != null && obj.targetColor !in config.allowedColors) {
                    errors.add("$label targetColor (${obj.targetColor}) is not in level's allowedColors (${config.allowedColors.joinToString()})")
                }
            }
            ObjectiveType.DESTROY_BLOCKERS, ObjectiveType.CLEAR_BLOCKER -> {
                if (obj.targetCount <= 0) {
                    errors.add("$label targetCount must be > 0 (got ${obj.targetCount})")
                }
            }
            ObjectiveType.REPAIR_BROKEN -> {
                if (obj.targetCount <= 0) {
                    errors.add("$label targetCount must be > 0 (got ${obj.targetCount})")
                }
            }
            ObjectiveType.CLEAR_DARK_HEARTS -> {
                if (obj.targetCount <= 0) {
                    errors.add("$label targetCount must be > 0 (got ${obj.targetCount})")
                }
            }
            ObjectiveType.COLLECT_SPECIAL, ObjectiveType.CREATE_SPECIALS, ObjectiveType.COLLECT_GIFT -> {
                if (obj.targetCount <= 0) {
                    errors.add("$label targetCount must be > 0 (got ${obj.targetCount})")
                }
            }
            ObjectiveType.SCORE, ObjectiveType.REACH_SCORE -> {
                if (obj.targetCount <= 0) {
                    errors.add("$label targetCount must be > 0 (got ${obj.targetCount})")
                }
            }
            ObjectiveType.CLEAR_BOARD -> {
                // targetCount can be positive or 0 (auto-derived)
            }
            ObjectiveType.CLEAR_SPECIFIC_CELLS -> {
                if (obj.targetCells.isEmpty() && obj.targetCount <= 0) {
                    errors.add("$label must specify targetCells coordinates or a positive targetCount")
                }
                for (coord in obj.targetCells) {
                    if (coord.row !in 0 until config.rows || coord.col !in 0 until config.cols) {
                        errors.add("$label target cell ($coord) is out of board bounds (${config.rows}x${config.cols})")
                    }
                }
            }
        }
    }
}
