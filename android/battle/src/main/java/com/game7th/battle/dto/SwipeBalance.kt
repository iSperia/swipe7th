package com.game7th.battle.dto

import com.game7th.battle.unit.CappedStat
import com.game7th.battle.unit.UnitStats
import com.game7th.metagame.dto.UnitType
import com.game7th.swiped.api.InventoryItemFullInfoDto
import com.game7th.swiped.api.PersonageAttributeStatsDto
import com.game7th.swiped.api.PersonageDto

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
        val slime_armored: PersonageBalance,
        val slime_boss: PersonageBalance,
        val red_slime: PersonageBalance,
        val mother_slime: PersonageBalance,
        val bhastuse_jolly: PersonageBalance,
        val father_slime: PersonageBalance,
        val gladiator: PersonageBalance,
        val freeze_mage: PersonageBalance,
        val poison_archer: PersonageBalance,
        val dryad: PersonageBalance,
        val dryad_queen: PersonageBalance
) {
    private fun calculateHealth(p: PersonageDto) = p.level * stats.healthPerLevel + p.stats.body * stats.healthPerBody
    private fun calculateArmor(p: PersonageDto) = p.stats.body * stats.armorPerBody
    private fun calculateRegeneration(p: PersonageDto) = (p.stats.spirit * stats.regenerationPerSpirit).toInt()
    private fun calculateEvasion(p: PersonageDto) = p.stats.spirit * stats.evasionPerSpirit
    private fun calculateResist(p: PersonageDto) = p.stats.mind * stats.resistPerMind
    private fun calculateWisdom(p: PersonageDto) = p.stats.mind * stats.wizdomMultiplier

    fun produceBaseStats(p: PersonageDto): UnitStats {
        return UnitStats(UnitType.valueOf(p.unit), p.level, p.stats.body, p.stats.spirit, p.stats.mind, calculateHealth(p).let { CappedStat(it, it) },
            calculateArmor(p), calculateResist(p), calculateResist(p), calculateEvasion(p), calculateRegeneration(p), calculateWisdom(p))
    }

    fun produceGearStats(p: PersonageDto, items: List<InventoryItemFullInfoDto>): UnitStats {
        val flatBody = items.sumBy { ((it.template.gbFlatBody ?: 0f) * it.level).toInt() }
        val flatSpirit = items.sumBy { ((it.template.gbFlatSpirit ?: 0f) * it.level).toInt() }
        val flatMind = items.sumBy { ((it.template.gbFlatMind?:0f) * it.level).toInt() }
        val percBody = items.sumBy { ((it.template.gbPercBody?:0f) * it.level).toInt() }
        val percSpirit = items.sumBy { ((it.template.gbPercSpirit?:0f) * it.level).toInt() }
        val percMind = items.sumBy { ((it.template.gbPercMind?:0f) * it.level).toInt() }

        val _body = updated(p.stats.body, flatBody, percBody)
        val _spirit = updated(p.stats.spirit, flatSpirit, percSpirit)
        val _mind = updated(p.stats.mind, flatMind, percMind)

        val _p = p.copy(stats = PersonageAttributeStatsDto(_body, _spirit, _mind))

        val flatHp = items.sumBy { ((it.template.gbFlatHp?:0f) * it.level).toInt() }
        val flatArmor = items.sumBy { ((it.template.gbFlatArmor?:0f) * it.level).toInt() }
        val flatRegeneration = items.sumBy { ((it.template.gbFlatRegeneration?:0f) * it.level).toInt() }
        val flatEvasion = items.sumBy { ((it.template.gbFlatEvasion?:0f) * it.level).toInt() }
        val flatResist = items.sumBy { ((it.template.gbFlatResist?:0f) * it.level).toInt() }
        val flatWisdom = items.sumBy { ((it.template.gbFlatWisdom?:0f) * it.level).toInt() }

        val percHp = items.sumBy { ((it.template.gbPercHp?:0f) * it.level).toInt() }
        val percArmor = items.sumBy { ((it.template.gbPercArmor?:0f) * it.level).toInt() }
        val percRegeneration = items.sumBy { ((it.template.gbPercRegeneration?:0f) * it.level).toInt() }
        val percEvasion = items.sumBy { ((it.template.gbPercEvasion?:0f) * it.level).toInt() }
        val percResist = items.sumBy { ((it.template.gbPercResist?:0f) * it.level).toInt() }
        val percWisdom = items.sumBy { ((it.template.gbPercWisdom?:0f) * it.level).toInt() }

        val resist = updated(calculateResist(_p), flatResist, percResist)

        return UnitStats(UnitType.valueOf(_p.unit), _p.level, _body, _mind, _spirit,
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
