package com.game7th.battle.ability

import com.game7th.battle.SwipeBattle
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.tilefield.tile.TileType
import com.game7th.battle.unit.BattleUnit

class ConsumeOnUseTrigger : AbilityTrigger {

    lateinit var tileType: TileType
    lateinit var body: suspend (battle: SwipeBattle, tile: SwipeTile, unit: BattleUnit) -> Unit

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.AbilityUseEvent -> {
                if (event.tile.type == tileType && event.tile.tier1()) {
                    //We are using ourselves
                    event.battle.tileField.removeById(event.tile.id)
                    event.battle.notifyTileRemoved(event.tile.id)

                    body(event.battle, event.tile, unit)
                }
            }
        }
    }
}
