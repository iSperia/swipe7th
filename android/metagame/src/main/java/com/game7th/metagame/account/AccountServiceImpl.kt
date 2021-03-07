package com.game7th.metagame.account

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.inventory.InventoryItem
import com.game7th.metagame.unit.UnitType
import com.google.gson.Gson
import kotlin.math.exp
import kotlin.math.max
import kotlin.random.Random

class AccountServiceImpl(
        private val gson: Gson,
        private val storage: PersistentStorage,
        private val files: FileProvider,
        private val gearService: GearService
) : AccountService {

    var pool: PersonagePool

    init {
        val dataString = storage.get(KEY_PERSONAGES)
        pool = if (dataString == null) {

            val initialData = PersonagePool(
                    ((1..10).map {
                        val lvl = it * 2 - 1
                        val primary = max(1, lvl / 2)
                        val tertiary = (lvl - primary) / 3
                        val secondary = lvl - primary - tertiary
                        PersonageData(
                                unit = UnitType.GLADIATOR,
                                level = lvl,
                                experience = 0,
                                stats = PersonageAttributeStats(primary, secondary, tertiary),
                                id = it,
                                items = mutableListOf())
                    } + (1..10).map {
                        val lvl = it * 2 - 1
                        val primary = max(1, lvl / 2)
                        val tertiary = (lvl - primary) / 3
                        val secondary = lvl - primary - tertiary
                        PersonageData(
                                unit = UnitType.POISON_ARCHER,
                                level = lvl,
                                experience = 0,
                                stats = PersonageAttributeStats(tertiary, primary, secondary),
                                id = 100 + it,
                                items = mutableListOf())
                    }).sortedBy { it.level },
                    nextPersonageId = 200
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

    private fun savePersonagePool(pool: PersonagePool) {
        val dataString = gson.toJson(pool)
        storage.put(KEY_PERSONAGES, dataString)
    }

    override fun addPersonageExperience(personageId: Int, experience: Int): PersonageExperienceResult {
        return pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
            val newExp = personage.experience + experience
            val nextLevelExp = ((personage.level - 1) + exp(personage.level * 0.1f)).toInt() * personage.level * 50
            val personageUpdateResult: PersonageExperienceResult
            val updatedPersonage = if (newExp >= nextLevelExp) {
                val rolls = 1
                var bodyBonus = 0
                var spiritBonus = 0
                var mindBonus = 0
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

                personageUpdateResult = PersonageExperienceResult(true, personage.level + 1, PersonageAttributeStats(bodyBonus, spiritBonus, mindBonus), personage.experience, nextLevelExp, nextLevelExp)

                personage.copy(level = personage.level + 1, stats = personage.stats.copy(
                        personage.stats.body + bodyBonus,
                        personage.stats.spirit + spiritBonus,
                        personage.stats.mind + mindBonus),
                        experience = 0)

            } else {
                personageUpdateResult = PersonageExperienceResult(false, 0, null, personage.experience, newExp, nextLevelExp)
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
            savePersonagePool(pool)

            personageUpdateResult
        } ?: PersonageExperienceResult(false, 0, null, 0, 0, 0)
    }

    override fun equipItem(personageId: Int, item: InventoryItem) {
        pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
            val itemToReplace = personage.items.firstOrNull { it.node == item.node }
            itemToReplace?.let {
                personage.items.remove(it)
                gearService.addRewards(listOf(RewardData.ArtifactRewardData(it)))
                gearService.removeItem(item)
            }
            personage.items.add(item)
            savePersonagePool(pool)
        }
    }

    override fun dequipItem(personageId: Int, item: InventoryItem) {
        pool.personages.firstOrNull { it.id == personageId }?.let { personage ->
            personage.items.remove(item)
            gearService.addRewards(listOf(RewardData.ArtifactRewardData(item)))
            savePersonagePool(pool)
        }
    }

    companion object {
        const val KEY_PERSONAGES = "account.personages"
    }
}