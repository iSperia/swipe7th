package com.game7th.swipe.game.battle.tutorial

import com.game7th.battle.ability.AbilityTrigger
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.TutorialKeys
import com.game7th.swipe.game.GameScreen

class Act1L4Talk(
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

                screen.showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l4_fb_1"]!!) {
                    screen.showDialog("vp_strange_figure", "Strange figure", game.context.texts["ttr_a1l4_fb_2"]!!) {
                        screen.showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l4_fb_3"]!!) {
                            screen.showDialog("vp_strange_figure", "Strange figure", game.context.texts["ttr_a1l4_fb_4"]!!) {
                                screen.showDialog("vp_bhastuse", "Bhastuse", game.context.texts["ttr_a1l4_fb_5"]!!) {
                                    screen.showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l4_fb_6"]!!) {
                                        screen.showDialog("vp_strange_figure", "Strange figure", game.context.texts["ttr_a1l4_fb_7"]!!) {
                                            screen.preventBottomSwipe = false
                                            screen.preventTopSwipe = false
                                            screen.preventLeftSwipe = false
                                            screen.preventRightSwipe = false
                                            game.storage.put(TutorialKeys.ACT1_L4_TALK, true.toString())
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