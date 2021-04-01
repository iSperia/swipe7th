package com.game7th.battle.unit.npc

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.dto.BattleEvent
import com.game7th.battle.dto.TileTemplate
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.toViewModel
import com.game7th.battle.unit.CappedStat
import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.dto.UnitType

fun produceSlimeArmored(balance: SwipeBalance, level: Int): UnitStats {
    val hp = balance.slime_armored.hp * level
    val slime = UnitStats(UnitType.SLIME_ARMORED, level = level, health = CappedStat(hp, hp), armor = (balance.slime_armored.k3 * level).toInt())
    slime += ability {
        ticker {
            bodies[TickerEntry(balance.slime_armored.w1, balance.slime_armored.t1, "sword")] = { battle, unit ->
                val damage = (unit.stats.level * balance.slime_armored.k1).toInt()
                if (damage > 0) {
                    battle.findClosestAliveEnemy(unit)?.let { target ->
                        battle.notifyAttack(unit, listOf(target), 0)
                        battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                    }
                }
            }
            bodies[TickerEntry(balance.slime_armored.w2, balance.slime_armored.t2, "leaf")] = { battle, unit ->
                battle.tileField.calculateFreePosition()?.let { position ->
                    val tile = SwipeTile(TileTemplate("slime_splash", 0), battle.tileField.newTileId(), balance.slime_armored.d2, true)
                    battle.tileField.tiles[position] = tile
                    battle.notifyAttack(unit, emptyList(), 1)
                    battle.notifyEvent(BattleEvent.CreateTileEvent(tile.toViewModel(), position, -1))
                }
            }
        }
    }
    return slime
}