package com.game7th.battle.action

import com.game7th.battle.DamageVector
import com.game7th.battle.SwipeBattle
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.unit.BattleUnit

interface BattleAction {

    suspend fun processAction(
            battle: SwipeBattle,
            unit: BattleUnit,
            maxStackSize: Int,
            stackSize: Int)
}

class AttackAction : BattleAction {

    lateinit var target: (SwipeBattle, BattleUnit) -> List<BattleUnit>
    lateinit var damage: (SwipeBattle, BattleUnit, BattleUnit, Int, Int) -> DamageVector
    var attackIndex = 0

    override suspend fun processAction(battle: SwipeBattle, unit: BattleUnit, maxStackSize: Int, stackSize: Int) {
        val targets = target(battle, unit)
        val damages = targets.map { target ->
            val damage = damage(battle, unit, target, maxStackSize, stackSize)
            val damageResult = battle.processDamage(target, unit, damage)
            Pair(target, damageResult)
        }

        battle.notifyAttack(unit, damages, attackIndex)
        targets.forEach {
            battle.notifyEvent(BattleEvent.PersonageUpdateEvent(it.toViewModel()))
        }
    }
}