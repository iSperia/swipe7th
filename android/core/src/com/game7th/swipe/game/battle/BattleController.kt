package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.EffectGdxModel
import com.game7th.swipe.game.battle.model.FigureGdxModel

/**
 * GDX graph controller for battle
 */
class BattleController(
        private val context: GameContextWrapper
) {

    private val effects = mutableListOf<SteppedGeneratorEffectController>()

    private val personages = mutableListOf<FigureController>()

    init {
        (2 downTo 0).forEach {
            personages.add(FigureController(context,
                    FigureGdxModel("personage_gladiator", "personage_gladiator", emptyList()),
                    80f + it * 48f,
                    it * 32f))

        }
    }

    fun testEffect() {
        val controller = SteppedGeneratorEffectController(
                0f,
                0f,
                480f,
                context,
                EffectGdxModel("gladiator_wave", "personage_gladiator", 15, 0.03f, 64, 128)
        )
        effects.add(controller)
    }

    fun render(batch: SpriteBatch, delta: Float) {

        personages.forEach {
            it.render(batch, delta)
        }
        effects.forEach {
            it.render(batch, delta)
        }

    }
}