package com.game7th.battle.personage

import com.game7th.battle.PersonageConfig
import com.game7th.battle.balance.SwipeBalance

object PersonageFactory {

    fun producePersonage(config: PersonageConfig, balance: SwipeBalance, id: Int): SwipePersonage? {
        val totalStats = (config.level - 1) * (1 + config.level) / 2
        val tertiaryStat = totalStats / 6
        val secondaryStat = totalStats / 3
        val primaryStat = totalStats - secondaryStat - tertiaryStat

        val primaryStatTotal = primaryStat + 6
        val secondaryStatTotal = secondaryStat + 4
        val tertiaryStatTotal = tertiaryStat + 2

        return when (config.codeName) {
            "gladiator" -> {
                val hp = balance.personageHealthBase + balance.personageBodyMultiply * primaryStatTotal + balance.personageLevelMultiply * (config.level-1)
                return Gladiator(id, balance, PersonageStats(
                        primaryStatTotal,
                        hp,
                        hp,
                        0,
                        secondaryStatTotal,
                        0,
                        0,
                        tertiaryStatTotal,
                        0,
                        0,
                        config.level))
            }
            else -> null
        }
    }
}