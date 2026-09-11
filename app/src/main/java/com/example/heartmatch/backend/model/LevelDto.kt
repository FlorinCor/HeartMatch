package com.example.heartmatch.backend.model

import com.example.heartmatch.engine.loader.JsonObject
import com.example.heartmatch.engine.loader.JsonParser
import com.example.heartmatch.engine.model.LevelConfig
import com.example.heartmatch.engine.model.LevelDifficulty
import com.example.heartmatch.engine.model.ObjectiveConfig

data class ObjectiveSummary(
    val type: String,
    val targetCount: Int,
    val targetColor: String? = null,
    val targetBlocker: String? = null,
    val targetSpecial: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type,
        "targetCount" to targetCount,
        "targetColor" to targetColor,
        "targetBlocker" to targetBlocker,
        "targetSpecial" to targetSpecial
    ).filterValues { it != null }

    companion object {
        fun fromConfig(config: ObjectiveConfig): ObjectiveSummary {
            return ObjectiveSummary(
                type = config.type.name,
                targetCount = config.targetCount,
                targetColor = config.targetColor?.name,
                targetBlocker = config.targetBlocker?.name,
                targetSpecial = config.targetSpecial?.name
            )
        }
    }
}

data class LevelSummary(
    val levelId: Int,
    val name: String,
    val difficulty: String,
    val moves: Int,
    val starThresholds: List<Int>,
    val objectives: List<ObjectiveSummary>
) {
    fun toMap(): Map<String, Any> = mapOf(
        "levelId" to levelId,
        "name" to name,
        "difficulty" to difficulty,
        "moves" to moves,
        "starThresholds" to starThresholds,
        "objectives" to objectives.map { it.toMap() }
    )

    companion object {
        fun fromConfig(config: LevelConfig): LevelSummary {
            return LevelSummary(
                levelId = config.id,
                name = config.name,
                difficulty = config.difficulty.name,
                moves = config.moveLimit ?: 30,
                starThresholds = config.starThresholds.toList(),
                objectives = config.objectives.map { ObjectiveSummary.fromConfig(it) }
            )
        }
    }
}

data class LevelDetail(
    val levelId: Int,
    val configuration: Map<String, Any?>,
    val difficulty: String,
    val objectives: List<ObjectiveSummary>
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "levelId" to levelId,
        "configuration" to configuration,
        "difficulty" to difficulty,
        "objectives" to objectives.map { it.toMap() }
    )

    companion object {
        fun fromConfig(config: LevelConfig, rawJson: String? = null): LevelDetail {
            val configMap = if (rawJson != null) {
                try {
                    val parsed = JsonParser.parse(rawJson).asObject()
                    jsonObjectToMap(parsed)
                } catch (e: Exception) {
                    configToMap(config)
                }
            } else {
                configToMap(config)
            }

            return LevelDetail(
                levelId = config.id,
                configuration = configMap,
                difficulty = config.difficulty.name,
                objectives = config.objectives.map { ObjectiveSummary.fromConfig(it) }
            )
        }

        private fun configToMap(config: LevelConfig): Map<String, Any?> {
            return mapOf(
                "id" to config.id,
                "name" to config.name,
                "difficulty" to config.difficulty.name,
                "rows" to config.rows,
                "cols" to config.cols,
                "moveLimit" to config.moveLimit,
                "timeLimitSeconds" to config.timeLimitSeconds,
                "starThresholds" to config.starThresholds.toList(),
                "allowedColors" to config.allowedColors.map { it.name }
            )
        }

        private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> {
            val map = mutableMapOf<String, Any?>()
            obj.members.forEach { (k, v) ->
                map[k] = jsonElementToAny(v)
            }
            return map
        }

        private fun jsonElementToAny(elem: com.example.heartmatch.engine.loader.JsonElement): Any? {
            return when (elem) {
                is com.example.heartmatch.engine.loader.JsonPrimitive -> {
                    if (elem.isString) elem.content
                    else if (elem.content == "true" || elem.content == "false") elem.content.toBoolean()
                    else if (elem.content.contains('.')) elem.content.toDoubleOrNull() ?: elem.content
                    else elem.content.toLongOrNull() ?: elem.content
                }
                is com.example.heartmatch.engine.loader.JsonObject -> jsonObjectToMap(elem)
                is com.example.heartmatch.engine.loader.JsonArray -> elem.elements.map { jsonElementToAny(it) }
                is com.example.heartmatch.engine.loader.JsonNull -> null
            }
        }
    }
}
