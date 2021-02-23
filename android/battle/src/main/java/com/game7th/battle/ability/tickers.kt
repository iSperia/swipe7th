package com.game7th.battle.ability

import com.game7th.battle.SwipeBattle
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit
import kotlin.random.Random

data class TickerEntry(
        val weight: Int,
        val ticksToTrigger: Int,
        val name: String
)

typealias TickerBody = suspend (battle: SwipeBattle, unit:  BattleUnit) -> Unit

class TickerTrigger : AbilityTrigger {

    var tick = 0
    var ticksToTrigger = 0

    val bodies = mutableMapOf<TickerEntry, TickerBody>()

    private var body: TickerBody? = null

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.TickEvent -> {
                if (body == null) {
                    //check out the ticker
                    val sumWeight = bodies.keys.sumBy { it.weight }
                    val roll = Random.nextInt(1, sumWeight + 1)
                    var sum = 0
                    val nextEntry = bodies.entries.firstOrNull {
                        sum += it.key.weight
                        sum >= roll
                    }
                    nextEntry?.let {
                        body = it.value
                        tick = 0
                        ticksToTrigger = it.key.ticksToTrigger
                    }
                } else {
                    if (!event.preventTickers) {
                        tick++
                        if (tick >= ticksToTrigger && unit.isNotStunned() && unit.stats.health.value > 0) {
                            tick = 0
                            body!!(event.battle, unit)
                        }
                        unit.stats.tick = tick
                        event.battle.notifyEvent(BattleEvent.PersonageUpdateEvent(unit.toViewModel()))
                    }
                }
            }
        }
    }
}