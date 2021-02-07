package com.game7th.battle.personage

data class PersonageViewModel(
        val stats: PersonageStats,
        val skin: String,
        val id: Int
)

fun SwipePersonage.toViewModel(): PersonageViewModel {
    return PersonageViewModel(
            stats.copy(),
            skin,
            id
    )
}
