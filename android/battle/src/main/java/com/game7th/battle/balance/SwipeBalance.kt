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

data class SlimeBalance(
    val flatDmg: Int,
    val scaleDmg: Float,
    val m: Float,
    val baseHp: Int,
    val levelHp: Int
)

data class CitadelWarlockBalance(
        val flatDmg: Int,
        val scaleDmg: Float,
        val m: Float,
        val baseHp: Int,
        val levelHp: Int,
        val healPercentage: Float
)


data class GladiatorBalance(
        val c: Int,
        val k: Float,
        val str_k: Float,
        val tile_k: Float
)

data class PoisonArcherBalance(
        val c: Int,
        val k: Float,
        val spi_k: Float,
        val tile_k: Float,
        val dot_k: Float,
        val tile_m: Float,
        val d: Int
)

data class GunnerBalance(
    val c: Int,
    val k: Float,
    val mind_k: Float,
    val tile_k: Float,
    val bullets: Int
)

data class SwipeBalance(
        val version: String,

        val stats: StatBalance,
        val slime: SlimeBalance,
        val citadel_warlock: CitadelWarlockBalance,
        val gladiator: GladiatorBalance,
        val poison_archer: PoisonArcherBalance,
        val gunner: GunnerBalance
)
