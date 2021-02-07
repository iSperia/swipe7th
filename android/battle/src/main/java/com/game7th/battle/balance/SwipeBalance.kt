package com.game7th.battle.balance

data class SwipeBalance(
        val version: String,
        val baseSkillTileEmitProbability: Float,
        val secondarySkillTileEmitProbability: Float,
        val defaultTier1Threshold: Int,
        val defaultTier2Threshold: Int,

        val slimeFlatDamage: Int,
        val slimeScaleDamage: Int,
        val slimeMulti: Float
)
