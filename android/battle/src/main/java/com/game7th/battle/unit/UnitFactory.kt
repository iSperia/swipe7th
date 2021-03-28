package com.game7th.battle.unit

import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.unit.npc.*
import com.game7th.battle.unit.personages.produceGladiator
import com.game7th.battle.unit.personages.produceToxicArcher
import com.game7th.metagame.account.dto.PersonageAttributeStats
import com.game7th.metagame.dto.UnitType

object UnitFactory {
    fun produce(type: UnitType, balance: SwipeBalance, level: Int, unitStats: UnitStats?): UnitStats? {
        return when (type) {
            UnitType.GLADIATOR -> produceGladiator(balance, unitStats!!)//UnitStats(UnitType.GLADIATOR, 100, 20, 20, 20, CappedStat(5000,5000), 1000, 1000, 500,100,150))// unitStats!!)
            UnitType.POISON_ARCHER -> produceToxicArcher(balance, unitStats!!)
            UnitType.GREEN_SLIME -> produceGreenSlime(balance, level)
            UnitType.PURPLE_SLIME -> producePurpleSlime(balance, level)
            UnitType.SLIME_MOTHER -> produceSlimeMother(balance, level)
            UnitType.SLIME_FATHER -> produceSlimeFather(balance, level)
            UnitType.SLIME_BOSS -> produceSlimeBoss(balance, level)
            else -> null
        }
    }

    fun producePersonage(
            b: SwipeBalance,
            unitType: UnitType,
            level: Int,
            stats: PersonageAttributeStats,
            processor: (UnitStats) -> Unit
    ): UnitStats {
        val body = stats.body
        val spirit = stats.spirit
        val mind = stats.mind

        val health = b.stats.baseHealth + b.stats.healthPerBody * body + b.stats.healthPerLevel * level
        val armor = body * b.stats.armorPerBody
        val resist = mind * b.stats.resistPerMind

        return UnitStats(
                unitType = unitType,
                level = level,
                body = body,
                spirit = spirit,
                mind = mind,
                health = CappedStat(health, health),
                armor = armor,
                resist = resist,
                resistMax = resist,
                regeneration = (b.stats.regenerationPerSpirit * spirit).toInt(),
                evasion = b.stats.evasionPerSpirit * spirit,
                wisdom = mind * b.stats.wizdomMultiplier
        ).apply { processor(this) }
    }
}
