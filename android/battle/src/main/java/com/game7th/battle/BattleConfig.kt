package com.game7th.battle

data class BattleConfig(
        val personages: List<PersonageConfig>,
        val npcs: List<NpcConfig>
)

data class PersonageConfig(
        val codeName: String,
        val level: Int
)

data class NpcConfig(
        val codeName: String,
        val level: Int
)