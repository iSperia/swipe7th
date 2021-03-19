package com.game7th.battle

import com.game7th.battle.dto.BattleConfig
import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.dto.BattleEvent
import com.game7th.battle.dto.TileViewModel
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.TileField
import com.game7th.battle.tilefield.TileFieldEvent
import com.game7th.battle.tilefield.TileFieldMerger
import com.game7th.battle.tilefield.tile.*
import com.game7th.battle.unit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SwipeBattle(
        val balance: SwipeBalance,
        val inputFlow: Flow<Pair<Int, Int>>
) {

    private val coroutineContext = newSingleThreadContext("battle")

    val events = MutableSharedFlow<BattleEvent>()

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

    var combo = 0

    var wave = 0

    val units = mutableListOf<BattleUnit>()

    var gameEnded = false

    suspend fun propagateInternalEvent(event: InternalBattleEvent) {
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

        inputFlow.collect { (dx, dy) ->
            processSwipe(dx, dy)
        }
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
        checkAutoTickTiles()
        produceGuaranteedTile()
        propagateInternalEvent(InternalBattleEvent.TickEvent(this, preventTickers))
    }

    private suspend fun checkAutoTickTiles() {
        tileField.tiles.forEach {
            if (it.value.autoDecrement) {
                if (it.value.stackSize > 1) {
                    val newTile = it.value.copy(stackSize = it.value.stackSize - 1)
                    tileField.tiles[it.key] = newTile
                    notifyTileUpdated(newTile)
                } else {
                    tileField.tiles.remove(it.key)
                    notifyTileRemoved(it.value.id)
                }
            }
        }
    }

    private suspend fun generateInitialPersonages(config: BattleConfig) = withContext(coroutineContext) {
        config.personages.withIndex().forEach {
            val unitStats = UnitFactory.produce(it.value.name, balance, it.value.level, it.value.unitStats)
            unitStats?.let { stats ->
                val unit = BattleUnit(newPersonageId(), it.index, stats, Team.LEFT)
                units.add(unit)
                notifyEvent(BattleEvent.CreatePersonageEvent(unit.toViewModel(), it.index))
            }
        }

        generateNpcs(config)
        propagateInternalEvent(InternalBattleEvent.BattleStartedEvent(this@SwipeBattle))
    }

    private suspend fun generateNpcs(config: BattleConfig) {
        notifyEvent(BattleEvent.NewWaveEvent(wave))
        config.waves[wave].withIndex().forEach {
            val unitStats = UnitFactory.produce(it.value.name, balance, it.value.level, null)
            val position = 4 - it.index
            unitStats?.let { stats ->
                val unit = BattleUnit(newPersonageId(), position, stats, Team.RIGHT)
                units.add(unit)
                notifyEvent(BattleEvent.CreatePersonageEvent(unit.toViewModel(), position))
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
            gameEnded = true
            notifyEvent(BattleEvent.DefeatEvent)
        } else if (rightTeamCount == 0) {
            if (wave < config.waves.size - 1) {
                println("!!! Increment wave to ${wave + 1} ${Thread.currentThread()}")
                wave++
                clearNpcs()
                units.removeAll { it.team == Team.RIGHT }
                generateNpcs(config)
                propagateInternalEvent(InternalBattleEvent.BattleStartedEvent(this@SwipeBattle))
            } else {
                gameEnded = true
                notifyEvent(BattleEvent.VictoryEvent)
            }
        }
    }

    private suspend fun clearNpcs() {
        units.filter { it.team == Team.RIGHT }.forEach {
            notifyEvent(BattleEvent.RemovePersonageEvent(it.id))
        }
        units.removeAll { it.team == Team.RIGHT }
    }

    private suspend fun processSwipe(dx: Int, dy: Int) = withContext(coroutineContext) {
        if (!gameEnded) {
            var motionEvents = tileField.attemptSwipe(dx, dy, true)
            var hadAnyEvents = false
            val eventCache = mutableListOf<BattleEvent>()
            while (motionEvents.isNotEmpty()) {
                hadAnyEvents = true
                eventCache.add(BattleEvent.SwipeMotionEvent(motionEvents))
                motionEvents = tileField.attemptSwipe(dx, dy, true)
            }
            val totalMerges = eventCache.sumBy { be ->
                (be as? BattleEvent.SwipeMotionEvent)?.events?.count { it is TileFieldEvent.MergeTileEvent } ?: 0
            }
            if (hadAnyEvents) {
                combo = if (totalMerges > 0) combo + totalMerges else 0
                notifyEvent(BattleEvent.ComboUpdateEvent(combo))

                eventCache.forEach {
                    notifyEvent(it)
                }

                processTick(false)
                processTickUnits()
                tick++

                checkDeadPersonages()

                if (tileField.attemptSwipe(-1, 0, false).isEmpty() &&
                        tileField.attemptSwipe(1, 0, false).isEmpty() &&
                        tileField.attemptSwipe(0, -1, false).isEmpty() &&
                        tileField.attemptSwipe(0, 1, false).isEmpty()) {
                            gameEnded = true
                    //we have no moves
                    notifyEvent(BattleEvent.DefeatEvent)
                }
            }
        }
    }

    private suspend fun processTickUnits() {
        aliveUnits().forEach { unit ->
            processHeal(unit, unit.stats.regeneration)
            var needPersonageUpdate = false

            unit.stats.ailments.forEach { ailment ->
                when (ailment.ailmentType) {
                    AilmentType.POISON -> {
                        processAilmentDamage(unit, DamageVector(0, 0, ailment.value.toInt()))
                        notifyEvent(BattleEvent.ShowAilmentEffect(unit.id, "ailment_poison"))
                    }
                    AilmentType.SCORCH -> {
                        processAilmentDamage(unit, DamageVector(0, ailment.value.toInt(), 0))
                        notifyEvent(BattleEvent.ShowAilmentEffect(unit.id, "effect_scorch"))
                    }
                    AilmentType.STUN -> {
                        needPersonageUpdate = true
                        notifyEvent(BattleEvent.ShowAilmentEffect(unit.id, "ailment_paralize"))
                    }
                }
                ailment.ticks--
            }

            unit.stats.ailments = unit.stats.ailments.filter { it.ticks > 0 }.toMutableList()
            if (needPersonageUpdate) notifyEvent(BattleEvent.PersonageUpdateEvent(unit.toViewModel()))
        }
    }

    suspend fun notifyAttack(source: BattleUnit, targets: List<BattleUnit>, attackIndex: Int) {
        notifyEvent(BattleEvent.PersonageAttackEvent(source.toViewModel(), targets.map { it.toViewModel() }, attackIndex))
    }

    suspend fun processDamage(target: BattleUnit, source: BattleUnit, damage: DamageVector): DamageProcessResult {
        val damage = DamageCalculator.calculateDamage(balance, source.stats, target.stats, damage)
        val totalDamage = damage.damage.totalDamage()
        if (totalDamage > 0 || damage.armorDeplete > 0 || damage.resistDeplete > 0) {
            target.stats.health.value = max(0, target.stats.health.value - totalDamage)

            notifyEvent(BattleEvent.PersonageDamageEvent(target.toViewModel(), damage.damage.totalDamage()))
            if (target.stats.health.value <= 0) {
                notifyEvent(BattleEvent.PersonageDeadEvent(target.toViewModel()))
            }
        } else if (damage.status == DamageProcessStatus.DAMAGE_EVADED) {
            notifyEvent(BattleEvent.ShowAilmentEffect(target.id, "ailment_evade"))
        }
        return damage
    }

    suspend fun processAilmentDamage(target: BattleUnit, damage: DamageVector) {
        target.stats.health.value = max(0, target.stats.health.value - damage.totalDamage())
        notifyEvent(BattleEvent.PersonageDamageEvent(target.toViewModel(), damage.totalDamage()))
        if (target.stats.health.value <= 0) {
            notifyEvent(BattleEvent.PersonageDeadEvent(target.toViewModel()))
        }
    }

    suspend fun notifyTileRemoved(id: Int) {
        notifyEvent(BattleEvent.RemoveTileEvent(id))
    }

    suspend fun notifyTileUpdated(tile: SwipeTile) {
        notifyEvent(BattleEvent.UpdateTileEvent(tile.id, tile.toViewModel()))
    }

    suspend fun applyPoison(target: BattleUnit, poisonTicks: Int, poisonDmg: Int) {
        target.stats.ailments.add(UnitAilment(AilmentType.POISON, poisonTicks, poisonDmg.toFloat()))
        notifyEvent(BattleEvent.ShowAilmentEffect(target.id, "ailment_poison"))
    }

    suspend fun applyScorch(target: BattleUnit, ticks: Int, dmg: Int) {
        target.stats.ailments.add(UnitAilment(AilmentType.SCORCH, ticks, dmg.toFloat()))
        notifyEvent(BattleEvent.ShowAilmentEffect(target.id, "effect_scorch"))
    }

    suspend fun applyStun(target: BattleUnit, ticks: Int) {
        val existingStun = target.stats.ailments.firstOrNull { it.ailmentType == AilmentType.STUN }
        if (existingStun != null) {
            existingStun.ticks += ticks
        } else {
            target.stats.ailments.add(UnitAilment(AilmentType.STUN, ticks, 0f))
        }
        notifyEvent(BattleEvent.ShowAilmentEffect(target.id, "ailment_paralize"))
        notifyEvent(BattleEvent.PersonageUpdateEvent(target.toViewModel()))
    }

    suspend fun notifyEvent(event: BattleEvent) {
        println("!!! $event")
        events.emit(event)
    }

    fun findClosestAliveEnemy(unit: BattleUnit): BattleUnit? {
        return aliveUnits().filter { it.team != unit.team }.minBy { abs(it.position - unit.position) }
    }

    fun aliveEnemies(unit: BattleUnit): List<BattleUnit> {
        return aliveUnits().filter { it.team != unit.team }
    }

    suspend fun processHeal(unit: BattleUnit, amount: Int) {
        if (amount > 0 && unit.stats.health.notCapped()) {
            val healAmount = min(unit.stats.health.maxValue - unit.stats.health.value, amount)
            unit.stats.health.value += healAmount
            notifyEvent(BattleEvent.PersonageUpdateEvent(unit.toViewModel()))
            notifyEvent(BattleEvent.PersonageHealEvent(unit.toViewModel(), healAmount))
        }
    }

    suspend fun notifyPersonageUpdated(unit: BattleUnit) {
        notifyEvent(BattleEvent.PersonageUpdateEvent(unit.toViewModel()))
    }

    fun calculateFreeNpcPosition(): Int {
        return (4 downTo 1).firstOrNull { index -> aliveUnits().firstOrNull { it.position == index } == null}?.let { it } ?: -1
    }

    fun aliveAllies(unit: BattleUnit): List<BattleUnit> {
        return units.filter { it.isAlive() && it.team == unit.team && it.id != unit.id }
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
