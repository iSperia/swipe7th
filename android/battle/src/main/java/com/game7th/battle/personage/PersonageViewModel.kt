package com.game7th.battle.personage

data class PersonageViewModel(
        val stats: PersonageStats,
        val skin: String,
        val portrait: String,
        val id: Int,
        val team: Int
)
