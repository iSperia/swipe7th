package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.swipe.game.GameContextWrapper

abstract class ElementController(
        protected val context: GameContextWrapper
) {

    abstract fun render(batch: SpriteBatch, delta: Float)

    open fun dispose() {}
}