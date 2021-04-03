package com.game7th.battle.ability

import com.game7th.battle.DamageVector
import com.game7th.battle.SwipeBattle
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit

class IncomingDamagePreprocessor : AbilityTrigger {

    var targetId: Int = 0
    var processor: (suspend (SwipeBattle, BattleUnit, DamageVector) -> DamageVector)? = null

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.PreprocessDamage -> {
                if (event.unit.id == targetId) {
                    //looks liks our character is gonna be kicked
                    val delta = processor?.invoke(event.battle, unit, event.damage)
                    delta?.let { event.delta.add(delta) }
                }
            }
        }
    }
}