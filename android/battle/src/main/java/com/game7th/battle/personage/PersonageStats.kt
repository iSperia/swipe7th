package com.game7th.battle.personage

data class PersonageStats(
        var body: Int,
        var health: Int,
        var armor: Int,

        var spirit: Int,
        var regeneration: Int,
        var magicDefense: Int,

        var mind: Int,
        var effectiveness: Int,
        var energyShield: Int,

        var level: Int
)