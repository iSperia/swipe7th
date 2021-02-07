package com.game7th.balance_tool

import com.game7th.battle.balance.SwipeBalance
import com.google.gson.Gson
import java.io.FileInputStream
import java.io.InputStreamReader

fun main(args: List<String>) {

    val gson = Gson()

    //read the balance
    val balance = gson.fromJson<SwipeBalance>(InputStreamReader(FileInputStream(args[0])), SwipeBalance::class.java)
}