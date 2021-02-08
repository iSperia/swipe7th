package com.game7th.battle.npc

import com.game7th.battle.PersonageConfig
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.personage.PersonageStats

object NpcFactory {

    fun produceNpc(config: PersonageConfig, balance: SwipeBalance, id: Int): NpcPersonage? {
        return when (config.codeName) {
            "slime" -> {
                val hp = (1 + config.level * (2 * config.level)) + balance.slimeBaseHealth
                return SlimePersonage(id, PersonageStats(
                        0,
                        hp,
                        hp,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        config.level))
            }
            else -> null
        }
    }
}