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
                    var count = 0
                    //we are triggering this one
                    event.battle.tileField.tiles.entries.filter {
                        val dx = abs((it.key % 5) - (event.position % 5))
                        val dy = abs((it.key / 5) - (event.position / 5))
                        val skin = it.value.type.skin
                        dx <= range && dy <= range && skin == this.sourceSkin
                    }.forEach { (position, tile) ->
                        tilesToRemove.add(position)
                        count++
                    }
                    if (count > 0) {
                        tilesToAction.add(Pair(event.tile, count))
                    }
                }
            }
            is InternalBattleEvent.AttackDamageEvent -> {
                if (event.damage.status == DamageProcessStatus.DAMAGE_DEALT) {
                    tilesToAction.firstOrNull { it.first.id == event.tile.id }?.let { actionTile ->
                        action.processAction(event.battle, unit, ParametrizedMeta(actionTile.second.toFloat() * event.damage.totalDamage()))
                    }
                }
            }
            is InternalBattleEvent.TickEvent -> {
                tilesToRemove.forEach {
                    val tile = event.battle.tileField.tiles[it]
                    tile?.let { tile ->
                        event.battle.tileField.tiles.remove(it)
                        event.battle.notifyTileRemoved(tile.id)
                    }
                }
                tilesToRemove.clear()
                tilesToAction.clear()
            }
        }
    }
}