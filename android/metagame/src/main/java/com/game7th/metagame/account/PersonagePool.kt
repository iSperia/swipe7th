package com.game7th.metagame.account

import com.game7th.metagame.unit.UnitType

data class PersonageStats(
        val body: Int,
        val spirit: Int,
        val mind: Int
)

data class PersonageData(
        val unit: UnitType,
        val level: Int,
        val experience: Int,
        val stats: PersonageStats,
        val id: Int
)

data class PersonageExperienceResult(
        val levelUp: Boolean,
        val newLevel: Int,
        val gainedStats: PersonageStats?,
        val oldExp: Int,
        val newExp: Int,
        val maxExp: Int
)

data class PersonagePool(
    val personages: List<PersonageData>,
    val nextPersonageId: Int
)
