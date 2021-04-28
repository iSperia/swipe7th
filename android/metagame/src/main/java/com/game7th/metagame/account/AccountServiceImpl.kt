package com.game7th.metagame.account

import com.game7th.metagame.network.CloudApi
import com.game7th.swiped.api.AccountDto
import com.game7th.swiped.api.PersonageAttributesDto
import com.game7th.swiped.api.PersonageDto

class AccountServiceImpl(
        private val api: CloudApi
) : AccountService {

    var account: AccountDto? = null

    var pool: List<PersonageDto> = emptyList()

    private var dirty = false

    override suspend fun init() {
        refreshAccount()
        reloadPersonages()
    }

    private suspend fun reloadPersonages() {
        pool = api.getPersonages()
    }

    override suspend fun refreshPersonages() {
        dirty = true
    }

    private suspend fun refreshAccount() {
        account = api.getAccount()
    }

    override suspend fun refreshBalance() {
        account = api.getAccount()
    }

    override suspend fun getPersonages(): List<PersonageDto> {
        if (dirty) {
            reloadPersonages()
            dirty = false
        }
        return pool
    }

    override suspend fun getPersonageGearStats(personageId: String): PersonageAttributesDto {
        return api.getPersonageGearedStats(personageId)
    }

    override suspend fun getBalance(): Map<String, Int> {
        return account?.balances ?: emptyMap()
    }


    data class ScriptedAttrBonusSetup(
            val level: Int,
            val weight: Int
    )
}

