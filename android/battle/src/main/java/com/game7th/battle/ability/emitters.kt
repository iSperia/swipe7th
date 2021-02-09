package com.game7th.battle.ability

import com.game7th.battle.EfficencyCalculator
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.tilefield.tile.TileType
import com.game7th.battle.toViewModel
import com.game7th.battle.unit.BattleUnit

/**
 * This is the default skill tile emitter used by most of personages
 */
class DefaultSkillTileEmitter : AbilityTrigger {

    lateinit var tileType: TileType
    var tier1: Int = 5
    var tier2: Int = 10

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {

        val amount = when (event) {
            is InternalBattleEvent.ProduceGuaranteedTileEvent -> 1
            is InternalBattleEvent.TickEvent -> EfficencyCalculator.calculateStackSize(
                    event.battle.balance,
                    unit.stats.level,
                    unit.stats.wisdom)
            else -> 0
        }

        if (amount > 0) {
            val position: Int? = event.battle.tileField.calculateFreePosition()
            position?.let { position ->

                val tile = SwipeTile(
                        tileType,
                        event.battle.tileField.newTileId(),
                        amount,
                        tier1,
                        tier2)

                event.battle.tileField.tiles[position] = tile
                event.battle.notifyEvent(BattleEvent.CreateTileEvent(tile.toViewModel(), position))
            }
        }
    }
}

