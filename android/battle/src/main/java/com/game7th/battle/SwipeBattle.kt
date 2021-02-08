package com.game7th.battle

import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.event.TileViewModel
import com.game7th.battle.npc.NpcFactory
import com.game7th.battle.npc.NpcPersonage
import com.game7th.battle.npc.SlimePersonage
import com.game7th.battle.npc.toViewModel
import com.game7th.battle.personage.*
import com.game7th.battle.tilefield.TileField
import com.game7th.battle.tilefield.TileFieldContext
import com.game7th.battle.tilefield.tile.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class SwipeBattle(val balance: SwipeBalance) {

    private val coroutineContext = newSingleThreadContext("battle")

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

    var tick = 0

    suspend fun initialize(config: BattleConfig) = withContext(coroutineContext) {
        generateInitialPersonages(config)
        generateInitialTiles()
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

        personages.values.filter { it.stats.health > 0 }.forEach { personage ->
            personage.abilities.forEach { ability ->
                val abilityEvents = ability.processEmit(false, balance, tileField, personage)
                abilityEvents.forEach { events.send(it) }
            }
        }
    }

    private suspend fun generateInitialPersonages(config: BattleConfig) = withContext(coroutineContext) {
        config.personages.withIndex().forEach {
            val personage = PersonageFactory.producePersonage(it.value, balance, newPersonageId())
            personage?.let { personage ->
                personages[it.index] = personage
                events.send(BattleEvent.CreatePersonageEvent(personages[it.index]!!.toViewModel(), it.index))
            }
        }

        config.npcs.withIndex().forEach {
            val npc = NpcFactory.produceNpc(it.value, balance, newPersonageId())
            val position = it.index + 5
            npc?.let { npc ->
                npcs[position] = npc
                events.send(BattleEvent.CreatePersonageEvent(npc.toViewModel(), position))
            }
        }
    }

    private fun newPersonageId(): Int {
        return personageId.also { personageId++ }
    }

    private suspend fun checkDeadPersonages() {
        if (personages.values.none { it.stats.health > 0 }) {
            //player has lost
            events.send(BattleEvent.DefeatEvent)
            events.close()
        } else if (npcs.values.none { it.stats.health > 0 }) {
            //player has won
            events.send(BattleEvent.VictoryEvent)
            events.close()
        }
    }

    suspend fun processSwipe(dx: Int, dy: Int) = withContext(coroutineContext) {
        if (!events.isClosedForSend) {
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
                processTickUnits()
                processTickNpc()
                tick++

                checkDeadPersonages()
            }
        }
    }

    suspend fun attemptActivateTile(id: Int) = withContext(coroutineContext) {
        if (!events.isClosedForSend) {
            tileField.tiles.entries.firstOrNull { it.value.id == id }?.let { (position, tile) ->
                personages.filter { it.value.stats.health > 0 }.entries.firstOrNull { (position, personage) ->
                    personage.abilities.firstOrNull { ability ->
                        ability.attemptUseAbility(this@SwipeBattle, personage, tileField, position, tile)
                    } != null
                }
            }
            checkDeadPersonages()
        }
    }

    private suspend fun processTickNpc() {
        npcs.entries.filter { it.value.stats.health > 0 }.forEach { (position, npc) ->
            npc.abilities.forEach { ability ->
                ability.tick(this, npc)
            }
        }
    }

    private suspend fun processTickUnits() {
        //calculate regeneration
        alivePersonages().forEach {
            if (it.value.stats.regeneration > 0 && it.value.stats.health < it.value.stats.maxHealth) {
                it.value.stats.health = min(it.value.stats.maxHealth, it.value.stats.health + it.value.stats.regeneration)
                events.send(BattleEvent.PersonageUpdateEvent(it.value.toViewModel()))
            }

        }
        aliveNpcs().forEach {
            if (it.value.stats.regeneration > 0 && it.value.stats.health < it.value.stats.maxHealth) {
                it.value.stats.health = min(it.value.stats.maxHealth, it.value.stats.health + it.value.stats.regeneration)
                events.send(BattleEvent.PersonageUpdateEvent(it.value.toViewModel()))
            }
            val personage = it.value
            if (personage.ailments.poisonDuration > 0) {
                personage.ailments.poisonDuration--
                processAilmentDamage(personage, DamageVector(0, 0, personage.ailments.poisonDamage))
                events.send(BattleEvent.ShowAilmentEffect(personage.id, "effect_poision"))
                if (personage.ailments.poisonDuration == 0) {
                    personage.ailments.poisonDamage = 0
                }
            }
        }
    }

    fun alivePersonages() = personages.entries.filter { it.value.stats.health > 0 }
    fun aliveNpcs() = npcs.entries.filter { it.value.stats.health > 0 }

    suspend fun notifyAttack(personage: NpcPersonage, target: SwipePersonage) {
        events.send(BattleEvent.PersonageAttackEvent(personage.toViewModel(), target.toViewModel()))
    }

    /*
     * TODO: abstract out npc/personages as battle units
     */

    suspend fun processDamage(target: SwipePersonage, source: NpcPersonage, damage: DamageVector) {
        val damage = DamageCalculator.calculateDamage(balance, source.stats, target.stats, damage)
        val totalDamage = damage.damage.totalDamage()
        if (totalDamage > 0 || damage.armorDeplete > 0 || damage.resistDeplete > 0) {
            target.stats.health = max(0, target.stats.health - totalDamage)
            target.stats.armor = max(0, target.stats.armor - damage.armorDeplete)
            target.stats.magicDefense = max(0, target.stats.magicDefense - damage.resistDeplete)
            events.send(BattleEvent.PersonageDamageEvent(target.toViewModel(), damage.damage.totalDamage()))
        } else if (damage.status == DamageProcessStatus.DAMAGE_EVADED) {
            events.send(BattleEvent.PersonageDamageEvadedEvent(target.toViewModel()))
        }
    }

    suspend fun processDamage(target: NpcPersonage, source: SwipePersonage, damage: DamageVector): DamageProcessResult {
        val damage = DamageCalculator.calculateDamage(balance, source.stats, target.stats, damage)
        val totalDamage = damage.damage.totalDamage()
        if (totalDamage > 0 || damage.armorDeplete > 0 || damage.resistDeplete > 0) {
            target.stats.health = max(0, target.stats.health - totalDamage)
            target.stats.armor = max(0, target.stats.armor - damage.armorDeplete)
            target.stats.magicDefense = max(0, target.stats.magicDefense - damage.resistDeplete)
            events.send(BattleEvent.PersonageDamageEvent(target.toViewModel(), damage.damage.totalDamage()))
        } else if (damage.status == DamageProcessStatus.DAMAGE_EVADED) {
            events.send(BattleEvent.PersonageDamageEvadedEvent(target.toViewModel()))
        }
        return damage
    }

    suspend fun processAilmentDamage(target: NpcPersonage, damage: DamageVector) {
        target.stats.health = max(0, target.stats.health - damage.totalDamage())
        events.send(BattleEvent.PersonageDamageEvent(target.toViewModel(), damage.totalDamage()))
    }

    fun findClosestAlivePersonage(): SwipePersonage? {
        return personages.entries.filter { it.value.stats.health > 0 }.maxBy { it.key }?.value
    }

    suspend fun notifyTileRemoved(id: Int) {
        events.send(BattleEvent.RemoveTileEvent(id))
    }

    suspend fun notifyAoeProjectile(skin: String, personage: SwipePersonage) {
        events.send(BattleEvent.ShowNpcAoeEffect(skin, personage.id))
    }

    suspend fun notifyTargetedProjectile(skin: String, personage: SwipePersonage, target: NpcPersonage) {
        events.send(BattleEvent.ShowProjectile(skin, personage.id, target.id))
    }

    suspend fun applyPoison(target: NpcPersonage, poisonTicks: Int, poisonDmg: Int) {
        target.ailments.poisonDamage += poisonDmg
        target.ailments.poisonDuration += poisonTicks
        events.send(BattleEvent.ShowAilmentEffect(target.id, "effect_poision"))
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