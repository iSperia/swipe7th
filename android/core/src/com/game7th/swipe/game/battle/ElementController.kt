package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.swipe.game.GameContextWrapper

abstract class ElementController(
        protected val context: GameContextWrapper,
        protected val battle: BattleController,
        val id: Int
) {

    var zIndex = 10000

    abstract fun render(batch: SpriteBatch, delta: Float)

    open fun dispose() {}

    companion object {
        val Z_INDEX_HUD = 100000
    }
}