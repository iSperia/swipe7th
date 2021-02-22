package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.event.TileViewModel
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.personage.*
import com.game7th.battle.tilefield.TileField
import com.game7th.battle.tilefield.TileFieldMerger
import com.game7th.battle.tilefield.tile.*
import com.game7th.battle.unit.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SwipeBattle(val balance: SwipeBalance) {

    private val coroutineContext = newSingleThreadContext("battle")

    val events = Channel<BattleEvent>()

    private val tileFieldMerger: TileFieldMerger = object : TileFieldMerger {
        override suspend fun merge(tile1: SwipeTile, tile2: SwipeTile): SwipeTile? {
            val event = InternalBattleEvent.TileMergeEvent(this@SwipeBattle, tile1, tile2)
            propagateInternalEvent(event)
            return event.result
       }
    }

    lateinit var config: BattleConfig
    val tileField = TileField(tileFieldMerger)

    var personageId = 0
    var tick = 0

    var wave = 0

    val units = mutableListOf<BattleUnit>()

    private suspend fun propagateInternalEvent(event: InternalBattleEvent) {
        aliveUnits().forEach { battleUnit ->
            battleUnit.stats.abilities.flatMap { it.triggers }.forEach { trigger ->
                trigger.process(event, battleUnit)
            }
        }
    }

    private fun aliveUnits() = units.filter { it.stats.health.value > 0 }

    suspend fun initialize(config: BattleConfig) = withContext(coroutineContext) {
        this@SwipeBattle.config = config
        generateInitialPersonages(config)
        generateInitialTiles()
        propagateInternalEvent(InternalBattleEvent.BattleStartedEvent(this@SwipeBattle))
    }

    private suspend fun generateInitialTiles() = withContext(coroutineContext) {
        for (i in 0 until 6) {
            produceGuaranteedTile()
        }
    }

    private suspend fun produceGuaranteedTile() {
        val event = InternalBattleEvent.ProduceGuaranteedTileEvent(this@SwipeBattle)
        propagateInternalEvent(event)
    }

    private suspend fun processTick(preventTickers: Boolean) {
        produceGuaranteedTile()
        propagateInternalEvent(InternalBattleEvent.TickEvent(this, preventTickers))
    }

    private suspend fun generateInitialPersonages(config: BattleConfig) = withContext(coroutineContext) {
        config.personages.withIndex().forEach {
            val unitStats = UnitFactory.produce(it.value.name, balance, it.value.level)
            unitStats?.let { stats ->
                val unit = BattleUnit(newPersonageId(), it.index, stats, Team.LEFT)
                units.add(unit)
                events.send(BattleEvent.CreatePersonageEvent(unit.toViewModel(), it.index))
            }
        }

        generateNpcs(config)
        propagateInternalEvent(InternalBattleEvent.BattleStartedEvent(this@SwipeBattle))
    }

    private suspend fun generateNpcs(config: BattleConfig) {
        events.send(BattleEvent.NewWaveEvent(wave))
        config.waves[wave].withIndex().forEach {
            val unitStats = UnitFactory.produce(it.value.name, balance, it.value.level)
            val position = 4 - it.index
            unitStats?.let { stats ->
                val unit = BattleUnit(newPersonageId(), position, stats, Team.RIGHT)
                units.add(unit)
                events.send(BattleEvent.CreatePersonageEvent(unit.toViewModel(), position))
            }
        }
    }

    private fun newPersonageId(): Int {
        return personageId.also { personageId++ }
    }

    private suspend fun checkDeadPersonages() {
        val leftTeamCount = aliveUnits().count { it.team == Team.LEFT }
        val rightTeamCount = aliveUnits().count { it.team == Team.RIGHT }
        if (leftTeamCount == 0) {
            events.send(BattleEvent.DefeatEvent)
            events.close()
        } else if (rightTeamCount == 0) {
            if (wave < config.waves.size - 1) {
                wave++
                clearNpcs()
                units.removeAll { it.team == Team.RIGHT }
                generateNpcs(config)
                propagateInternalEvent(InternalBattleEvent.BattleStartedEvent(this@SwipeBattle))
            } else {
                events.send(BattleEvent.VictoryEvent)
                events.close()
            }
        }
    }

    private suspend fun clearNpcs() {
        units.filter { it.team == Team.RIGHT }.forEach {
            events.send(BattleEvent.RemovePersonageEvent(it.id))
        }
    }

    suspend fun processSwipe(dx: Int, dy: Int) = withContext(coroutineContext) {
        if (!events.isClosedForSend) {
            var motionEvents = tileField.attemptSwipe(dx, dy)
            var hadAnyEvents = false
            val eventCache = mutableListOf<BattleEvent>()
            while (motionEvents.isNotEmpty()) {
                hadAnyEvents = true
                eventCache.add(BattleEvent.SwipeMotionEvent(motionEvents))
                //TODO: post process stacks stages etc.
                motionEvents = tileField.attemptSwipe(dx, dy)
            }
            if (hadAnyEvents) {
                runBlocking {
                    eventCache.forEach { events.send(it) }
                }
                processTick(false)
                processTickUnits()
                tick++

                checkDeadPersonages()
            }
        }
    }

    suspend fun attemptActivateTile(id: Int) = withContext(coroutineContext) {
        if (!events.isClosedForSend) {
            tileField.tiles.values.firstOrNull { it.id == id }?.let { tile ->
                propagateInternalEvent(InternalBattleEvent.AbilityUseEvent(this@SwipeBattle, tile))
                checkDeadPersonages()
            }
        }
    }

    private suspend fun processTickUnits() {
        aliveUnits().forEach { unit ->
            processHeal(unit, unit.stats.regeneration.toFloat())
            var needPersonageUpdate = false

            unit.stats.ailments.forEach { ailment ->
                when (ailment.ailmentType) {
                    AilmentType.POISON -> {
                        processAilmentDamage(unit, DamageVector(0, 0, ailment.value.toInt()))
                        events.send(BattleEvent.ShowAilmentEffect(unit.id, "effect_poison"))
                    }
                    AilmentType.SCORCH -> {
                        processAilmentDamage(unit, DamageVector(0, ailment.value.toInt(), 0))
                        events.send(BattleEvent.ShowAilmentEffect(unit.id, "effect_scorch"))
                    }
                    AilmentType.STUN -> {
                        needPersonageUpdate = true
                        events.send(BattleEvent.ShowAilmentEffect(unit.id, "effect_stun"))
                    }
                }
                ailment.ticks--
            }

            unit.stats.ailments = unit.stats.ailments.filter { it.ticks > 0 }.toMutableList()
            if (needPersonageUpdate) events.send(BattleEvent.PersonageUpdateEvent(unit.toViewModel()))
        }
    }

    suspend fun notifyAttack(source: BattleUnit, targets: List<Pair<BattleUnit, DamageProcessResult>>, attackIndex: Int) {
        events.send(BattleEvent.PersonageAttackEvent(source.toViewModel(), targets.map { Pair(it.first.toViewModel(), it.second) }, attackIndex))
    }

    /*
     * TODO: abstract out npc/personages as battle units
     */

    suspend fun processDamage(target: BattleUnit, source: BattleUnit, damage: DamageVector): DamageProcessResult {
        val damage = DamageCalculator.calculateDamage(balance, source.stats, target.stats, damage)
        val totalDamage = damage.damage.totalDamage()
        if (totalDamage > 0 || damage.armorDeplete > 0 || damage.resistDeplete > 0) {
            target.stats.health.value = max(0, target.stats.health.value - totalDamage)
            target.stats.armor.value = max(0, target.stats.armor.value - damage.armorDeplete)
            target.stats.resist.value = max(0, target.stats.resist.value - damage.resistDeplete)
            events.send(BattleEvent.PersonageDamageEvent(target.toViewModel(), damage.damage.totalDamage()))
        } else if (damage.status == DamageProcessStatus.DAMAGE_EVADED) {
            events.send(BattleEvent.PersonageDamageEvadedEvent(target.toViewModel()))
        }
        return damage
    }

    suspend fun processAilmentDamage(target: BattleUnit, damage: DamageVector) {
        target.stats.health.value = max(0, target.stats.health.value - damage.totalDamage())
        events.send(BattleEvent.PersonageDamageEvent(target.toViewModel(), damage.totalDamage()))
    }

    suspend fun notifyTileRemoved(id: Int) {
        events.send(BattleEvent.RemoveTileEvent(id))
    }

    suspend fun applyPoison(target: BattleUnit, poisonTicks: Int, poisonDmg: Int) {
        val currentPoison = target.stats.ailments.firstOrNull { it.ailmentType == AilmentType.POISON }
        if (currentPoison != null) {
            currentPoison.value += poisonDmg
            currentPoison.ticks += poisonTicks
        } else {
            target.stats.ailments.add(UnitAilment(AilmentType.POISON, poisonTicks, poisonDmg.toFloat()))
        }
        events.send(BattleEvent.ShowAilmentEffect(target.id, "effect_poison"))
    }

    suspend fun applyScorch(target: BattleUnit, ticks: Int, dmg: Int) {
        target.stats.ailments.add(UnitAilment(AilmentType.SCORCH, ticks, dmg.toFloat()))
        events.send(BattleEvent.ShowAilmentEffect(target.id, "effect_scorch"))
    }

    suspend fun applyStun(target: BattleUnit, ticks: Int) {
        target.stats.ailments.add(UnitAilment(AilmentType.STUN, ticks, 0f))
        events.send(BattleEvent.ShowAilmentEffect(target.id, "effect_stun"))
        events.send(BattleEvent.PersonageUpdateEvent(target.toViewModel()))
    }

    suspend fun notifyEvent(event: BattleEvent) {
        events.send(event)

    }

    fun findClosestAliveEnemy(unit: BattleUnit): BattleUnit? {
        return aliveUnits().filter { it.team != unit.team }.minBy { abs(it.position - unit.position) }
    }

    fun aliveEnemies(unit: BattleUnit): List<BattleUnit> {
        return aliveUnits().filter { it.team != unit.team }
    }

    suspend fun processHeal(unit: BattleUnit, amount: Float) {
        val hp = amount.toInt()
        if (hp > 0 && unit.stats.health.notCapped()) {
            val healAmount = min(unit.stats.health.maxValue - unit.stats.health.value, hp)
            unit.stats.health.value += healAmount
            events.send(BattleEvent.PersonageUpdateEvent(unit.toViewModel()))
        }
    }
}

fun SwipeTile.toViewModel(): TileViewModel {
    return TileViewModel(
            id,
            type.skin,
            stackSize,
            type.maxStackSize
    )
}
