package com.game7th.battle.unit.personages

import com.game7th.battle.DamageVector
import com.game7th.battle.action.AttackAction
import com.game7th.battle.dto.BattleEvent
import com.game7th.battle.dto.SwipeBalance
import com.game7th.battle.dto.TileTemplate
import com.game7th.battle.tilefield.tile.TileNames
import com.game7th.battle.unit.UnitStats
import kotlin.math.max
import kotlin.math.min

fun produceFreezeMage(balance: SwipeBalance, unitId: Int, unitStats: UnitStats) =
        unitStats.let { stats ->
            val boltWeight = 35
            val vortexWeight = 35
            val armorWeight = if (unitStats.level < 6) 0 else 30
            val boltTemplate = TileTemplate(TileNames.FREEZE_MAGE_BOLT, balance.freeze_mage.t1)
            val vortexTemplate = TileTemplate(TileNames.FREEZE_MAGE_VORTEX, balance.freeze_mage.t2)
            val armorTemplate = TileTemplate(TileNames.FREEZE_MAGE_ARMOR, 0)
            stats.addAbility {
                defaultEmitter { skills.addAll(listOf(boltWeight to boltTemplate, vortexWeight to vortexTemplate, armorWeight to armorTemplate)) }
                defaultMerger { tileType = boltTemplate.skin }
                defaultMerger { tileType = vortexTemplate.skin }
                defaultMerger { tileType = armorTemplate.skin }
                consume {
                    template = boltTemplate
                    action = AttackAction().apply {
                        attackIndex = 0
                        target = { battle, unit -> battle.aliveEnemies(unit).let { if (it.isEmpty()) emptyList() else listOf(it.random()) } }
                        damage = { _, unit, target, ss, ms ->
                            if (target.stats.isFrozen()) {
                                (balance.freeze_mage.k1 * unit.stats.mind * ss / ms).toInt().let { DamageVector(0, it, 0) }
                            } else {
                                DamageVector(0, 0, 0)
                            }
                        }
                        action = { battle, unit, target, ss, ms, combo ->
                            battle.applyFrozen(target, balance.freeze_mage.d1)
                        }
                    }
                    consume {
                        template = vortexTemplate
                        action = AttackAction().apply {
                            attackIndex = 1
                            target = { battle, unit -> battle.aliveEnemies(unit) }
                            damage = { _, unit, target, ss, ms ->
                                ((if (target.stats.isFrozen()) (1f + balance.freeze_mage.w2 / 100f * (unit.stats.spirit)) else 1f) *
                                        balance.freeze_mage.k2 * (unit.stats.mind + unit.stats.spirit) * ss / ms).toInt().let { DamageVector(0, it, 0) }
                            }
                        }
                    }
                    preprocessIncomingDamage {
                        targetId = unitId
                        processor = { battle, unit, damage ->
                            val magicPerStack = (unit.stats.spirit * balance.freeze_mage.k3).toInt()
                            val physicPerStack = (unit.stats.body * balance.freeze_mage.k3).toInt()
                            val stackNeeded = max(damage.magical / magicPerStack, damage.physical / physicPerStack)
                            val stacks = battle.tileField.tiles.entries.filter { it.value.type.skin == TileNames.FREEZE_MAGE_ARMOR }.sortedBy { it.value.stackSize }
                            var targetStack = stacks.firstOrNull { it.value.stackSize >= stackNeeded } ?: stacks.lastOrNull()
                            targetStack?.let { tile ->
                                val physicalReduction = min(damage.physical, tile.value.stackSize * physicPerStack)
                                val magicReduction = min(damage.magical, tile.value.stackSize * magicPerStack)
                                battle.notifyEvent(BattleEvent.PersonageHealEvent(unit.toViewModel(), physicalReduction + magicReduction))
                                battle.notifyEvent(BattleEvent.ShowAilmentEffect(unit.id, "ailment_defense"))
                                battle.tileField.tiles.remove(tile.key)
                                battle.notifyTileRemoved(targetStack.value.id)
                                DamageVector(physicalReduction, magicReduction, 0)

                            } ?: DamageVector(0, 0, 0)
                        }
                    }
                }
            }
            stats
        }