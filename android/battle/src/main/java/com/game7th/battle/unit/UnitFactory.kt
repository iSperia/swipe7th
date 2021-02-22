package com.game7th.battle.unit

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.ability
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.TileTemplate
import com.game7th.battle.tilefield.tile.TileNames
import com.game7th.metagame.unit.UnitType
import kotlin.math.exp

enum class UnitStatPriority {
    PRIMARY, SECONDARY, TERTIARY;

    fun selectStat(s1: Int, s2: Int, s3: Int) = when (ordinal) {
        0 -> s1
        1 -> s2
        else -> s3
    }
}

object UnitFactory {
    fun produce(type: UnitType, balance: SwipeBalance, level: Int): UnitStats? {
        return when (type) {
            UnitType.GLADIATOR -> producePersonage(balance, "personage_gladiator", level, UnitStatPriority.PRIMARY, UnitStatPriority.SECONDARY, UnitStatPriority.TERTIARY) {
                it.addAbility {
                    defaultEmitter {
                        skills.add(it.body to TileTemplate(TileNames.GLADIATOR_STRIKE, 3))
                        skills.add(it.spirit to TileTemplate(TileNames.GLADIATOR_WAVE, 4))
                        skills.add(it.spirit to TileTemplate(TileNames.GLADIATOR_DROP, 0))
                    }
                    defaultMerger { tileType = TileNames.GLADIATOR_STRIKE }
                    defaultMerger { tileType = TileNames.GLADIATOR_WAVE }

                }
            }
            UnitType.GREEN_SLIME -> {
                val hp = balance.slime.hp.let { it.f + it.m * level + exp(it.k * level) }.toInt()
                val slime = UnitStats(skin = "personage_slime", level = level, health = CappedStat(hp, hp))
                slime += ability {
                    ticker {
                        ticksToTrigger = 3
                        body = { battle, unit ->
                            val damage = (battle.balance.slime.damage.f + (unit.stats.level * (2 * unit.stats.level - 1)) * battle.balance.slime.damage.m).toInt()
                            if (damage > 0) {
                                val target = battle.findClosestAliveEnemy(unit)
                                target?.let { target ->
                                    battle.notifyAttack(unit, target)
                                    battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                                }
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
            level: Int,
            bodyPriority: UnitStatPriority,
            spiritPriority: UnitStatPriority,
            mindPriority: UnitStatPriority,
            processor: (UnitStats) -> Unit
    ): UnitStats {
        val totalStats = (level - 1) * (level) / 2
        val tertiaryStat = totalStats / 6 + 2
        val secondaryStat = totalStats / 3 + 4
        val primaryStat = totalStats - secondaryStat - tertiaryStat + 12

        val body = bodyPriority.selectStat(primaryStat, secondaryStat, tertiaryStat)
        val spirit = spiritPriority.selectStat(primaryStat, secondaryStat, tertiaryStat)
        val mind = mindPriority.selectStat(primaryStat, secondaryStat, tertiaryStat)

        val health = b.stats.baseHealth + b.stats.healthPerBody * body + b.stats.healthPerLevel * (level - 1)
        val armor = body * b.stats.armorPerBody
        val resist = mind * b.stats.resistPerMind

        return UnitStats(
                skin = skin,
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