package com.game7th.battle.balance

data class StatBalance(
        val baseHealth: Int,
        val healthPerBody: Int,
        val healthPerLevel: Int,
        val armorPerBody: Int,
        val resistPerMind: Int,
        val regenerationPerSpirit: Float,
        val wizdomMultiplier: Int,
        val evasionPerSpirit: Int
)

data class PersonageBalance(
        val hp: Int = 1,
        val k1: Float = 0f,
        val k2: Float = 0f,
        val k3: Float = 0f,
        val t1: Int = 0,
        val t2: Int = 0,
        val t3: Int = 0,
        val w1: Int = 0,
        val w2: Int = 0,
        val w3: Int = 0,
        val d1: Int = 0,
        val d2: Int = 0,
        val d3: Int = 0
)

data class SwipeBalance(
        val version: String,

        val stats: StatBalance,

        val slime: PersonageBalance,
        val red_slime: PersonageBalance,
        val mother_slime: PersonageBalance,
        val father_slime: PersonageBalance,
        val gladiator: PersonageBalance,
        val poison_archer: PersonageBalance
)
