package com.game7th.battle.unit

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.action.AttackAction
import com.game7th.battle.action.RegenerateParametrizedAmountAction
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.event.TileTemplate
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.tilefield.tile.TileNames
import com.game7th.battle.toViewModel
import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.unit.UnitType

enum class UnitStatPriority {
    PRIMARY, SECONDARY, TERTIARY;

    fun selectStat(s1: Int, s2: Int, s3: Int) = when (ordinal) {
        0 -> s1
        1 -> s2
        else -> s3
    }
}

object UnitFactory {
    fun produce(type: UnitType, balance: SwipeBalance, level: Int, stats: PersonageAttributeStats): UnitStats? {
        return when (type) {
            UnitType.GLADIATOR -> producePersonage(balance, "personage_gladiator", "portrait_gladiator", level, stats) {
                val strikeTemplate = TileTemplate(TileNames.GLADIATOR_STRIKE, balance.gladiator.t1)
                val waveTemplate = TileTemplate(TileNames.GLADIATOR_WAVE, balance.gladiator.t2)
                val dropTemplate = TileTemplate(TileNames.GLADIATOR_DROP, 0)
                it.addAbility {
                    defaultEmitter { skills.addAll(listOf(it.body to strikeTemplate, it.spirit to waveTemplate, it.mind to dropTemplate)) }
                    defaultMerger { tileType = strikeTemplate.skin }
                    defaultMerger { tileType = waveTemplate.skin }
                    consume {
                        template = waveTemplate
                        action = AttackAction().apply {
                            attackIndex = 1
                            target = { battle, unit -> battle.aliveEnemies(unit)}
                            damage = { battle, unit, target, ss, ms -> (balance.gladiator.a2n * unit.stats.spirit * ss / ms).toInt().let { DamageVector(it, 0, 0) }}
                        }
                    }
                    consume {
                        template = strikeTemplate
                        action = AttackAction().apply {
                            target = { battle, unit -> battle.findClosestAliveEnemy(unit)?.let { listOf(it) } ?: emptyList() }
                            damage = { battle, unit, target, ss, ms -> (balance.gladiator.a1n * unit.stats.body * ss / ms).toInt().let { DamageVector(it, 0, 0) } }
                        }
                    }
                    distancedConsumeOnAttackDamage {
                        range = 1
                        tileSkins.addAll(listOf(strikeTemplate.skin, waveTemplate.skin))
                        sourceSkin = dropTemplate.skin
                        action = RegenerateParametrizedAmountAction(balance.gladiator.a3n)
                    }
                }
            }
            UnitType.GREEN_SLIME -> {
                val hp = balance.slime.hp.calculate(level)
                val slime = UnitStats(skin = "personage_slime", portrait = "portrait_slime", level = level, health = CappedStat(hp, hp))
                slime += ability {
                    ticker {
                        bodies[TickerEntry(3, 3, "attack")] = { battle, unit ->
                            val damage = balance.slime.damage.calculate(unit.stats.level)
                            if (damage > 0) {
                                val target = battle.findClosestAliveEnemy(unit)
                                target?.let { target ->
                                    val damageResult = battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                                    battle.notifyAttack(unit, listOf(Pair(target, damageResult)), 0)
                                }
                            }
                        }
                        bodies[TickerEntry(1, 2, "impact")] = { battle, unit ->
                            battle.tileField.calculateFreePosition()?.let { position ->
                                val tile = SwipeTile(TileTemplate("slime_splash", balance.slime.a2l), battle.tileField.newTileId(), balance.slime.a2l, true)
                                battle.tileField.tiles[position] = tile
                                battle.notifyEvent(BattleEvent.CreateTileEvent(tile.toViewModel(), position))
                                battle.notifyAttack(unit, emptyList(), 0)
                            }
                        }
                    }
                }
                slime
            }
            else -> null
        }
    }

    private fun findIndex(roll: Int, weights: List<Int>): Int {
        var sum = 0
        weights.withIndex().forEach {
            if (sum + it.value > roll) {
                return it.index
            } else {
                sum += it.value
            }
        }
        return weights.size - 1
    }

    private fun producePersonage(
            b: SwipeBalance,
            skin: String,
            portrait: String,
            level: Int,
            stats: PersonageAttributeStats,
            processor: (UnitStats) -> Unit
    ): UnitStats {
        val body = stats.body
        val spirit = stats.spirit
        val mind = stats.mind

        val health = b.stats.baseHealth + b.stats.healthPerBody * body + b.stats.healthPerLevel * level
        val armor = body * b.stats.armorPerBody
        val resist = mind * b.stats.resistPerMind

        return UnitStats(
                skin = skin,
                portrait = portrait,
                level = level,
                body = body,
                spirit = spirit,
                mind = mind,
                health = CappedStat(health, health),
                armor = CappedStat(armor, armor),
                resist = CappedStat(resist, resist),
                regeneration = (b.stats.regenerationPerSpirit * spirit).toInt(),
                evasion = b.stats.evasionPerSpirit * spirit,
                wisdom = mind
        ).apply { processor(this) }
    }
}