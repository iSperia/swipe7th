package com.game7th.battle.unit

import com.game7th.battle.ability.UnitAbility
import com.game7th.battle.personage.PersonageStats
import com.game7th.battle.personage.PersonageViewModel

enum class Team {
    LEFT, RIGHT
}

data class CappedStat(
        var value: Int,
        val maxValue: Int
) {
    fun notCapped() = value < maxValue
}

/**
 * The unit placed inside a battle
 */
data class BattleUnit(
        val id: Int,
        val position: Int,
        val stats: UnitStats,
        val team: Team
) {
    fun toViewModel(): PersonageViewModel = PersonageViewModel(
            stats = PersonageStats(
                    body = stats.body,
                    health = stats.health.value,
                    maxHealth = stats.health.maxValue,
                    armor = stats.armor.value,
                    maxArmor = stats.armor.maxValue,
                    level = stats.level,
                    spirit = stats.spirit,
                    regeneration = stats.regeneration,
                    evasion = stats.evasion,
                    mind = stats.mind,
                    effectiveness = stats.wisdom,
                    magicDefense = stats.resist.value,
                    maxMagicDefense = stats.resist.maxValue,
                    tick = stats.tick,
                    maxTick = stats.maxTick,
                    isStunned = !isNotStunned()
            ),
            skin = stats.skin,
            id = id,
            team = team.ordinal
    )

    fun isNotStunned(): Boolean {
        return stats.ailments.firstOrNull { it.ailmentType == AilmentType.STUN } == null
    }
}

enum class AilmentType {
    POISON, STUN, SCORCH
}

data class UnitAilment(
        val ailmentType: AilmentType,
        var ticks: Int,
        var value: Float
)

/**
 * Generic unit information
 */
data class UnitStats(
        val skin: String,

        val level: Int,
        val body: Int = 0,
        val mind: Int = 0,
        val spirit: Int = 0,

        val health: CappedStat,
        val armor: CappedStat = CappedStat(0, 0),
        val resist: CappedStat = CappedStat(0, 0),
        val evasion: Int = 0,
        val regeneration: Int = 0,
        val wisdom: Int = 0,

        var tick: Int = 0,
        var maxTick: Int = 0,

        var ailments: MutableList<UnitAilment> = mutableListOf()
) {
    val abilities = mutableListOf<UnitAbility>()

    operator fun plusAssign(ability: UnitAbility) {
        abilities.add(ability)
    }

    fun addAbility(init: UnitAbility.() -> Unit): UnitAbility {
        val ability = UnitAbility()
        ability.init()
        abilities.add(ability)
        return ability
    }
}
