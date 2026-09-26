package com.example.heartmatch.engine

import com.example.heartmatch.engine.core.*
import com.example.heartmatch.engine.model.*
import com.example.heartmatch.data.RewardRules
import org.junit.Assert.*
import org.junit.Test

class GameplayImprovementsTest {
    private fun engine(objective: ObjectiveConfig = ObjectiveConfig(ObjectiveType.SCORE, 999999)) =
        HeartMatchEngine(LevelConfig(id=1, name="Regression", rows=6, cols=6, randomSeed=39,
            objectives=listOf(objective), moveLimit=20))

    @Test fun extraMovesReviveOnlyPlayableGames() {
        val e=engine(); val s=e.getState()
        s.movesRemaining=0; s.status=GameStatus.GAME_OVER
        assertTrue(e.addExtraMoves()); assertEquals(5,s.movesRemaining)
        assertEquals(GameStatus.READY_FOR_INPUT,s.status)
        assertEquals(5,s.extraMovesGranted)
        s.status=GameStatus.OBJECTIVE_COMPLETED
        assertFalse(e.addExtraMoves()); assertEquals(5,s.movesRemaining)
    }

    @Test fun placedBombKeepsColorAndCannotReplaceBlocker() {
        val e=engine(); val c=Coord(0,0); val s=e.getState()
        s.board.setTile(c,Tile.Normal(color=HeartColor.BLUE))
        assertTrue(e.placeBooster(SpecialHeartType.BOMB_HEART,c))
        assertEquals(HeartColor.BLUE,s.board.getTile(c)!!.matchColor)
        s.board.setTile(c,Tile.Blocker(blockerType=BlockerType.STONE_HEART))
        assertFalse(e.placeBooster(SpecialHeartType.RAINBOW_HEART,c))
        assertTrue(s.board.getTile(c) is Tile.Blocker)
    }

    @Test fun hammerIsOneLayerAndDoesNotAdvanceDarkness() {
        val e=engine(); val s=e.getState(); val c=Coord(2,2); val dark=Coord(5,5)
        s.board.setTile(c,Tile.Blocker(blockerType=BlockerType.STONE_HEART,durability=2))
        s.board.setTile(dark,Tile.Blocker.createDark(spreadIntervalTurns=2))
        val events=e.applyHammer(c)
        assertEquals(1,events.filterIsInstance<EngineEvent.BlockerDamaged>().single().remainingDurability)
        assertTrue(events.none { it is EngineEvent.DarkHeartSpread })
        assertEquals(20,s.movesRemaining)
        assertEquals(0,(s.board.getTile(dark) as Tile.Blocker).turnsSurvived)
    }

    @Test fun blastsCreditColorsAndScoreBreakdownMatchesTotal() {
        val e=engine(ObjectiveConfig(ObjectiveType.COLLECT_COLOR,999,targetColor=HeartColor.BLUE))
        val s=e.getState(); val c=Coord(2,2)
        s.board.setTile(c,Tile.Special(specialType=SpecialHeartType.BOMB_HEART,baseColor=HeartColor.BLUE))
        s.board.setTile(Coord(2,3),Tile.Normal(color=HeartColor.BLUE))
        val events=e.applyHammer(c)
        assertTrue(s.objectives.first().currentCount>=2)
        assertEquals(s.score,s.scoreBreakdown.total)
        assertEquals(s.score,events.filterIsInstance<EngineEvent.ScoreStep>().sumOf { it.points })
        assertTrue(events.any { it is EngineEvent.SpecialTriggered })
    }

    @Test fun winHasStarAndPurchasedMovesCannotFarmEfficiencyBonus() {
        fun win(extra: Boolean): GameState {
            val e=engine(ObjectiveConfig(ObjectiveType.COLLECT_COLOR,1,targetColor=HeartColor.BLUE))
            if(extra) assertTrue(e.addExtraMoves())
            e.getState().board.setTile(Coord(0,0),Tile.Normal(color=HeartColor.BLUE))
            e.applyHammer(Coord(0,0)); return e.getState()
        }
        val original=win(false); val boosted=win(true)
        assertTrue(original.isWon); assertTrue(original.earnedStars>=1)
        assertEquals(original.scoreBreakdown.remainingMoves,boosted.scoreBreakdown.remainingMoves)
        assertEquals(original.score,original.scoreBreakdown.total)
    }

