package com.game7th.battle.personage

import com.game7th.battle.PersonageConfig
import com.game7th.battle.balance.SwipeBalance

object PersonageFactory {

    fun producePersonage(config: PersonageConfig, balance: SwipeBalance, id: Int): SwipePersonage? {
        val totalStats = (config.level - 1) * (config.level) / 2
        val tertiaryStat = totalStats / 6
        val secondaryStat = totalStats / 3
        val primaryStat = totalStats - secondaryStat - tertiaryStat

        val primaryStatTotal = primaryStat + 6
        val secondaryStatTotal = secondaryStat + 4
        val tertiaryStatTotal = tertiaryStat + 2

        return when (config.codeName) {
            "gladiator" -> {
                val hp = balance.personageHealthBase + balance.personageBodyMultiply * primaryStatTotal + balance.personageLevelMultiply * (config.level-1)
                val armor = primaryStatTotal * balance.personageArmorPerBodyMultiply
                val magicDefense = tertiaryStatTotal * balance.personageMagicDefPerMindMultiply
                return Gladiator(id, balance, PersonageStats(
                        primaryStatTotal,
                        hp,
                        hp,
                        armor,
                        armor,
                        secondaryStatTotal,
                        (balance.personageRegenerationPerBodyMultiply * secondaryStatTotal).toInt(),
                        secondaryStatTotal,
                        tertiaryStatTotal,
                        tertiaryStatTotal,
                        magicDefense,
                        magicDefense,
                        config.level))
            }
            "poison_archer" -> {
                val hp = balance.personageHealthBase + balance.personageBodyMultiply * tertiaryStatTotal + balance.personageLevelMultiply * (config.level-1)
                val armor = tertiaryStatTotal * balance.personageArmorPerBodyMultiply
                val magicDefense = secondaryStatTotal * balance.personageMagicDefPerMindMultiply
                return PoisonArcher(id, balance, PersonageStats(
                        tertiaryStatTotal,
                        hp,
                        hp,
                        armor,
                        armor,
                        primaryStatTotal,
                        (balance.personageRegenerationPerBodyMultiply * primaryStatTotal).toInt(),
                        secondaryStatTotal,
                        secondaryStatTotal,
                        secondaryStatTotal,
                        magicDefense,
                        magicDefense,
                        config.level))
            }
            else -> null
        }
    }
}