package com.game7th.battle.npc

import com.game7th.battle.SwipeBattle

abstract class NpcAbility {

    abstract suspend fun tick(battle: SwipeBattle, personage: NpcPersonage)

    abstract suspend fun primaryAbility(battle: SwipeBattle, personage: NpcPersonage)
}

class SlimeAttackAbility : NpcAbility() {

    var tick = 0

    override suspend fun tick(battle: SwipeBattle, personage: NpcPersonage) {
        tick++
        if (tick == 3) {

            //do attack
            primaryAbility(battle, personage)

            tick = 0
        }
    }

    override suspend fun primaryAbility(battle: SwipeBattle, personage: NpcPersonage) {
        val damage = (battle.balance.slimeFlatDamage + (battle.balance.slimeScaleDamage * (personage.stats.level - 1)) * battle.balance.slimeMulti).toInt()
        if (damage > 0) {
            val target = battle.findClosestAlivePersonage()
            target?.let { target ->
                battle.notifyAttack(personage, target)
                battle.processDamage(target, personage, damage, 0, 0)
            }
        }
    }
}