    @Test fun lightRemovesExtraLayerAndWoodRewardsTwoSidedMatches() {
        val b=Board.createEmpty(5,5); val c=Coord(2,2)
        b.setTile(c,Tile.Blocker(blockerType=BlockerType.STONE_HEART,durability=2))
        assertEquals(1,BlockerHandler().applyBlockerDamage(b,emptySet(),setOf(c),setOf(c)).destroyedBlockers.size)
        b.setTile(c,Tile.Blocker(blockerType=BlockerType.WOODEN_HEART,durability=2))
        assertEquals(1,BlockerHandler().applyBlockerDamage(b,setOf(Coord(2,1),Coord(1,2)),emptySet()).destroyedBlockers.size)
    }

    @Test fun hintsAndPreviewsDoNotMutateBoardOrRandomSequence() {
        val a=engine(); val b=engine()
        val moves=a.getRankedMoves(); a.previewTarget(Coord(0,0)); a.getRankedMoves()
        val move=moves.first()
        a.swap(move.first,move.second); b.swap(move.first,move.second)
        assertEquals(a.getState().score,b.getState().score)
        for(c in a.getState().board.getAllPlayableCoords()) assertEquals(a.getState().board.getTile(c)?.matchColor,b.getState().board.getTile(c)?.matchColor)
    }

    @Test fun lightActivationDestroysTwoLayerShell() {
        val e=engine(); val s=e.getState()
        s.board.setTile(Coord(2,2),Tile.Special(specialType=SpecialHeartType.LIGHT_HEART,baseColor=HeartColor.BLUE))
        s.board.setTile(Coord(2,3),Tile.Blocker(blockerType=BlockerType.STONE_HEART,durability=2))
        val events=e.applyHammer(Coord(2,2))
        assertTrue(events.filterIsInstance<EngineEvent.BlockerDestroyed>().any { it.coord==Coord(2,3) })
    }

    @Test fun giftPreviewIsStableAndPrioritizesUnfinishedGoals() {
        val e=engine(ObjectiveConfig(ObjectiveType.COLLECT_COLOR,999,targetColor=HeartColor.BLUE))
        val b=e.getState().board
        for(c in b.getAllPlayableCoords()) b.setTile(c,Tile.Normal(color=HeartColor.RED))
        b.setTile(Coord(0,0),Tile.Normal(color=HeartColor.BLUE))
        b.setTile(Coord(0,1),Tile.Normal(color=HeartColor.BLUE))
        b.setTile(Coord(3,3),Tile.Special(specialType=SpecialHeartType.GIFT_HEART))
        val preview=e.previewTarget(Coord(3,3))
        assertTrue(Coord(0,0) in preview); assertTrue(Coord(0,1) in preview)
        assertEquals(preview,e.previewTarget(Coord(3,3)))
        assertTrue(b.getTile(Coord(3,3)) is Tile.Special)
    }

    @Test fun impossibleShufflePreservesEveryObjectiveTile() {
        val b=Board.createEmpty(1,4)
        val tiles=listOf(Tile.Special(specialType=SpecialHeartType.GIFT_HEART),
            Tile.Blocker(blockerType=BlockerType.BROKEN_HEART),
            Tile.Normal(color=HeartColor.RED),Tile.Normal(color=HeartColor.BLUE))
        tiles.forEachIndexed { i,t -> b.setTile(Coord(0,i),t) }
        assertFalse(BoardReshuffler().reshuffle(b,listOf(HeartColor.RED,HeartColor.BLUE),DeterministicRng(8)))
        tiles.forEachIndexed { i,t -> assertEquals(t,b.getTile(Coord(0,i))) }
    }

    @Test fun rewardPolicyPreservesReplayValueAndPausesDailyProgress() {
        assertEquals(150,RewardRules.completionCoins(false,0,3))
        assertEquals(15,RewardRules.completionCoins(true,3,1))
        assertEquals(65,RewardRules.completionCoins(true,1,3))
        assertEquals(5,RewardRules.nextClaim(4)); assertEquals(1,RewardRules.nextClaim(7))
        assertTrue(RewardRules.milestone(10,false)); assertFalse(RewardRules.milestone(10,true))
    }
}
