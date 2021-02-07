package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.personage.PersonageStats
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

object DamageCalculator {

    fun calculateDamage(
            balance: SwipeBalance,
            source: PersonageStats,
            target: PersonageStats,
            damage: DamageVector): DamageProcessResult {

        val evadeProb = (1 - (source.level * balance.statsEvasionK / (source.level * balance.statsEvasionK + target.evasion.toDouble().pow(balance.statsEvasionP.toDouble())))).toFloat()
        println("EVADE PROB = $evadeProb")
        val evasionRoll = Random.nextDouble(1.0).toFloat()

        return if (evasionRoll < evadeProb) {
            DamageProcessResult(DamageVector(0,0,0), 0, 0, DamageProcessStatus.DAMAGE_EVADED)
        } else {
            val physArmorReduction = if (target.armor > 0) min(Random.nextInt(target.armor + 1), damage.physical) else 0

            DamageProcessResult(damage.copy(
                    physical = damage.physical - physArmorReduction
            ), physArmorReduction, 0, DamageProcessStatus.DAMAGE_DEALT)
        }
    }
}