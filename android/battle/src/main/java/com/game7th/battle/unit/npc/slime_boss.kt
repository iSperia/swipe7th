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
import kotlin.math.max

fun produceSlimeBoss(balance: SwipeBalance, level: Int): UnitStats {
    val hp = balance.slime_boss.hp * level
    val slime = UnitStats(UnitType.SLIME_BOSS, level = level, health = CappedStat(hp, hp))
    slime += ability {
        ticker {
            bodies[TickerEntry(balance.slime_boss.w1, balance.slime_boss.t1, "sword")] = { battle, unit ->
                val damage = (unit.stats.level * balance.slime_boss.k1).toInt()
                if (damage > 0) {
                    //physical attack
                    battle.findClosestAliveEnemy(unit)?.let { target ->
                        battle.notifyAttack(unit, listOf(target), 0)
                        battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                        battle.applyStun(target, max(1, (unit.stats.level.toFloat() / target.stats.level * balance.slime_boss.d1).toInt()))
                    }
                }
            }
            bodies[TickerEntry(balance.slime_boss.w2, balance.slime_boss.t2, "magic")] = { battle, unit ->
                val damage = (unit.stats.level * balance.slime_boss.k2).toInt()
                if (damage > 0) {
                    //physical attack
                    battle.aliveEnemies(unit).random().let { target ->
                        battle.notifyAttack(unit, listOf(target), 1)
                        battle.processDamage(target, unit, DamageVector(0, damage, 0))
                    }
                }

                //generate two nodes
                (0..2).forEach { _ ->
                    battle.tileField.calculateFreePosition()?.let { position ->
                        val tile = SwipeTile(TileTemplate("slime_splash", 0), battle.tileField.newTileId(), balance.slime_boss.d2, true)
                        battle.tileField.tiles[position] = tile
                        battle.notifyEvent(BattleEvent.CreateTileEvent(tile.toViewModel(), position, -1))
                    }
                }
            }
        }
    }
    return slime
}