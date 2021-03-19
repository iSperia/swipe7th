package com.game7th.battle.ability

import com.game7th.battle.action.BattleAction
import com.game7th.battle.dto.TileTemplate
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit

class ConsumeTrigger : AbilityTrigger {

    lateinit var template: TileTemplate

    lateinit var action: BattleAction

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        if (event is InternalBattleEvent.TickEvent && unit.isAlive()) {
            event.battle.tileField.tiles.forEach { (index, tile) ->
                if (tile.type.skin == template.skin && tile.stackSize >= template.maxStackSize) {
                    //we are done for now, consume the tile
                    event.battle.tileField.removeById(tile.id)
                    event.battle.notifyTileRemoved(tile.id)

                    event.battle.propagateInternalEvent(InternalBattleEvent.TileConsumedEvent(event.battle, tile, index))

                    action.processAction(event.battle, unit, unit, unit, tile)
                }
            }
        }
    }
}