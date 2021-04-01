package com.game7th.battle.unit.npc

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.unit.CappedStat
import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.dto.UnitType

fun produceDryad(balance: SwipeBalance, level: Int): UnitStats {
    val hp = balance.dryad.hp * level
    val resist = balance.dryad.k3 * level
    val slime = UnitStats(UnitType.DRYAD, level = level, health = CappedStat(hp, hp), resistMax = resist.toInt(), resist = resist.toInt())
    slime += ability {
        ticker {
            bodies[TickerEntry(balance.dryad.w1, balance.dryad.t1, "sword")] = { battle, unit ->
                val damage = (unit.stats.level * balance.dryad.k1).toInt()
                if (damage > 0) {
                    val target = battle.aliveEnemies(unit).let { if (it.isEmpty()) emptyList() else listOf(it.random()) }
                    target.firstOrNull()?.let { target ->
                        battle.notifyAttack(unit, listOf(target), 0)
                        battle.processDamage(target, unit, DamageVector(0, damage, 0))
                    }
                }
            }
        }
    }
    return slime
}