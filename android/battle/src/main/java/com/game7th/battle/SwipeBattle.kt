package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.event.TileViewModel
import com.game7th.battle.npc.NpcPersonage
import com.game7th.battle.npc.SlimePersonage
import com.game7th.battle.npc.toViewModel
import com.game7th.battle.personage.Gladiator
import com.game7th.battle.personage.PersonageStats
import com.game7th.battle.personage.SwipePersonage
import com.game7th.battle.personage.toViewModel
import com.game7th.battle.tilefield.TileField
import com.game7th.battle.tilefield.TileFieldContext
import com.game7th.battle.tilefield.tile.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

class SwipeBattle(val balance: SwipeBalance) {

    private val coroutineContext = newSingleThreadContext("battle")

    private val swipes = Channel<Pair<Int, Int>>()

    val events = Channel<BattleEvent>()

    val tileFieldContext: TileFieldContext = object : TileFieldContext {
        override fun merge(tile: SwipeTile, swipeTile: SwipeTile): SwipeTile? {
            personages.flatMap { it.value.abilities }.forEach {
                it.mergeTile(balance, tileField, tile, swipeTile)?.let { return it }
            }
            return null
        }
    }

    val tileField = TileField(tileFieldContext)

    val personages = mutableMapOf<Int, SwipePersonage>()

    val npcs = mutableMapOf<Int, NpcPersonage>()

    var personageId = 0

    suspend fun initialize() = withContext(coroutineContext) {
        generateInitialPersonages()
        generateInitialTiles()
        processSwipes()
    }

    private suspend fun generateInitialTiles() = withContext(coroutineContext) {
        for (i in 0..5) {
            processTickEmit()
        }
    }

    private suspend fun processTickEmit() {
        //guaranteed
        personages.values.filter { it.stats.health > 0 }.let { personages ->
            if (personages.isNotEmpty()) {
                personages.random().let { personage ->
                    val abilityEvents = personage.abilities[0].processEmit(true, balance, tileField, personage)
                    abilityEvents.forEach { events.send(it) }
                }
            }
        }

        personages.values.forEach { personage ->
            personage.abilities.forEach { ability ->
                val abilityEvents = ability.processEmit(false, balance, tileField, personage)
                abilityEvents.forEach { events.send(it) }
            }
        }
    }

    private suspend fun generateInitialPersonages() = withContext(coroutineContext) {
        personages[1] = Gladiator(
                newPersonageId(),
                balance,
                PersonageStats(30, 10000, 10, 20, 3, 10, 10, 3, 10, 4))
        events.send(BattleEvent.CreatePersonageEvent(personages[1]!!.toViewModel(), 1))

        npcs[5] = SlimePersonage(newPersonageId(), PersonageStats(0, 30, 0, 0, 0, 0, 0, 0, 0, 1))
        events.send(BattleEvent.CreatePersonageEvent(npcs[5]!!.toViewModel(), 5))
    }

    private fun newPersonageId(): Int {
        return personageId.also { personageId++ }
    }

    private suspend fun processSwipes() = withContext(coroutineContext) {
        for (swipe in swipes) {
            val dx = swipe.first
            val dy = swipe.second
            var motionEvents = tileField.attemptSwipe(dx, dy)
            var hadAnyEvents = false
            while (motionEvents.isNotEmpty()) {
                hadAnyEvents = true
                events.send(BattleEvent.SwipeMotionEvent(motionEvents))
                //TODO: post process stacks stages etc.
                motionEvents = tileField.attemptSwipe(dx, dy)
            }
            if (hadAnyEvents) {
                processTickEmit()
                processTickNpc()
            }
        }
    }

    suspend fun processSwipe(dx: Int, dy: Int) {
        swipes.send(Pair(dx, dy))
    }

    suspend fun attemptActivateTile(id: Int) = withContext(coroutineContext) {
        tileField.tiles.entries.firstOrNull { it.value.id == id }?.let { (position, tile) ->
            personages.flatMap { it.value.abilities }.forEach { ability ->
                ability.processTileDoubleTap(this@SwipeBattle, tileField, position, tile)
            }
        }
    }

    suspend fun processTickNpc() {
        npcs.entries.forEach { (position, npc) ->
            npc.abilities.forEach { ability ->
                ability.tick(this, npc)
            }
        }
    }

    suspend fun notifyAttack(personage: NpcPersonage, target: SwipePersonage) {
        events.send(BattleEvent.PersonageAttackEvent(personage.toViewModel(), target.toViewModel()))
    }

    suspend fun processDamage(target: SwipePersonage, source: NpcPersonage, physical: Int, magical: Int, chaos: Int) {
        //TODO: apply shields and stuff
        target.stats.health -= physical + magical + chaos
        events.send(BattleEvent.PersonageDamageEvent(target.toViewModel(), physical, magical, chaos))
    }

    fun findClosestAlivePersonage(): SwipePersonage? {
        return personages.entries.filter { it.value.stats.health > 0 }.maxBy { it.key }?.value
    }

    suspend fun notifyTileRemoved(id: Int) {
        events.send(BattleEvent.RemoveTileEvent(id))
    }
}

fun SwipeTile.toViewModel(): TileViewModel {
    return TileViewModel(
            id,
            type.skin,
            stackSize,
            tileBackground(type),
            tileBackgroundIndex(stage),
            foreground()
    )
}

fun tileBackground(type: TileType) = if (type.background) "tile_bg" else null

fun tileBackgroundIndex(stage: TileStage) = when (stage) {
    TileStage.ABILITY_TIER_1, TileStage.ABILITY_TIER_2 -> 1
    else -> 0
}

fun SwipeTile.foreground(): String? = when {
    stage == TileStage.ABILITY_TIER_2 -> type.fraction.id
    else -> null
}