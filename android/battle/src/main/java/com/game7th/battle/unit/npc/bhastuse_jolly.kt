package com.game7th.battle.unit.npc

import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.dto.BattleEvent
import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.unit.*
import com.game7th.metagame.dto.UnitType

fun produceBhastuseJolly(balance: SwipeBalance, unitId: Int, level: Int): UnitStats {
    val hp = level * balance.bhastuse_jolly.hp
    val slime = UnitStats(UnitType.BHASTUSE_JOLLY, level = level, health = CappedStat(hp, hp), maxPhase = 1, phaseThresholds = listOf(10))
    slime += ability {
        phaser {
            target = unitId
            phase = 1
            action = { battle ->
                //destroy unit, and add a slime boss over it
                battle.units.firstOrNull { it.id == target }?.let { unit ->
                    battle.notifyEvent(BattleEvent.ShowSpeech("vp_bhastuse_jolly", "ued_bhastuse_jolly_phase", "BHASTUSE_JOLLY"))
                    battle.destroyUnit(unit)
                    val newUnit = produceSlimeBoss(balance, unit.stats.level)
                    val battleUnit = BattleUnit(battle.newPersonageId(), unit.position, newUnit, unit.team)
                    battle.units.add(battleUnit)
                    battle.notifyEvent(BattleEvent.CreatePersonageEvent(battleUnit.toViewModel(), battleUnit.position))
                    battle.notifyEvent(BattleEvent.ShowSpeech("vp_slime_boss", "ued_bhastuse_jolly_phase_transmute", "BHASTUSE_JELLY"))
                }
            }
        }

        ticker {
            bodies[TickerEntry(balance.bhastuse_jolly.w1, balance.bhastuse_jolly.t1, "leaf")] = { battle, unit ->
                val position = battle.calculateFreeNpcPosition()
                if (position > 0) {
                    val personageId = battle.newPersonageId()
                    val producedUnit = UnitFactory.produce(UnitType.PURPLE_SLIME, balance,personageId, (unit.stats.level * balance.bhastuse_jolly.k1).toInt() + 1, null)
                    val battleUnit = BattleUnit(personageId, position, producedUnit!!, Team.RIGHT)
                    battle.units.add(battleUnit)
                    battle.notifyEvent(BattleEvent.PersonagePositionedAbilityEvent(unit.toViewModel(), position, 0))
                    battle.notifyEvent(BattleEvent.CreatePersonageEvent(battleUnit.toViewModel(), battleUnit.position))
                }
            }
            bodies[TickerEntry(balance.bhastuse_jolly.w2, balance.bhastuse_jolly.t2, "summon")] = { battle, unit ->
                val position = battle.calculateFreeNpcPosition()
                if (position > 0) {
                    val personageId = battle.newPersonageId()
                    val producedUnit = UnitFactory.produce(UnitType.SLIME_ARMORED, balance,personageId, (unit.stats.level * balance.bhastuse_jolly.k2).toInt() + 1, null)
                    val battleUnit = BattleUnit(personageId, position, producedUnit!!, Team.RIGHT)
                    battle.units.add(battleUnit)
                    battle.notifyEvent(BattleEvent.PersonagePositionedAbilityEvent(unit.toViewModel(), position, 0))
                    battle.notifyEvent(BattleEvent.CreatePersonageEvent(battleUnit.toViewModel(), battleUnit.position))
                }
            }
        }
    }
    return slime
}