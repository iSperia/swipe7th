package com.game7th.battle.ability

import com.game7th.battle.SwipeBattle
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit
import kotlin.math.min
import kotlin.random.Random

class TickerTrigger : AbilityTrigger {

    var tick = 0
    var ticksToTrigger = 0
    lateinit var body: suspend (battle: SwipeBattle, unit: BattleUnit) -> Unit

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.TickEvent -> {
                if (!event.preventTickers) {
                    tick++
                    if (tick >= ticksToTrigger && unit.isNotStunned()) {
                        tick = 0
                        body(event.battle, unit)
                    }
                    unit.stats.tick = tick
                    event.battle.notifyEvent(BattleEvent.PersonageUpdateEvent(unit.toViewModel()))
                }
            }
            is InternalBattleEvent.BattleStartedEvent -> {
                tick = Random.nextInt(min(3, ticksToTrigger))
                unit.stats.maxTick = ticksToTrigger
                unit.stats.tick = tick
                event.battle.notifyEvent(BattleEvent.PersonageUpdateEvent(unit.toViewModel()))
            }
        }
    }
}