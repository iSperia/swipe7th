package com.game7th.battle.unit

import com.game7th.battle.DamageProcessStatus
import com.game7th.battle.DamageVector
import com.game7th.battle.ability.ability
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.tilefield.tile.TileType
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

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
            UnitType.GLADIATOR -> producePersonage(balance, "p_gladiator", level, UnitStatPriority.PRIMARY, UnitStatPriority.SECONDARY, UnitStatPriority.TERTIARY) {
                it.addAbility {
                    defaultEmitter { tileType = TileType.GLADIATOR_STRIKE; tier1 = 5; tier2 = 10 }
                    defaultMerger { tileType = TileType.GLADIATOR_STRIKE }
                    consumeExecute {
                        tileType = TileType.GLADIATOR_STRIKE
                        body = { battle, tile, unit: BattleUnit ->

                            val b = battle.balance
                            val damage = (b.gladiator.c
                                    * (1 + b.gladiator.k * unit.stats.level + b.gladiator.str_k * unit.stats.body)
                                    * (1 + tile.stackSize * b.gladiator.tile_k)).toInt()

                            battle.aliveEnemies(unit).forEach { enemy ->
                                battle.processDamage(enemy, unit, DamageVector(damage, 0, 0))
                                battle.notifyAoeProjectile("gladiator_wave", unit, 1)
                            }
                        }
                    }
                }
            }
            UnitType.POISON_ARCHER -> producePersonage(balance, "personage_ranger", level, UnitStatPriority.TERTIARY, UnitStatPriority.PRIMARY, UnitStatPriority.SECONDARY) {
                it.addAbility {
                    defaultEmitter { tileType = TileType.POISON_ARROW; tier1 = 5; tier2 = 10 }
                    defaultMerger { tileType = TileType.POISON_ARROW }
                    consumeExecute {
                        tileType = TileType.POISON_ARROW
                        body = { battle, tile, unit ->

                            val b = battle.balance
                            val target = battle.aliveEnemies(unit).random()
                            val damage = (b.poison_archer.c
                                    * (1 + b.poison_archer.k * unit.stats.level + b.poison_archer.spi_k * unit.stats.spirit)
                                    * (1 + tile.stackSize * b.poison_archer.tile_k)).toInt()
                            val result = battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                            battle.notifyTargetedProjectile("projectile_arrow", unit, target)

                            if (result.status != DamageProcessStatus.DAMAGE_EVADED) {
                                val poisonTicks = b.poison_archer.d
                                val poisonDmg = (b.poison_archer.dot_k * damage * sqrt(b.poison_archer.tile_m * tile.stackSize)).toInt()
                                battle.applyPoison(target, poisonTicks, poisonDmg)
                            }
                        }
                    }
                }
            }
            UnitType.MACHINE_GUNNER -> producePersonage(balance, "personage_gunner", level, UnitStatPriority.SECONDARY, UnitStatPriority.TERTIARY, UnitStatPriority.PRIMARY) {
                it.addAbility {
                    defaultEmitter { tileType = TileType.MACHINE_GUN; tier1 = 5; tier2 = 10 }
                    defaultMerger { tileType = TileType.MACHINE_GUN }
                    consumeExecute {
                        tileType = TileType.MACHINE_GUN
                        body = { battle, tile, unit ->
                            val b = battle.balance
                            val bulletDamage = (b.gunner.c
                                    * (1 + b.gunner.k * unit.stats.level + b.gunner.mind_k * unit.stats.mind)
                                    * (1 + tile.stackSize * b.gunner.tile_k)).toInt()
                            val bulletAmount = b.gunner.bullets
                            val enemies = battle.aliveEnemies(unit).sortedByDescending { abs(unit.position - it.position) }
                            val weights = enemies.withIndex().map { it.index + 1 }
                            val totalWeight = weights.sumBy { it }
                            (0 until bulletAmount)
                                    .map { Random.nextInt(totalWeight) }
                                    .map { findIndex(it, weights) }
                                    .map { enemies[it] }
                                    .forEach { enemy ->
                                        battle.processDamage(enemy, unit, DamageVector(bulletDamage, 0, 0))
                                        battle.notifyTargetedProjectile("projectile_bullet", unit, enemy)
                                    }
                        }
                    }
                }
            }
            UnitType.GREEN_SLIME -> {
                val hp = balance.slime.hp.let { it.f + it.m * level + exp(it.k * level) }.toInt()
                val slime = UnitStats(skin = "personage_slime", level = level, health = CappedStat(hp, hp))
                slime += ability {
                    ticker {
                        ticksToTrigger = 3
                        body = { battle, unit ->
                            val damage = (battle.balance.slime.flatDmg + (unit.stats.level * (2 * unit.stats.level - 1)) * battle.balance.slime.m).toInt()
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
            UnitType.CITADEL_WARLOCK -> {
                val hp = balance.citadel_warlock.hp.let { it.f + it.m * level + exp(it.k * level) }.toInt()
                val warlock = UnitStats(skin = "personage_citadel_warlock", level = level, health = CappedStat(hp, hp))
                warlock += ability {
                    ticker {
                        ticksToTrigger = 5
                        body = { battle, unit ->
                            battle.notifyAoeProjectile("projectile_skull", unit, -1)
                            val damage = (battle.balance.citadel_warlock.flatDmg + (unit.stats.level * (2 * unit.stats.level - 1)) * battle.balance.citadel_warlock.m).toInt()

                            if (damage > 0) {
                                battle.aliveEnemies(unit).forEach { enemy ->
                                    val result = battle.processDamage(enemy, unit, DamageVector(0, damage, 0))
                                    if (result.status != DamageProcessStatus.DAMAGE_EVADED) {
                                        //heal up!
                                        battle.processHeal(unit, damage * battle.balance.citadel_warlock.healPercentage)
                                    }
                                }
                            }
                        }
                    }
                }
                warlock
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