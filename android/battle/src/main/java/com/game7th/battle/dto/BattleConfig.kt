package com.game7th.battle.dto

import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.dto.UnitType
import com.game7th.swiped.api.PersonageAttributeStatsDto

data class BattleConfig(
        val personages: List<PersonageConfig>,
        val waves: List<List<PersonageConfig>>
)

data class PersonageConfig(
        val name: UnitType,
        val level: Int,
        val stats: PersonageAttributeStatsDto,
        val unitStats: UnitStats?
)