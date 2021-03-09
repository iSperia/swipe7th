package com.game7th.battle.unit.personages

import com.game7th.battle.DamageVector
import com.game7th.battle.action.AttackAction
import com.game7th.battle.action.RegenerateParametrizedAmountAction
import com.game7th.battle.balance.SwipeBalance
import com.game7th.battle.event.TileTemplate
import com.game7th.battle.tilefield.tile.TileNames
import com.game7th.battle.unit.UnitStats

fun produceGladiator(balance: SwipeBalance, unitStats: UnitStats) =
        unitStats.let { stats ->
            val strikeTemplate = TileTemplate(TileNames.GLADIATOR_STRIKE, balance.gladiator.t1)
            val waveTemplate = TileTemplate(TileNames.GLADIATOR_WAVE, balance.gladiator.t2)
            val dropTemplate = TileTemplate(TileNames.GLADIATOR_DROP, 1)
            stats.addAbility {
                defaultEmitter { skills.addAll(listOf(50 to strikeTemplate, 30 to waveTemplate, 20 to dropTemplate)) }
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
            stats
        }