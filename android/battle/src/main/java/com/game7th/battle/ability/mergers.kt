package com.game7th.battle.ability

import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.unit.BattleUnit

/**
 * Merger that uses tiles and merges them into stacks
 */
class DefaultStackMerger : AbilityTrigger {

    lateinit var tileType: String

    var autoCut = false

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.TileMergeEvent -> {
                if (
                        event.result == null &&
                        event.tile1.type.skin == tileType &&
                        event.tile2.type.skin == tileType &&
                        (!autoCut || event.tile1.stackSize < event.tile1.type.maxStackSize) &&
                        (!autoCut || event.tile2.stackSize < event.tile2.type.maxStackSize)) {
                    val stackSize = event.tile1.stackSize + event.tile2.stackSize
                    val finalStackSize = if (autoCut && stackSize > event.tile2.type.maxStackSize) event.tile2.type.maxStackSize else stackSize
                    event.result = SwipeTile(event.tile1.type, event.tile2.id, finalStackSize)
                }
            }
        }
    }
}
