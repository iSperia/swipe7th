package com.game7th.battle.personage

data class PersonageStats(
        var body: Int,
        var health: Int,
        var maxHealth: Int,
        var armor: Int,

        var spirit: Int,
        var regeneration: Int,
        var evasion: Int,

        var mind: Int,
        var effectiveness: Int,
        var resist: Int,
        var resistMax: Int,

        var level: Int,

        val tick: Int,
        val maxTick: Int,
        val tickAbility: String?,

        val isStunned: Boolean
)