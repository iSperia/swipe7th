package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import kotlin.random.Random

object EfficencyCalculator {

    fun calculateStackSize(balance: SwipeBalance, level: Int, efficency: Int): Int {
        val efficencyValue = balance.personageEfficencyStackKoef * efficency.toFloat() / level
        val extraProb = efficencyValue % 1
        return efficencyValue.toInt() + if (Random.nextFloat() < extraProb) 1 else 0
    }
}