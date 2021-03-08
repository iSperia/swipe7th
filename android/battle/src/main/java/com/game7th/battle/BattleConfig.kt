package com.game7th.battle

import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.unit.UnitType

data class BattleConfig(
        val personages: List<PersonageConfig>,
        val waves: List<List<PersonageConfig>>
)

data class PersonageConfig(
        val name: UnitType,
        val level: Int,
        val stats: PersonageAttributeStats,
        val unitStats: UnitStats?
)