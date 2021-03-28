package com.game7th.battle.dto

import com.game7th.battle.unit.CappedStat
import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.account.dto.PersonageAttributeStats
import com.game7th.metagame.account.dto.PersonageData

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
        val initialTiles: Int,

        val slime: PersonageBalance,
        val slime_boss: PersonageBalance,
        val red_slime: PersonageBalance,
        val mother_slime: PersonageBalance,
        val father_slime: PersonageBalance,
        val gladiator: PersonageBalance,
        val poison_archer: PersonageBalance
) {
    private fun calculateHealth(p: PersonageData) = p.level * stats.healthPerLevel + p.stats.body * stats.healthPerBody
    private fun calculateArmor(p: PersonageData) = p.stats.body * stats.armorPerBody
    private fun calculateRegeneration(p: PersonageData) = (p.stats.spirit * stats.regenerationPerSpirit).toInt()
    private fun calculateEvasion(p: PersonageData) = p.stats.spirit * stats.evasionPerSpirit
    private fun calculateResist(p: PersonageData) = p.stats.mind * stats.resistPerMind
    private fun calculateWisdom(p: PersonageData) = p.stats.mind * stats.wizdomMultiplier

    fun produceBaseStats(p: PersonageData): UnitStats {
        return UnitStats(p.unit, p.level, p.stats.body, p.stats.spirit, p.stats.mind, calculateHealth(p).let { CappedStat(it, it) },
            calculateArmor(p), calculateResist(p), calculateResist(p), calculateEvasion(p), calculateRegeneration(p), calculateWisdom(p))
    }

    fun produceGearStats(p: PersonageData): UnitStats {
        val flatBody = p.items.sumBy { it.gbFlatBody * it.level }
        val flatSpirit = p.items.sumBy { it.gbFlatSpirit * it.level }
        val flatMind = p.items.sumBy { it.gbFlatMind * it.level }
        val percBody = p.items.sumBy { it.gbPercBody * it.level }
        val percSpirit = p.items.sumBy { it.gbPercSpirit * it.level }
        val percMind = p.items.sumBy { it.gbPercMind * it.level }

        val _body = updated(p.stats.body, flatBody, percBody)
        val _spirit = updated(p.stats.spirit, flatSpirit, percSpirit)
        val _mind = updated(p.stats.mind, flatMind, percMind)

        val _p = p.copy(stats = PersonageAttributeStats(_body, _spirit, _mind))

        val flatHp = p.items.sumBy { it.gbFlatHp * it.level }
        val flatArmor = p.items.sumBy { it.gbFlatArmor * it.level }
        val flatRegeneration = p.items.sumBy { it.gbFlatRegeneration * it.level }
        val flatEvasion = p.items.sumBy { it.gbFlatEvasion * it.level }
        val flatResist = p.items.sumBy { it.gbFlatResist * it.level }
        val flatWisdom = p.items.sumBy { it.gbFlatWisdom * it.level }

        val percHp = p.items.sumBy { it.gbPercHp * it.level }
        val percArmor = p.items.sumBy { it.gbPercArmor * it.level }
        val percRegeneration = p.items.sumBy { it.gbPercRegeneration * it.level }
        val percEvasion = p.items.sumBy { it.gbPercEvasion * it.level }
        val percResist = p.items.sumBy { it.gbPercResist * it.level }
        val percWisdom = p.items.sumBy { it.gbPercWisdom * it.level }

        val resist = updated(calculateResist(_p), flatResist, percResist)

        return UnitStats(_p.unit, _p.level, _body, _mind, _spirit,
            updated(calculateHealth(_p), flatHp, percHp).let { CappedStat(it, it) },
            updated(calculateArmor(_p), flatArmor, percArmor),
                resist,
                resist,
            updated(calculateEvasion(_p), flatEvasion, percEvasion),
            updated(calculateRegeneration(_p), flatRegeneration, percRegeneration),
            updated(calculateWisdom(_p), flatWisdom, percWisdom))
    }

    private fun updated(value: Int, flat: Int, percent: Int): Int {
        return ((value+flat) * (1f + percent / 100f)).toInt()
    }
}
