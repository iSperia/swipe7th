package com.game7th.metagame.account

interface AccountService {

    fun getPersonages(): List<PersonageData>

    fun addPersonageExperience(personageId: Int, experience: Int) : PersonageExperienceResult
}