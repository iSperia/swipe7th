package com.game7th.battle

import com.game7th.battle.unit.UnitType

data class BattleConfig(
        val personages: List<PersonageConfig>,
        val npcs: List<PersonageConfig>
)

data class PersonageConfig(
        val name: UnitType,
        val level: Int
)