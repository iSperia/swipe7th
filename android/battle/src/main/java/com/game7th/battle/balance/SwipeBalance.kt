package com.game7th.battle.balance

import kotlin.math.exp
import kotlin.random.Random

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

data class HpBalance(
    val f: Int,
    val k: Float,
    val m: Int
)

/**
 * DMG = F + (LVL-1) * S
 */
data class LinearDamageBalance(
        val f: Float,     //flat
        val m: Float    //scale
)

data class PolinomBalance(
        val k: Float,
        val a: Float,
        val f: Float,
        val c: Float
) {
    fun calculate(x: Int) = (k * Math.pow(x.toDouble(), a.toDouble()).toFloat() + f * x + c).toInt()
}

/**
 * DMG = F + LVL * ( 2 * LVL - 1 ) * M
 */
data class SquareDamageBalance(
        val f: Float,
        val m: Float
)

/**
 * DMG = O * ( EXP(K*LVL) + F + M * LVL )
 * O - overall koef
 */
data class ExponentialDamageBalance(
        val k: Float,
        val f: Float,
        val m: Float,
        val o: Float
) {
    fun calculate(level: Int): Int {
        return (o * (exp(k * level) + f + m * level)).toInt()
    }
}

data class RangeBalance(
        val min: Int,
        val max: Int
) {
    fun calculate() = Random.nextInt(min, max + 1)
}

data class SlimeBalance(
    val hp: PolinomBalance,
    val damage: PolinomBalance,
    val a2l: Int
)

data class RedSlimeBalance(
        val hp: PolinomBalance,
        val damage: PolinomBalance,
        val a2l: Int
)

data class MotherSlimeBalance(
        val hp: PolinomBalance,
        val damage: PolinomBalance,
        val a2l: Int,
        val regen: Float
)

data class FatherSlimeBalance(
        val hp: PolinomBalance,
        val damage: PolinomBalance,
        val a2: PolinomBalance
)

data class CitadelWarlockBalance(
        val damage: ExponentialDamageBalance,
        val hp: HpBalance,
        val healPercentage: Float
)

data class FireElementBalance(
        val damage: ExponentialDamageBalance,
        val hp: HpBalance,
        val scorch_k: Float,
        val scorch_duration: Int
)

data class EarthElementBalance(
        val damage: ExponentialDamageBalance,
        val hp: HpBalance,
        val stun_duration: RangeBalance
)

data class BossBloodKnightBalance(
        val damage: ExponentialDamageBalance,
        val hp: HpBalance,
        val heal_k: Float
)

data class GladiatorBalance(
        val c: Int,
        val k: Float,
        val a1n: Float,
        val a2n: Float,
        val a3n: Float,
        val t1: Int,
        val t2: Int,
        val a3p: Float
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
        val red_slime: RedSlimeBalance,
        val mother_slime: MotherSlimeBalance,
        val father_slime: FatherSlimeBalance,
        val citadel_warlock: CitadelWarlockBalance,
        val fire_element: FireElementBalance,
        val earth_element: EarthElementBalance,
        val boss_blood_knight: BossBloodKnightBalance,
        val gladiator: GladiatorBalance,
        val poison_archer: PoisonArcherBalance,
        val gunner: GunnerBalance
)
