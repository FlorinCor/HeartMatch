package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.BlockerHandler
import com.example.heartmatch.engine.core.DeterministicRng
import com.example.heartmatch.engine.core.HeartMatchEngine
import com.example.heartmatch.engine.core.TileSpawner
import com.example.heartmatch.engine.core.TurnPipeline
import com.example.heartmatch.engine.model.*
import org.junit.Assert.*
import org.junit.Test

class BlockerTest {

    private val blockerHandler = BlockerHandler()

    @Test
    fun testStoneHeartCannotBeSwappedAndTakesAdjacentDamage() {
        val stone = Tile.Blocker.createStone(durability = 3)
        assertFalse("Stone Heart cannot be swapped", stone.isMovable)
        assertFalse("Stone Heart cannot be directly matched", stone.isMatchable)
        assertTrue("Stone Heart can fall with gravity", stone.canFall)
        assertEquals(3, stone.durability)
        assertEquals(3, stone.hitPoints)

        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(2, 2), stone)

        // Hit 1: adjacent match at (2, 1)
        val result1 = blockerHandler.applyBlockerDamage(
            board = board,
            matchedCoords = setOf(Coord(2, 1)),
            directHitCoords = emptySet()
        )
        assertEquals(1, result1.damagedBlockers.size)
        assertEquals(0, result1.destroyedBlockers.size)
        val stone1 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(2, stone1.durability)

        // Hit 2: adjacent match at (1, 2)
        val result2 = blockerHandler.applyBlockerDamage(
            board = board,
            matchedCoords = setOf(Coord(1, 2)),
            directHitCoords = emptySet()
        )
        assertEquals(1, result2.damagedBlockers.size)
        val stone2 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(1, stone2.durability)

