package com.game7th.battle.unit.personages

import com.game7th.battle.DamageVector
import com.game7th.battle.action.AttackAction
import com.game7th.battle.action.RegenerateParametrizedAmountAction
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.TileTemplate
import com.game7th.battle.tilefield.tile.TileNames
import com.game7th.battle.unit.UnitFactory
import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.unit.UnitType

fun produceGladiator(balance: SwipeBalance, level: Int, attrs: PersonageAttributeStats) =
        UnitFactory.producePersonage(balance, UnitType.GLADIATOR, level, attrs) { stats ->
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
                        target = { battle, unit -> battle.aliveEnemies(unit) }
                        damage = { _, unit, _, ss, ms -> (balance.gladiator.k2 * unit.stats.spirit * ss / ms).toInt().let { DamageVector(it, 0, 0) } }
                    }
                }
                consume {
                    template = strikeTemplate
                    action = AttackAction().apply {
                        target = { battle, unit ->
                            battle.findClosestAliveEnemy(unit)?.let { listOf(it) }
                                    ?: emptyList()
                        }
                        damage = { _, unit, _, ss, ms -> (balance.gladiator.k1 * unit.stats.body * ss / ms).toInt().let { DamageVector(it, 0, 0) } }
                    }
                }
                distancedConsumeOnAttackDamage {
                    range = 1
                    tileSkins.addAll(listOf(strikeTemplate.skin, waveTemplate.skin))
                    sourceSkin = dropTemplate.skin
                    action = RegenerateParametrizedAmountAction(stats.mind * balance.gladiator.k3 / 100)
                }
            }
        }