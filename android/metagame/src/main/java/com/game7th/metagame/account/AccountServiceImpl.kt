package com.game7th.metagame.account

import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.unit.UnitType
import com.google.gson.Gson
import kotlin.math.exp
import kotlin.random.Random

class AccountServiceImpl(
        private val gson: Gson,
        private val storage: PersistentStorage
) : AccountService {

    var pool: PersonagePool

    init {
        val dataString = storage.get(KEY_PERSONAGES)
        pool = if (dataString == null) {
            val initialData = PersonagePool(
                    listOf(PersonageData(
                            unit = UnitType.GLADIATOR,
                            level = 1,
                            experience = 0,
                            stats = PersonageAttributeStats(4, 2, 1),
                            id = 0
                    ),
                            PersonageData(
                                    unit = UnitType.GLADIATOR,
                                    level = 10,
                                    experience = 0,
                                    stats = PersonageAttributeStats(26	,17	,8),
                                    id = 1
                            ),
                            PersonageData(
                                    unit = UnitType.GLADIATOR,
                                    level = 100,
                                    experience = 0,
                                    stats = PersonageAttributeStats(2529	,1685	,842),
                                    id = 2
                            )
                    ),

                    nextPersonageId = 3
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
                val rolls = personage.level + 1
                var bodyBonus = 0
                var spiritBonus = 0
                var mindBonus = 0
                (1..rolls).forEach {
                    val roll = Random.nextInt(personage.unit.bodyWeight + personage.unit.mindWeight + personage.unit.spiritWeight) + 1
                    if (roll <= personage.unit.bodyWeight) {
                        bodyBonus++
                    } else if (roll <= personage.unit.bodyWeight + personage.unit.spiritWeight) {
                        spiritBonus++
                    } else {
                        mindBonus++
                    }
                }
                personageUpdateResult = PersonageExperienceResult(true, personage.level + 1, PersonageAttributeStats(bodyBonus, spiritBonus, mindBonus), personage.experience, nextLevelExp, nextLevelExp)

                personage.copy(level = personage.level + 1, stats = personage.stats.copy(
                        personage.stats.body + bodyBonus,
                        personage.stats.spirit + spiritBonus,
                        personage.stats.mind + spiritBonus),
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

    companion object {
        const val KEY_PERSONAGES = "account.personages"
    }
}