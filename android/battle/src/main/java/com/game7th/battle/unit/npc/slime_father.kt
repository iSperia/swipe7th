package com.game7th.battle.unit.npc

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.dto.BattleEvent
import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.unit.CappedStat
import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.dto.UnitType
import kotlin.math.max

fun produceSlimeFather(balance: SwipeBalance, level: Int): UnitStats {
    val hp = level * balance.father_slime.hp
    val slime = UnitStats(UnitType.SLIME_FATHER, level = level, health = CappedStat(hp, hp), armor = level * balance.father_slime.k3.toInt())
    slime += ability {
        ticker {
            bodies[TickerEntry(balance.father_slime.w1, balance.father_slime.t1, "sword")] = { battle, unit ->
                val damage = (unit.stats.level * balance.father_slime.k1).toInt()
                if (damage > 0) {
                    battle.findClosestAliveEnemy(unit)?.let { target ->
                        battle.notifyAttack(unit, listOf(target), 0)
                        battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                    }
                }
            }
            bodies[TickerEntry(balance.father_slime.w2, balance.father_slime.t2, "leaf")] = { battle, unit ->
                battle.findClosestAliveEnemy(unit)?.let { target ->
                    battle.notifyEvent(BattleEvent.PersonagePositionedAbilityEvent(unit.toViewModel(), unit.position, 0))
                    battle.applyStun(target, max(1, (balance.father_slime.k2 * unit.stats.level.toFloat() / target.stats.level).toInt()))
                }
            }
        }
    }
    return slime
}