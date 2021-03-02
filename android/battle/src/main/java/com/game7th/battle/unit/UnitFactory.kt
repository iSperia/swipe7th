package com.game7th.battle.unit

import com.game7th.battle.DamageVector
import com.game7th.battle.ability.TickerEntry
import com.game7th.battle.ability.ability
import com.game7th.battle.action.ApplyParalizeAction
import com.game7th.battle.action.ApplyPoisonAction
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

object UnitFactory {
    fun produce(type: UnitType, balance: SwipeBalance, level: Int, stats: PersonageAttributeStats): UnitStats? {
        return when (type) {
            UnitType.GLADIATOR -> producePersonage(balance, "personage_gladiator", "portrait_gladiator", level, stats) { stats ->
                val strikeTemplate = TileTemplate(TileNames.GLADIATOR_STRIKE, balance.gladiator.t1)
                val waveTemplate = TileTemplate(TileNames.GLADIATOR_WAVE, balance.gladiator.t2)
                val dropTemplate = TileTemplate(TileNames.GLADIATOR_DROP, 1)
                stats.addAbility {
                    defaultEmitter { skills.addAll(listOf(stats.body to strikeTemplate, stats.spirit to waveTemplate, stats.mind to dropTemplate)) }
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
                        action = RegenerateParametrizedAmountAction(
                                Math.pow(stats.mind.toDouble(), balance.gladiator.a3p.toDouble()).toFloat() * balance.gladiator.a3n)
                    }
                }
            }
            UnitType.POISON_ARCHER -> producePersonage(balance, "personage_poison_archer", "portrait_poison_archer", level, stats) { stats ->
                val strikeTemplate = TileTemplate(TileNames.POISON_ARCHER_STRIKE, balance.poison_archer.t1)
                val poisonTemplate = TileTemplate(TileNames.POISON_ARCHER_POISON, balance.poison_archer.t2)
                val paralizeTemplate = TileTemplate(TileNames.POISON_ARCHER_PARALIZE, balance.poison_archer.t3)
                stats.addAbility {
                    defaultEmitter { skills.addAll(listOf(stats.body to paralizeTemplate, stats.spirit to strikeTemplate, stats.mind to poisonTemplate)) }
                    defaultMerger { tileType = strikeTemplate.skin }
                    defaultMerger { tileType = poisonTemplate.skin; autoCut = true }
                    defaultMerger { tileType = paralizeTemplate.skin; autoCut = true }
                    consume {
                        template = strikeTemplate
                        action = AttackAction().apply {
                            attackIndex = 0
                            target = { battle, unit -> listOf(battle.aliveEnemies(unit).random()) }
                            damage = { battle, unit, target, ss, ms -> (balance.poison_archer.a1n * unit.stats.spirit * ss / ms).toInt().let { DamageVector(it, 0, 0) }}
                        }
                    }
                    distancedConsumeOnAttackDamage {
                        range = 100
                        tileSkins.add(strikeTemplate.skin)
                        sourceSkin = poisonTemplate.skin
                        action = ApplyPoisonAction(balance.poison_archer.a2l, balance.poison_archer.a2ds * stats.mind)
                    }
                    distancedConsumeOnAttackDamage {
                        range = 100
                        tileSkins.add(strikeTemplate.skin)
                        sourceSkin = paralizeTemplate.skin
                        action = ApplyParalizeAction(balance.poison_archer.a3d)
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
            UnitType.PURPLE_SLIME -> {
                val hp = balance.red_slime.hp.calculate(level)
                val slime = UnitStats(skin = "personage_red_slime", portrait = "portrait_slime_red", level = level, health = CappedStat(hp, hp))
                slime += ability {
                    ticker {
                        bodies[TickerEntry(3, 3, "attack")] = { battle, unit ->
                            val damage = balance.red_slime.damage.calculate(unit.stats.level)
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
                                val tile = SwipeTile(TileTemplate("slime_splash", balance.red_slime.a2l), battle.tileField.newTileId(), balance.red_slime.a2l, true)
                                battle.tileField.tiles[position] = tile
                                battle.notifyEvent(BattleEvent.CreateTileEvent(tile.toViewModel(), position))
                                battle.notifyAttack(unit, emptyList(), 0)
                            }
                        }
                    }
                }
                slime
            }
            UnitType.SLIME_MOTHER -> {
                val hp = balance.mother_slime.hp.calculate(level)
                val slime = UnitStats(skin = "personage_slime_mother", portrait = "portrait_slime_mother", level = level, health = CappedStat(hp, hp), regeneration = balance.mother_slime.regen * hp / 100f)
                slime += ability {
                    ticker {
                        bodies[TickerEntry(1, 3, "attack")] = { battle, unit ->
                            val damage = balance.mother_slime.damage.calculate(unit.stats.level)
                            if (damage > 0) {
                                val target = battle.findClosestAliveEnemy(unit)
                                target?.let { target ->
                                    val damageResult = battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                                    battle.notifyAttack(unit, listOf(Pair(target, damageResult)), 0)
                                }
                            }
                        }
                        bodies[TickerEntry(2, 4, "impact")] = { battle, unit ->
                            val position = battle.calculateFreeNpcPosition()
                            if (position > 0) {
                                val producedUnit = produce(UnitType.GREEN_SLIME, balance, unit.stats.level, PersonageAttributeStats(0,0,0))
                                val battleUnit = BattleUnit(battle.personageId++, position, producedUnit!!, Team.RIGHT)
                                battle.units.add(battleUnit)
                                battle.notifyEvent(BattleEvent.PersonagePositionedAbilityEvent(unit.toViewModel(), position, 0))
                                battle.notifyEvent(BattleEvent.CreatePersonageEvent(battleUnit.toViewModel(), battleUnit.position))
                            }
                        }
                    }
                }
                slime
            }
            UnitType.SLIME_FATHER -> {
                val hp = balance.father_slime.hp.calculate(level)
                val slime = UnitStats(UnitType.SLIME_FATHER.getSkin(), UnitType.SLIME_FATHER.getPortrait(), level = level, health = CappedStat(hp, hp))
                slime += ability {
                    ticker {
                        bodies[TickerEntry(2, 3, "attack")] = { battle, unit ->
                            val damage = balance.father_slime.damage.calculate(unit.stats.level)
                            if (damage > 0) {
                                val target = battle.findClosestAliveEnemy(unit)
                                target?.let { target ->
                                    val damageResult = battle.processDamage(target, unit, DamageVector(damage, 0, 0))
                                    battle.notifyAttack(unit, listOf(Pair(target, damageResult)), 0)
                                }
                            }
                        }
                        bodies[TickerEntry(1, 3, "impact")] = { battle, unit ->
                            val allies = battle.aliveAllies(unit)
                            val armor = balance.father_slime.a2.calculate(unit.stats.level)
                            battle.notifyAttack(unit, emptyList(), 0)
                            allies.forEach {
                                val newArmor = it.stats.armor.value + armor
                                it.stats.armor = CappedStat(newArmor, newArmor)
                                battle.notifyPersonageUpdated(it)
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
                regeneration = b.stats.regenerationPerSpirit * spirit,
                evasion = b.stats.evasionPerSpirit * spirit,
                wisdom = mind
        ).apply { processor(this) }
    }
}