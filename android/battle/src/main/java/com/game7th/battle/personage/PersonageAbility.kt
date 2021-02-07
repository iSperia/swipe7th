package com.game7th.battle.ability

import com.game7th.battle.DamageVector
import com.game7th.battle.SwipeBattle
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.personage.SwipePersonage
import com.game7th.battle.tilefield.TileField
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.tilefield.tile.TileStage
import com.game7th.battle.tilefield.tile.TileType
import com.game7th.battle.toViewModel
import kotlin.random.Random

abstract class PersonageAbility(
        val skin: String
) {
    abstract fun processEmit(isGuaranteed: Boolean, balance: SwipeBalance, tileField: TileField, personage: SwipePersonage): List<BattleEvent>

    abstract fun mergeTile(balance: SwipeBalance, tileField: TileField, tile1: SwipeTile, tile2: SwipeTile): SwipeTile?

    abstract suspend fun attemptUseAbility(battle: SwipeBattle, personage: SwipePersonage, tileField: TileField, position: Int, tile: SwipeTile): Boolean
}

class GladiatorStrike(balance: SwipeBalance): PersonageAbility("skill_tile_holy_strike") {

    override fun processEmit(isGuaranteed: Boolean, balance: SwipeBalance, tileField: TileField, personage: SwipePersonage): List<BattleEvent> {
        val result = mutableListOf<BattleEvent>()
        val isProc = isGuaranteed || Random.nextInt(100) < balance.baseSkillTileEmitProbability
        if (isProc) {
            val position: Int? = tileField.calculateFreePosition()
            position?.let { position ->
                val tile = SwipeTile(TileType.GLADIATOR_STRIKE, tileField.newTileId(), 1, TileStage.ABILITY_TIER_0)
                tileField.tiles[position] = tile
                result.add(BattleEvent.CreateTileEvent(tile.toViewModel(), position))
            }
        }
        return result
    }

    override fun mergeTile(balance: SwipeBalance, tileField: TileField, tile1: SwipeTile, tile2: SwipeTile): SwipeTile? {
        if (tile1.type != TileType.GLADIATOR_STRIKE || tile2.type != TileType.GLADIATOR_STRIKE) return null
        val stackSize = tile1.stackSize + tile2.stackSize
        val tier = when {
            stackSize > balance.defaultTier2Threshold -> TileStage.ABILITY_TIER_2
            stackSize > balance.defaultTier1Threshold -> TileStage.ABILITY_TIER_1
            else -> TileStage.ABILITY_TIER_0
        }
        return SwipeTile(TileType.GLADIATOR_STRIKE, tile2.id, stackSize, tier)
    }

    override suspend fun attemptUseAbility(battle: SwipeBattle, personage: SwipePersonage, tileField: TileField, position: Int, tile: SwipeTile): Boolean {
        if (tile.type == TileType.GLADIATOR_STRIKE) {
            when (tile.stage) {
                TileStage.ABILITY_TIER_1,
                TileStage.ABILITY_TIER_2 -> {
                    tileField.removeById(tile.id)
                    battle.notifyTileRemoved(tile.id)

                    val b = battle.balance
                    val damage = (b.gladiatorAtkTier1DmgConst
                            * (1 + b.gladiatorAtkTier1LvlKoef * personage.stats.level + b.gladiatorAtkTier1StrKoef * personage.stats.body)
                            * (1+tile.stackSize * b.gladiatorAtkTIer1TileKoef)).toInt()

                    battle.npcs.filter { it.value.stats.health > 0 }.forEach { (position, npc) ->
                        battle.processDamage(npc, personage, DamageVector(damage, 0, 0))
                        battle.notifyAoeProjectile("gladiator_wave", personage)
                    }
                    return true
                }
            }
        }
        return false
    }
}
