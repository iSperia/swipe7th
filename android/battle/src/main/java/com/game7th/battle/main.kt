package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.InputStreamReader
import kotlin.random.Random

fun main(args: Array<String>) {

    val gson = Gson()

    //read the balance
    val balance = gson.fromJson<SwipeBalance>(InputStreamReader(FileInputStream(args[0])), SwipeBalance::class.java)
    val config = gson.fromJson<BattleConfig>(InputStreamReader(FileInputStream(args[1])), BattleConfig::class.java)

    runBlocking { emulateBattle(balance, config) }
}

suspend fun emulateBattle(balance: SwipeBalance, config: BattleConfig) {
    val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    GlobalScope.async(handler) {

        val results = (0..999).map {
            async {
                val battle = SwipeBattle(balance)

                var job: Job? = null

                val victory: Deferred<Boolean> = async {
                    var result = false
                    for (event in battle.events) {
                        if (event is BattleEvent.VictoryEvent) {
                            result = true
                        } else if (event is BattleEvent.DefeatEvent) {
                            result = false
                        }
                    }
                    job?.cancel()
                    result
                }

                async {
                    battle.initialize(config)
                }

                job = async(handler) {
                    while (true) {
                        val r = Random.nextInt(4)
                        val dx = if (r == 0) 1 else if (r == 1) -1 else 0
                        val dy = if (r == 2) 1 else if (r == 3) -1 else 0
                        battle.processSwipe(dx, dy)
                    }
                }

                val isVictory = victory.await()
                Pair(isVictory, battle.tick)
            }
        }

        val awaited = results.map { it.await() }
        val average = awaited.sumBy { it.second } / awaited.size
        val victories = awaited.count { it.first }
        val winrate = victories.toFloat() / awaited.size
        val averageVictoryLength = awaited.filter { it.first }.sumBy { it.second } / victories.toFloat()
        val averageDefeatLength = awaited.filter { !it.first }.sumBy { it.second } / (awaited.size - victories).toFloat()
        println("total games played : ${awaited.size}. winrate = ${winrate*100} average length = $average VICTORIES: $averageVictoryLength LOSES: $averageDefeatLength")

    }.await()

}