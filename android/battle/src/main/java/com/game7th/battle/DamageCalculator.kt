package com.game7th.battle

import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.unit.UnitStats
import kotlin.random.Random

object DamageCalculator {

    fun calculateDamage(
            balance: SwipeBalance,
            source: UnitStats,
            target: UnitStats,
            damage: DamageVector): DamageProcessResult {

        val evadeProb = target.evasion.toFloat() / (target.evasion + source.level)

        val evasionRoll = Random.nextDouble(1.0).toFloat()

        return if (evasionRoll < evadeProb) {
            DamageProcessResult(DamageVector(0,0,0), 0, 0, DamageProcessStatus.DAMAGE_EVADED)
        } else {
            val physReduction = (damage.physical.toFloat() * target.armor / (target.armor + damage.physical)).toInt()
            val magicReduction = (damage.magical.toFloat() * target.resist / (target.resist + damage.magical)).toInt()

            DamageProcessResult(damage.copy(
                    physical = damage.physical - physReduction,
                    magical = damage.magical - magicReduction
            ), physReduction, magicReduction, DamageProcessStatus.DAMAGE_DEALT)
        }
    }
}