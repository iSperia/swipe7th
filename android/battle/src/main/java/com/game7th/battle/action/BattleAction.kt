package com.game7th.battle.action

import com.game7th.battle.DamageVector
import com.game7th.battle.SwipeBattle
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.unit.BattleUnit

interface BattleAction {

    suspend fun processAction(
            battle: SwipeBattle,
            unit: BattleUnit,
            source: BattleUnit,
            target: BattleUnit,
            meta: Any) {}
}

class AttackAction : BattleAction {

    lateinit var target: (SwipeBattle, BattleUnit) -> List<BattleUnit>
    lateinit var damage: (SwipeBattle, BattleUnit, BattleUnit, Int, Int) -> DamageVector
    var attackIndex = 0

    override suspend fun processAction(battle: SwipeBattle, unit: BattleUnit, source: BattleUnit, target: BattleUnit, meta: Any) {
        val tile = meta as? SwipeTile
        tile ?: return
        val targets = target(battle, unit)
        val damages = targets.map { target ->
            val damage = damage(battle, unit, target, tile.stackSize, tile.type.maxStackSize)
            val damageResult = battle.processDamage(target, unit, damage)
            Pair(target, damageResult)
        }

        damages.forEach {
            battle.propagateInternalEvent(InternalBattleEvent.AttackDamageEvent(battle, it.second, tile, source, it.first))
        }
        battle.notifyAttack(unit, damages, attackIndex)
        targets.forEach {
            battle.notifyEvent(BattleEvent.PersonageUpdateEvent(it.toViewModel()))
        }
    }
}

data class ParametrizedMeta(
    val parameter: Float
)

class RegenerateParametrizedAmountAction(
        val perParameter: Float
) : BattleAction {

    override suspend fun processAction(battle: SwipeBattle, unit: BattleUnit, source: BattleUnit, target: BattleUnit, meta: Any) {
        val regen = meta as? ParametrizedMeta
        regen ?: return
        val amount = regen.parameter * perParameter
        battle.processHeal(unit, amount)
    }
}

class ApplyPoisonAction(
        private val duration: Int,
        private val damage: Float
) : BattleAction {

    override suspend fun processAction(battle: SwipeBattle, unit: BattleUnit, source: BattleUnit, target: BattleUnit, meta: Any) {
        battle.applyPoison(target, duration, damage.toInt())
    }
}

class ApplyParalizeAction(
        val duration: Int
) : BattleAction {

    override suspend fun processAction(battle: SwipeBattle, unit: BattleUnit, source: BattleUnit, target: BattleUnit, meta: Any) {
        battle.applyStun(target, duration)
    }
}


