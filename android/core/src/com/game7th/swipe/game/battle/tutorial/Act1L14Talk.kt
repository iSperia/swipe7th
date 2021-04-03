package com.game7th.swipe.game.battle.tutorial

import com.game7th.battle.ability.AbilityTrigger
import com.game7th.battle.internal_event.InternalBattleEvent
import com.game7th.battle.unit.BattleUnit
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.TutorialKeys
import com.game7th.swipe.game.GameScreen

class Act1L14Talk(
        private val screen: GameScreen,
        private val game: SwipeGameGdx
) : AbilityTrigger {

    var shown = false

    override suspend fun process(event: InternalBattleEvent, unit: BattleUnit) {
        when (event) {
            is InternalBattleEvent.BattleStartedEvent -> {

                if (!shown) {
                    shown = true

                    screen.preventBottomSwipe = true
                    screen.preventTopSwipe = true
                    screen.preventLeftSwipe = true
                    screen.preventRightSwipe = true

                    screen.showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l14_fb_1"]!!) {
                        screen.showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l14_fb_2"]!!) {
                            screen.showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l14_fb_3"]!!) {
                                    screen.preventBottomSwipe = false
                                    screen.preventTopSwipe = false
                                    screen.preventLeftSwipe = false
                                    screen.preventRightSwipe = false
                                    game.storage.put(TutorialKeys.ACT1_L11_TALK, true.toString())
                                }
                        }
                    }
                }
            }
        }
    }
}
