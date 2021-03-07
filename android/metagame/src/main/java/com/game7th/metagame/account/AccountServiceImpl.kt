package com.game7th.metagame.account

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.inventory.GearConfig
import com.game7th.metagame.inventory.InventoryItem
import com.game7th.metagame.inventory.InventoryPool
import com.game7th.metagame.inventory.ItemNode
import com.game7th.metagame.unit.UnitType
import com.google.gson.Gson
import kotlin.math.exp
import kotlin.math.max
import kotlin.random.Random

class AccountServiceImpl(
        private val gson: Gson,
        private val storage: PersistentStorage,
        private val files: FileProvider
) : AccountService {

    var pool: PersonagePool

    var inventory: InventoryPool

    val gearConfig: GearConfig

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
                                items = emptyList())
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
                                items = emptyList())
                    }).sortedBy { it.level },
                    nextPersonageId = 200
            )
            savePersonagePool(initialData)
            initialData
        } else {
            gson.fromJson<PersonagePool>(dataString, PersonagePool::class.java)
        }

        val inventoryString = storage.get(KEY_INVENTORY)
        inventory = if (inventoryString == null) {
            val initialData = InventoryPool(
                    items = (1..100).map { InventoryItem(gbFlatBody = it, level = it, node = ItemNode.BODY, name = "TEST_ITEM") }.toMutableList()
            )
            initialData
        } else {
            gson.fromJson<InventoryPool>(inventoryString, InventoryPool::class.java)
        }

        gearConfig = gson.fromJson<GearConfig>(files.getFileContent("artifacts.json"), GearConfig::class.java)
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

                //ok, we have some rewards
                val rewards = mutableListOf<RewardData>()
                val totalPoints = experience / 50
                val r1 = Random.nextInt(totalPoints)
                getArtifactReward(r1 + 1)?.let { rewards.add(it) }
                if (Random.nextBoolean()) {
                    val r2 = Random.nextInt(totalPoints - r1)
                    getArtifactReward(r2 + 1)?.let { rewards.add(it) }
                    if (Random.nextBoolean()) {
                        val r3 = Random.nextInt(totalPoints - r1 - r2)
                        getArtifactReward(r3 + 1)?.let { rewards.add(it) }
                    }
                }

                rewards.forEach {
                    when (it) {
                        is RewardData.ArtifactRewardData -> {
                            inventory.items.add(it.item)
                        }
                    }
                }
                storage.put(KEY_INVENTORY, gson.toJson(inventory)) //save inventory to storage

                personageUpdateResult = PersonageExperienceResult(true, personage.level + 1, PersonageAttributeStats(bodyBonus, spiritBonus, mindBonus), personage.experience, nextLevelExp, nextLevelExp, rewards)

                personage.copy(level = personage.level + 1, stats = personage.stats.copy(
                        personage.stats.body + bodyBonus,
                        personage.stats.spirit + spiritBonus,
                        personage.stats.mind + mindBonus),
                        experience = 0)

            } else {
                personageUpdateResult = PersonageExperienceResult(false, 0, null, personage.experience, newExp, nextLevelExp, emptyList())
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
        } ?: PersonageExperienceResult(false, 0, null, 0, 0, 0, emptyList())
    }

    private fun getArtifactReward(level: Int): RewardData.ArtifactRewardData? {
        val filteredArtifacts = gearConfig.items.filter { it.maxLevel >= level && it.minLevel <= level }
        val totalWeight = filteredArtifacts.sumBy { it.weight }
        val roll = Random.nextInt(1, totalWeight + 1)
        var sum = 0
        return filteredArtifacts.firstOrNull {
            sum += it.weight
            sum >= roll
        }?.let {
            RewardData.ArtifactRewardData(it.template.copy(level = level))
        }
    }

    companion object {
        const val KEY_PERSONAGES = "account.personages"
        const val KEY_INVENTORY = "account.inventory"
    }
}