        // Hit 3: adjacent match at (3, 2) destroys it
        val result3 = blockerHandler.applyBlockerDamage(
            board = board,
            matchedCoords = setOf(Coord(3, 2)),
            directHitCoords = emptySet()
        )
        assertEquals(1, result3.destroyedBlockers.size)
        assertNull(board.getTile(Coord(2, 2)))
    }

    @Test
    fun testIceHeartHitPointsAndPayloadRelease() {
        val ice = Tile.Blocker.createIce(
            hitPoints = 2,
            payload = Tile.Normal(color = HeartColor.PINK),
            color = HeartColor.PINK
        )
        assertFalse("Ice Heart cannot be swapped", ice.isMovable)
        assertEquals(2, ice.hitPoints)
        assertEquals(2, ice.durability)

        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(2, 2), ice)

        // Hit 1 reduces hit points
        val result1 = blockerHandler.applyBlockerDamage(
            board = board,
            matchedCoords = setOf(Coord(2, 3)),
            directHitCoords = emptySet()
        )
        assertEquals(1, result1.damagedBlockers.size)
        val iceAfterHit1 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(1, iceAfterHit1.hitPoints)

        // Hit 2 shatters ice and releases payload
        val result2 = blockerHandler.applyBlockerDamage(
            board = board,
            matchedCoords = setOf(Coord(2, 1)),
            directHitCoords = emptySet()
        )
        assertEquals(1, result2.destroyedBlockers.size)
        val freedTile = board.getTile(Coord(2, 2))
        assertTrue(freedTile is Tile.Normal)
        assertEquals(HeartColor.PINK, (freedTile as Tile.Normal).color)
        assertTrue(freedTile.isMovable)
    }

    @Test
    fun testWoodenHeartMultiLayerProgression() {
        val wood = Tile.Blocker.createWooden(layers = 3)
        assertFalse("Wooden Heart cannot be swapped", wood.isMovable)
        assertFalse("Wooden Heart cannot be directly matched", wood.isMatchable)
        assertEquals(3, wood.layers)

        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(2, 2), wood)

        // Layer 1 removed
        blockerHandler.applyBlockerDamage(board, setOf(Coord(2, 1)), emptySet())
        val wood1 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(2, wood1.layers)

        // Layer 2 removed
        blockerHandler.applyBlockerDamage(board, setOf(Coord(2, 3)), emptySet())
        val wood2 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(1, wood2.layers)

        // Layer 3 removed -> Destroyed
        val result = blockerHandler.applyBlockerDamage(board, setOf(Coord(1, 2)), emptySet())
        assertEquals(1, result.destroyedBlockers.size)
        assertNull(board.getTile(Coord(2, 2)))
    }

    @Test
    fun testBarbedHeartDamagedByAdjacentMatches() {
        val barbed = Tile.Blocker.createBarbed(durability = 2)
        assertFalse("Barbed Heart cannot be directly matched", barbed.isMatchable)
        assertFalse("Barbed Heart cannot be swapped", barbed.isMovable)

        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(2, 2), barbed)

        // Adjacent match damages barbed heart
        val res1 = blockerHandler.applyBlockerDamage(board, setOf(Coord(2, 1)), emptySet())
        assertEquals(1, res1.damagedBlockers.size)
        val barbed1 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(1, barbed1.durability)

        // Second adjacent hit destroys barbed heart
        val res2 = blockerHandler.applyBlockerDamage(board, setOf(Coord(3, 2)), emptySet())
        assertEquals(1, res2.destroyedBlockers.size)
        assertNull(board.getTile(Coord(2, 2)))
    }

    @Test
    fun testBrokenHeartRepairsIntoNormalHeart() {
        val broken = Tile.Blocker.createBroken(repairsRequired = 2, color = HeartColor.GREEN)
        assertTrue("Broken Heart can be moved", broken.isMovable)
        assertFalse("Broken Heart cannot be matched directly", broken.isMatchable)
        assertEquals(2, broken.repairsRemaining)

        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(2, 2), broken)

        // Repair 1: adjacent match at (2, 1)
        val res1 = blockerHandler.applyBlockerDamage(board, setOf(Coord(2, 1)), emptySet())
        assertEquals(1, res1.damagedBlockers.size)
        val broken1 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(1, broken1.repairsRemaining)

        // Repair 2: adjacent match at (2, 3) completes repair
        val res2 = blockerHandler.applyBlockerDamage(board, setOf(Coord(2, 3)), emptySet())
        assertEquals(1, res2.destroyedBlockers.size)

        val repairedTile = board.getTile(Coord(2, 2))
        assertNotNull(repairedTile)
        assertTrue("Broken Heart becomes a normal heart", repairedTile is Tile.Normal)
        assertEquals(HeartColor.GREEN, (repairedTile as Tile.Normal).color)
        assertTrue(repairedTile.isMovable)
        assertTrue(repairedTile.isMatchable)
    }

    @Test
    fun testChainedHeartReducesChainCountAndUnlocks() {
        val chained = Tile.Blocker.createChained(
            chainCount = 2,
            payload = Tile.Normal(color = HeartColor.YELLOW),
            color = HeartColor.YELLOW
        )
        assertFalse("Chained Heart cannot move", chained.isMovable)
        assertFalse("Chained Heart cannot fall", chained.canFall)
        assertTrue("Chained Heart is matchable", chained.isMatchable)
        assertEquals(2, chained.chainCount)

        val board = Board.createEmpty(5, 5)
        board.setTile(Coord(1, 1), chained)

        // Hit 1: adjacent match
        val res1 = blockerHandler.applyBlockerDamage(board, setOf(Coord(1, 2)), emptySet())
        assertEquals(1, res1.damagedBlockers.size)
        val chained1 = board.getTile(Coord(1, 1)) as Tile.Blocker
        assertEquals(1, chained1.chainCount)

        // Hit 2: adjacent match breaks chain
        val res2 = blockerHandler.applyBlockerDamage(board, setOf(Coord(2, 1)), emptySet())
        assertEquals(1, res2.destroyedBlockers.size)

        val unlockedTile = board.getTile(Coord(1, 1))
        assertTrue(unlockedTile is Tile.Normal)
        assertEquals(HeartColor.YELLOW, (unlockedTile as Tile.Normal).color)
        assertTrue(unlockedTile.isMovable)
        assertTrue(unlockedTile.canFall)
    }

    @Test
    fun testDarkHeartSpreadsAfterConfigurableSurvivingTurns() {
        val board = Board.createEmpty(5, 5)
        // Dark heart with spread interval of 2 turns
        val darkHeart = Tile.Blocker.createDark(spreadIntervalTurns = 2)
        board.setTile(Coord(2, 2), darkHeart)
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.RED))

        val rng = DeterministicRng(42L)

        // Turn 1 (undamaged): survives 1 turn, should NOT spread yet (interval is 2)
        val spreadTurn1 = blockerHandler.processDarkHeartSpread(board, darkHeartDamagedThisTurn = false, rng = rng)
        assertNull("Should not spread on turn 1", spreadTurn1)
        val dhAfterTurn1 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(1, dhAfterTurn1.turnsSurvived)

        // Turn 2 (undamaged): reaches 2 turns survived, spreads to (2, 3)
        val spreadTurn2 = blockerHandler.processDarkHeartSpread(board, darkHeartDamagedThisTurn = false, rng = rng)
        assertNotNull("Should spread on turn 2", spreadTurn2)
        assertEquals(Coord(2, 2), spreadTurn2!!.source)
        assertEquals(Coord(2, 3), spreadTurn2.target)

        val targetTile = board.getTile(Coord(2, 3))
        assertTrue(targetTile is Tile.Blocker)
        assertEquals(BlockerType.DARK_HEART, (targetTile as Tile.Blocker).blockerType)

        // Source turn count is reset to 0
        val sourceAfterSpread = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(0, sourceAfterSpread.turnsSurvived)
    }

    @Test
    fun testDarkHeartDamagedResetsTurnSurvivalCounter() {
        val board = Board.createEmpty(5, 5)
        val darkHeart = Tile.Blocker.createDark(spreadIntervalTurns = 2, durability = 2)
        board.setTile(Coord(2, 2), darkHeart)
        board.setTile(Coord(2, 3), Tile.Normal(color = HeartColor.RED))

        val rng = DeterministicRng(42L)

        // Turn 1: survives undamaged (turnsSurvived = 1)
        blockerHandler.processDarkHeartSpread(board, darkHeartDamagedThisTurn = false, rng = rng)
        val dh1 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(1, dh1.turnsSurvived)

        // Turn 2: damaged this turn -> turn counter resets to 0 and no spread
        val spreadTurn2 = blockerHandler.processDarkHeartSpread(board, darkHeartDamagedThisTurn = true, rng = rng)
        assertNull(spreadTurn2)
        val dh2 = board.getTile(Coord(2, 2)) as Tile.Blocker
        assertEquals(0, dh2.turnsSurvived)
    }

    @Test
    fun testConfigurableBlockerPerLevel() {
        val levelConfig = LevelConfig(
            id = 10,
            name = "Blocker Showcase",
            rows = 6,
            cols = 6,
            blockerConfig = BlockerConfig(
                stoneDurability = 4,
                iceHitPoints = 3,
                woodenLayers = 4,
                barbedDurability = 3,
                brokenRepairsRequired = 3,
                chainedChainCount = 3,
                darkHeartSpreadTurns = 3
            )
        )

        assertEquals(4, levelConfig.blockerConfig.stoneDurability)
        assertEquals(3, levelConfig.blockerConfig.iceHitPoints)
        assertEquals(4, levelConfig.blockerConfig.woodenLayers)
        assertEquals(3, levelConfig.blockerConfig.barbedDurability)
        assertEquals(3, levelConfig.blockerConfig.brokenRepairsRequired)
        assertEquals(3, levelConfig.blockerConfig.chainedChainCount)
        assertEquals(3, levelConfig.blockerConfig.darkHeartSpreadTurns)
    }

    @Test
    fun testTurnPipelineIntegrationWithBrokenAndChainedHearts() {
        val board = Board.createEmpty(5, 5)
        // Fill entire board with a checkered pattern of BLUE and GREEN so no initial matches exist
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val color = if ((r + c) % 2 == 0) HeartColor.BLUE else HeartColor.GREEN
                board.setTile(Coord(r, c), Tile.Normal(color = color))
            }
        }

        // Setup a swap move on row 2:
        // (2, 0) = RED, (2, 1) = RED, (2, 2) = YELLOW
        // (1, 2) = RED (swapping (1, 2) down to (2, 2) creates a RED match of 3 at (2,0), (2,1), (2,2))
        board.setTile(Coord(2, 0), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(2, 1), Tile.Normal(color = HeartColor.RED))
        board.setTile(Coord(2, 2), Tile.Normal(color = HeartColor.YELLOW))
        board.setTile(Coord(1, 2), Tile.Normal(color = HeartColor.RED))

        // Place a Broken Heart at (3, 1) adjacent to (2, 1)
        val broken = Tile.Blocker.createBroken(repairsRequired = 1, color = HeartColor.PURPLE)
        board.setTile(Coord(3, 1), broken)

        val objective = Objective(ObjectiveConfig(ObjectiveType.CLEAR_BLOCKER, targetBlocker = BlockerType.BROKEN_HEART, targetCount = 1))
        val gameState = GameState(
            levelId = 1,
            status = GameStatus.READY_FOR_INPUT,
            movesRemaining = 10,
            board = board,
            objectives = listOf(objective)
        )

        val tileSpawner = TileSpawner(
            allowedColors = listOf(HeartColor.YELLOW, HeartColor.PINK),
            rng = DeterministicRng(99L)
        )
        val pipeline = TurnPipeline(tileSpawner = tileSpawner)

        // Swap (1, 2) with (2, 2) to complete a Red match at row 2
        val result = pipeline.executeSwap(gameState, Coord(1, 2), Coord(2, 2))

        assertTrue(result.isSuccessfulMove)
        assertTrue("Broken heart objective should be completed", objective.isFulfilled)
        val tileAtRepaired = gameState.board.getTile(Coord(3, 1))
        assertNotNull(tileAtRepaired)
        assertTrue(tileAtRepaired is Tile.Normal)
        assertEquals(HeartColor.PURPLE, (tileAtRepaired as Tile.Normal).color)
    }
}
