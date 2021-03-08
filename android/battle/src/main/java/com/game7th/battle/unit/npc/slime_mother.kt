package com.game7th.battle.unit.npc

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.unit.*
import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.unit.UnitType

fun produceSlimeMother(balance: SwipeBalance, level: Int): UnitStats {
    val hp = level * balance.mother_slime.hp
    val slime = UnitStats(UnitType.SLIME_MOTHER, level = level, health = CappedStat(hp, hp), regeneration = balance.mother_slime.k3.toInt() * level)
    slime += ability {
        ticker {
            bodies[TickerEntry(balance.mother_slime.w1, balance.mother_slime.t1, "attack")] = { battle, unit ->
                val damage = (unit.stats.level * balance.mother_slime.k1).toInt()
                if (damage > 0) {
                    battle.findClosestAliveEnemy(unit)?.let { target ->
                        battle.notifyAttack(unit, listOf(target), 0)
                        battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                    }
                }
            }
            bodies[TickerEntry(balance.mother_slime.w2, balance.mother_slime.t2, "impact")] = { battle, unit ->
                val position = battle.calculateFreeNpcPosition()
                if (position > 0) {
                    val producedUnit = UnitFactory.produce(UnitType.GREEN_SLIME, balance, unit.stats.level, null)
                    val battleUnit = BattleUnit(battle.personageId++, position, producedUnit!!, Team.RIGHT)
                    battle.units.add(battleUnit)
                    battle.notifyEvent(BattleEvent.PersonagePositionedAbilityEvent(unit.toViewModel(), position, 0))
                    battle.notifyEvent(BattleEvent.CreatePersonageEvent(battleUnit.toViewModel(), battleUnit.position))
                }
            }
        }
    }
    return slime
}