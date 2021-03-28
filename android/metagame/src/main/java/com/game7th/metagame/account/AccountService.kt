package com.game7th.metagame.account

import com.game7th.metagame.account.dto.PersonageBalance
import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.account.dto.PersonageData
import com.game7th.metagame.account.dto.PersonageExperienceResult
import com.game7th.metagame.inventory.dto.InventoryItem

interface AccountService {

    fun getPersonages(): List<PersonageData>

    fun addPersonageExperience(personageId: Int, experience: Int) : List<PersonageExperienceResult>

    fun equipItem(personageId: Int, item: InventoryItem)

    fun dequipItem(personageId: Int, item: InventoryItem)

    fun getBalance(): PersonageBalance

    fun fund(currency: Currency, amount: Int): PersonageBalance

    fun spend(currency: Currency, amount: Int): PersonageBalance
}