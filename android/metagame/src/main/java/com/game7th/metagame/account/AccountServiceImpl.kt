package com.game7th.metagame.account

import com.game7th.metagame.network.CloudApi
import com.game7th.swiped.api.AccountDto
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
        }
        return pool
    }

//
//    override fun addPersonageExperience(personageId: Int, experience: Int): List<PersonageExperienceResult> {
//        var expLeft = experience
//        val result = mutableListOf<PersonageExperienceResult>()
//        while (expLeft > 0) {
//            pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
//                val newExp = personage.experience + experience
//                val nextLevelExp = ((personage.level - 1) + exp(personage.level * 0.1f)).toInt() * personage.level * 50
//                val updatedPersonage = if (newExp >= nextLevelExp) {
//                    expLeft = newExp - nextLevelExp
//                    val rolls = 1
//                    var bodyBonus = 0
//                    var spiritBonus = 0
//                    var mindBonus = 0
//                    if (personage.level > 5) {
//                        (1..rolls).forEach {
//                            val roll = Random.nextInt(personage.unit.bodyWeight + personage.unit.mindWeight + personage.unit.spiritWeight)
//                            if (roll < personage.unit.bodyWeight) {
//                                bodyBonus++
//                            } else if (roll < personage.unit.bodyWeight + personage.unit.spiritWeight) {
//                                spiritBonus++
//                            } else {
//                                mindBonus++
//                            }
//                        }
//                    } else {
//                        bodyBonus = scriptedBonuses.count { it.level == personage.level && it.weight == personage.unit.bodyWeight }
//                        mindBonus = scriptedBonuses.count { it.level == personage.level && it.weight == personage.unit.mindWeight }
//                        spiritBonus = scriptedBonuses.count { it.level == personage.level && it.weight == personage.unit.spiritWeight }
//                    }
//
//                    result.add(PersonageExperienceResult(true, personage.level + 1, PersonageAttributeStats(bodyBonus, spiritBonus, mindBonus), personage.experience, nextLevelExp, nextLevelExp))
//
//                    personage.copy(level = personage.level + 1, stats = personage.stats.copy(
//                            personage.stats.body + bodyBonus,
//                            personage.stats.spirit + spiritBonus,
//                            personage.stats.mind + mindBonus),
//                            experience = 0)
//
//                } else {
//                    expLeft = 0
//                    result.add(PersonageExperienceResult(false, 0, null, personage.experience, newExp, nextLevelExp))
//                    personage.copy(experience = newExp)
//                }
//
//                val updatedData = pool.personages.map { personage ->
//                    if (personage.id == personageId) {
//                        updatedPersonage
//                    } else {
//                        personage
//                    }
//                }
//                pool = pool.copy(personages = updatedData)
//            }
//        }
//        savePersonagePool(pool)
//        return result
//    }

//    override fun equipItem(personageId: Int, item: InventoryItemFullInfoDto) {
//        pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
//            val itemToReplace = personage.items.firstOrNull { it.template.node == item.template.node }
//            itemToReplace?.let {
//                personage.items.remove(it)
//                gearService.addRewards(listOf(RewardData.ArtifactRewardData(it)))
//            }
//            gearService.removeItem(item)
//            personage.items.add(item)
//            savePersonagePool(pool)
//        }
//    }
//
//    override fun dequipItem(personageId: Int, item: InventoryItemFullInfoDto) {
//        pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
//            if (personage.items.remove(item)) {
//                gearService.addRewards(listOf(RewardData.ArtifactRewardData(item)))
//                savePersonagePool(pool)
//            }
//        }
//    }

    override suspend fun getBalance(): Map<String, Int> {
        return account?.balances ?: emptyMap()
    }


    data class ScriptedAttrBonusSetup(
            val level: Int,
            val weight: Int
    )
}

