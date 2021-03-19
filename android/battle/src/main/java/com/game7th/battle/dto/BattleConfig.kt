package com.game7th.battle.dto

import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.account.dto.PersonageAttributeStats
import com.game7th.metagame.dto.UnitType

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