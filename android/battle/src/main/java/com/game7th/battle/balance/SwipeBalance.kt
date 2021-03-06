package com.game7th.battle.balance

import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.account.PersonageData

data class StatBalance(
        val baseHealth: Int,
        val healthPerBody: Int,
        val healthPerLevel: Int,
        val armorPerBody: Int,
        val resistPerMind: Int,
        val regenerationPerSpirit: Float,
        val wizdomMultiplier: Int,
        val evasionPerSpirit: Int,
        val comboMultiplier: Float
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
) {
    fun calculateHealth(p: PersonageData) = p.level * stats.healthPerLevel + p.stats.body * stats.healthPerBody
    fun calculateArmor(p: PersonageData) = p.stats.body * stats.armorPerBody
    fun calculateRegeneration(p: PersonageData) = (p.stats.spirit * stats.regenerationPerSpirit).toInt()
    fun calculateEvasion(p: PersonageData) = p.stats.spirit * stats.evasionPerSpirit
    fun calculateResist(p: PersonageData) = p.stats.mind * stats.resistPerMind
    fun calculateWisdom(p: PersonageData) = p.stats.mind * stats.wizdomMultiplier
}
