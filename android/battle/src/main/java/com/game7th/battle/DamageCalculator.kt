package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.personage.PersonageStats
import com.game7th.battle.unit.UnitStats
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

object DamageCalculator {

    fun calculateDamage(
            balance: SwipeBalance,
            source: UnitStats,
            target: UnitStats,
            damage: DamageVector): DamageProcessResult {

        val evadeProb = (1 - (source.level * balance.stats.evasion.k / (source.level * balance.stats.evasion.k + target.evasion.toDouble().pow(balance.stats.evasion.p.toDouble())))).toFloat()
        println("EVADE PROB = $evadeProb")
        val evasionRoll = Random.nextDouble(1.0).toFloat()

        return if (evasionRoll < evadeProb) {
            DamageProcessResult(DamageVector(0,0,0), 0, 0, DamageProcessStatus.DAMAGE_EVADED)
        } else {
            val physArmorReduction = if (target.armor.value > 0) min(Random.nextInt(target.armor.value + 1), damage.physical) else 0
            val magicDefenseReduction = if (target.resist.value > 0) min(Random.nextInt(target.resist.value + 1), damage.magical) else 0

            DamageProcessResult(damage.copy(
                    physical = damage.physical - physArmorReduction,
                    magical = damage.magical - magicDefenseReduction
            ), physArmorReduction, magicDefenseReduction, DamageProcessStatus.DAMAGE_DEALT)
        }
    }
}