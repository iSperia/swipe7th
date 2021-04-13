package com.game7th.metagame.account.dto

import com.game7th.metagame.dto.UnitType
import com.game7th.swiped.api.InventoryItemFullInfoDto

data class PersonageAttributeStats(
        val body: Int,
        val spirit: Int,
        val mind: Int
) {
    override fun toString() = (if (body > 0) "BODY: + $body" else "") + (if (spirit > 0) "SPIRIT + $spirit" else "") + (if (mind > 0) "MIND + $mind" else "")

    fun plus(stats: PersonageAttributeStats) = PersonageAttributeStats(body + stats.body, spirit + stats.spirit, mind + stats.mind)
}

data class PersonageData(
        val unit: UnitType,
        val level: Int,
        val experience: Int,
        val stats: PersonageAttributeStats,
        val id: Int,
        val items: MutableList<InventoryItemFullInfoDto>
)

data class PersonageExperienceResult(
        val levelUp: Boolean,
        val newLevel: Int,
        val gainedStats: PersonageAttributeStats?,
        val oldExp: Int,
        val newExp: Int,
        val maxExp: Int
)

data class PersonagePool(
        val personages: List<PersonageData>,
        val nextPersonageId: Int
)
