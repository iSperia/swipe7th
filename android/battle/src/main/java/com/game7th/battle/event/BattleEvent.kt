package com.game7th.battle.event

import com.game7th.battle.personage.PersonageViewModel
import com.game7th.battle.tilefield.TileFieldEvent

data class TileViewModel(
        val id: Int,
        val skin: String,
        val stackSize: Int,
        val background: String?,
        val backgroundIndex: Int?,
        val fractionForeground: String?
)

sealed class BattleEvent {

    data class CreateTileEvent(
        val tile: TileViewModel,
        val position: Int
    ) : BattleEvent()

    data class SwipeMotionEvent(
            val events: List<TileFieldEvent>
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
            val target: PersonageViewModel
    ) : BattleEvent()

    data class PersonageDamageEvent(
            val personage: PersonageViewModel,
            val damage: Int
    ) : BattleEvent()

    data class PersonageUpdateEvent(
            val personage: PersonageViewModel
    ) : BattleEvent()

    data class PersonageDamageEvadedEvent(
            val personage: PersonageViewModel
    ) : BattleEvent()

    data class ShowNpcAoeEffect(
            val skin: String,
            val personageId: Int
    ) : BattleEvent()

    object VictoryEvent: BattleEvent()
    object DefeatEvent : BattleEvent()
}