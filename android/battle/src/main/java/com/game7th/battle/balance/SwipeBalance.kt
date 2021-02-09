package com.game7th.battle.balance

data class StatBalance(
        val baseHealth: Int,
        val healthPerBody: Int,
        val healthPerLevel: Int,
        val armorPerBody: Int,
        val resistPerMind: Int,
        val regenerationPerSpirit: Float,
        val wizdomMultiplier: Float,
        val evasionPerSpirit: Int,
        val evasion: EvasionBalance
)

data class EvasionBalance(
        val k: Int,
        val p: Float
)

data class SwipeBalance(
        val version: String,
        val defaultTier1Threshold: Int,
        val defaultTier2Threshold: Int,

        val stats: StatBalance,

        val slimeFlatDamage: Int,
        val slimeScaleDamage: Int,
        val slimeMulti: Float,
        val slimeBaseHealth: Int,
        val slimeLevelMulti: Int,

        val gladiatorAtkTier1DmgConst: Int,
        val gladiatorAtkTier1LvlKoef: Float,
        val gladiatorAtkTier1StrKoef: Float,
        val gladiatorAtkTIer1TileKoef: Float,

        val poisonArcherAtkTier1Const: Int,
        val poisonArcherAtkTier1LvlKoef: Float,
        val poisonArcherAtkTier1SpiritKoef: Float,
        val poisonArcherTier1TileKoef: Float,
        val poisonArcherDotKoef: Float,
        val poisonArcherTileMultiplier: Float,
        val poisonArcherPoisonDuration: Int
)
