package com.game7th.swipe.game.battle.tutorial

import com.game7th.battle.ability.AbilityTrigger
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.TutorialKeys
import com.game7th.swipe.game.GameScreen

class Act1L5Talk(
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

                screen.showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l5_fb_1"]!!) {
                    screen.showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l5_fb_2"]!!) {
                        screen.showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l5_fb_3"]!!) {
                            screen.showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l5_fb_4"]!!) {
                                screen.preventBottomSwipe = false
                                screen.preventTopSwipe = false
                                screen.preventLeftSwipe = false
                                screen.preventRightSwipe = false
                                game.storage.put(TutorialKeys.ACT1_L5_TALK, true.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}
