package com.game7th.battle.balance

data class SwipeBalance(
        val version: String,
        val baseSkillTileEmitProbability: Float,
        val secondarySkillTileEmitProbability: Float,
        val defaultTier1Threshold: Int,
        val defaultTier2Threshold: Int,

        val personageHealthBase: Int,
        val personageBodyMultiply: Int,
        val personageLevelMultiply: Int,

        val slimeFlatDamage: Int,
        val slimeScaleDamage: Int,
        val slimeMulti: Float,
        val slimeBaseHealth: Int,
        val slimeLevelMulti: Int,

        val gladiatorAtkTier1DmgConst: Int,
        val gladiatorAtkTier1LvlKoef: Float,
        val gladiatorAtkTier1StrKoef: Float,
        val gladiatorAtkTIer1TileKoef: Float
)
