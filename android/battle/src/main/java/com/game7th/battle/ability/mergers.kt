package com.game7th.battle.ability

import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.tilefield.tile.TileStage
import com.game7th.battle.tilefield.tile.TileType
import com.game7th.battle.unit.BattleUnit

/**
 * Merger that uses tiles and merges them into stacks
 */
class DefaultStackMerger : AbilityTrigger {

    lateinit var tileType: TileType

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.TileMergeEvent -> {
                if (event.result == null && event.tile1.type == tileType && event.tile2.type == tileType) {
                    val stackSize = event.tile1.stackSize + event.tile2.stackSize
                    event.result = SwipeTile(tileType, event.tile2.id, stackSize, event.tile2.thresholdTier1, event.tile2.thresholdTier2)
                }
            }
        }
    }
}
