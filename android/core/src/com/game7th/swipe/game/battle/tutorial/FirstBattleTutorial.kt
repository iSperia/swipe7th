package com.game7th.swipe.game.battle.tutorial

import com.game7th.battle.ability.AbilityTrigger
import com.game7th.battle.dto.TileTemplate
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.tilefield.tile.TileNames
import com.game7th.battle.unit.BattleUnit
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.dialog.DismissStrategy
import com.game7th.swipe.game.GameScreen

class FirstBattleTutorial(
        private val screen: GameScreen,
        private val game: SwipeGameGdx
) : AbilityTrigger {

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.BattleStartedEvent -> {
                screen.preventBottomSwipe = true
                screen.preventTopSwipe = true
                screen.preventLeftSwipe = true
                screen.preventRightSwipe = true

                screen.showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l1_fb_1"]!!) {
                    screen.showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l1_fb_2"]!!) {
                        screen.showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l1_fb_3"]!!) {
                            screen.showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l1_fb_4"]!!) {
                                screen.showFocusView("ttr_a1l1_fb_5", screen.calcLeftPersonageRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                    screen.showFocusView("ttr_a1l1_fb_6", screen.calcRightPersonageRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                        screen.showFocusView("ttr_a1l1_fb_7", screen.calcLeftPersonageHpBarRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                            screen.showFocusView("ttr_a1l1_fb_8", screen.calcRightPersonageHpBarRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                screen.showFocusView("ttr_a1l1_fb_9", screen.calcTileFieldRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                    screen.showFocusView("ttr_a1l1_fb_10", screen.calcTileRect(7), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                        screen.showFocusView("ttr_a1l1_fb_11", screen.calcTileRect(9), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                            screen.showFocusView("ttr_a1l1_fb_12", screen.calcTileRect(17), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                screen.preventLeftSwipe = false
                                                                screen.dismissFocusOnSwipe = true
                                                                screen.showFingerAnimation(-1, 0)
                                                                screen.showFocusView("ttr_a1l1_fb_13", screen.calcTileFieldRect(), DismissStrategy.DISMISS_FORCED) {
                                                                    screen.dismissFingerAnimation()
                                                                    screen.preventLeftSwipe = true
                                                                    screen.dismissFocusOnSwipe = false
                                                                    screen.showFocusView("ttr_a1l1_fb_14", screen.calcTileRect(5), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                        screen.showFocusView("ttr_a1l1_fb_15", screen.calcComboRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                            screen.showFocusView("ttr_a1l1_fb_16", screen.calcTileRect(13), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                                screen.showFocusView("ttr_a1l1_fb_17", screen.calcRightPersonageSkillRect(), DismissStrategy.DISMISS_ON_OUTSIDE)
                                                                                screen.preventBottomSwipe = false
                                                                                screen.dismissFocusOnSwipe = true
                                                                                screen.showFingerAnimation(0, -1)
                                                                                screen.showFocusView("ttr_a1l1_fb_18", screen.calcTileFieldRect(), DismissStrategy.DISMISS_FORCED) {
                                                                                    screen.dismissFingerAnimation()
                                                                                    screen.dismissFocusOnSwipe = false
                                                                                    screen.preventBottomSwipe = true
                                                                                    screen.showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l1_fb_19"]!!) {
                                                                                        screen.showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l1_fb_20"]!!) {
                                                                                            screen.showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l1_fb_21"]!!) {
                                                                                                screen.preventBottomSwipe = false
                                                                                                screen.preventTopSwipe = false
                                                                                                screen.preventLeftSwipe = false
                                                                                                screen.preventRightSwipe = false
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is InternalBattleEvent.ScriptedInitialTiles -> {
                event.tiles = mapOf(
                        7 to SwipeTile(TileTemplate(TileNames.GLADIATOR_STRIKE, 3), -4, 1, false),
                        9 to SwipeTile(TileTemplate(TileNames.GLADIATOR_STRIKE, 3), -3, 1, false),
                        17 to SwipeTile(TileTemplate(TileNames.GLADIATOR_STRIKE, 3), -2, 1, false)
                )
            }
            is InternalBattleEvent.ScriptedTilesTick -> {
                if (event.tick == 1) {
                    event.tiles = mapOf(
                            13 to SwipeTile(TileTemplate(TileNames.GLADIATOR_STRIKE, 3), -3, 1, false)
                    )
                }
            }
        }
    }
}