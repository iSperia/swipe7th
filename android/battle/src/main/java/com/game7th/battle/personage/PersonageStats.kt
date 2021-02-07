package com.game7th.battle.personage

data class PersonageStats(
        var body: Int,
        var health: Int,
        var maxHealth: Int,
        var armor: Int,
        var maxArmor: Int,

        var spirit: Int,
        var regeneration: Int,
        var evasion: Int,

        var mind: Int,
        var effectiveness: Int,
        var magicDefense: Int,
        var maxMagicDefense: Int,

        var level: Int
)