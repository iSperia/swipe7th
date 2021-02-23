package com.game7th.battle.ability

import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.unit.BattleUnit

/**
 * Merger that uses tiles and merges them into stacks
 */
class DefaultStackMerger : AbilityTrigger {

    lateinit var tileType: String

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.TileMergeEvent -> {
                if (event.result == null && event.tile1.type.skin == tileType && event.tile2.type.skin == tileType) {
                    val stackSize = event.tile1.stackSize + event.tile2.stackSize
                    event.result = SwipeTile(event.tile1.type, event.tile2.id, stackSize)
                }
            }
        }
    }
}
