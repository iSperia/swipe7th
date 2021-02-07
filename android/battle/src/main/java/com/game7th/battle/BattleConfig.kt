package com.game7th.battle

data class BattleConfig(
        val personages: List<PersonageConfig>,
        val npcs: List<PersonageConfig>
)

data class PersonageConfig(
        val codeName: String,
        val level: Int
)