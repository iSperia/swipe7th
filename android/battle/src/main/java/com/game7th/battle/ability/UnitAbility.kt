package com.game7th.battle.ability

import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit

interface AbilityTrigger {
    suspend fun process(event: InternalBattleEvent, unit: BattleUnit)
}

class UnitAbility {
    val triggers = mutableListOf<AbilityTrigger>()

    /**BEGIN OF DSL*/

    fun defaultEmitter(init: DefaultSkillTileEmitter.() -> Unit) : DefaultSkillTileEmitter {
        val trigger = DefaultSkillTileEmitter()
        trigger.init()
        triggers.add(trigger)
        return trigger
    }

    fun defaultMerger(init: DefaultStackMerger.() -> Unit) : DefaultStackMerger {
        val trigger = DefaultStackMerger()
        trigger.init()
        triggers.add(trigger)
        return trigger
    }

    fun ticker(init: TickerTrigger.() -> Unit) : TickerTrigger {
        val trigger = TickerTrigger()
        trigger.init()
        triggers.add(trigger)
        return trigger
    }

    fun consume(init: ConsumeTrigger.() -> Unit) : ConsumeTrigger {
        val trigger = ConsumeTrigger()
        trigger.init()
        triggers.add(trigger)
        return trigger
    }

    fun preprocessIncomingDamage(init: IncomingDamagePreprocessor.() -> Unit): IncomingDamagePreprocessor {
        val trigger = IncomingDamagePreprocessor()
        trigger.init()
        triggers.add(trigger)
        return trigger
    }

    fun distancedConsumeOnAttackDamage(init: DistancedConsumeOnAttackDamageTriggerEvent.() -> Unit) : DistancedConsumeOnAttackDamageTriggerEvent {
        val trigger = DistancedConsumeOnAttackDamageTriggerEvent()
        trigger.init()
        triggers.add(trigger)
        return trigger
    }

    fun phaser(init: PhaseTrigger.() -> Unit) : PhaseTrigger {
        val trigger = PhaseTrigger()
        trigger.init()
        triggers.add(trigger)
        return trigger
    }

    /**END OF DSL*/
}

fun ability(init: UnitAbility.() -> Unit): UnitAbility {
    val ability = UnitAbility()
    ability.init()
    return ability
}


