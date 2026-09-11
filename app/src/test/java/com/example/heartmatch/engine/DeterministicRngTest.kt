package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.DeterministicRng
import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class DeterministicRngTest {

    @Test
    fun testSameSeedProducesIdenticalOutput() {
        val seed = 987654321L
        val rng1 = DeterministicRng(seed)
        val rng2 = DeterministicRng(seed)

        val colors = listOf(HeartColor.RED, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)

        val list1 = (1..50).map { rng1.pickWeightedColor(colors) }
        val list2 = (1..50).map { rng2.pickWeightedColor(colors) }

        assertEquals(list1, list2)
    }

    @Test
    fun testDifferentSeedProducesDifferentOutput() {
        val rng1 = DeterministicRng(111L)
        val rng2 = DeterministicRng(999L)

        val colors = listOf(HeartColor.RED, HeartColor.BLUE, HeartColor.GREEN, HeartColor.YELLOW)

        val list1 = (1..50).map { rng1.pickWeightedColor(colors) }
        val list2 = (1..50).map { rng2.pickWeightedColor(colors) }

        assertNotEquals(list1, list2)
    }

    @Test
    fun testEngineDeterminismAcrossTurns() {
        fun runSimulation(seed: Long): List<Pair<Int, Int>> {
            val engine = HeartMatchEngine()
            val config = LevelConfig(
                id = 1,
                name = "Test Level",
                rows = 7,
                cols = 7,
                randomSeed = seed,
                moveLimit = 10
            )
            engine.loadLevel(config)

            val movesScoreList = mutableListOf<Pair<Int, Int>>()
            for (step in 1..5) {
                val possibleMoves = engine.getPossibleMoves()
                if (possibleMoves.isEmpty()) break
                val (from, to) = possibleMoves.first()
                val result = engine.swap(from, to)
                movesScoreList.add(result.scoreDelta to engine.getState().score)
            }
            return movesScoreList
        }

        val run1 = runSimulation(42L)
        val run2 = runSimulation(42L)

        assertEquals(run1, run2)
    }
}
