package com.game7th.battle.event

import com.game7th.battle.personage.PersonageViewModel
import com.game7th.battle.tilefield.TileFieldEvent

data class TileTemplate(
        val skin: String,
        val maxStackSize: Int
)

data class TileViewModel(
        val id: Int,
        val skin: String,
        val stackSize: Int,
        val maxStackSize: Int
)

sealed class BattleEvent {

    data class CreateTileEvent(
        val tile: TileViewModel,
        val position: Int,
        val sourcePosition: Int
    ) : BattleEvent()

    data class SwipeMotionEvent(
            val events: List<TileFieldEvent>
    ) : BattleEvent()

    data class ComboUpdateEvent(
            val combo: Int
    ) : BattleEvent()

    data class UpdateTileEvent(
            val id: Int,
            val tile: TileViewModel
    ) : BattleEvent()

    data class RemoveTileEvent(
            val id: Int
    ) : BattleEvent()

    data class CreatePersonageEvent(
            val personage: PersonageViewModel,
            val position: Int
    ) : BattleEvent()

    data class PersonageAttackEvent(
            val source: PersonageViewModel,
            val targets: List<PersonageViewModel>,
            val attackIndex: Int
    ) : BattleEvent()

    data class PersonagePositionedAbilityEvent(
            val source: PersonageViewModel,
            val target: Int,
            val attackIndex: Int
    ) : BattleEvent()

    data class PersonageDamageEvent(
            val personage: PersonageViewModel,
            val damage: Int
    ) : BattleEvent()

    data class PersonageDeadEvent(
            val personage: PersonageViewModel
    ) : BattleEvent()

    data class PersonageUpdateEvent(
            val personage: PersonageViewModel
    ) : BattleEvent()

    data class PersonageHealEvent(
            val personage: PersonageViewModel,
            val amount: Int
    ) : BattleEvent()

    data class ShowNpcAoeEffect(
            val skin: String,
            val personageId: Int,
            val direction: Int
    ) : BattleEvent()

    data class ShowProjectile(
            val skin: String,
            val sourceId: Int,
            val targetId: Int
    ) : BattleEvent()

    data class ShowAilmentEffect(
            val target: Int,
            val effectSkin: String
    ) : BattleEvent()

    data class RemovePersonageEvent(
            val target: Int
    ) : BattleEvent()

    data class NewWaveEvent(
            val wave: Int
    ) : BattleEvent()

    object VictoryEvent: BattleEvent()
    object DefeatEvent : BattleEvent()
}