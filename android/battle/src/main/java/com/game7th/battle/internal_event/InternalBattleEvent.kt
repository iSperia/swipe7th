package com.game7th.battle.internal_event

import com.game7th.battle.DamageProcessResult
import com.game7th.battle.DamageVector
import com.game7th.battle.SwipeBattle
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.unit.BattleUnit

sealed class InternalBattleEvent(
        val battle: SwipeBattle
) {

    /**
     * Event is sent when guaranteed tile is needed. Handlers should add candidates to list,
     * then battle choses random of it
     */
    class ProduceGuaranteedTileEvent(
            battle: SwipeBattle
    ) : InternalBattleEvent(battle)

    /**
     * Event is sent each tick happens
     */
    class TickEvent(
            battle: SwipeBattle,
            val preventTickers: Boolean = false
    ) : InternalBattleEvent(battle)

    /**
     * Event is sent each time tile is attempted to get merged
     */
    class TileMergeEvent(
            battle: SwipeBattle,
            val tile1: SwipeTile,
            val tile2: SwipeTile
    ) : InternalBattleEvent(battle) {
        var result: SwipeTile? = null
    }

    /**
     * Event is sent each time user tries to use ability
     */
    class AbilityUseEvent(
            battle: SwipeBattle,
            val tile: SwipeTile
    ) : InternalBattleEvent(battle)

    /**
     * Launched once battle started
     */
    class BattleStartedEvent(
            battle: SwipeBattle
    ) : InternalBattleEvent(battle)

    class AttackDamageEvent(
            battle: SwipeBattle,
            val damage: DamageProcessResult,
            val tile: SwipeTile,
            val source: BattleUnit,
            val target: BattleUnit
    ) : InternalBattleEvent(battle)

    class TileConsumedEvent(
            battle: SwipeBattle,
            val tile: SwipeTile,
            val position: Int
    ) : InternalBattleEvent(battle)

    class ScriptedInitialTiles(
            battle: SwipeBattle,
            var tiles: Map<Int, SwipeTile>?
    ) : InternalBattleEvent(battle)

    class ScriptedTilesTick(
            battle: SwipeBattle,
            val tick: Int,
            var tiles: Map<Int, SwipeTile>?
    ) : InternalBattleEvent(battle)

    class UnitPhaseTriggered(
            battle: SwipeBattle,
            val unit: BattleUnit
    ) : InternalBattleEvent(battle)

    class PreprocessDamage(
            battle: SwipeBattle,
            val damage: DamageVector,
            val delta: MutableList<DamageVector>,
            val unit: BattleUnit
    ) : InternalBattleEvent(battle)

}