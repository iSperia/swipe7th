package com.game7th.battle.unit.personages

import com.game7th.battle.DamageVector
import com.game7th.battle.action.ApplyParalizeAction
import com.game7th.battle.action.ApplyPoisonAction
import com.game7th.battle.action.AttackAction
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.TileTemplate
import com.game7th.battle.tilefield.tile.TileNames
import com.game7th.battle.unit.UnitFactory
import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.unit.UnitType

fun produceToxicArcher(balance: SwipeBalance, level: Int, attrs: PersonageAttributeStats) =
        UnitFactory.producePersonage(balance, UnitType.POISON_ARCHER, level, attrs) { stats ->
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
                        target = { battle, unit -> battle.aliveEnemies(unit).let { if (it.isEmpty()) emptyList() else listOf(it.random()) } }
                        damage = { _, unit, _, ss, ms -> (balance.poison_archer.k1 * unit.stats.spirit * ss / ms).toInt().let { DamageVector(it, 0, 0) } }
                    }
                }
                distancedConsumeOnAttackDamage {
                    range = 100
                    tileSkins.add(strikeTemplate.skin)
                    sourceSkin = poisonTemplate.skin
                    action = ApplyPoisonAction(balance.poison_archer.d2, balance.poison_archer.k2 * stats.mind)
                }
                distancedConsumeOnAttackDamage {
                    range = 100
                    tileSkins.add(strikeTemplate.skin)
                    sourceSkin = paralizeTemplate.skin
                    action = ApplyParalizeAction((balance.poison_archer.k3 + stats.body / balance.poison_archer.d3).toInt())
                }
            }
        }