package com.game7th.battle.ability

import com.game7th.battle.DamageProcessStatus
import com.game7th.battle.action.BattleAction
import com.game7th.battle.action.ParametrizedMeta
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.unit.BattleUnit
import kotlin.math.abs

class DistancedConsumeOnAttackDamageTriggerEvent: AbilityTrigger {

    var range: Int = 1
    var tileSkins = mutableListOf<String>()
    lateinit var sourceSkin: String

    //tiles to remove at end of tick if any triggers happened
    private var tilesToRemove = mutableListOf<Int>()
    private var tilesToAction = mutableListOf<Pair<SwipeTile, Int>>()

    lateinit var action: BattleAction

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.TileConsumedEvent -> {
                if (tileSkins.contains(event.tile.type.skin)) {
                    processTileSkinConsumed(event)
                }
            }
            is InternalBattleEvent.AttackDamageEvent -> {
                processAttackDamage(event, unit)
            }
            is InternalBattleEvent.TickEvent -> {
                processTick(event)
            }
        }
    }

    private suspend fun processTick(event: InternalBattleEvent) {
        val ttrCopy = tilesToRemove.toList()
        ttrCopy.forEach { tileId ->
            val entry = event.battle.tileField.tiles.entries.firstOrNull { it.value.id == tileId }
            entry?.let { entry ->
                event.battle.tileField.tiles.remove(entry.key)
                event.battle.notifyTileRemoved(entry.value.id)
                event.battle.propagateInternalEvent(InternalBattleEvent.TileConsumedEvent(event.battle, entry.value, entry.key))
            }
        }
        tilesToRemove.removeAll(ttrCopy)
        tilesToAction.clear()
    }

    private suspend fun processAttackDamage(event: InternalBattleEvent.AttackDamageEvent, unit: BattleUnit) {
        if (event.damage.status == DamageProcessStatus.DAMAGE_DEALT) {
            tilesToAction.firstOrNull { it.first.id == event.tile.id }?.let { actionTile ->
                action.processAction(
                        event.battle,
                        unit,
                        event.source,
                        event.target,
                        ParametrizedMeta(actionTile.second.toFloat() * event.damage.totalDamage()))
            }
        }
    }

    private fun processTileSkinConsumed(event: InternalBattleEvent.TileConsumedEvent) {
        var count = 0
        //we are triggering this one
        event.battle.tileField.tiles.entries.filter {
            val dx = abs((it.key % 5) - (event.position % 5))
            val dy = abs((it.key / 5) - (event.position / 5))
            val skin = it.value.type.skin
            dx <= range && dy <= range && skin == this.sourceSkin && it.value.stackSize == it.value.type.maxStackSize
        }.forEach { (position, tile) ->
            tilesToRemove.add(tile.id)
            count++
        }
        if (count > 0) {
            tilesToAction.add(Pair(event.tile, count))
        }
    }
}