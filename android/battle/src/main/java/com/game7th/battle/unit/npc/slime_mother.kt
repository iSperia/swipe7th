package com.game7th.battle.unit.npc

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.dto.BattleEvent
import com.game7th.battle.unit.*
import com.game7th.metagame.dto.UnitType

fun produceSlimeMother(balance: SwipeBalance, level: Int): UnitStats {
    val hp = level * balance.mother_slime.hp
    val slime = UnitStats(UnitType.SLIME_MOTHER, level = level, health = CappedStat(hp, hp))
    slime += ability {
        ticker {
            bodies[TickerEntry(balance.mother_slime.w1, balance.mother_slime.t1, "sword")] = { battle, unit ->
                val damage = (unit.stats.level * balance.mother_slime.k1).toInt()
                if (damage > 0) {
                    battle.findClosestAliveEnemy(unit)?.let { target ->
                        battle.notifyAttack(unit, listOf(target), 0)
                        battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                    }
                }
            }
            bodies[TickerEntry(balance.mother_slime.w2, balance.mother_slime.t2, "summon")] = { battle, unit ->
                val position = battle.calculateFreeNpcPosition()
                if (position > 0) {
                    val producedUnit = UnitFactory.produce(UnitType.GREEN_SLIME, balance, unit.stats.level, null)
                    val battleUnit = BattleUnit(battle.personageId++, position, producedUnit!!, Team.RIGHT)
                    battle.units.add(battleUnit)
                    battle.notifyEvent(BattleEvent.PersonagePositionedAbilityEvent(unit.toViewModel(), position, 0))
                    battle.notifyEvent(BattleEvent.CreatePersonageEvent(battleUnit.toViewModel(), battleUnit.position))
                }
            }
            bodies[TickerEntry(balance.mother_slime.w3, balance.mother_slime.t3, "leaf")] = { battle, unit ->
                val healAmount = balance.mother_slime.k3.toInt() * unit.stats.level
                battle.notifyEvent(BattleEvent.PersonagePositionedAbilityEvent(unit.toViewModel(), unit.position, 0))
                battle.notifyEvent(BattleEvent.ShowAilmentEffect(unit.id, "ailment_heal"))
                battle.processHeal(unit, healAmount)
            }
        }
    }
    return slime
}