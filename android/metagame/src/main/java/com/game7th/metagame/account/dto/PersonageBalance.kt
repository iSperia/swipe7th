package com.game7th.metagame.account.dto

enum class Currency {
    GOLD, GEMS, DUST
}

data class PersonageBalance(
        val currencies: MutableMap<Currency, Int>
)