package com.game7th.battle

import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.unit.UnitStats
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

object DamageCalculator {

    fun calculateDamage(
            balance: SwipeBalance,
            source: UnitStats,
            target: UnitStats,
            damage: DamageVector): DamageProcessResult {

        val evadeProb = sqrt(target.evasion.toFloat()) / (sqrt(target.evasion.toFloat()) + source.level)

        val evasionRoll = Random.nextDouble(1.0).toFloat()

        return if (evasionRoll < evadeProb) {
            DamageProcessResult(DamageVector(0,0,0), 0, 0, 0, DamageProcessStatus.DAMAGE_EVADED)
        } else {

            val magicReduction = min(damage.magical.toFloat(), target.resist * 2f).toInt()
            val resistAfterMr = (target.resist - magicReduction / 2f).toInt()
            val resistPhysReduction = min(damage.physical.toFloat() / 2f, resistAfterMr.toFloat()).toInt()
            val damageAfterResistPhysReduction = damage.physical.toFloat() - resistPhysReduction

            val armorReduction = (damageAfterResistPhysReduction * target.armor / (target.armor + damageAfterResistPhysReduction)).toInt()

            DamageProcessResult(damage.copy(
                    physical = damage.physical - armorReduction - resistPhysReduction,
                    magical = damage.magical - magicReduction
            ), armorReduction + resistPhysReduction, magicReduction, magicReduction / 2 + resistPhysReduction, DamageProcessStatus.DAMAGE_DEALT)
        }
    }
}