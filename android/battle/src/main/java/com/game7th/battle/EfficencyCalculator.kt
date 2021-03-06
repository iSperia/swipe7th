package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import kotlin.random.Random

object EfficencyCalculator {

    fun calculateStackSize(
            balance: SwipeBalance,
            wisdom: Int,
            stackSize: Int,
            combo: Int): Int {

        val chance = (wisdom + combo) / 100f
        return (1..stackSize).count {
            val roll = Random.nextFloat()
            roll <= chance
        }
    }
}