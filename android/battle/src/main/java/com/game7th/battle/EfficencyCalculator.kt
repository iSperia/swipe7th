package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import kotlin.random.Random

object EfficencyCalculator {

    fun calculateStackSize(balance: SwipeBalance, level: Int, efficency: Int): Int {
        val rollProb = efficency.toFloat() / (level + efficency)
        val rollCount = 1 + efficency / balance.stats.wizdomMultiplier
        return (1..rollCount).count { Random.nextFloat() < rollProb }
    }
}