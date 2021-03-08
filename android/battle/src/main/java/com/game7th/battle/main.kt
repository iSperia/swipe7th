package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.unit.UnitConfig
import com.game7th.metagame.unit.UnitType
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.util.*
import kotlin.random.Random

data class EncounterData(
        val name: String,
        val waves: List<List<UnitConfig>>,
        val wrmin: Float
)

data class TestData(
        val personages: List<UnitType>,
        val encounters: List<EncounterData>
)

data class EmulationResult(
        val winrate: Float,
        val victoryLength: Float,
        val defeatLength: Float
)

val runsPerExperiment = 50
val zeroStats = PersonageAttributeStats(0,0,0)

fun main(args: Array<String>) {

    val gson = Gson()

    //read the balance
    val balance = gson.fromJson<SwipeBalance>(InputStreamReader(FileInputStream(args[0])), SwipeBalance::class.java)
    val config = gson.fromJson<TestData>(InputStreamReader(FileInputStream(args[1])), TestData::class.java)

    val writer = FileWriter("./r_${Date().toString().replace(" ", "_").replace(":","-")}.csv", false)
    config.personages.forEach { personageType ->
        writer.append("${personageType},${(1..100).joinToString { it.toString() }}\n")
        config.encounters.forEach { encounter ->
            writer.append(encounter.name).append(",")
            (1..100).forEach { personageLevel ->

                val totalStats = personageLevel * (personageLevel + 1) / 2 + 6
                val battleConfig = BattleConfig(
                        listOf(PersonageConfig(personageType, personageLevel, PersonageAttributeStats(
                                personageType.bodyWeight * totalStats / 6,
                                personageType.spiritWeight * totalStats / 6,
                                personageType.mindWeight * totalStats / 6), null)),
                        encounter.waves.map { wave -> wave.map { PersonageConfig(it.unitType, it.level + personageLevel, zeroStats, null) } }
                        )
                val result = runBlocking { emulateBattle(balance, battleConfig) }
                writer.append((result.winrate*100f).toInt().toString())
                if (personageLevel < 100) writer.append(",")
            }
            writer.append("\n")
        }
    }
    writer.flush()
    writer.close()
}

suspend fun emulateBattle(balance: SwipeBalance, config: BattleConfig): EmulationResult {
    val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    return GlobalScope.async(handler) {

        val results = (1..runsPerExperiment).map {
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

        EmulationResult(winrate, averageVictoryLength, averageDefeatLength)
    }.await()

}