package com.game7th.battle.unit

import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.unit.npc.produceGreenSlime
import com.game7th.battle.unit.npc.producePurpleSlime
import com.game7th.battle.unit.npc.produceSlimeFather
import com.game7th.battle.unit.npc.produceSlimeMother
import com.game7th.battle.unit.personages.produceGladiator
import com.game7th.battle.unit.personages.produceToxicArcher
import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.unit.UnitType

object UnitFactory {
    fun produce(type: UnitType, balance: SwipeBalance, level: Int, stats: PersonageAttributeStats): UnitStats? {
        return when (type) {
            UnitType.GLADIATOR -> produceGladiator(balance, level, stats)
            UnitType.POISON_ARCHER -> produceToxicArcher(balance, level, stats)
            UnitType.GREEN_SLIME -> produceGreenSlime(balance, level)
            UnitType.PURPLE_SLIME -> producePurpleSlime(balance, level)
            UnitType.SLIME_MOTHER -> produceSlimeMother(balance, level)
            UnitType.SLIME_FATHER -> produceSlimeFather(balance, level)
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
                regeneration = (b.stats.regenerationPerSpirit * spirit).toInt(),
                evasion = b.stats.evasionPerSpirit * spirit,
                intelligence = mind
        ).apply { processor(this) }
    }
}
