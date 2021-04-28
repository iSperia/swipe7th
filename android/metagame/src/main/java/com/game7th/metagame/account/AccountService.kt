package com.game7th.metagame.account

import com.game7th.swiped.api.PersonageAttributesDto
import com.game7th.swiped.api.PersonageDto

interface AccountService {

    suspend fun init(): Unit

    suspend fun getPersonages(): List<PersonageDto>

    suspend fun refreshPersonages()

    suspend fun getBalance(): Map<String, Int>

    suspend fun refreshBalance()

    suspend fun getPersonageGearStats(personageId: String): PersonageAttributesDto
}