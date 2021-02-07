package com.game7th.battle.npc

import com.game7th.battle.personage.PersonageStats
import com.game7th.battle.personage.PersonageViewModel

abstract class NpcPersonage(
        val skin: String,
        val id: Int,
        val stats: PersonageStats,
        val abilities: List<NpcAbility>
) {
}

fun NpcPersonage.toViewModel(): PersonageViewModel {
    return PersonageViewModel(stats.copy(), skin, id)
}

class SlimePersonage(id: Int, stats: PersonageStats) : NpcPersonage("personage_slime", id, stats, listOf(
        SlimeAttackAbility()
))
