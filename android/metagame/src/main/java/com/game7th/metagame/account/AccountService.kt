package com.game7th.metagame.account

import com.game7th.metagame.inventory.InventoryItem

interface AccountService {

    fun getPersonages(): List<PersonageData>

    fun addPersonageExperience(personageId: Int, experience: Int) : List<PersonageExperienceResult>

    fun equipItem(personageId: Int, item: InventoryItem)

    fun dequipItem(personageId: Int, item: InventoryItem)
}