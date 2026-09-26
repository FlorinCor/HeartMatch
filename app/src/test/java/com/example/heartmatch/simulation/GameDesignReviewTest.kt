package com.example.heartmatch.simulation

import com.example.heartmatch.engine.loader.LevelJsonParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Uses the shipped assets, never the generated fallback. Reports are reproducible seed sweeps. */
class GameDesignReviewTest {
    @Test fun packagedCampaignReview() {
        val assets = File("src/main/assets/levels")
        assertEquals(200, assets.listFiles()!!.count { it.extension == "json" })
        val simulator = GameSimulator()
        val report = File("build/reports/balance").apply { mkdirs() }
        val rows = mutableListOf("level,strategy,samples,win_rate,remaining_on_win,mean_score,one_star,two_star,three_star,boosters_per_game")
        val calibration = mutableListOf("level,wins,two_star,three_star")
        for (level in 1..200) {
            val config = LevelJsonParser().parse(File(assets, "level_%03d.json".format(level)).readText())
            val all = AgentStrategy.entries.associateWith { strategy ->
                (1L..8L).map { seed -> simulator.simulateGame(config, seed * 997 + level, strategy) }
            }
            assertTrue("Level $level must have an unassisted objective-focused win in the fixed seed set", all.getValue(AgentStrategy.STRATEGIC).any { it.won })
            all.forEach { (strategy, results) ->
                val wins = results.filter { it.won }
                assertTrue(results.all { it.turnsPlayed <= 150 })
                assertTrue(wins.all { it.earnedStars in 1..3 })
                rows += listOf(level, strategy, results.size, wins.size / 8.0,
                    if (wins.isEmpty()) 0.0 else wins.map { it.movesRemaining }.average(),
                    results.map { it.finalScore }.average(), wins.count { it.earnedStars == 1 },
                    wins.count { it.earnedStars == 2 }, wins.count { it.earnedStars == 3 },
                    results.map { it.boostersUsed.values.sum() }.average()).joinToString(",")
            }
            val scores = all.filterKeys { it != AgentStrategy.BOOSTER_ASSISTED }.values.flatten().filter { it.won }.map { it.finalScore }.sorted()
            if (scores.size >= 4) {
                fun percentile(f: Double) = ((scores[((scores.size - 1) * f).toInt()] + 99) / 100) * 100
                val two = percentile(0.40).coerceAtLeast(200)
                calibration += "$level,${scores.size},$two,${percentile(0.80).coerceAtLeast(two + 100)}"
            } else calibration += "$level,${scores.size},${config.starThresholds.second},${config.starThresholds.third}"
        }
        File(report, "campaign.csv").writeText(rows.joinToString("\n") + "\n")
        File(report, "star-calibration.csv").writeText(calibration.joinToString("\n") + "\n")
    }
}
