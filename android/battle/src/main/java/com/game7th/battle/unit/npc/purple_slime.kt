package com.game7th.battle.unit.npc

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.event.TileTemplate
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.toViewModel
import com.game7th.battle.unit.CappedStat
import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.unit.UnitType

fun producePurpleSlime(balance: SwipeBalance, level: Int): UnitStats {
    val hp = balance.red_slime.hp * level
    val slime = UnitStats(UnitType.PURPLE_SLIME, level = level, health = CappedStat(hp, hp))
    slime += ability {
        ticker {
            bodies[TickerEntry(balance.red_slime.w1, balance.red_slime.t1, "attack")] = { battle, unit ->
                val damage = (unit.stats.level * balance.red_slime.k1).toInt()
                if (damage > 0) {
                    battle.findClosestAliveEnemy(unit)?.let { target ->
                        battle.notifyAttack(unit, listOf(target), 0)
                        battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                    }
                }
            }
            bodies[TickerEntry(balance.red_slime.w2, balance.red_slime.t2, "impact")] = { battle, unit ->
                battle.tileField.calculateFreePosition()?.let { position ->
                    val tile = SwipeTile(TileTemplate("slime_splash", balance.red_slime.d2 + 1), battle.tileField.newTileId(), balance.red_slime.d2, true)
                    battle.tileField.tiles[position] = tile
                    battle.notifyEvent(BattleEvent.CreateTileEvent(tile.toViewModel(), position, -1))
                    battle.notifyAttack(unit, emptyList(), 1)
                }
            }
        }
    }
    return slime
}