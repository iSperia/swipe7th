package com.game7th.battle.ability

import com.game7th.battle.SwipeBattle
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit

class PhaseTrigger : AbilityTrigger {

    var target = -1
    var phase = -1
    var action: (suspend (SwipeBattle) -> Unit)? = null

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        if (event is InternalBattleEvent.UnitPhaseTriggered && event.unit.id == target && event.unit.stats.phase == phase) {
            action?.invoke(event.battle)
        }
    }
}