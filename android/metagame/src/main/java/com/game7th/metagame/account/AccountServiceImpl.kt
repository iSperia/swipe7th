package com.game7th.metagame.account

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.dto.*
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.dto.UnitType
import com.game7th.swiped.api.Currency
import com.game7th.swiped.api.InventoryItemFullInfoDto
import com.google.gson.Gson
import kotlin.math.exp
import kotlin.random.Random

class AccountServiceImpl(
        private val gson: Gson,
        private val storage: PersistentStorage,
        private val files: FileProvider,
        private val gearService: GearService
) : AccountService {

    var pool: PersonagePool

    val personageBalance: PersonageBalance

    init {
        val balanceString = storage.get(KEY_BALANCE)
        personageBalance = if (balanceString == null) {
            PersonageBalance(Currency.values().map { it to 0 }.toMap().toMutableMap())
                    .apply {
//                        currencies[Currency.GOLD] = 10000
                        currencies[Currency.GEMS] = 1000
//                        currencies[Currency.DUST] = 10000
                    }
        } else {
            gson.fromJson<PersonageBalance>(balanceString, PersonageBalance::class.java)
        }
        val dataString = storage.get(KEY_PERSONAGES)
        pool = if (dataString == null) {
            val initialData = PersonagePool(
                    mutableListOf(
                            PersonageData(
                                    unit = UnitType.GLADIATOR,
                                    level = 1,
                                    experience = 0,
                                    stats = PersonageAttributeStats(1, 0, 0),
                                    id = 0,
                                    items = mutableListOf())
                    ),
                    nextPersonageId = 2
            )
            savePersonagePool(initialData)
            initialData
        } else {
            gson.fromJson<PersonagePool>(dataString, PersonagePool::class.java)
        }
    }

    override fun getPersonages(): List<PersonageData> {
        return pool.personages
    }

    override fun addPersonage(personage: UnitType) {
        val max = listOf(personage.bodyWeight, personage.spiritWeight, personage.mindWeight).withIndex().maxBy { it.value }?.index ?: 0
        val personage = PersonageData(personage, 1, 0, PersonageAttributeStats(if (max == 0) 1 else 0, if (max == 1) 1 else 0, if (max == 2) 1 else 0), pool.nextPersonageId, mutableListOf())
        pool = pool.copy(personages = pool.personages + personage, nextPersonageId = pool.nextPersonageId + 1)
        savePersonagePool(pool)
    }

    private fun savePersonagePool(pool: PersonagePool) {
        val dataString = gson.toJson(pool)
        storage.put(KEY_PERSONAGES, dataString)
    }

    private fun saveBalance() {
        val balanceString = gson.toJson(personageBalance)
        storage.put(KEY_BALANCE, balanceString)
    }

    override fun addPersonageExperience(personageId: Int, experience: Int): List<PersonageExperienceResult> {
        var expLeft = experience
        val result = mutableListOf<PersonageExperienceResult>()
        while (expLeft > 0) {
            pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
                val newExp = personage.experience + experience
                val nextLevelExp = ((personage.level - 1) + exp(personage.level * 0.1f)).toInt() * personage.level * 50
                val updatedPersonage = if (newExp >= nextLevelExp) {
                    expLeft = newExp - nextLevelExp
                    val rolls = 1
                    var bodyBonus = 0
                    var spiritBonus = 0
                    var mindBonus = 0
                    if (personage.level > 5) {
                        (1..rolls).forEach {
                            val roll = Random.nextInt(personage.unit.bodyWeight + personage.unit.mindWeight + personage.unit.spiritWeight)
                            if (roll < personage.unit.bodyWeight) {
                                bodyBonus++
                            } else if (roll < personage.unit.bodyWeight + personage.unit.spiritWeight) {
                                spiritBonus++
                            } else {
                                mindBonus++
                            }
                        }
                    } else {
                        bodyBonus = scriptedBonuses.count { it.level == personage.level && it.weight == personage.unit.bodyWeight }
                        mindBonus = scriptedBonuses.count { it.level == personage.level && it.weight == personage.unit.mindWeight }
                        spiritBonus = scriptedBonuses.count { it.level == personage.level && it.weight == personage.unit.spiritWeight }
                    }

                    result.add(PersonageExperienceResult(true, personage.level + 1, PersonageAttributeStats(bodyBonus, spiritBonus, mindBonus), personage.experience, nextLevelExp, nextLevelExp))

                    personage.copy(level = personage.level + 1, stats = personage.stats.copy(
                            personage.stats.body + bodyBonus,
                            personage.stats.spirit + spiritBonus,
                            personage.stats.mind + mindBonus),
                            experience = 0)

                } else {
                    expLeft = 0
                    result.add(PersonageExperienceResult(false, 0, null, personage.experience, newExp, nextLevelExp))
                    personage.copy(experience = newExp)
                }

                val updatedData = pool.personages.map { personage ->
                    if (personage.id == personageId) {
                        updatedPersonage
                    } else {
                        personage
                    }
                }
                pool = pool.copy(personages = updatedData)
            }
        }
        savePersonagePool(pool)
        return result
    }

    override fun equipItem(personageId: Int, item: InventoryItemFullInfoDto) {
        pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
            val itemToReplace = personage.items.firstOrNull { it.template.node == item.template.node }
            itemToReplace?.let {
                personage.items.remove(it)
                gearService.addRewards(listOf(RewardData.ArtifactRewardData(it)))
            }
            gearService.removeItem(item)
            personage.items.add(item)
            savePersonagePool(pool)
        }
    }

    override fun dequipItem(personageId: Int, item: InventoryItemFullInfoDto) {
        pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
            if (personage.items.remove(item)) {
                gearService.addRewards(listOf(RewardData.ArtifactRewardData(item)))
                savePersonagePool(pool)
            }
        }
    }

    override fun getBalance(): PersonageBalance {
        return personageBalance.copy()
    }

    override fun fund(currency: com.game7th.swiped.api.Currency, amount: Int): PersonageBalance {
        personageBalance.currencies[currency] = (personageBalance.currencies[currency] ?: 0) + amount
        saveBalance()
        return getBalance()
    }

    override fun spend(currency: Currency, amount: Int): PersonageBalance {
        val currentBalance = personageBalance.currencies[currency] ?: 0
        if (currentBalance >= amount) {
            personageBalance.currencies[currency] = currentBalance - amount
            saveBalance()
        }
        return getBalance()
    }

    override fun addRewards(rewards: List<RewardData>) {
        rewards.forEach { reward ->
            when (reward) {
                is RewardData.CurrencyRewardData -> {
                    fund(reward.currency, reward.amount)
                }
            }
        }
    }

    companion object {
        const val KEY_PERSONAGES = "account.personages"
        const val KEY_BALANCE = "account.balance"
        val scriptedBonuses = listOf(
            ScriptedAttrBonusSetup(1, 3),
                ScriptedAttrBonusSetup(2,2),
                ScriptedAttrBonusSetup(3, 3),
                ScriptedAttrBonusSetup(4,2),
                ScriptedAttrBonusSetup(5, 1)
        )
    }

    data class ScriptedAttrBonusSetup(
            val level: Int,
            val weight: Int
    )
}

