package com.game7th.battle.unit.npc

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.dto.BattleEvent
import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.dto.TileTemplate
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.toViewModel
import com.game7th.battle.unit.CappedStat
import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.dto.UnitType
import kotlin.random.Random

fun produceDryadQueen(balance: SwipeBalance, level: Int): UnitStats {
    val hp = balance.dryad_queen.hp * level
    val resist = balance.dryad_queen.k3 * level
    val slime = UnitStats(UnitType.DRYAD_QUEEN, level = level, health = CappedStat(hp, hp), resistMax = resist.toInt(), resist = resist.toInt())
    slime += ability {
        ticker {
            bodies[TickerEntry(balance.dryad_queen.w1, balance.dryad_queen.t1, "sword")] = { battle, unit ->
                val damage = (unit.stats.level * balance.dryad_queen.k1).toInt()
                if (damage > 0) {
                    val target = battle.aliveEnemies(unit).let { if (it.isEmpty()) emptyList() else listOf(it.random()) }
                    target.firstOrNull()?.let { target ->
                        battle.notifyAttack(unit, listOf(target), 0)
                        battle.processDamage(target, unit, DamageVector(0, damage, 0))
                    }
                }
            }
            bodies[TickerEntry(balance.dryad_queen.w2, balance.dryad_queen.t2, "leaf")] = { battle, unit ->
                battle.notifyEvent(BattleEvent.PersonagePositionedAbilityEvent(unit.toViewModel(), unit.position, 0))
                val row = Random.nextInt(5)
                val column = Random.nextInt(5)
                (0..4).forEach {
                    var p = row * 5 + it
                    var tile = battle.tileField.tiles[p]
                    battle.tileField.tiles.remove(p)
                    tile?.let { battle.notifyTileRemoved(it.id) }
                    battle.notifyEvent(BattleEvent.ShowTileEffect(p, "tile_flower"))

                    p = it * 5 + column
                    tile = battle.tileField.tiles[p]
                    battle.tileField.tiles.remove(p)
                    tile?.let { battle.notifyTileRemoved(it.id) }
                    battle.notifyEvent(BattleEvent.ShowTileEffect(p, "tile_flower"))
                }
            }
        }
    }
    return slime
}