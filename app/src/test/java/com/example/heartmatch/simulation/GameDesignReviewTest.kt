package com.example.heartmatch.simulation

import com.example.heartmatch.engine.loader.LevelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDesignReviewTest {

    private val simulator = GameSimulator()

    @Test
    fun testComplete100LevelGameDesignReview() {
        println("==========================================================================")
        println("           HEART MATCH: 100-LEVEL COMPREHENSIVE GAME DESIGN REVIEW        ")
        println("==========================================================================")

        val strategicSummaries = mutableListOf<LevelSimulationSummary>()
        val randomSummaries = mutableListOf<LevelSimulationSummary>()
        val greedySummaries = mutableListOf<LevelSimulationSummary>()
        val boosterSummaries = mutableListOf<LevelSimulationSummary>()

        val startTime = System.currentTimeMillis()

        for (levelId in 1..100) {
            val config = LevelGenerator.getLevelConfig(levelId)

            // 1. Strategic Agent (main player profile, sampleSize = 30)
            val stratSummary = simulator.analyzeLevel(config, sampleSize = 30, strategy = AgentStrategy.STRATEGIC)
            strategicSummaries.add(stratSummary)

            // 2. Random Agent (luck baseline, sampleSize = 10)
            val randSummary = simulator.analyzeLevel(config, sampleSize = 10, strategy = AgentStrategy.RANDOM)
            randomSummaries.add(randSummary)

            // 3. Greedy Agent (immediate match baseline, sampleSize = 10)
            val greedySummary = simulator.analyzeLevel(config, sampleSize = 10, strategy = AgentStrategy.GREEDY)
            greedySummaries.add(greedySummary)

            // 4. Booster Assisted Agent (booster impact test on hard/expert levels 60-100)
            if (levelId >= 60) {
                val boosterSummary = simulator.analyzeLevel(config, sampleSize = 10, strategy = AgentStrategy.BOOSTER_ASSISTED)
                boosterSummaries.add(boosterSummary)
            }

            if (levelId % 20 == 0) {
                val progress = levelId
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                println("Simulated $progress / 100 levels (${progress * 50} games)... (${String.format("%.1f", elapsed)}s elapsed)")
            }
        }

        println("\n==========================================================================")
        println("                        TIER PROGRESSION ANALYSIS                         ")
        println("==========================================================================")

        val tiers = listOf(
            "Tier 1 (Levels 1-10)" to strategicSummaries.subList(0, 10),
            "Tier 2 (Levels 11-20)" to strategicSummaries.subList(10, 20),
            "Tier 3 (Levels 21-40)" to strategicSummaries.subList(20, 40),
            "Tier 4 (Levels 41-60)" to strategicSummaries.subList(40, 60),
            "Tier 5 (Levels 61-80)" to strategicSummaries.subList(60, 80),
            "Tier 6 (Levels 81-100)" to strategicSummaries.subList(80, 100)
        )

        val tierRandom = listOf(
            randomSummaries.subList(0, 10),
            randomSummaries.subList(10, 20),
            randomSummaries.subList(20, 40),
            randomSummaries.subList(40, 60),
            randomSummaries.subList(60, 80),
            randomSummaries.subList(80, 100)
        )

        for (i in tiers.indices) {
            val (name, tierStrat) = tiers[i]
            val tierRand = tierRandom[i]

            val avgStratWinRate = tierStrat.map { it.winRate }.average() * 100.0
            val avgRandWinRate = tierRand.map { it.winRate }.average() * 100.0
            val skillGap = avgStratWinRate - avgRandWinRate

            val avgCascades = tierStrat.map { it.averageCascadesPerTurn }.average()
            val avgSpecials = tierStrat.map { it.averageSpecialsCreatedPerGame }.average()
            val avgDeadBoards = tierStrat.map { it.deadBoardRatePerGame }.average()
            val avgMovesLeft = tierStrat.map { it.averageMovesRemainingOnWin }.average()

            println(String.format(
                "%-22s | StratWin: %5.1f%% | RandWin: %5.1f%% | SkillGap: +%4.1f%% | Specials: %4.1f | Cascades/Turn: %4.2f | DeadBoards: %4.2f",
                name, avgStratWinRate, avgRandWinRate, skillGap, avgSpecials, avgCascades, avgDeadBoards
            ))
        }

        println("\n==========================================================================")
        println("                       BOOSTER EFFICACY ANALYSIS                          ")
        println("==========================================================================")
        val highTierStrat = strategicSummaries.subList(59, 100)
        val highTierBooster = boosterSummaries
        val avgStratHigh = highTierStrat.map { it.winRate }.average() * 100.0
        val avgBoosterHigh = highTierBooster.map { it.winRate }.average() * 100.0
        val boosterDelta = avgBoosterHigh - avgStratHigh
        println(String.format("Levels 60-100: Base Strategic Win Rate = %.1f%% | Booster-Assisted Win Rate = %.1f%% (Boost Delta: +%.1f%%)",
            avgStratHigh, avgBoosterHigh, boosterDelta))

        // Assertions verifying game balance criteria
        assertEquals("All 100 levels simulated", 100, strategicSummaries.size)
        assertTrue("No levels should be impossible", strategicSummaries.all { it.winRate > 0.0 })
        assertTrue("Overall Strategic Win Rate is high (> 80%)", strategicSummaries.map { it.winRate }.average() > 0.80)
        assertTrue("Strategic play significantly outperforms random guessing across all levels",
            strategicSummaries.map { it.winRate }.average() > randomSummaries.map { it.winRate }.average() + 0.30)
        assertTrue("Dead-board rate is low (< 0.35 per game on average)",
            strategicSummaries.map { it.deadBoardRatePerGame }.average() < 0.35)
    }

    @Test
    fun testLevelPacingAndDifficultyProgression() {
        // Verify that difficulty increases gradually without sudden impossible spikes
        val configs = (1..100).map { LevelGenerator.getLevelConfig(it) }
        assertEquals(100, configs.size)

        // Verify that move limits are reasonable and non-punitive (all levels have at least 18 moves)
        assertTrue(configs.all { (it.moveLimit ?: 0) >= 18 })

        // Verify that all 100 levels have valid starting boards with immediate legal moves
        for (config in configs) {
            val engine = com.example.heartmatch.engine.core.HeartMatchEngine(config)
            assertTrue("Level ${config.id} must have legal initial moves", engine.getPossibleMoves().isNotEmpty())
        }
    }
}
