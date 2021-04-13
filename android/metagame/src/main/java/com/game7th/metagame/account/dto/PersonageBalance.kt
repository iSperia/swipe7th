package com.game7th.metagame.account.dto

import com.game7th.swiped.api.Currency


data class PersonageBalance(
        val currencies: MutableMap<Currency, Int>
)