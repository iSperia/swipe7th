package com.game7th.battle.ability

import com.game7th.battle.EfficencyCalculator
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.event.TileTemplate
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.toViewModel
import com.game7th.battle.unit.BattleUnit
import kotlin.random.Random

/**
 * This is the default skill tile emitter used by most of personages
 */
class DefaultSkillTileEmitter : AbilityTrigger {

    val skills = mutableListOf<Pair<Int, TileTemplate>>()

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {

        val amount = when (event) {
            is InternalBattleEvent.ProduceGuaranteedTileEvent -> 1
            is InternalBattleEvent.TileConsumedEvent -> EfficencyCalculator.calculateStackSize(
                    event.battle.balance,
                    unit.stats.intelligence,
                    event.tile.stackSize,
                    event.battle.combo
            )
            else -> 0
        }

        var sourcePosition = when (event) {
            is InternalBattleEvent.TileConsumedEvent -> event.position
            else -> -1
        }

        if (amount > 0) {
            val totalWeight = skills.sumBy { it.first }

            (1..amount).forEach {

                val roll = Random.nextInt(1, totalWeight + 1)
                var sum = 0
                val skill = skills.firstOrNull {
                    sum += it.first
                    sum >= roll
                }

                skill?.let { skill ->
                    val position: Int? = event.battle.tileField.calculateFreePosition()
                    position?.let { position ->

                        val tile = SwipeTile(
                                skill.second,
                                event.battle.tileField.newTileId(),
                                1)

                        event.battle.tileField.tiles[position] = tile
                        event.battle.notifyEvent(BattleEvent.CreateTileEvent(tile.toViewModel(), position, sourcePosition))
                    }
                }
            }
        }
    }
}

