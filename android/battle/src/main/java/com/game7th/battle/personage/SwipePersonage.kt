package com.game7th.battle.personage

import com.game7th.battle.ability.GladiatorStrike
import com.game7th.battle.ability.PersonageAbility
import com.game7th.battle.balance.SwipeBalance

sealed class SwipePersonage(
        val id: Int,
        val skin: String,
        val stats: PersonageStats,
        val abilities: List<PersonageAbility>
)

class Gladiator(
        id: Int,
        balance: SwipeBalance,
        stats: PersonageStats
) : SwipePersonage(id, "p_gladiator", stats, listOf(GladiatorStrike(balance)))